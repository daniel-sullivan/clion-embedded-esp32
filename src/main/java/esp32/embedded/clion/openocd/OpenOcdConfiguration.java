package esp32.embedded.clion.openocd;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.debugger.CLionDebuggerKind;
import com.jetbrains.cidr.cpp.execution.remote.DebuggerData;
import com.jetbrains.cidr.execution.CidrCommandLineState;
import com.jetbrains.cidr.execution.CidrExecutableDataHolder;
import java.util.Objects;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * (c) elmot on 29.9.2017.
 */
public class OpenOcdConfiguration extends CMakeAppRunConfiguration implements CidrExecutableDataHolder {
    public static final DebuggerData DEF_DEBUGGER = new DebuggerData(CLionDebuggerKind.Bundled.GDB.INSTANCE);
    public static final int DEF_GDB_PORT = 3333;
    public static final int DEF_TELNET_PORT = 4444;
    public static final DownloadType DEF_DOWNLOAD_TYPE = DownloadType.ALWAYS;
    public static final String DEF_PROGRAM_OFFSET = "0x10000";
    public static final String DEF_BOOT_OFFSET = "0x0";
    public static final String DEF_BOOT_BIN_PATH = "build/bootloader/bootloader.bin";
    public static final boolean DEF_BOOT_BIN_PATH_SET = false;

    public static final String DEF_PART_OFFSET = "0x8000";
    public static final String DEF_PART_BIN_PATH = "build/partition_table/partition-table.bin";
    public static final boolean DEF_PART_BIN_PATH_SET = false;
    public static final ResetType DEF_RESET_TYPE = ResetType.HALT;
    public static final boolean DEF_FLUSH_REGS = true;
    public static final boolean DEF_BREAK_FUNCTION = true;
    public static final String DEF_BREAK_FUNCTION_NAME = "app_main";

    public static final ProgramType DEF_PROGRAM_TYPE = ProgramType.PROGRAM_ESP;
    public static final boolean DEF_APPEND_VERIFY = true;
    public static final String DEF_ADD_PROG_PARAM = "";
    public static final boolean DEF_ADD_PROG_PARAM_SET = false;

    private static final String ATTR_DEBUGGER = "gdb_debugger";
    private static final String ATTR_GDB_PORT = "gdb_port";
    private static final String ATTR_TELNET_PORT = "telnet_port";
    private static final String ATTR_BOARD_CONFIG = "board_config";
    private static final String ATTR_INTERFACE_CONFIG = "interface_config";
    private static final String ATTR_BOOT_PATH_SET_CONFIG = "boot_path_set_cfg";
    private static final String ATTR_BOOT_PATH_CONFIG = "boot_path_cfg";
    private static final String ATTR_BOOT_OFFSET_CONFIG = "boot_offset_cfg";

    private static final String ATTR_PART_PATH_SET_CONFIG = "part_path_set_cfg";
    private static final String ATTR_PART_PATH_CONFIG = "part_path_cfg";
    private static final String ATTR_PART_OFFSET_CONFIG = "part_offset_cfg";
    private static final String ATTR_PROGRAM_OFFSET_CONFIG = "program_offset_cfg";
    public static final String ATTR_DOWNLOAD_TYPE = "download_type";
    public static final String ATTR_RESET_TYPE = "reset_type";
    public static final String ATTR_FLUSH_REGS = "flush_regs";
    public static final String ATTR_BREAK_FUNCTION = "break";
    public static final String ATTR_BREAK_FUNCTION_NAME = "break_function";

    public static final String ATTR_PROGRAM_TYPE_CONFIG = "prog_type_cfg";
    public static final String ATTR_APPEND_VERIFY_CONFIG = "app_verify_cfg";
    public static final String ATTR_ADD_PROG_PARAM_CONFIG = "add_prog_param_cfg";
    public static final String ATTR_ADD_PROG_PARAM_SET_CONFIG = "add_prog_param_set_cfg";


    private DebuggerData debuggerData = DEF_DEBUGGER;
    private int gdbPort = DEF_GDB_PORT;
    private int telnetPort = DEF_TELNET_PORT;
    private String boardConfigFile;
    private String interfaceConfigFile;
    private DownloadType downloadType = DEF_DOWNLOAD_TYPE;
    private String offset = DEF_PROGRAM_OFFSET;
    private ResetType resetType = DEF_RESET_TYPE;
    private boolean flushRegs = DEF_FLUSH_REGS;
    private boolean initialBreak = DEF_BREAK_FUNCTION;
    private String initialBreakName = DEF_BREAK_FUNCTION_NAME;

