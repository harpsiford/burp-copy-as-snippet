package example.contextmenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MySettingsPanel implements SettingsPanel {

    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;
    private final JPanel panel;

    private final PresetTableModel tableModel;
    private final JTable presetTable;

    // Editor fields
    private final JTextField nameField;
    private final JComboBox<String> scopeCombo;
    private final JTextArea headerRegexesArea;
    private final JTextArea cookieRegexesArea;
    private final JTextArea templateArea;
    private final JButton deleteButton;
    private final JButton duplicateButton;
    private final JButton saveButton;
    private final JButton cancelButton;

    // Hotkey fields
    private final JCheckBox hotkeyEnabledCheckbox;
    private final JTextField hotkeyField;

    private int editingRow = -1;
    private boolean addingNew = false;

    public MySettingsPanel(MontoyaApi api, PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;

        // --- Preset table ---
        tableModel = new PresetTableModel();
        presetTable = new JTable(tableModel);
        presetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetTable.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        presetTable.getColumnModel().getColumn(0).setMaxWidth(50);
        presetTable.getColumnModel().getColumn(0).setMinWidth(50);
        JScrollPane tableScroll = new JScrollPane(presetTable);
        tableScroll.setPreferredSize(new Dimension(400, 150));

        // --- Action buttons ---
        JButton addButton = new JButton("Add");
        deleteButton = new JButton("Delete");
        duplicateButton = new JButton("Duplicate");
        JButton restoreDefaultsButton = new JButton("Restore defaults");

        deleteButton.setEnabled(false);
        duplicateButton.setEnabled(false);

        addButton.addActionListener(e -> onAdd());
        deleteButton.addActionListener(e -> onDelete());
        duplicateButton.addActionListener(e -> onDuplicate());
        restoreDefaultsButton.addActionListener(e -> onRestoreDefaults());

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonBar.add(addButton);
        buttonBar.add(deleteButton);
        buttonBar.add(duplicateButton);
        buttonBar.add(Box.createHorizontalStrut(20));
        buttonBar.add(restoreDefaultsButton);

        // --- Top section (table + buttons) ---
        JPanel topSection = new JPanel(new BorderLayout(0, 5));
        topSection.add(tableScroll, BorderLayout.CENTER);
        topSection.add(buttonBar, BorderLayout.SOUTH);

        // --- Editor form ---
        nameField = new JTextField();
        scopeCombo = new JComboBox<>(new String[]{"User", "Project"});

        headerRegexesArea = new JTextArea(6, 40);
        headerRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cookieRegexesArea = new JTextArea(4, 40);
        cookieRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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

        JLabel placeholderHint = new JLabel("Template placeholders: {{request}}, {{response}}");
        placeholderHint.setFont(placeholderHint.getFont().deriveFont(Font.ITALIC, 11f));

        editorPanel.add(nameRow);
        editorPanel.add(Box.createVerticalStrut(5));
        editorPanel.add(scopeRow);
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(labeledScroll("Header regexes (one per line):", headerRegexesArea));
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(labeledScroll("Cookie regexes (one per line):", cookieRegexesArea));
        editorPanel.add(Box.createVerticalStrut(10));
        editorPanel.add(placeholderHint);
        editorPanel.add(labeledScroll("Template:", templateArea));
        editorPanel.add(Box.createVerticalStrut(5));
        editorPanel.add(editorButtons);

        setEditorEnabled(false);

        // --- Hotkey section ---
        hotkeyEnabledCheckbox = new JCheckBox("Enable keyboard shortcut (works in HTTP message editor)");
        hotkeyEnabledCheckbox.setSelected(presetStore.isHotkeyEnabled());

        hotkeyField = new JTextField(presetStore.getHotkeyString(), 20);
        hotkeyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hotkeyField.setEnabled(presetStore.isHotkeyEnabled());

        hotkeyEnabledCheckbox.addActionListener(e -> {
            hotkeyField.setEnabled(hotkeyEnabledCheckbox.isSelected());
        });

        JLabel hotkeyHint = new JLabel("Suggested: " + PresetStore.DEFAULT_HOTKEY + "  (uses the first enabled preset)");
        hotkeyHint.setFont(hotkeyHint.getFont().deriveFont(Font.ITALIC, 11f));

        JButton hotkeyApplyButton = new JButton("Apply");
        hotkeyApplyButton.addActionListener(e -> onApplyHotkey());

        JPanel hotkeyRow = new JPanel(new BorderLayout(5, 0));
        hotkeyRow.add(new JLabel("Shortcut:"), BorderLayout.WEST);
        hotkeyRow.add(hotkeyField, BorderLayout.CENTER);
        hotkeyRow.add(hotkeyApplyButton, BorderLayout.EAST);
        hotkeyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel hotkeyPanel = new JPanel();
        hotkeyPanel.setLayout(new BoxLayout(hotkeyPanel, BoxLayout.Y_AXIS));
        hotkeyPanel.setBorder(BorderFactory.createTitledBorder("Keyboard shortcut"));
        hotkeyPanel.add(hotkeyEnabledCheckbox);
        hotkeyPanel.add(Box.createVerticalStrut(5));
        hotkeyPanel.add(hotkeyRow);
        hotkeyPanel.add(Box.createVerticalStrut(3));
        hotkeyPanel.add(hotkeyHint);

        // --- Main layout ---
        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
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

    // --- Table model ---

    private static class PresetRow {
        final Preset preset;
        final String scope; // "User", "Project", or "Built-in"

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

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

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

    // --- Actions ---

    private void onAdd() {
        addingNew = true;
        editingRow = -1;
        presetTable.clearSelection();
        Preset defaults = Preset.createDefault();
        nameField.setText("");
        scopeCombo.setSelectedItem("User");
        headerRegexesArea.setText(String.join("\n", defaults.getHeaderRegexes()));
        cookieRegexesArea.setText(String.join("\n", defaults.getCookieRegexes()));
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

    private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Name cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String scope = (String) scopeCombo.getSelectedItem();
        List<String> headers = parseLines(headerRegexesArea.getText());
        List<String> cookies = parseLines(cookieRegexesArea.getText());
        String template = templateArea.getText();

        // Preserve the current enabled state if editing, default to true for new
        boolean enabled = true;
        if (editingRow >= 0) {
            enabled = tableModel.getRow(editingRow).preset.isEnabled();
        }

        Preset preset = new Preset(name, headers, cookies, template, enabled);

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
            clearEditor();
            setEditorEnabled(false);
        } else {
            PresetRow row = tableModel.getRow(sel);
            boolean builtIn = "Built-in".equals(row.scope);
            deleteButton.setEnabled(!builtIn);
            duplicateButton.setEnabled(true);
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

    // --- Helpers ---

    private void populateEditor(Preset preset, String scope) {
        nameField.setText(preset.getName());
        scopeCombo.setSelectedItem(scope.equals("Project") ? "Project" : "User");
        headerRegexesArea.setText(String.join("\n", preset.getHeaderRegexes()));
        cookieRegexesArea.setText(String.join("\n", preset.getCookieRegexes()));
        templateArea.setText(preset.getTemplate());
    }

    private void clearEditor() {
        nameField.setText("");
        scopeCombo.setSelectedIndex(0);
        headerRegexesArea.setText("");
        cookieRegexesArea.setText("");
        templateArea.setText("");
    }

    private void setEditorEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        scopeCombo.setEnabled(enabled);
        headerRegexesArea.setEnabled(enabled);
        cookieRegexesArea.setEnabled(enabled);
        templateArea.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private void reloadTable() {
        List<PresetRow> rows = new ArrayList<>();

        boolean defaultOverridden = false;
        for (Preset p : presetStore.getUserPresets()) {
            if ("Default".equals(p.getName())) defaultOverridden = true;
            rows.add(new PresetRow(p, "User"));
        }
        for (Preset p : presetStore.getProjectPresets()) {
            if ("Default".equals(p.getName())) defaultOverridden = true;
            rows.add(new PresetRow(p, "Project"));
        }

        if (!defaultOverridden) {
            rows.add(0, new PresetRow(Preset.createDefault(), "Built-in"));
        }

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
}
