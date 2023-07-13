package esp32.embedded.clion.openocd;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import esp32.embedded.clion.openocd.OpenOcdConfiguration.DownloadType;
import esp32.embedded.clion.openocd.OpenOcdConfiguration.ProgramType;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.NotNull;

public class OpenOcdConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private IntegerField gdbPort;
    private IntegerField telnetPort;
    private ExtendableTextField offset;
    private JXRadioGroup<OpenOcdConfiguration.ResetType> resetGroup;
    private JCheckBox flushRegsCheck;
    private JCheckBox initialBreakpointCheck;
    private ExtendableTextField initialBreakpointName;
    private FileChooseInput boardConfigFile;
    private FileChooseInput interfaceConfigFile;

    private FileChooseInput.BinFile bootloaderFile;
    private ExtendableTextField bootloaderOffset;
    private FileChooseInput.BinFile partitionTableFile;
    private ExtendableTextField partitionTableOffset;
    private String openocdHome;
    private JXRadioGroup<DownloadType> downloadGroup;

    private JXRadioGroup<ProgramType> programType;
    private JCheckBox appendVerify;


    public OpenOcdConfigurationEditor(Project project,
                                      @NotNull CMakeBuildConfigurationHelper cMakeBuildConfigurationHelper) {
        super(project, cMakeBuildConfigurationHelper);
    }

    @Override
    protected void applyEditorTo(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) throws ConfigurationException {
        super.applyEditorTo(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocdConfiguration = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        String boardConfig = boardConfigFile.getText().trim();
        ocdConfiguration.setBoardConfigFile(boardConfig.isEmpty() ? null : boardConfig);

        String interfaceConfig = interfaceConfigFile.getText().trim();
        ocdConfiguration.setInterfaceConfigFile(interfaceConfig.isEmpty() ? null : interfaceConfig);

        String bootPath = bootloaderFile.getPath().trim();
        ocdConfiguration.setBootBinPath(bootPath.isEmpty() ? null : bootPath);

        String partPath = partitionTableFile.getPath().trim();
        ocdConfiguration.setPartitionBinPath(partPath.isEmpty() ? null : partPath);

        gdbPort.validateContent();
        telnetPort.validateContent();
        ocdConfiguration.setGdbPort(gdbPort.getValue());
        ocdConfiguration.setTelnetPort(telnetPort.getValue());
        ocdConfiguration.setDownloadType(downloadGroup.getSelectedValue());
        ocdConfiguration.setProgramType(programType.getSelectedValue());
        ocdConfiguration.setAppendVerify(appendVerify.isSelected());

        ocdConfiguration.setOffset(offset.getText());
        ocdConfiguration.setBootOffset(bootloaderOffset.getText());
        ocdConfiguration.setPartitionOffset(partitionTableOffset.getText());
        ocdConfiguration.setResetType(resetGroup.getSelectedValue());
        ocdConfiguration.setFlushRegs(flushRegsCheck.isSelected());
        ocdConfiguration.setInitialBreak(initialBreakpointCheck.isSelected());
        ocdConfiguration.setInitialBreakName(initialBreakpointName.getText());
    }

    @Override
    protected void resetEditorFrom(@NotNull CMakeAppRunConfiguration cMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration);

        OpenOcdConfiguration ocd = (OpenOcdConfiguration) cMakeAppRunConfiguration;

        openocdHome = ocd.getProject().getComponent(OpenOcdSettingsState.class).openOcdHome;

        boardConfigFile.setText(ocd.getBoardConfigFile());
        interfaceConfigFile.setText(ocd.getInterfaceConfigFile());

        String root = ModuleRootManager.getInstance(ModuleManager.getInstance(myProject).getModules()[0])
                .getExcludeRoots()[0].getParent().getPath();

        String bootBinPath = ocd.getBootBinPath();
        if (bootBinPath != null)
            bootBinPath = bootBinPath.replaceAll(root + "/", "");
        bootloaderFile.setText(bootBinPath);

        String partitionPath = ocd.getPartitionBinPath();
        if (partitionPath != null)
            partitionPath = partitionPath.replaceAll(root + "/", "");
        partitionTableFile.setText(partitionPath);

        gdbPort.setText(String.valueOf(ocd.getGdbPort()));

        telnetPort.setText(String.valueOf(ocd.getTelnetPort()));
        downloadGroup.setSelectedValue(ocd.getDownloadType());
        programType.setSelectedValue(ocd.getProgramType());
        appendVerify.setSelected(ocd.getAppendVerify());

        offset.setText(ocd.getOffset());
        bootloaderOffset.setText(ocd.getBootOffset());
        partitionTableOffset.setText(ocd.getPartitionOffset());
        resetGroup.setSelectedValue(ocd.getResetType());
        flushRegsCheck.setSelected(ocd.getFlushRegs());
        initialBreakpointCheck.setSelected(ocd.getInitialBreak());
        initialBreakpointName.setText(ocd.getInitialBreakName());
    }

    @Override
    protected void createEditorInner(JPanel panel, GridBag gridBag) {
        super.createEditorInner(panel, gridBag);

        for (Component component : panel.getComponents()) {
            if (component instanceof CommonProgramParametersPanel) {
                component.setVisible(false);//todo get rid of this hack
            }
        }

        panel.add(new JLabel("Board config file:"), gridBag.nextLine().next());

        boardConfigFile = new FileChooseInput.BoardCfg("Board config", VfsUtil.getUserHomeDir(),
                this::getOpenocdHome);
        panel.add(boardConfigFile, gridBag.next().coverLine());

        panel.add(new JLabel("Interface config file:"), gridBag.nextLine().next());

        interfaceConfigFile = new FileChooseInput.InterfaceCfg("Interface config", VfsUtil.getUserHomeDir(),
                this::getOpenocdHome);
        panel.add(interfaceConfigFile, gridBag.next().coverLine());

        VirtualFile contentRoot = ModuleRootManager.getInstance(ModuleManager.getInstance(myProject).getModules()[0])
                .getExcludeRoots()[0].getParent();

        JPanel bootloaderPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        bootloaderPanel.add(new JLabel("Bootloader binary:"));
        bootloaderFile = new FileChooseInput.BinFile("Bootloader file", VfsUtil.getUserHomeDir(), contentRoot);
        bootloaderPanel.add(bootloaderFile);

        bootloaderPanel.add(new JLabel("Bootloader offset:"));
        bootloaderOffset = addOffsetInput(OpenOcdConfiguration.DEF_BOOT_OFFSET);
        bootloaderPanel.add(bootloaderOffset);

        panel.add(bootloaderPanel, gridBag.nextLine().next().coverLine());

        JPanel partitionPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        partitionPanel.add(new JLabel("Partition Table binary:"));
        partitionTableFile = new FileChooseInput.BinFile("Partition Table file", VfsUtil.getUserHomeDir(), contentRoot);
        partitionPanel.add(partitionTableFile);

        partitionPanel.add(new JLabel("Partition Table offset:"));
        partitionTableOffset = addOffsetInput(OpenOcdConfiguration.DEF_PART_OFFSET);
        partitionPanel.add(partitionTableOffset);

        panel.add(partitionPanel, gridBag.nextLine().next().coverLine());

        panel.add(new JLabel("OpenOCD command:"), gridBag.nextLine().next());
        programType = new JXRadioGroup<>(ProgramType.values());
        panel.add(programType, gridBag.next().coverLine());

        appendVerify = new JCheckBox("Append verify parameter", OpenOcdConfiguration.DEF_APPEND_VERIFY);
        panel.add(appendVerify, gridBag.nextLine().next());

        JPanel portsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

        gdbPort = addPortInput(portsPanel, "GDB port", OpenOcdConfiguration.DEF_GDB_PORT);
        portsPanel.add(Box.createHorizontalStrut(10));

        telnetPort = addPortInput(portsPanel, "Telnet port", OpenOcdConfiguration.DEF_TELNET_PORT);

        panel.add(portsPanel, gridBag.nextLine().next().coverLine());

        panel.add(new JLabel("Download Options"), gridBag.nextLine().next());

        panel.add(createDownloadSelector(), gridBag.nextLine().coverLine());

        panel.add(createGDBSettingsSelector(), gridBag.nextLine().coverLine());
    }

    @NotNull
    private JPanel createDownloadSelector() {
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        GridLayout downloadGrid = new GridLayout(3, 2);
        downloadPanel.setLayout(downloadGrid);

        downloadPanel.add(new JLabel("Offset:"));
        offset = addOffsetInput(OpenOcdConfiguration.DEF_PROGRAM_OFFSET);

        downloadPanel.add(offset);

        downloadPanel.add(new JLabel("Perform:"));
        downloadGroup = new JXRadioGroup<>(DownloadType.values());
        downloadPanel.add(downloadGroup);

        downloadPanel.add(new JLabel("Reset:"));
        resetGroup = new JXRadioGroup<>(OpenOcdConfiguration.ResetType.values());
        resetGroup.addActionListener(e -> {
            if (resetGroup.getSelectedValue().supportsBreakpoints()) {
                initialBreakpointCheck.setVisible(true);
                if (initialBreakpointCheck.isSelected()) initialBreakpointName.setVisible(true);
            } else {
                initialBreakpointCheck.setVisible(false);
                initialBreakpointName.setVisible(false);
            }
        });
        downloadPanel.add(resetGroup);

        return downloadPanel;
    }

    private JPanel createGDBSettingsSelector() {
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        GridLayout settingsGrid = new GridLayout(2, 4);
        settingsPanel.setLayout(settingsGrid);

        flushRegsCheck = new JCheckBox("Flush registers", OpenOcdConfiguration.DEF_FLUSH_REGS);
        settingsPanel.add(flushRegsCheck);

        settingsPanel.add(new JPanel()); // Placeholder

        initialBreakpointCheck = new JCheckBox("Break on function", OpenOcdConfiguration.DEF_BREAK_FUNCTION);
        initialBreakpointCheck.addItemListener(e -> initialBreakpointName.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        settingsPanel.add(initialBreakpointCheck);

        initialBreakpointName = new ExtendableTextField(OpenOcdConfiguration.DEF_BREAK_FUNCTION_NAME);
        settingsPanel.add(initialBreakpointName);

        return settingsPanel;
    }

    private IntegerField addPortInput(JPanel portsPanel, String label, int defaultValue) {
        portsPanel.add(new JLabel(label + ": "));
        IntegerField field = new IntegerField(label, 1024, 65535);
        field.setDefaultValue(defaultValue);
        field.setColumns(5);
        portsPanel.add(field);
        return field;
    }

    private ExtendableTextField addOffsetInput(String defaultValue) {
        ExtendableTextField field = new ExtendableTextField(defaultValue);
        field.setColumns(5);
        return field;
    }

    private String getOpenocdHome() {
        return openocdHome;
    }

}
