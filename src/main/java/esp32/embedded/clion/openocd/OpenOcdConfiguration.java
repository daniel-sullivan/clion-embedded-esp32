package esp32.embedded.clion.openocd;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * (c) elmot on 29.9.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OpenOcdConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {
    public static final int DEF_GDB_PORT = 3333;
    public static final int DEF_TELNET_PORT = 4444;
    public static final String DEF_PROGRAM_OFFSET = "0x10000";
    public static final String DEF_BOOT_OFFSET = "0x0";
    public static final String DEF_BOOT_BIN_PATH = "build/bootloader/bootloader.bin";

    public static final String DEF_PART_OFFSET = "0x8000";
    public static final String DEF_PART_BIN_PATH = "build/partition_table/partition-table.bin";
    public static final boolean DEF_HAR = true;
    public static final boolean DEF_FLUSH_REGS = true;
    public static final boolean DEF_BREAK_FUNCTION = true;
    public static final String DEF_BREAK_FUNCTION_NAME = "app_main";

    public static final ProgramType DEF_PROGRAM_TYPE = ProgramType.PROGRAM_ESP32;

    private static final String ATTR_GDB_PORT = "gdb_port";
    private static final String ATTR_TELNET_PORT = "telnet_port";
    private static final String ATTR_BOARD_CONFIG = "board_config";
    private static final String ATTR_INTERFACE_CONFIG = "interface_config";
    private static final String ATTR_BOOT_PATH_CONFIG = "boot_path_cfg";
    private static final String ATTR_BOOT_OFFSET_CONFIG = "boot_offset_cfg";

    private static final String ATTR_PART_PATH_CONFIG = "part_path_cfg";
    private static final String ATTR_PART_OFFSET_CONFIG = "part_offset_cfg";
    public static final String ATTR_DOWNLOAD_TYPE = "download_type";
    public static final String ATTR_HAR = "halt_on_reset";
    public static final String ATTR_FLUSH_REGS = "flush_regs";
    public static final String ATTR_BREAK_FUNCTION = "break";
    public static final String ATTR_BREAK_FUNCTION_NAME = "break_function";

    public static final String ATTR_PROGRAM_TYPE_CONFIG = "prog_type_cfg";


    private int gdbPort = DEF_GDB_PORT;
    private int telnetPort = DEF_TELNET_PORT;
    private String boardConfigFile;
    private String interfaceConfigFile;
    private DownloadType downloadType = DownloadType.ALWAYS;
    private String offset = DEF_PROGRAM_OFFSET;
    private boolean haltOnReset = DEF_HAR;
    private boolean flushRegs = DEF_FLUSH_REGS;
    private boolean initialBreak = DEF_BREAK_FUNCTION;
    private String initialBreakName = DEF_BREAK_FUNCTION_NAME;

    private String bootloaderOffset = DEF_BOOT_OFFSET;
    private String bootloaderBinPath = DEF_BOOT_BIN_PATH;
    private String partitionOffset = DEF_PART_OFFSET;
    private String partitionBinPath = DEF_PART_BIN_PATH;

    private ProgramType programType = ProgramType.PROGRAM_ESP32;

    public enum DownloadType {

        ALWAYS,
        UPDATED_ONLY,
        NONE;

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }
    }

    public enum ProgramType {
        PROGRAM_ESP,
        PROGRAM_ESP32;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public static String toBeautyString(String obj) {
        return StringUtil.toTitleCase(obj.toLowerCase().replace("_", " "));
    }

    public enum ResetType {
        RUN("init;reset run;"),
        INIT("init;reset init;"),
        HALT("init;reset halt"),
        NONE("");

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }

        ResetType(String command) {
            this.command = command;
        }

        private final String command;

        public final String getCommand() {
            return command;
        }

    }

    @SuppressWarnings("WeakerAccess")
    public OpenOcdConfiguration(Project project, ConfigurationFactory configurationFactory, String targetName) {
        super(project, configurationFactory, targetName);
    }

    @Nullable
    @Override
    public CidrCommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        return new CidrCommandLineState(environment, new OpenOcdLauncher(this));
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        boardConfigFile = element.getAttributeValue(ATTR_BOARD_CONFIG);
        interfaceConfigFile = element.getAttributeValue(ATTR_INTERFACE_CONFIG);
        gdbPort = readIntAttr(element, ATTR_GDB_PORT, DEF_GDB_PORT);
        telnetPort = readIntAttr(element, ATTR_TELNET_PORT, DEF_TELNET_PORT);
        downloadType = readEnumAttr(element, ATTR_DOWNLOAD_TYPE, DownloadType.ALWAYS);
        haltOnReset = readBoolAttr(element, ATTR_HAR, DEF_HAR);
        flushRegs = readBoolAttr(element, ATTR_FLUSH_REGS, DEF_FLUSH_REGS);
        initialBreak = readBoolAttr(element, ATTR_BREAK_FUNCTION, DEF_BREAK_FUNCTION);
        initialBreakName = element.getAttributeValue(ATTR_BREAK_FUNCTION_NAME, null, DEF_BREAK_FUNCTION_NAME);

        bootloaderBinPath = element.getAttributeValue(ATTR_BOOT_PATH_CONFIG, null, DEF_BOOT_BIN_PATH);
        bootloaderOffset = element.getAttributeValue(ATTR_BOOT_OFFSET_CONFIG, null, DEF_BOOT_OFFSET);

        partitionBinPath = element.getAttributeValue(ATTR_PART_PATH_CONFIG, null, DEF_PART_BIN_PATH);
        partitionOffset = element.getAttributeValue(ATTR_PART_OFFSET_CONFIG, null, DEF_PART_OFFSET);

        String programTypeStr = element.getAttributeValue(ATTR_PROGRAM_TYPE_CONFIG);
        programType = programTypeStr != null ? ProgramType.valueOf(programTypeStr) : DEF_PROGRAM_TYPE;
    }

    private int readIntAttr(@NotNull Element element, String name, int def) {
        String s = element.getAttributeValue(name);
        if (StringUtil.isEmpty(s)) return def;
        return Integer.parseUnsignedInt(s);
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<?>> T readEnumAttr(@NotNull Element element, String name, T def) {
        String s = element.getAttributeValue(name);
        if (StringUtil.isEmpty(s)) return def;
        try {
            return (T) Enum.valueOf(def.getDeclaringClass(), s);
        } catch (Throwable t) {
            return def;
        }
    }

    private boolean readBoolAttr(@NotNull Element element, String name, boolean def) {
        String s = element.getAttributeValue(name);
        if (StringUtil.isEmpty(s)) return def;
        try {
            return Boolean.parseBoolean(s);
        } catch (Throwable t) {
            return def;
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute(ATTR_GDB_PORT, "" + gdbPort);
        element.setAttribute(ATTR_TELNET_PORT, "" + telnetPort);
        if (boardConfigFile != null) {
            element.setAttribute(ATTR_BOARD_CONFIG, boardConfigFile);
        }
        if (interfaceConfigFile != null) {
            element.setAttribute(ATTR_INTERFACE_CONFIG, interfaceConfigFile);
        }
        element.setAttribute(ATTR_DOWNLOAD_TYPE, downloadType.name());
        element.setAttribute(ATTR_HAR, String.valueOf(haltOnReset));
        element.setAttribute(ATTR_FLUSH_REGS, String.valueOf(flushRegs));
        element.setAttribute(ATTR_BREAK_FUNCTION, String.valueOf(initialBreak));
        element.setAttribute(ATTR_BREAK_FUNCTION_NAME, initialBreakName);

        if (bootloaderBinPath != null) {
            element.setAttribute(ATTR_BOOT_PATH_CONFIG, bootloaderBinPath);
        }
        element.setAttribute(ATTR_BOOT_OFFSET_CONFIG, Objects.requireNonNullElse(bootloaderOffset, DEF_BOOT_OFFSET));

        if (partitionBinPath != null) {
            element.setAttribute(ATTR_PART_PATH_CONFIG, partitionBinPath);
        }
        element.setAttribute(ATTR_PART_OFFSET_CONFIG, Objects.requireNonNullElse(partitionOffset, DEF_PART_OFFSET));

        element.setAttribute(ATTR_PROGRAM_TYPE_CONFIG, programType.name());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();
        checkPort(gdbPort);
        checkPort(telnetPort);
        if (gdbPort == telnetPort) {
            throw new RuntimeConfigurationException("Port values should be different");
        }
        if (StringUtil.isEmpty(boardConfigFile)) {
            throw new RuntimeConfigurationException("Board config file is not defined");
        }
    }

    private void checkPort(int port) throws RuntimeConfigurationException {
        if (port <= 1024 || port > 65535)
            throw new RuntimeConfigurationException("Port value must be in the range [1024...65535]");
    }

    public int getGdbPort() {
        return gdbPort;
    }

    public void setGdbPort(int gdbPort) {
        this.gdbPort = gdbPort;
    }

    public int getTelnetPort() {
        return telnetPort;
    }

    public void setTelnetPort(int telnetPort) {
        this.telnetPort = telnetPort;
    }

    public String getBoardConfigFile() {
        return boardConfigFile;
    }

    public String getInterfaceConfigFile() {
        return interfaceConfigFile;
    }

    public void setBoardConfigFile(String boardConfigFile) {
        this.boardConfigFile = boardConfigFile;
    }

    public void setInterfaceConfigFile(String interfaceConfigFile) {
        this.interfaceConfigFile = interfaceConfigFile;
    }


    public DownloadType getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public boolean getHAR() {
        return haltOnReset;
    }

    public void setHAR(boolean haltOnReset) {
        this.haltOnReset = haltOnReset;
    }

    public boolean getFlushRegs() {
        return flushRegs;
    }

    public void setFlushRegs(boolean flushRegs) {
        this.flushRegs = flushRegs;
    }

    public boolean getInitialBreak() {
        return initialBreak;
    }

    public void setInitialBreak(boolean initialBreak) {
        this.initialBreak = initialBreak;
    }

    public String getInitialBreakName() {
        return initialBreakName;
    }

    public void setInitialBreakName(String initialBreakName) {
        this.initialBreakName = initialBreakName;
    }

    public String getBootOffset() {
        return bootloaderOffset;
    }

    public void setBootOffset(String offset) {
        this.bootloaderOffset = offset;
    }

    public String getPartitionOffset() {
        return partitionOffset;
    }

    public void setPartitionOffset(String offset) {
        this.partitionOffset = offset;
    }

    public String getBootBinPath() {
        return bootloaderBinPath;
    }

    public void setBootBinPath(String path) {
        this.bootloaderBinPath = path;
    }

    public String getPartitionBinPath() {
        return partitionBinPath;
    }

    public void setPartitionBinPath(String path) {
        this.partitionBinPath = path;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public void setProgramType(ProgramType programType) {
        this.programType = programType;
    }
}
