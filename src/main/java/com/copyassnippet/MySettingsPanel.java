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
import java.util.List;

public class MySettingsPanel implements SettingsPanel {

    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;
    private final JPanel panel;

    private final PresetTableModel tableModel;
    private final JTable presetTable;

    private final PresetFormPanel presetFormPanel;
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
        presetFormPanel = new PresetFormPanel();
        presetFormPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> onCancel());

        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editorButtons.add(saveButton);
        editorButtons.add(cancelButton);
        editorButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel editorPanel = new JPanel(new BorderLayout(0, 0));
        editorPanel.setBorder(BorderFactory.createTitledBorder("Preset editor"));

        editorPanel.add(presetFormPanel, BorderLayout.CENTER);
        editorPanel.add(editorButtons, BorderLayout.SOUTH);

        setEditorEnabled(false);

        // --- Hotkey section ---
        hotkeyEnabledCheckbox = new JCheckBox("Enable keyboard shortcut (works in HTTP message editor)");
        hotkeyEnabledCheckbox.setSelected(presetStore.isHotkeyEnabled());
        Icon hkIcon = scaledNativeCheckboxIcon(
                hotkeyEnabledCheckbox.getFontMetrics(hotkeyEnabledCheckbox.getFont()).getHeight() - 2);
        if (hkIcon != null) hotkeyEnabledCheckbox.setIcon(hkIcon);

        hotkeyField = new JTextField(presetStore.getHotkeyString(), 20);
        hotkeyField.setFont(UIManager.getFont("TextField.font"));
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

    private static class PresetRow {
        final Preset preset;
        final PresetScope scope;

        PresetRow(Preset preset, PresetScope scope) {
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
                case 2: return row.scope.displayName();
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
        presetFormPanel.setFormData(PresetFormMapper.forNewPreset());
        setEditorEnabled(true);
        presetFormPanel.focusNameField();
    }

    private void onDelete() {
        int sel = presetTable.getSelectedRow();
        if (sel < 0 || tableModel.getRow(sel).scope.isBuiltIn()) return;
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
        presetFormPanel.setFormData(
                PresetFormMapper.fromPreset(row.preset, PresetScope.USER).withName(row.preset.getName() + " (copy)")
        );
        setEditorEnabled(true);
        presetFormPanel.focusNameField();
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
        removePreset("Default", PresetScope.USER);
        removePreset("Default", PresetScope.PROJECT);
        onCancel();
        reloadTable();
    }

    private void onSave() {
        PresetFormData formData = presetFormPanel.getFormData();
        String validationError = PresetFormMapper.firstValidationError(formData);
        if (validationError != null) {
            JOptionPane.showMessageDialog(panel, validationError, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PresetScope scope = formData.getScope();

        // Preserve the current enabled state if editing, default to true for new
        boolean enabled = true;
        if (editingRow >= 0) {
            enabled = tableModel.getRow(editingRow).preset.isEnabled();
        }

        Preset preset = PresetFormMapper.toPreset(formData, enabled);
        String savedName = preset.getName();

        // If editing an existing row and the name/scope changed, remove the old one
        if (editingRow >= 0) {
            PresetRow oldRow = tableModel.getRow(editingRow);
            if (!oldRow.preset.getName().equals(savedName) || oldRow.scope != scope) {
                removePreset(oldRow.preset.getName(), oldRow.scope);
            }
        }

        savePreset(preset, scope);
        addingNew = false;
        editingRow = -1;
        reloadTable();
        // Re-select the saved preset
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRow(i).preset.getName().equals(savedName)) {
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
            boolean builtIn = row.scope.isBuiltIn();
            deleteButton.setEnabled(!builtIn);
            duplicateButton.setEnabled(true);
            moveUpButton.setEnabled(sel > 0);
            moveDownButton.setEnabled(sel < tableModel.getRowCount() - 1);
            editingRow = builtIn ? -1 : sel;
            showPresetInEditor(row);
        }
    }

    private void showPresetInEditor(PresetRow row) {
        boolean builtIn = row.scope.isBuiltIn();
        populateEditor(row.preset, row.scope);
        setEditorEnabled(!builtIn);
        saveButton.setEnabled(!builtIn);
    }

    private void persistEnabledToggle(PresetRow row) {
        if (row.scope.isBuiltIn()) {
            // Save as a user-level preset to persist the enabled flag
            savePreset(row.preset, PresetScope.USER);
            reloadTable();
        } else {
            savePreset(row.preset, row.scope);
        }
    }

    private void populateEditor(Preset preset, PresetScope scope) {
        presetFormPanel.setFormData(PresetFormMapper.fromPreset(preset, scope));
    }

    private void clearEditor() {
        presetFormPanel.setFormData(PresetFormMapper.empty());
    }

    private void setEditorEnabled(boolean enabled) {
        presetFormPanel.setFormEnabled(enabled);
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private void reloadTable() {
        List<PresetRow> rows = new ArrayList<>();
        for (PresetResolver.ResolvedPreset resolvedPreset : presetStore.getResolvedPresetEntries()) {
            rows.add(new PresetRow(resolvedPreset.getPreset(), resolvedPreset.getScope()));
        }

        tableModel.setRows(rows);
    }

    private void savePreset(Preset preset, PresetScope scope) {
        switch (scope.toEditableScope()) {
            case PROJECT:
                List<Preset> projectList = new ArrayList<>(presetStore.getProjectPresets());
                projectList.removeIf(p -> p.getName().equals(preset.getName()));
                projectList.add(preset);
                presetStore.setProjectPresets(projectList);
                break;
            case USER:
                List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
                userList.removeIf(p -> p.getName().equals(preset.getName()));
                userList.add(preset);
                presetStore.setUserPresets(userList);
                break;
            default:
                throw new IllegalStateException("Unexpected editable scope: " + scope);
        }
    }

    private void removePreset(String name, PresetScope scope) {
        switch (scope.toEditableScope()) {
            case PROJECT:
                List<Preset> projectList = new ArrayList<>(presetStore.getProjectPresets());
                projectList.removeIf(p -> p.getName().equals(name));
                presetStore.setProjectPresets(projectList);
                break;
            case USER:
                List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
                userList.removeIf(p -> p.getName().equals(name));
                presetStore.setUserPresets(userList);
                break;
            default:
                throw new IllegalStateException("Unexpected editable scope: " + scope);
        }
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