    private String bootloaderOffset = DEF_BOOT_OFFSET;
    private String bootloaderBinPath = DEF_BOOT_BIN_PATH;
    private boolean bootloaderBinPathSet = DEF_BOOT_BIN_PATH_SET;
    private String partitionOffset = DEF_PART_OFFSET;
    private String partitionBinPath = DEF_PART_BIN_PATH;
    private boolean partitionBinPathSet = DEF_PART_BIN_PATH_SET;

    private ProgramType programType = DEF_PROGRAM_TYPE;
    private boolean appendVerify = DEF_APPEND_VERIFY;
    private String additionalProgramParameters = DEF_ADD_PROG_PARAM;
    private boolean additionalProgramParametersSet = DEF_ADD_PROG_PARAM_SET;

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
        HALT("init; reset halt", true, true),
        RESET("init; reset"),
        RUN("init; reset run;"),
        INIT("init; reset init;"),
        NONE("");

        @Override
        public String toString() {
            return toBeautyString(super.toString());
        }

        ResetType(String command) {
            this(command, false, false);
        }

        ResetType(String command, boolean supportsBreakpoints, boolean needsInit) {
            this.command = command;
            this.supportsBreakpoints = supportsBreakpoints;
            this.needsInit = needsInit;
        }

        private final String command;
        private final boolean supportsBreakpoints;
        private final boolean needsInit;

        public final String getCommand() {
            return command;
        }

        public final boolean supportsBreakpoints() {
            return supportsBreakpoints;
        }

