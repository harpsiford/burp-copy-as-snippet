package com.copyassnippet;

import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MySettingsPanel implements SettingsPanel {

    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;
    private final JPanel panel;

    private final PresetTableModel tableModel;
    private final JTable presetTable;

    private final JTextField nameField;
    private final JComboBox<String> scopeCombo;
    private final JTextArea headerRegexesArea;
    private final JTextArea cookieRegexesArea;
    private final JTextArea paramRegexesArea;
    private final JTextField replacementStringField;
    private final RedactionRuleTableModel ruleTableModel;
    private final JTable ruleTable;
    private final JButton ruleAddButton;
    private final JButton ruleDeleteButton;
    private final JTextArea templateArea;
    private final JButton deleteButton;
    private final JButton duplicateButton;
    private final JButton moveUpButton;
    private final JButton moveDownButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    private final JCheckBox hotkeyEnabledCheckbox;
    private final JTextField hotkeyField;

    private int editingRow = -1;
    private boolean addingNew = false;

    public MySettingsPanel(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;

        // --- Preset table ---
        tableModel = new PresetTableModel();
        presetTable = new JTable(tableModel);
        presetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetTable.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        presetTable.getColumnModel().getColumn(0).setMaxWidth(50);
        presetTable.getColumnModel().getColumn(0).setMinWidth(50);
        presetTable.getColumnModel().getColumn(0).setCellRenderer(new NativeSizedBooleanRenderer());
        presetTable.getColumnModel().getColumn(0).setCellEditor(new NativeSizedBooleanEditor());
        JScrollPane tableScroll = new JScrollPane(presetTable);
        tableScroll.setPreferredSize(new Dimension(400, 150));

        // --- Action buttons ---
        JButton addButton = new JButton("Add");
        deleteButton = new JButton("Delete");
        duplicateButton = new JButton("Duplicate");
        moveUpButton = new JButton("Up");
        moveDownButton = new JButton("Down");
        JButton restoreDefaultsButton = new JButton("Restore defaults");

        deleteButton.setEnabled(false);
        duplicateButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);

        addButton.addActionListener(e -> onAdd());
        deleteButton.addActionListener(e -> onDelete());
        duplicateButton.addActionListener(e -> onDuplicate());
        moveUpButton.addActionListener(e -> onMoveUp());
        moveDownButton.addActionListener(e -> onMoveDown());
        restoreDefaultsButton.addActionListener(e -> onRestoreDefaults());

        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.Y_AXIS));
        for (JButton btn : new JButton[]{addButton, deleteButton, duplicateButton, moveUpButton, moveDownButton, restoreDefaultsButton}) {
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
            buttonBar.add(btn);
            buttonBar.add(Box.createVerticalStrut(3));
        }
        buttonBar.add(Box.createVerticalGlue());

        // Compact row height matching Burp's native tables
        presetTable.setIntercellSpacing(new Dimension(0, 0));
        presetTable.setRowHeight(presetTable.getFontMetrics(presetTable.getFont()).getHeight() + 2);

        JPanel topSection = new JPanel(new BorderLayout(5, 0));
        topSection.add(buttonBar, BorderLayout.WEST);
        topSection.add(tableScroll, BorderLayout.CENTER);

        // --- Editor form ---
        nameField = new JTextField();
        scopeCombo = new JComboBox<>(new String[]{"User", "Project"});

        headerRegexesArea = new JTextArea(6, 30);
        headerRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cookieRegexesArea = new JTextArea(6, 20);
        cookieRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        paramRegexesArea = new JTextArea(6, 20);
        paramRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        replacementStringField = new JTextField(Preset.DEFAULT_REPLACEMENT, 20);
        replacementStringField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        ruleTableModel = new RedactionRuleTableModel();
        ruleTable = new JTable(ruleTableModel);
        ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ruleTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Regex", "Cookie", "Header", "Param"});
        ruleTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(typeCombo));
        ruleTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        ruleTable.getColumnModel().getColumn(0).setMaxWidth(100);
        ruleTable.setIntercellSpacing(new Dimension(0, 0));
        ruleTable.setRowHeight(ruleTable.getFontMetrics(ruleTable.getFont()).getHeight() + 2);

        ruleAddButton = new JButton("Add");
        ruleDeleteButton = new JButton("Delete");
        ruleDeleteButton.setEnabled(false);
        ruleAddButton.addActionListener(e -> onRuleAdd());
        ruleDeleteButton.addActionListener(e -> onRuleDelete());
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

        templateArea = new JTextArea(5, 40);
        templateArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> onCancel());

        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editorButtons.add(saveButton);
        editorButtons.add(cancelButton);

        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));
        editorPanel.setBorder(BorderFactory.createTitledBorder("Preset editor"));

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

        editorPanel.add(nameRow);
        editorPanel.add(Box.createVerticalStrut(5));
        editorPanel.add(scopeRow);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(regexColumns);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(replacementRow);
        editorPanel.add(Box.createVerticalStrut(5));
        editorPanel.add(rulePanel);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(placeholderHint);
        editorPanel.add(labeledScroll("Template:", templateArea));
        editorPanel.add(Box.createVerticalStrut(5));
        editorPanel.add(editorButtons);

        setEditorEnabled(false);

        // --- Hotkey section ---
        hotkeyEnabledCheckbox = new JCheckBox("Enable keyboard shortcut (works in HTTP message editor)");
        hotkeyEnabledCheckbox.setSelected(presetStore.isHotkeyEnabled());
        Icon hkIcon = scaledNativeCheckboxIcon(
                hotkeyEnabledCheckbox.getFontMetrics(hotkeyEnabledCheckbox.getFont()).getHeight() - 2);
        if (hkIcon != null) hotkeyEnabledCheckbox.setIcon(hkIcon);

        hotkeyField = new JTextField(presetStore.getHotkeyString(), 20);
        hotkeyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hotkeyField.setEnabled(presetStore.isHotkeyEnabled());

        hotkeyEnabledCheckbox.addActionListener(e -> {
            hotkeyField.setEnabled(hotkeyEnabledCheckbox.isSelected());
        });

        JButton hotkeyApplyButton = new JButton("Apply");
        hotkeyApplyButton.addActionListener(e -> onApplyHotkey());

        JPanel hotkeyRow = new JPanel(new BorderLayout(5, 0));
        hotkeyRow.add(new JLabel("Shortcut:"), BorderLayout.WEST);
        hotkeyRow.add(hotkeyField, BorderLayout.CENTER);
        hotkeyRow.add(hotkeyApplyButton, BorderLayout.EAST);
        hotkeyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel hotkeyPanel = new JPanel();
        hotkeyPanel.setLayout(new BoxLayout(hotkeyPanel, BoxLayout.Y_AXIS));
        hotkeyPanel.setBorder(BorderFactory.createTitledBorder("Keyboard shortcut (uses the first enabled preset)"));
        hotkeyEnabledCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeyPanel.add(hotkeyEnabledCheckbox);
        hotkeyPanel.add(Box.createVerticalStrut(5));
        hotkeyPanel.add(hotkeyRow);
        hotkeyPanel.add(Box.createVerticalStrut(3));

        // --- Main layout ---
        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        topSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topWrapper.add(topSection);
        topWrapper.add(Box.createVerticalStrut(10));
        topWrapper.add(hotkeyPanel);

        panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topWrapper, BorderLayout.NORTH);
        panel.add(editorPanel, BorderLayout.CENTER);

        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                reloadTable();
            }
        });

        reloadTable();
    }

    @Override
    public JPanel uiComponent() {
        return panel;
    }

    // --- Redaction rule table model ---

    static class RedactionRuleTableModel extends AbstractTableModel {
        private final List<RedactionRule> rules = new ArrayList<>();

        void setRules(List<RedactionRule> newRules) {
            rules.clear();
            rules.addAll(newRules);
            fireTableDataChanged();
        }

        List<RedactionRule> getRules() {
            return new ArrayList<>(rules);
        }

        @Override public int getRowCount() { return rules.size(); }
        @Override public int getColumnCount() { return 2; }

        @Override
        public String getColumnName(int col) {
            return col == 0 ? "Type" : "Pattern";
        }

        @Override public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public Object getValueAt(int row, int col) {
            RedactionRule r = rules.get(row);
            return col == 0 ? r.getType().displayName() : r.getPattern();
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            RedactionRule r = rules.get(row);
            if (col == 0) {
                r.setType(RedactionRule.Type.fromDisplayName((String) value));
            } else {
                r.setPattern((String) value);
            }
            fireTableCellUpdated(row, col);
        }

        void addRule(RedactionRule rule) {
            rules.add(rule);
            fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
        }

        void removeRule(int row) {
            rules.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    private static class PresetRow {
        final Preset preset;
        final String scope;

        PresetRow(Preset preset, String scope) {
            this.preset = preset;
            this.scope = scope;
        }
    }

    private class PresetTableModel extends AbstractTableModel {
        private final List<PresetRow> rows = new ArrayList<>();

        void setRows(List<PresetRow> newRows) {
            rows.clear();
            rows.addAll(newRows);
            fireTableDataChanged();
        }

        PresetRow getRow(int idx) {
            return rows.get(idx);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return 3; }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return "Show";
                case 1: return "Name";
                case 2: return "Scope";
                default: return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PresetRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0: return row.preset.isEnabled();
                case 1: return row.preset.getName();
                case 2: return row.scope;
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0) return;
            PresetRow row = rows.get(rowIndex);
            boolean enabled = (Boolean) aValue;
            row.preset.setEnabled(enabled);
            fireTableCellUpdated(rowIndex, columnIndex);
            persistEnabledToggle(row);
        }
    }

    private void onAdd() {
        addingNew = true;
        editingRow = -1;
        presetTable.clearSelection();
        Preset defaults = Preset.createDefault();
        nameField.setText("");
        scopeCombo.setSelectedItem("User");
        headerRegexesArea.setText(String.join("\n", defaults.getHeaderRegexes()));
        cookieRegexesArea.setText(String.join("\n", defaults.getCookieRegexes()));
        paramRegexesArea.setText(String.join("\n", defaults.getParamRegexes()));
        replacementStringField.setText(defaults.getReplacementString());
        ruleTableModel.setRules(defaults.getRedactionRules());
        templateArea.setText(defaults.getTemplate());
        setEditorEnabled(true);
        nameField.requestFocusInWindow();
    }

    private void onDelete() {
        int sel = presetTable.getSelectedRow();
        if (sel < 0 || "Built-in".equals(tableModel.getRow(sel).scope)) return;
        PresetRow row = tableModel.getRow(sel);
        int confirm = JOptionPane.showConfirmDialog(panel,
                "Delete preset \"" + row.preset.getName() + "\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        removePreset(row.preset.getName(), row.scope);
        onCancel();
        reloadTable();
    }

    private void onDuplicate() {
        int sel = presetTable.getSelectedRow();
        if (sel < 0) return;
        PresetRow row = tableModel.getRow(sel);
        addingNew = true;
        editingRow = -1;
        presetTable.clearSelection();
        populateEditor(row.preset, "User");
        nameField.setText(row.preset.getName() + " (copy)");
        setEditorEnabled(true);
        nameField.requestFocusInWindow();
    }

    private void onMoveUp() {
        int sel = presetTable.getSelectedRow();
        if (sel <= 0) return;
        swapAndPersistOrder(sel, sel - 1);
        presetTable.setRowSelectionInterval(sel - 1, sel - 1);
    }

    private void onMoveDown() {
        int sel = presetTable.getSelectedRow();
        if (sel < 0 || sel >= tableModel.getRowCount() - 1) return;
        swapAndPersistOrder(sel, sel + 1);
        presetTable.setRowSelectionInterval(sel + 1, sel + 1);
    }

    private void swapAndPersistOrder(int from, int to) {
        List<String> order = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            order.add(tableModel.getRow(i).preset.getName());
        }
        String moved = order.remove(from);
        order.add(to, moved);
        presetStore.setPresetOrder(order);
        reloadTable();
    }

    private void onApplyHotkey() {
        boolean enabled = hotkeyEnabledCheckbox.isSelected();
        String hotkey = hotkeyField.getText().trim();
        if (enabled && hotkey.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Shortcut cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        presetStore.setHotkeyEnabled(enabled);
        presetStore.setHotkeyString(hotkey);
        hotkeyManager.applyFromSettings();
    }

    private void onRestoreDefaults() {
        removePreset("Default", "User");
        removePreset("Default", "Project");
        onCancel();
        reloadTable();
    }

    private void onRuleAdd() {
        // Commit any in-progress cell edit before adding
        if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();
        ruleTableModel.addRule(new RedactionRule(RedactionRule.Type.REGEX, ""));
        int newRow = ruleTableModel.getRowCount() - 1;
        ruleTable.setRowSelectionInterval(newRow, newRow);
        ruleTable.editCellAt(newRow, 1);
    }

    private void onRuleDelete() {
        if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();
        int sel = ruleTable.getSelectedRow();
        if (sel >= 0) ruleTableModel.removeRule(sel);
    }

    private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Name cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Commit any in-progress cell edit in the rule table
        if (ruleTable.isEditing()) ruleTable.getCellEditor().stopCellEditing();

        String scope = (String) scopeCombo.getSelectedItem();
        List<String> headers = parseLines(headerRegexesArea.getText());
        List<String> cookies = parseLines(cookieRegexesArea.getText());
        List<String> params = parseLines(paramRegexesArea.getText());
        String replacement = replacementStringField.getText();
        List<RedactionRule> rules = ruleTableModel.getRules();
        String template = templateArea.getText();
        String regexError = RegexValidation.firstValidationError(headers, cookies, params, rules);
        if (regexError != null) {
            JOptionPane.showMessageDialog(panel, regexError, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Preserve the current enabled state if editing, default to true for new
        boolean enabled = true;
        if (editingRow >= 0) {
            enabled = tableModel.getRow(editingRow).preset.isEnabled();
        }

        Preset preset = new Preset(name, headers, cookies, params, rules, replacement, template, enabled);

        // If editing an existing row and the name/scope changed, remove the old one
        if (editingRow >= 0) {
            PresetRow oldRow = tableModel.getRow(editingRow);
            if (!oldRow.preset.getName().equals(name) || !oldRow.scope.equals(scope)) {
                removePreset(oldRow.preset.getName(), oldRow.scope);
            }
        }

        savePreset(preset, scope);
        addingNew = false;
        editingRow = -1;
        reloadTable();
        // Re-select the saved preset
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRow(i).preset.getName().equals(name)) {
                presetTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void onCancel() {
        addingNew = false;
        editingRow = -1;
        // Re-trigger selection to repopulate editor from current selection
        int sel = presetTable.getSelectedRow();
        if (sel >= 0) {
            showPresetInEditor(tableModel.getRow(sel));
        } else {
            clearEditor();
            setEditorEnabled(false);
        }
    }

    private void onSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        // Don't override editor if user is adding a new preset
        if (addingNew) return;
        int sel = presetTable.getSelectedRow();
        if (sel < 0) {
            deleteButton.setEnabled(false);
            duplicateButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            clearEditor();
            setEditorEnabled(false);
        } else {
            PresetRow row = tableModel.getRow(sel);
            boolean builtIn = "Built-in".equals(row.scope);
            deleteButton.setEnabled(!builtIn);
            duplicateButton.setEnabled(true);
            moveUpButton.setEnabled(sel > 0);
            moveDownButton.setEnabled(sel < tableModel.getRowCount() - 1);
            editingRow = builtIn ? -1 : sel;
            showPresetInEditor(row);
        }
    }

    private void showPresetInEditor(PresetRow row) {
        boolean builtIn = "Built-in".equals(row.scope);
        populateEditor(row.preset, row.scope);
        setEditorEnabled(!builtIn);
        saveButton.setEnabled(!builtIn);
    }

    private void persistEnabledToggle(PresetRow row) {
        if ("Built-in".equals(row.scope)) {
            // Save as a user-level preset to persist the enabled flag
            savePreset(row.preset, "User");
            reloadTable();
        } else {
            savePreset(row.preset, row.scope);
        }
    }

    private void populateEditor(Preset preset, String scope) {
        nameField.setText(preset.getName());
        scopeCombo.setSelectedItem(scope.equals("Project") ? "Project" : "User");
        headerRegexesArea.setText(String.join("\n", preset.getHeaderRegexes()));
        cookieRegexesArea.setText(String.join("\n", preset.getCookieRegexes()));
        paramRegexesArea.setText(String.join("\n", preset.getParamRegexes()));
        replacementStringField.setText(preset.getReplacementString());
        ruleTableModel.setRules(preset.getRedactionRules());
        templateArea.setText(preset.getTemplate());
    }

    private void clearEditor() {
        nameField.setText("");
        scopeCombo.setSelectedIndex(0);
        headerRegexesArea.setText("");
        cookieRegexesArea.setText("");
        paramRegexesArea.setText("");
        replacementStringField.setText(Preset.DEFAULT_REPLACEMENT);
        ruleTableModel.setRules(List.of());
        templateArea.setText("");
    }

    private void setEditorEnabled(boolean enabled) {
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
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private void reloadTable() {
        Map<String, PresetRow> byName = new LinkedHashMap<>();

        byName.put("Default", new PresetRow(Preset.createDefault(), "Built-in"));

        for (Preset p : presetStore.getUserPresets()) {
            byName.put(p.getName(), new PresetRow(p, "User"));
        }
        for (Preset p : presetStore.getProjectPresets()) {
            byName.put(p.getName(), new PresetRow(p, "Project"));
        }

        List<String> order = presetStore.getPresetOrder();
        List<PresetRow> rows = new ArrayList<>();
        for (String name : order) {
            PresetRow row = byName.remove(name);
            if (row != null) rows.add(row);
        }
        rows.addAll(byName.values());

        tableModel.setRows(rows);
    }

    private void savePreset(Preset preset, String scope) {
        if ("Project".equals(scope)) {
            List<Preset> list = new ArrayList<>(presetStore.getProjectPresets());
            list.removeIf(p -> p.getName().equals(preset.getName()));
            list.add(preset);
            presetStore.setProjectPresets(list);
        } else {
            List<Preset> list = new ArrayList<>(presetStore.getUserPresets());
            list.removeIf(p -> p.getName().equals(preset.getName()));
            list.add(preset);
            presetStore.setUserPresets(list);
        }
    }

    private void removePreset(String name, String scope) {
        if ("Project".equals(scope)) {
            List<Preset> list = new ArrayList<>(presetStore.getProjectPresets());
            list.removeIf(p -> p.getName().equals(name));
            presetStore.setProjectPresets(list);
        } else if ("User".equals(scope)) {
            List<Preset> list = new ArrayList<>(presetStore.getUserPresets());
            list.removeIf(p -> p.getName().equals(name));
            presetStore.setUserPresets(list);
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

    // Returns the native L&F checkbox icon scaled to targetSize, or null if no scaling is needed.
    private static Icon scaledNativeCheckboxIcon(int targetSize) {
        Icon raw = UIManager.getIcon("CheckBox.icon");
        if (raw == null || raw.getIconWidth() <= 0 || raw.getIconWidth() == targetSize) return null;
        return new Icon() {
            @Override public int getIconWidth() { return targetSize; }
            @Override public int getIconHeight() { return targetSize; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                double s = (double) targetSize / raw.getIconWidth();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.translate(x, y);
                g2.scale(s, s);
                raw.paintIcon(c, g2, 0, 0);
                g2.dispose();
            }
        };
    }

    // Renders the boolean "Show" column using the native L&F checkbox icon scaled to the table font.
    private static class NativeSizedBooleanRenderer extends JCheckBox implements TableCellRenderer {
        private Icon cachedIcon;

        NativeSizedBooleanRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            if (cachedIcon == null) {
                int target = table.getFontMetrics(table.getFont()).getHeight() - 2;
                cachedIcon = scaledNativeCheckboxIcon(target);
                if (cachedIcon != null) setIcon(cachedIcon);
            }
            return this;
        }
    }

    // Toggles the boolean value on click and immediately commits, using the native-sized renderer.
    private static class NativeSizedBooleanEditor extends AbstractCellEditor implements TableCellEditor {
        private boolean value;
        private final NativeSizedBooleanRenderer renderer = new NativeSizedBooleanRenderer();

        @Override public Object getCellEditorValue() { return value; }
        @Override public boolean isCellEditable(EventObject e) { return true; }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object val,
                boolean isSelected, int row, int col) {
            value = !Boolean.TRUE.equals(val);
            renderer.getTableCellRendererComponent(table, value, isSelected, false, row, col);
            SwingUtilities.invokeLater(this::stopCellEditing);
            return renderer;
        }
    }
}
