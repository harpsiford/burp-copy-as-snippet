package com.copyassnippet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class PresetFormPanel extends JPanel {

    private final JTextField nameField;
    private final JComboBox<PresetScope> scopeCombo;
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
        scopeCombo = new JComboBox<>(PresetScope.EDITABLE_VALUES);
        Font textAreaFont = UIManager.getFont("TextArea.font");
        Font textFieldFont = UIManager.getFont("TextField.font");

        headerRegexesArea = new JTextArea(6, 30);
        headerRegexesArea.setFont(textAreaFont);

        cookieRegexesArea = new JTextArea(6, 20);
        cookieRegexesArea.setFont(textAreaFont);

        paramRegexesArea = new JTextArea(6, 20);
        paramRegexesArea.setFont(textAreaFont);

        replacementStringField = new JTextField(Preset.DEFAULT_REPLACEMENT, 20);
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

        JPanel ruleButtonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        ruleButtonBar.add(ruleAddButton);
        ruleButtonBar.add(ruleDeleteButton);

        JPanel rulePanel = new JPanel(new BorderLayout(0, 2));
        rulePanel.add(new JLabel("Redaction rules (value replacement):"), BorderLayout.NORTH);
        rulePanel.add(ruleScroll, BorderLayout.CENTER);
        rulePanel.add(ruleButtonBar, BorderLayout.SOUTH);

        templateArea = new JTextArea(5, 40);
        templateArea.setFont(textAreaFont);

        JPanel nameRow = new JPanel(new BorderLayout(5, 0));
        nameRow.add(new JLabel("Name:"), BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel scopeRow = new JPanel(new BorderLayout(5, 0));
        scopeRow.add(new JLabel("Scope:"), BorderLayout.WEST);
        scopeRow.add(scopeCombo, BorderLayout.CENTER);
        scopeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel replacementRow = new JPanel(new BorderLayout(5, 0));
        replacementRow.add(new JLabel("Replacement string:"), BorderLayout.WEST);
        replacementRow.add(replacementStringField, BorderLayout.CENTER);
        replacementRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel placeholderHint = new JLabel("Template placeholders: {{request}}, {{response}}");
        Font labelFont = UIManager.getFont("Label.font");
        placeholderHint.setFont(labelFont.deriveFont(labelFont.getStyle() | Font.ITALIC));

        JPanel regexColumns = new JPanel(new GridLayout(1, 3, 10, 0));
        regexColumns.add(labeledScroll("Header regexes (one per line):", headerRegexesArea));
        regexColumns.add(labeledScroll("Cookie regexes (one per line):", cookieRegexesArea));
        regexColumns.add(labeledScroll("Param regexes (one per line):", paramRegexesArea));

        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        scopeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        regexColumns.setAlignmentX(Component.LEFT_ALIGNMENT);
        replacementRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rulePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        placeholderHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel templatePanel = labeledScroll("Template:", templateArea);
        templatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(nameRow);
        add(Box.createVerticalStrut(5));
        add(scopeRow);
        add(Box.createVerticalStrut(10));
        add(regexColumns);
        add(Box.createVerticalStrut(10));
        add(replacementRow);
        add(Box.createVerticalStrut(5));
        add(rulePanel);
        add(Box.createVerticalStrut(10));
        add(placeholderHint);
        add(templatePanel);
    }

    void setFormData(PresetFormData formData) {
        currentPresetId = formData.getPresetId();
        nameField.setText(formData.getName());
        scopeCombo.setSelectedItem(formData.getScope().toEditableScope());
        headerRegexesArea.setText(String.join("\n", formData.getHeaderRegexes()));
        cookieRegexesArea.setText(String.join("\n", formData.getCookieRegexes()));
        paramRegexesArea.setText(String.join("\n", formData.getParamRegexes()));
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
                (PresetScope) scopeCombo.getSelectedItem(),
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
        scopeCombo.setEnabled(enabled);
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

    private static JPanel labeledScroll(String label, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }
}