        public final boolean needsInit() {
            return needsInit;
        }

    }

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

        Element debuggerElement = element.getChild(ATTR_DEBUGGER);
        if (debuggerElement != null) {
            debuggerData.readExternal(debuggerElement);
        }

        gdbPort = readIntAttr(element, ATTR_GDB_PORT, DEF_GDB_PORT);
        telnetPort = readIntAttr(element, ATTR_TELNET_PORT, DEF_TELNET_PORT);
        downloadType = readEnumAttr(element, ATTR_DOWNLOAD_TYPE, DownloadType.ALWAYS);
        resetType = readEnumAttr(element, ATTR_RESET_TYPE, DEF_RESET_TYPE);
        flushRegs = readBoolAttr(element, ATTR_FLUSH_REGS, DEF_FLUSH_REGS);
        initialBreak = readBoolAttr(element, ATTR_BREAK_FUNCTION, DEF_BREAK_FUNCTION);
        initialBreakName = element.getAttributeValue(ATTR_BREAK_FUNCTION_NAME, null, DEF_BREAK_FUNCTION_NAME);

        offset = element.getAttributeValue(ATTR_PROGRAM_OFFSET_CONFIG, null, DEF_PROGRAM_OFFSET);

        bootloaderBinPathSet = readBoolAttr(element, ATTR_BOOT_PATH_SET_CONFIG, DEF_BOOT_BIN_PATH_SET);
        bootloaderBinPath = element.getAttributeValue(ATTR_BOOT_PATH_CONFIG, null,
                bootloaderBinPathSet ? null : DEF_BOOT_BIN_PATH);
        bootloaderOffset = element.getAttributeValue(ATTR_BOOT_OFFSET_CONFIG, null, DEF_BOOT_OFFSET);

        partitionBinPathSet = readBoolAttr(element, ATTR_PART_PATH_SET_CONFIG, DEF_PART_BIN_PATH_SET);
        partitionBinPath = element.getAttributeValue(ATTR_PART_PATH_CONFIG, null,
                partitionBinPathSet ? null : DEF_PART_BIN_PATH);
        partitionOffset = element.getAttributeValue(ATTR_PART_OFFSET_CONFIG, null, DEF_PART_OFFSET);

        String programTypeStr = element.getAttributeValue(ATTR_PROGRAM_TYPE_CONFIG);
        programType = programTypeStr != null ? ProgramType.valueOf(programTypeStr) : DEF_PROGRAM_TYPE;
        appendVerify = readBoolAttr(element, ATTR_APPEND_VERIFY_CONFIG, DEF_APPEND_VERIFY);

        additionalProgramParametersSet = readBoolAttr(element, ATTR_ADD_PROG_PARAM_SET_CONFIG, DEF_ADD_PROG_PARAM_SET);
        additionalProgramParameters = element.getAttributeValue(ATTR_ADD_PROG_PARAM_CONFIG, null,
                additionalProgramParametersSet ? null : DEF_ADD_PROG_PARAM);
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

        Element debuggerElement = new Element(ATTR_DEBUGGER);
        element.addContent(debuggerElement);
        debuggerData.writeExternal(debuggerElement);

        element.setAttribute(ATTR_GDB_PORT, String.valueOf(gdbPort));
        element.setAttribute(ATTR_TELNET_PORT, String.valueOf(telnetPort));
        if (boardConfigFile != null) {
            element.setAttribute(ATTR_BOARD_CONFIG, boardConfigFile);
        }
        if (interfaceConfigFile != null) {
            element.setAttribute(ATTR_INTERFACE_CONFIG, interfaceConfigFile);
        }
        element.setAttribute(ATTR_DOWNLOAD_TYPE, downloadType.name());
        element.setAttribute(ATTR_RESET_TYPE, resetType.name());
        element.setAttribute(ATTR_FLUSH_REGS, String.valueOf(flushRegs));
        element.setAttribute(ATTR_BREAK_FUNCTION, String.valueOf(initialBreak));
        element.setAttribute(ATTR_BREAK_FUNCTION_NAME, initialBreakName);

        element.setAttribute(ATTR_BOOT_PATH_SET_CONFIG, String.valueOf(bootloaderBinPathSet));
        element.setAttribute(ATTR_BOOT_PATH_CONFIG, bootloaderBinPath == null ? "" : bootloaderBinPath);
        element.setAttribute(ATTR_BOOT_OFFSET_CONFIG, Objects.requireNonNullElse(bootloaderOffset, DEF_BOOT_OFFSET));

        element.setAttribute(ATTR_PART_PATH_SET_CONFIG, String.valueOf(partitionBinPathSet));
        element.setAttribute(ATTR_PART_PATH_CONFIG, partitionBinPath == null ? "" : partitionBinPath);
        element.setAttribute(ATTR_PART_OFFSET_CONFIG, Objects.requireNonNullElse(partitionOffset, DEF_PART_OFFSET));

        element.setAttribute(ATTR_PROGRAM_OFFSET_CONFIG, Objects.requireNonNullElse(offset, DEF_PROGRAM_OFFSET));

        element.setAttribute(ATTR_PROGRAM_TYPE_CONFIG, programType.name());
        element.setAttribute(ATTR_APPEND_VERIFY_CONFIG, String.valueOf(appendVerify));

        element.setAttribute(ATTR_ADD_PROG_PARAM_SET_CONFIG, String.valueOf(additionalProgramParametersSet));
        element.setAttribute(ATTR_ADD_PROG_PARAM_CONFIG, additionalProgramParameters == null ? "" : additionalProgramParameters);
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
            throw new RuntimeConfigurationException("Port value must be in the range [1025...65535]");
    }

    public DebuggerData getDebuggerData() {
        return debuggerData;
    }

    public void setDebuggerData(DebuggerData debuggerData) {
        this.debuggerData = debuggerData;
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

    public ResetType getResetType() {
        return resetType;
    }

    public void setResetType(ResetType resetType) {
        this.resetType = resetType;
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
        this.bootloaderBinPathSet = true;
    }

    public String getPartitionBinPath() {
        return partitionBinPath;
    }

    public void setPartitionBinPath(String path) {
        this.partitionBinPath = path;
        this.partitionBinPathSet = true;
    }

    public ProgramType getProgramType() {
        return programType;
    }

    public void setProgramType(ProgramType programType) {
        this.programType = programType;
    }

    public boolean getAppendVerify() {
        return appendVerify;
    }

    public void setAppendVerify(boolean appendVerify) {
        this.appendVerify = appendVerify;
    }

    public String getAdditionalProgramParameters() {
        return additionalProgramParameters;
    }

    public void setAdditionalProgramParameters(String additionalProgramParameters) {
        this.additionalProgramParameters = additionalProgramParameters;
        this.additionalProgramParametersSet = additionalProgramParameters != null;
    }
}
