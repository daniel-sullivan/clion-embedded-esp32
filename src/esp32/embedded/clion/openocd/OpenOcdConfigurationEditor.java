package esp32.embedded.clion.openocd;

import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.panels.HorizontalBox;
import com.intellij.util.ui.GridBag;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration;
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor;
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper;
import org.jdesktop.swingx.JXRadioGroup;
import org.jetbrains.annotations.NotNull;
import esp32.embedded.clion.openocd.OpenOcdConfiguration.DownloadType;
import esp32.embedded.clion.openocd.OpenOcdConfiguration.ResetType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class OpenOcdConfigurationEditor extends CMakeAppRunConfigurationSettingsEditor {
    private IntegerField gdbPort;
    private IntegerField telnetPort;
    private ExtendableTextField offset;
    private JCheckBox harCheck;
    private JCheckBox flushRegsCheck;
    private JCheckBox initialBreakpointCheck;
    private ExtendableTextField initialBreakpointName;
    private FileChooseInput boardConfigFile;
    private FileChooseInput interfaceConfigFile;
    private String openocdHome;
    private JXRadioGroup<DownloadType> downloadGroup;


    @SuppressWarnings("WeakerAccess")
    public OpenOcdConfigurationEditor(Project project, @NotNull CMakeBuildConfigurationHelper cMakeBuildConfigurationHelper) {
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

        gdbPort.validateContent();
        telnetPort.validateContent();
        ocdConfiguration.setGdbPort(gdbPort.getValue());
        ocdConfiguration.setTelnetPort(telnetPort.getValue());
        ocdConfiguration.setDownloadType(downloadGroup.getSelectedValue());

        ocdConfiguration.setOffset(offset.getText());
        ocdConfiguration.setHAR(harCheck.isSelected());
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

        gdbPort.setText("" + ocd.getGdbPort());

        telnetPort.setText("" + ocd.getTelnetPort());
        downloadGroup.setSelectedValue(ocd.getDownloadType());

        offset.setText(ocd.getOffset());
        harCheck.setSelected(ocd.getHAR());
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

        boardConfigFile = new FileChooseInput.BoardCfg("Board config", VfsUtil.getUserHomeDir(), this::getOpenocdHome);
        panel.add(boardConfigFile, gridBag.next().coverLine());

        panel.add(new JLabel("Interface config file:"), gridBag.nextLine().next());

        interfaceConfigFile = new FileChooseInput.InterfaceCfg("Interface config", VfsUtil.getUserHomeDir(), this::getOpenocdHome);
        panel.add(interfaceConfigFile, gridBag.next().coverLine());


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
        GridLayout downloadGrid = new GridLayout(2, 2);
        downloadPanel.setLayout(downloadGrid);

        downloadPanel.add(new JLabel("Offset:"));
        offset = addOffsetInput(OpenOcdConfiguration.DEF_PROGRAM_OFFSET);

        downloadPanel.add(offset);

        downloadPanel.add(new JLabel("Perform:"));
        downloadGroup = new JXRadioGroup<>(DownloadType.values());
        downloadPanel.add(downloadGroup);

        return downloadPanel;
    }

    private JPanel createGDBSettingsSelector() {
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        GridLayout settingsGrid = new GridLayout(2,4);
        settingsPanel.setLayout(settingsGrid);

        flushRegsCheck = new JCheckBox("Flush registers", OpenOcdConfiguration.DEF_FLUSH_REGS);
        settingsPanel.add(flushRegsCheck);

        harCheck = new JCheckBox("Halt after reset", OpenOcdConfiguration.DEF_HAR);
        harCheck.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                initialBreakpointCheck.setVisible(true);
                if (initialBreakpointCheck.isSelected()) initialBreakpointName.setVisible(true);
            } else {
                initialBreakpointCheck.setVisible(false);
                initialBreakpointName.setVisible(false);
            }
        });
        settingsPanel.add(harCheck);

        initialBreakpointCheck = new JCheckBox("Break on function", OpenOcdConfiguration.DEF_BREAK_FUNCTION);
        initialBreakpointCheck.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                initialBreakpointName.setVisible(true);
            } else {
                initialBreakpointName.setVisible(false);
            }
        });
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

    private ExtendableTextField addOffsetInput( String defaultValue) {
        ExtendableTextField field = new ExtendableTextField(defaultValue);
        field.setColumns(5);
        return field;
    }

    private String getOpenocdHome() {
        return openocdHome;
    }

}
