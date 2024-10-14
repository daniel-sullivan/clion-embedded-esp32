package esp32.embedded.clion.openocd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionGDBDriverConfiguration;
import com.jetbrains.cidr.cpp.toolchains.CPPDebugger;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.execution.CidrLauncher;
import com.jetbrains.cidr.execution.debugger.CidrDebugProcess;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerCommandException;
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters;
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteGDBDebugProcess;
import esp32.embedded.clion.openocd.OpenOcdConfiguration.DownloadType;
import java.io.File;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

/**
 * (c) elmot on 19.10.2017.
 */

class OpenOcdLauncher extends CidrLauncher {

    private static final Key<AnAction> RESTART_KEY = Key.create(OpenOcdLauncher.class.getName() + "#restartAction");
    private final OpenOcdConfiguration openOcdConfiguration;

    OpenOcdLauncher(OpenOcdConfiguration openOcdConfiguration) {
        this.openOcdConfiguration = openOcdConfiguration;
    }

    @Override
    protected ProcessHandler createProcess(@NotNull CommandLineState commandLineState) throws ExecutionException {
        File runFile = findRunFile(commandLineState);
        findOpenOcdAction(commandLineState.getEnvironment().getProject()).stopOpenOcd();
        try {
            GeneralCommandLine commandLine = OpenOcdComponent
                    .createOcdCommandLine(openOcdConfiguration,
                            runFile, "reset", true);
            OSProcessHandler osProcessHandler = new OSProcessHandler(commandLine);
            osProcessHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    super.processTerminated(event);
                    Project project = commandLineState.getEnvironment().getProject();
                    if (event.getExitCode() == 0) {
                        Informational.showSuccessfulDownloadNotification(project);
                    } else {
                        Informational.showFailedDownloadNotification(project);
                    }
                }
            });
            return osProcessHandler;
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }
    }

    @NotNull
    @Override
    protected CidrDebugProcess createDebugProcess(@NotNull CommandLineState commandLineState,
                                                  @NotNull XDebugSession xDebugSession) throws ExecutionException {
        Project project = commandLineState.getEnvironment().getProject();
        OpenOcdSettingsState ocdSettings = project.getService(OpenOcdSettingsState.class);
        CidrRemoteDebugParameters remoteDebugParameters = new CidrRemoteDebugParameters();

        remoteDebugParameters.setSymbolFile(findRunFile(commandLineState).getAbsolutePath());
        remoteDebugParameters.setRemoteCommand("tcp:localhost:" + openOcdConfiguration.getGdbPort());

        CPPToolchains.Toolchain toolchain = openOcdConfiguration.getDebuggerData().getOrCreateDebuggerToolchain();
        if (ocdSettings.shippedGdb) {
            toolchain = toolchain.copy();
            File gdbFile = CidrDebuggerPathManager.getBundledGDBBinary();
            String gdbPath = gdbFile.getAbsolutePath();
            CPPDebugger cppDebugger = CPPDebugger.create(CPPDebugger.Kind.CUSTOM_GDB, gdbPath);
            toolchain.setDebugger(cppDebugger);
        }
        CLionGDBDriverConfiguration gdbDriverConfiguration = new CLionGDBDriverConfiguration(getProject(), toolchain);

        xDebugSession.stop();

        AtomicReference<CidrRemoteGDBDebugProcess> debugProcessRef = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                debugProcessRef.set(new CidrRemoteGDBDebugProcess(gdbDriverConfiguration,
                                remoteDebugParameters,
                                xDebugSession,
                                commandLineState.getConsoleBuilder(),
                                project1 -> new Filter[0]));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        CidrRemoteGDBDebugProcess debugProcess = debugProcessRef.get();

        debugProcess.getProcessHandler().addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                super.processWillTerminate(event, willBeDestroyed);
                findOpenOcdAction(project).stopOpenOcd();
            }
        });

        debugProcess.getProcessHandler().putUserData(RESTART_KEY,
                new AnAction("Reset", "MCU reset",
                        IconLoader.findIcon("esp32/embedded/clion/openocd/reset.png", OpenOcdLauncher.class)) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        XDebugSession session = debugProcess.getSession();
                        session.pause();
                        debugProcess.postCommand(drv -> {
                            try {
                                ProgressManager.getInstance().runProcess(() -> {
                                    while (drv.getState() != DebuggerDriver.TargetState.SUSPENDED) {
                                        Thread.yield();
                                    }
                                }, null);
                                drv.executeInterpreterCommand("monitor reset init");
                                session.resume();
                            } catch (DebuggerCommandException exception) {
                                Informational.showFailedDownloadNotification(e.getProject());
                            }
                        });
                    }
                }
        );

        new Thread(() -> {
            while (!debugProcess.getCurrentStateMessage().equals("Connected")) {
                try {
                    Thread.onSpinWait();
                } catch (Throwable ignored) {
                }
            }

            // Connected. Perform initialisation
            XDebugSession session = debugProcess.getSession();

            //  Check if we need any init
            if (openOcdConfiguration.getResetType().needsInit()) {
                session.pause();
                debugProcess.postCommand(drv -> {
                    try {
                        ProgressManager.getInstance().runProcess(() -> {
                            while (drv.getState() != DebuggerDriver.TargetState.SUSPENDED) {
                                Thread.yield();
                            }
                        }, null);

                        // Determine which commands need to be run
                        if (openOcdConfiguration.getFlushRegs()) {
                            drv.executeInterpreterCommand("flushregs");
                        }

                        if (openOcdConfiguration.getInitialBreak() && !openOcdConfiguration.getInitialBreakName().isEmpty()) {
                            drv.executeInterpreterCommand("thb " + openOcdConfiguration.getInitialBreakName());
                        }

                        session.resume();

                    } catch (DebuggerCommandException ignored) {
                    }
                });
            }

        }).start();

        return debugProcess;
    }

    @NotNull
    private File findRunFile(CommandLineState commandLineState) throws ExecutionException {
        String targetProfileName = commandLineState.getExecutionTarget().getDisplayName();
        CMakeAppRunConfiguration.BuildAndRunConfigurations runConfigurations = openOcdConfiguration
                .getBuildAndRunConfigurations(targetProfileName);
        if (runConfigurations == null) {
            throw new ExecutionException("Target is not defined");
        }
        File runFile = runConfigurations.getRunFile(getProject());
        if (runFile == null) {
            throw new ExecutionException("Run file is not defined for " + runConfigurations);
        }
        if (!runFile.exists() || !runFile.isFile()) {
            throw new ExecutionException("Invalid run file " + runFile.getAbsolutePath());
        }
        return runFile;
    }


    @NotNull
    @Override
    public XDebugProcess startDebugProcess(@NotNull CommandLineState commandLineState,
                                           @NotNull XDebugSession xDebugSession) throws ExecutionException {

        File runFile = null;
        if (openOcdConfiguration.getDownloadType() != DownloadType.NONE) {
            runFile = findRunFile(commandLineState);
            if (openOcdConfiguration.getDownloadType() == DownloadType.UPDATED_ONLY &&
                OpenOcdComponent.isLatestUploaded(runFile)) {
                runFile = null;
            }
        }

        try {
            xDebugSession.stop();
            OpenOcdComponent openOcdComponent = findOpenOcdAction(commandLineState.getEnvironment().getProject());
            openOcdComponent.stopOpenOcd();
            Future<OpenOcdComponent.Status> downloadResult = openOcdComponent.startOpenOcd(openOcdConfiguration,
                    runFile);

            ProgressManager progressManager = ProgressManager.getInstance();
            ThrowableComputable<OpenOcdComponent.Status, ExecutionException> process = () -> {
                try {
                    progressManager.getProgressIndicator().setIndeterminate(true);
                    while (true) {
                        try {
                            return downloadResult.get(500, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ignored) {
                            ProgressManager.checkCanceled();
                        }
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException(e);
                }
            };
            String progressTitle = runFile == null ? "Start OpenOCD" : "Firmware Download";
            OpenOcdComponent.Status downloadStatus = progressManager.runProcessWithProgressSynchronously(
                    process, progressTitle, true, getProject());
            if (downloadStatus == OpenOcdComponent.Status.FLASH_ERROR) {
                downloadResult.cancel(true);
                throw new ExecutionException("OpenOCD cancelled");
            }
            return super.startDebugProcess(commandLineState, xDebugSession);
        } catch (ConfigurationException e) {
            Informational.showPluginError(getProject(), e);
            throw new ExecutionException(e);
        }
    }

    @Override
    protected void collectAdditionalActions(@NotNull CommandLineState state, @NotNull ProcessHandler processHandler,
                                            @NotNull ExecutionConsole console,
                                            @NotNull List<? super AnAction> actions) throws ExecutionException {
        super.collectAdditionalActions(state, processHandler, console, actions);
        AnAction restart = processHandler.getUserData(RESTART_KEY);
        if (restart != null) {
            actions.add(restart);
        }
    }

    private OpenOcdComponent findOpenOcdAction(Project project) {
        return project.getService(OpenOcdComponent.class);
    }

    @NotNull
    @Override
    public Project getProject() {
        return openOcdConfiguration.getProject();
    }

}
