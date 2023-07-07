package esp32.embedded.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecutionErrorDialog;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenOcdComponent {

    public static final String SCRIPTS_PATH_SHORT = "scripts";
    public static final String SCRIPTS_PATH_MEDIUM = "openocd/" + SCRIPTS_PATH_SHORT;
    public static final String SCRIPTS_PATH_LONG = "share/openocd/" + SCRIPTS_PATH_SHORT;
    public static final String BIN_OPENOCD;
    private static final Key<Long> UPLOAD_LOAD_COUNT_KEY = new Key<>(OpenOcdConfiguration.class.getName() +
            "#LAST_DOWNLOAD_MOD_COUNT");
    private static final String ERROR_PREFIX = "Error: ";
    private static final String[] IGNORED_STRINGS = {
            "clearing lockup after double fault",
            "LIB_USB_NOT_SUPPORTED"};

    private final static String[] FAIL_STRINGS = {
            "** Programming Failed **", "communication failure", "** OpenOCD init failed **"};
    private static final String FLASH_SUCCESS_TEXT = "** Program Flash Complete! **";
    private static final Logger LOG = Logger.getInstance(OpenOcdComponent.class);
    private static final String ADAPTER_SPEED = "adapter speed";

    static {
        BIN_OPENOCD = "bin/openocd" + (OS.isWindows() ? ".exe" : "");
    }

    private final EditorColorsScheme myColorsScheme;
    private OSProcessHandler process;

    public OpenOcdComponent() {
        myColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    }

    @NotNull
    public static GeneralCommandLine createOcdCommandLine(OpenOcdConfiguration config, File fileToLoad,
                                                          @Nullable String additionalCommand, boolean shutdown) throws ConfigurationException {
        Project project = config.getProject();
        OpenOcdSettingsState ocdSettings = project.getComponent(OpenOcdSettingsState.class);
        if (StringUtil.isEmpty(config.getBoardConfigFile())) {
            throw new ConfigurationException("Board Config file is not defined.", "OpenOCD Run Error");
        }
        VirtualFile ocdHome = require(LocalFileSystem.getInstance().findFileByPath(ocdSettings.openOcdHome));
        VirtualFile ocdBinary = require(ocdHome.findFileByRelativePath(BIN_OPENOCD));
        File ocdBinaryIo = VfsUtil.virtualToIoFile(ocdBinary);
        GeneralCommandLine commandLine = new PtyCommandLine()
                .withWorkDirectory(ocdBinaryIo.getParentFile())
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withParameters("-c", "tcl_port disabled")
                .withExePath(ocdBinaryIo.getAbsolutePath());

        VirtualFile ocdScripts = require(OpenOcdSettingsState.findOcdScripts(ocdHome));
        commandLine.addParameters("-s", VfsUtil.virtualToIoFile(ocdScripts).getAbsolutePath());
        if (config.getGdbPort() != OpenOcdConfiguration.DEF_GDB_PORT) {
            commandLine.addParameters("-c", "gdb_port " + config.getGdbPort());
        }
        if (config.getTelnetPort() != OpenOcdConfiguration.DEF_TELNET_PORT) {
            commandLine.addParameters("-c", "telnet_port " + config.getTelnetPort());
        }

        if (!StringUtil.isEmpty(config.getInterfaceConfigFile())) {
            commandLine.addParameters("-f", config.getInterfaceConfigFile());
        }

        commandLine.addParameters("-f", config.getBoardConfigFile());

        if (!StringUtil.isEmpty(config.getBootBinPath())) {
            commandLine.addParameters("-c",
                    config.getProgramType().toString() + " " + config.getBootBinPath() + " "
                            + config.getBootOffset() + (config.getAppendVerify() ? " verify" : ""));
        }
        if (!StringUtil.isEmpty(config.getPartitionBinPath())) {
            commandLine.addParameters("-c",
                    config.getProgramType().toString() + " " + config.getPartitionBinPath() + " "
                            + config.getPartitionOffset() + (config.getAppendVerify() ? " verify" : ""));
        }

        if (fileToLoad != null) { // Program Command
            String command =
                    config.getProgramType().toString() + " " + fileToLoad.getAbsolutePath()
                            .replace(File.separatorChar, '/')
                            .replace(".elf", ".bin");
            if (config.getOffset() != null && !config.getOffset().isEmpty())
                command = command + " " + config.getOffset();

            if (config.getAppendVerify()) {
                command += " verify";
            }

            commandLine.addParameters("-c", command);
        }

        if (config.getHAR()) {
            commandLine.addParameters("-c", "init; reset halt");
        } else {
            commandLine.addParameters("-c", "init; reset");
        }

        commandLine.addParameters("-c", "echo \"" + FLASH_SUCCESS_TEXT + "\"");

        if (shutdown) {
            commandLine.addParameters("-c", "shutdown");
        }
        return commandLine;
    }

    @NotNull
    public static VirtualFile require(VirtualFile fileToCheck) throws ConfigurationException {
        if (fileToCheck == null) {
            openOcdNotFound();
        }
        return fileToCheck;
    }

    private static void openOcdNotFound() throws ConfigurationException {
        throw new ConfigurationException("Please open settings dialog and fix OpenOCD home", "OpenOCD Config Error");
    }

    public void stopOpenOcd() {
        if (process == null || process.isProcessTerminated() || process.isProcessTerminating())
            return;
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
            process.destroyProcess();
            process.waitFor(1000);
        });
    }

    public Future<Status> startOpenOcd(OpenOcdConfiguration config, @Nullable File fileToLoad) throws ConfigurationException {
        CompletableFuture<Status> ret = new CompletableFuture<>();
        if (config == null) {
            ret.obtrudeValue(Status.FLASH_ERROR);
            return ret;
        }
        GeneralCommandLine commandLine = createOcdCommandLine(config, fileToLoad, null, false);
        if (process != null && !process.isProcessTerminated()) {
            LOG.info("OpenOCD is already run");
            ret.obtrudeValue(Status.FLASH_ERROR);
            return ret;
        }
        VirtualFile virtualFile = fileToLoad != null ? VfsUtil.findFileByIoFile(fileToLoad, true) : null;
        Project project = config.getProject();
        try {
            process = new OSProcessHandler(commandLine) {
                @Override
                public boolean isSilentlyDestroyOnClose() {
                    return true;
                }
            };
            DownloadFollower downloadFollower = new DownloadFollower(virtualFile);
            process.addProcessListener(downloadFollower);
            RunContentExecutor openOCDConsole = new RunContentExecutor(project, process)
                    .withTitle("OpenOCD Console")
                    .withActivateToolWindow(true)
                    .withFilter(new ErrorFilter(project))
                    .withStop(process::destroyProcess,
                            () -> !process.isProcessTerminated() && !process.isProcessTerminating());

            openOCDConsole.run();
            ret.obtrudeValue(null); // Unneeded. Complete anyways.
            return downloadFollower;
        } catch (ExecutionException e) {
            ExecutionErrorDialog.show(e, "OpenOCD Start Failed", project);
            ret.obtrudeValue(Status.FLASH_ERROR);
            return ret;
        }
    }

    public enum Status {
        FLASH_SUCCESS,
        FLASH_WARNING,
        FLASH_ERROR,
    }

    public enum FlashedStatus {
        INITIALIZED, APP_OK;

        private FlashedStatus nextState;

        static {
            INITIALIZED.nextState = APP_OK;
            APP_OK.nextState = APP_OK;
        }

        public FlashedStatus getNextState() {
            return nextState;
        }
    }

    private class ErrorFilter implements Filter {
        private final Project project;

        ErrorFilter(Project project) {
            this.project = project;
        }

        /**
         * Filters line by creating an instance of {@link Result}.
         *
         * @param line         The line to be filtered. Note that the line must contain a line
         *                     separator at the end.
         * @param entireLength The length of the entire text including the line passed for filtration.
         * @return <tt>null</tt>, if there was no match, otherwise, an instance of {@link Result}
         */
        @Nullable
        @Override
        public Result applyFilter(@NotNull String line, int entireLength) {
            if (containsOneOf(line, FAIL_STRINGS)) {
                Informational.showFailedDownloadNotification(project);
                return new Result(0, line.length(), null,
                        myColorsScheme.getAttributes(ConsoleViewContentType.ERROR_OUTPUT_KEY)) {
                    @Override
                    public int getHighlighterLayer() {
                        return HighlighterLayer.ERROR;
                    }
                };
            } else if (line.equals(FLASH_SUCCESS_TEXT)) {
                Informational.showSuccessfulDownloadNotification(project);
            }
            return null;
        }
    }

    private class DownloadFollower extends CompletableFuture<Status> implements ProcessListener {
        @Nullable
        private final VirtualFile vRunFile;

        private FlashedStatus flashedStatusListen = FlashedStatus.INITIALIZED;

        DownloadFollower(@Nullable VirtualFile vRunFile) {
            this.vRunFile = vRunFile;
        }

        @Override
        public void startNotified(@NotNull ProcessEvent event) {
            //nothing to do
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            try {
                if (!isDone()) {
                    obtrudeValue(Status.FLASH_ERROR);
                }
            } catch (Exception e) {
                obtrudeValue(Status.FLASH_ERROR);
            }
        }

        @Override
        public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
            //nothing to do
        }

        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            String text = event.getText().trim();
            if (containsOneOf(text, FAIL_STRINGS)) {
                obtrudeValue(Status.FLASH_ERROR);
            } else if (vRunFile == null && text.startsWith(ADAPTER_SPEED)) {
                obtrudeValue(Status.FLASH_SUCCESS);
            } else if (text.contains(FLASH_SUCCESS_TEXT)) {
                if (flashedStatusListen == FlashedStatus.APP_OK) {
                    if (vRunFile != null) {
                        UPLOAD_LOAD_COUNT_KEY.set(vRunFile, vRunFile.getModificationCount());
                    }
                    obtrudeValue(Status.FLASH_SUCCESS);
                }
                flashedStatusListen = flashedStatusListen.getNextState();
            } else if (text.startsWith(ERROR_PREFIX) && !containsOneOf(text, IGNORED_STRINGS)) {
                obtrudeValue(Status.FLASH_WARNING);
            }
        }
    }

    private boolean containsOneOf(String text, String[] sampleStrings) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String sampleString : sampleStrings) {
            if (text.contains(sampleString)) return true;
        }
        return false;
    }

    public static boolean isLatestUploaded(File runFile) {
        VirtualFile vRunFile = VfsUtil.findFileByIoFile(runFile, true);
        Long latestDownloadModCount = UPLOAD_LOAD_COUNT_KEY.get(vRunFile);
        return vRunFile != null && Objects.equals(latestDownloadModCount, vRunFile.getModificationCount());
    }

}
