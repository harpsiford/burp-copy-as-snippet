package com.copyassnippet.ui.settings;

import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class PresetFormPanel extends JPanel {

    private final JTextField nameField;
    private final JTextArea headerRegexesArea;
    private final JTextArea cookieRegexesArea;
    private final JTextArea paramRegexesArea;
    private final JTextField replacementStringField;
    private final RedactionRuleTableModel ruleTableModel;
    private final JTable ruleTable;
    private final JButton ruleAddButton;
    private final JButton ruleDeleteButton;
    private final JTextArea templateArea;
    private String currentPresetId;
    private boolean formEnabled = true;

    PresetFormPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        nameField = new JTextField();
        Font textAreaFont = UIManager.getFont("TextArea.font");
        Font textFieldFont = UIManager.getFont("TextField.font");

        headerRegexesArea = new JTextArea(6, 30);
        headerRegexesArea.setFont(textAreaFont);

        cookieRegexesArea = new JTextArea(6, 20);
        cookieRegexesArea.setFont(textAreaFont);

        paramRegexesArea = new JTextArea(6, 20);
        paramRegexesArea.setFont(textAreaFont);

        replacementStringField = new JTextField(DefaultPresetFactory.DEFAULT_REPLACEMENT, 20);
        replacementStringField.setFont(textFieldFont);

        ruleTableModel = new RedactionRuleTableModel();
        ruleTable = new JTable(ruleTableModel);
        ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ruleTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        ruleTable.setIntercellSpacing(new Dimension(0, 0));
        ruleTable.setRowHeight(ruleTable.getFontMetrics(ruleTable.getFont()).getHeight() + 2);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Regex", "Cookie", "Header", "Param"});
        ruleTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(typeCombo));
        ruleTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        ruleTable.getColumnModel().getColumn(0).setMaxWidth(100);

        ruleAddButton = new JButton("Add");
        ruleDeleteButton = new JButton("Delete");
        ruleDeleteButton.setEnabled(false);

        ruleAddButton.addActionListener(e -> onRuleAdd());
        ruleDeleteButton.addActionListener(e -> onRuleDelete());
        ruleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ruleDeleteButton.setEnabled(formEnabled && ruleTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane ruleScroll = new JScrollPane(ruleTable);
        ruleScroll.setPreferredSize(new Dimension(500, 100));

        JPanel ruleButtonBar = new JPanel();
        ruleButtonBar.setLayout(new BoxLayout(ruleButtonBar, BoxLayout.Y_AXIS));
        ruleButtonBar.add(ruleAddButton);
        ruleButtonBar.add(Box.createVerticalStrut(3));
        ruleButtonBar.add(ruleDeleteButton);
        ruleButtonBar.add(Box.createVerticalGlue());

        JPanel ruleContent = new JPanel(new BorderLayout(5, 0));
        ruleContent.add(ruleButtonBar, BorderLayout.WEST);
        ruleContent.add(ruleScroll, BorderLayout.CENTER);

        JPanel ruleHeaderPanel = new JPanel();
        ruleHeaderPanel.setLayout(new BoxLayout(ruleHeaderPanel, BoxLayout.Y_AXIS));
        JLabel rulesHeader = sectionHeader("Redaction Rules");
        rulesHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        ruleHeaderPanel.add(rulesHeader);

        JPanel rulePanel = new JPanel(new BorderLayout(0, 2));
        rulePanel.add(ruleHeaderPanel, BorderLayout.NORTH);
        rulePanel.add(ruleContent, BorderLayout.CENTER);

        templateArea = new JTextArea(5, 40);
        templateArea.setFont(textAreaFont);

        JPanel nameRow = new JPanel(new BorderLayout(5, 0));
        nameRow.add(new JLabel("Name:"), BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel replacementRow = new JPanel(new BorderLayout(5, 0));
        replacementRow.add(new JLabel("Replacement string:"), BorderLayout.WEST);
        replacementRow.add(replacementStringField, BorderLayout.CENTER);
        replacementRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel regexColumns = new JPanel(new GridLayout(1, 3, 10, 0));
        regexColumns.add(labeledScroll("Header regexes (one per line):", headerRegexesArea));
        regexColumns.add(labeledScroll("Cookie regexes (one per line):", cookieRegexesArea));
        regexColumns.add(labeledScroll("Param regexes (one per line):", paramRegexesArea));

        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        regexColumns.setAlignmentX(Component.LEFT_ALIGNMENT);
        replacementRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rulePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        ruleHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel templateHeader = sectionHeader("Template");
        templateHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel templatePanel = labeledScroll("Available placeholders: {{request}}, {{response}}", templateArea);
        templatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(nameRow);
        add(Box.createVerticalStrut(5));
        add(regexColumns);
        add(Box.createVerticalStrut(10));
        add(replacementRow);
        add(Box.createVerticalStrut(5));
        add(rulePanel);
        add(Box.createVerticalStrut(10));
        add(templateHeader);
        add(Box.createVerticalStrut(2));
        add(templatePanel);
    }

    void setFormData(PresetFormData formData) {
        currentPresetId = formData.getPresetId();
        nameField.setText(formData.getName());
        headerRegexesArea.setText(joinLines(formData.getHeaderRegexes()));
        cookieRegexesArea.setText(joinLines(formData.getCookieRegexes()));
        paramRegexesArea.setText(joinLines(formData.getParamRegexes()));
        replacementStringField.setText(formData.getReplacementString());
        ruleTableModel.setRules(formData.getRedactionRules());
        templateArea.setText(formData.getTemplate());
    }

    PresetFormData getFormData() {
        if (ruleTable.isEditing()) {
            ruleTable.getCellEditor().stopCellEditing();
        }

        return new PresetFormData(
                currentPresetId,
                nameField.getText(),
                parseLines(headerRegexesArea.getText()),
                parseLines(cookieRegexesArea.getText()),
                parseLines(paramRegexesArea.getText()),
                replacementStringField.getText(),
                ruleTableModel.getRules(),
                templateArea.getText()
        );
    }

    void setFormEnabled(boolean enabled) {
        formEnabled = enabled;
        nameField.setEnabled(enabled);
        headerRegexesArea.setEnabled(enabled);
        cookieRegexesArea.setEnabled(enabled);
        paramRegexesArea.setEnabled(enabled);
        replacementStringField.setEnabled(enabled);
        ruleTable.setEnabled(enabled);
        ruleAddButton.setEnabled(enabled);
        ruleDeleteButton.setEnabled(enabled && ruleTable.getSelectedRow() >= 0);
        templateArea.setEnabled(enabled);
    }

    void focusNameField() {
        nameField.requestFocusInWindow();
    }

    private void onRuleAdd() {
        if (ruleTable.isEditing()) {
            ruleTable.getCellEditor().stopCellEditing();
        }
        ruleTableModel.addRule(new RedactionRule(RedactionRule.Type.REGEX, ""));
        int newRow = ruleTableModel.getRowCount() - 1;
        ruleTable.setRowSelectionInterval(newRow, newRow);
        ruleTable.editCellAt(newRow, 1);
    }

    private void onRuleDelete() {
        if (ruleTable.isEditing()) {
            ruleTable.getCellEditor().stopCellEditing();
        }
        int sel = ruleTable.getSelectedRow();
        if (sel >= 0) {
            ruleTableModel.removeRule(sel);
        }
    }

    private static List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String joinLines(List<String> lines) {
        List<String> sanitized = new ArrayList<>();
        for (String line : lines) {
            if (line != null) {
                sanitized.add(line);
            }
        }
        return String.join("\n", sanitized);
    }

    private static JPanel labeledScroll(String label, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private static JLabel sectionHeader(String title) {
        JLabel label = new JLabel(title);
        Font baseFont = UIManager.getFont("Label.font");
        Font sourceFont = baseFont != null ? baseFont : label.getFont();
        label.setFont(sourceFont.deriveFont(Font.BOLD, sourceFont.getSize2D() + 2f));
        return label;
    }
}
