package com.copyassnippet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog for creating a new preset, pre-populated with Default preset values.
 */
public class NewPresetDialog {

    private final PresetStore presetStore;

    public NewPresetDialog(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    public void show(Component parent) {
        Preset defaults = Preset.createDefault();

        JTextField nameField = new JTextField();
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"User", "Project"});

        JTextArea headerRegexesArea = new JTextArea(6, 30);
        headerRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        headerRegexesArea.setText(String.join("\n", defaults.getHeaderRegexes()));

        JTextArea cookieRegexesArea = new JTextArea(6, 20);
        cookieRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cookieRegexesArea.setText(String.join("\n", defaults.getCookieRegexes()));

        JTextArea paramRegexesArea = new JTextArea(6, 20);
        paramRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        paramRegexesArea.setText(String.join("\n", defaults.getParamRegexes()));

        JTextField replacementStringField = new JTextField(defaults.getReplacementString(), 20);
        replacementStringField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        MySettingsPanel.RedactionRuleTableModel ruleTableModel = new MySettingsPanel.RedactionRuleTableModel();
        ruleTableModel.setRules(defaults.getRedactionRules());
        JTable ruleTable = new JTable(ruleTableModel);
        ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ruleTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Regex", "Cookie", "Header", "Param"});
        ruleTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(typeCombo));
        ruleTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        ruleTable.getColumnModel().getColumn(0).setMaxWidth(100);
        ruleTable.setIntercellSpacing(new Dimension(0, 0));
        ruleTable.setRowHeight(ruleTable.getFontMetrics(ruleTable.getFont()).getHeight() + 2);

        JButton ruleAddButton = new JButton("Add");
        JButton ruleDeleteButton = new JButton("Delete");
        ruleDeleteButton.setEnabled(false);
        ruleAddButton.addActionListener(e -> {
            if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();
            ruleTableModel.addRule(new RedactionRule(RedactionRule.Type.REGEX, ""));
            int newRow = ruleTableModel.getRowCount() - 1;
            ruleTable.setRowSelectionInterval(newRow, newRow);
            ruleTable.editCellAt(newRow, 1);
        });
        ruleDeleteButton.addActionListener(e -> {
            if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();
            int sel = ruleTable.getSelectedRow();
            if (sel >= 0) ruleTableModel.removeRule(sel);
        });
        ruleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                ruleDeleteButton.setEnabled(ruleTable.getSelectedRow() >= 0);
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

        JTextArea templateArea = new JTextArea(5, 40);
        templateArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        templateArea.setText(defaults.getTemplate());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

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
        placeholderHint.setFont(placeholderHint.getFont().deriveFont(Font.ITALIC, 11f));

        JPanel regexColumns = new JPanel(new GridLayout(1, 3, 10, 0));
        regexColumns.add(labeledScroll("Header regexes (one per line):", headerRegexesArea));
        regexColumns.add(labeledScroll("Cookie regexes (one per line):", cookieRegexesArea));
        regexColumns.add(labeledScroll("Param regexes (one per line):", paramRegexesArea));

        form.add(nameRow);
        form.add(Box.createVerticalStrut(5));
        form.add(scopeRow);
        form.add(Box.createVerticalStrut(10));
        form.add(regexColumns);
        form.add(Box.createVerticalStrut(10));
        form.add(replacementRow);
        form.add(Box.createVerticalStrut(5));
        form.add(rulePanel);
        form.add(Box.createVerticalStrut(10));
        form.add(placeholderHint);
        form.add(labeledScroll("Template:", templateArea));

        form.setPreferredSize(new Dimension(800, 600));

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent, form, "Create new preset",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Name cannot be empty.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();

            String scope = (String) scopeCombo.getSelectedItem();
            List<String> headers = parseLines(headerRegexesArea.getText());
            List<String> cookies = parseLines(cookieRegexesArea.getText());
            List<String> params = parseLines(paramRegexesArea.getText());
            String replacement = replacementStringField.getText();
            List<RedactionRule> rules = ruleTableModel.getRules();
            String template = templateArea.getText();

            Preset preset = new Preset(name, headers, cookies, params, rules, replacement, template, true);

            if ("Project".equals(scope)) {
                List<Preset> list = new ArrayList<>(presetStore.getProjectPresets());
                list.removeIf(p -> p.getName().equals(name));
                list.add(preset);
                presetStore.setProjectPresets(list);
            } else {
                List<Preset> list = new ArrayList<>(presetStore.getUserPresets());
                list.removeIf(p -> p.getName().equals(name));
                list.add(preset);
                presetStore.setUserPresets(list);
            }
            return;
        }
    }

    private static List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static JPanel labeledScroll(String label, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }
}
