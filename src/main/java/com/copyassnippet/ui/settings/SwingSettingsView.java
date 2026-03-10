package com.copyassnippet.ui.settings;

import com.copyassnippet.hotkey.HotkeyDefinition;
import com.copyassnippet.hotkey.HotkeySettingsState;
import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.service.PresetResolver;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.lang.reflect.InvocationTargetException;

final class SwingSettingsView implements SettingsView {
    private Listener listener;
    private final Consumer<Component> themeApplier;
    private final Function<Component, Window> windowForComponent;
    private final Supplier<Frame> suiteFrameSupplier;

    private final JPanel panel;
    private final PresetTableModel tableModel;
    private final JTable presetTable;

    private final PresetFormPanel presetFormPanel;
    private final JButton addButton;
    private final JButton deleteButton;
    private final JButton duplicateButton;
    private final JButton editButton;
    private final JButton loadButton;
    private final JButton exportButton;
    private final JButton moveUpButton;
    private final JButton moveDownButton;
    private final JButton restoreDefaultsButton;
    private final JButton saveButton;
    private final JButton cancelButton;
    private JDialog editorDialog;
    private JDialog hotkeyCaptureDialog;
    private final JPanel editorPanel;

    private final JCheckBox hotkeyEnabledCheckbox;
    private final JTextField hotkeyField;
    private final JButton hotkeyChangeButton;
    private boolean busy;

    SwingSettingsView(
            Consumer<Component> themeApplier,
            Function<Component, Window> windowForComponent,
            Supplier<Frame> suiteFrameSupplier
    ) {
        this.themeApplier = themeApplier;
        this.windowForComponent = windowForComponent;
        this.suiteFrameSupplier = suiteFrameSupplier;
        tableModel = new PresetTableModel();
        tableModel.setEnabledToggleListener(this::notifyPresetEnabledToggled);

        presetTable = new JTable(tableModel);
        presetTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetTable.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        presetTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 2) {
                    return;
                }

                int row = presetTable.rowAtPoint(event.getPoint());
                int column = presetTable.columnAtPoint(event.getPoint());
                if (row < 0 || column == 0) {
                    return;
                }

                presetTable.setRowSelectionInterval(row, row);
                notifyEdit();
            }
        });
        presetTable.getColumnModel().getColumn(0).setMaxWidth(50);
        presetTable.getColumnModel().getColumn(0).setMinWidth(50);
        presetTable.getColumnModel().getColumn(0).setCellRenderer(new NativeSizedBooleanRenderer());
        presetTable.getColumnModel().getColumn(0).setCellEditor(new NativeSizedBooleanEditor());
        presetTable.setIntercellSpacing(new Dimension(0, 0));
        presetTable.setRowHeight(presetTable.getFontMetrics(presetTable.getFont()).getHeight() + 2);

        JScrollPane tableScroll = new JScrollPane(presetTable);
        tableScroll.setPreferredSize(new Dimension(400, 150));

        addButton = new JButton("Add");
        loadButton = new JButton("Load ...");
        exportButton = new JButton("Export ...");
        deleteButton = new JButton("Remove");
        duplicateButton = new JButton("Duplicate");
        editButton = new JButton("Edit");
        moveUpButton = new JButton("Up");
        moveDownButton = new JButton("Down");
        restoreDefaultsButton = new JButton("Restore defaults");

        loadButton.addActionListener(e -> notifyLoadPresets());
        exportButton.addActionListener(e -> notifyExportPreset());
        addButton.addActionListener(e -> notifyAdd());
        deleteButton.addActionListener(e -> notifyDelete());
        duplicateButton.addActionListener(e -> notifyDuplicate());
        editButton.addActionListener(e -> notifyEdit());
        moveUpButton.addActionListener(e -> notifyMoveUp());
        moveDownButton.addActionListener(e -> notifyMoveDown());
        restoreDefaultsButton.addActionListener(e -> notifyRestoreDefaults());

        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.Y_AXIS));
        for (JButton button : new JButton[]{addButton, editButton, deleteButton, duplicateButton, loadButton, exportButton, moveUpButton, moveDownButton}) {
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
            buttonBar.add(button);
            buttonBar.add(Box.createVerticalStrut(3));
        }
        buttonBar.add(Box.createVerticalGlue());

        JPanel topSection = new JPanel(new BorderLayout(5, 0));
        topSection.add(buttonBar, BorderLayout.WEST);
        topSection.add(tableScroll, BorderLayout.CENTER);

        presetFormPanel = new PresetFormPanel();
        presetFormPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        saveButton.addActionListener(e -> notifySave());
        cancelButton.addActionListener(e -> notifyCancel());

        JPanel editorButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        editorButtons.add(saveButton);
        editorButtons.add(cancelButton);
        editorButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        editorPanel = new JPanel(new BorderLayout(0, 0));
        editorPanel.add(presetFormPanel, BorderLayout.CENTER);
        editorPanel.add(editorButtons, BorderLayout.SOUTH);
        editorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        hotkeyEnabledCheckbox = new JCheckBox("Enable keyboard shortcut (works in the HTTP message editor, uses the first active preset)");
        Icon hotkeyIcon = scaledNativeCheckboxIcon(
                hotkeyEnabledCheckbox.getFontMetrics(hotkeyEnabledCheckbox.getFont()).getHeight() - 2);
        if (hotkeyIcon != null) {
            hotkeyEnabledCheckbox.setIcon(hotkeyIcon);
        }

        hotkeyField = new JTextField("", 20);
        hotkeyField.setFont(UIManager.getFont("TextField.font"));
        hotkeyField.setEditable(false);
        hotkeyField.setFocusable(false);

        hotkeyChangeButton = new JButton("Change...");
        hotkeyChangeButton.addActionListener(e -> showHotkeyCaptureDialog());
        hotkeyEnabledCheckbox.addActionListener(e -> {
            boolean enabled = hotkeyEnabledCheckbox.isSelected();
            hotkeyField.setEnabled(enabled);
            hotkeyChangeButton.setEnabled(enabled);
            notifyHotkeyEnabledToggled(enabled);
        });

        JPanel hotkeyRow = new JPanel(new BorderLayout(5, 0));
        hotkeyRow.add(new JLabel("Shortcut:"), BorderLayout.WEST);
        hotkeyRow.add(hotkeyField, BorderLayout.CENTER);
        hotkeyRow.add(hotkeyChangeButton, BorderLayout.EAST);
        hotkeyRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel hotkeyPanel = new JPanel();
        hotkeyPanel.setLayout(new BoxLayout(hotkeyPanel, BoxLayout.Y_AXIS));
        hotkeyEnabledCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeyPanel.add(hotkeyEnabledCheckbox);
        hotkeyPanel.add(Box.createVerticalStrut(5));
        hotkeyPanel.add(hotkeyRow);
        hotkeyPanel.add(Box.createVerticalStrut(3));
        JPanel hotkeySection = sectionPanel("Keyboard shortcut", hotkeyPanel);

        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BoxLayout(topWrapper, BoxLayout.Y_AXIS));
        JPanel presetsSection = sectionPanel("Presets", topSection);
        presetsSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        hotkeySection.setAlignmentX(Component.LEFT_ALIGNMENT);
        topWrapper.add(presetsSection);
        topWrapper.add(Box.createVerticalStrut(10));
        topWrapper.add(hotkeySection);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomActions.add(restoreDefaultsButton);

        panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(topWrapper, BorderLayout.NORTH);
        panel.add(bottomActions, BorderLayout.SOUTH);
        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                notifyViewShown();
            }
        });

        setPresetActions(false, false, false, false, false, false);
        setEditorEnabled(false);
    }

    @Override
    public JPanel uiComponent() {
        return panel;
    }

    void dispose() {
        if (SwingUtilities.isEventDispatchThread()) {
            disposeDialogs();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(this::disposeDialogs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Unable to dispose Copy as snippet dialogs.", exception.getCause());
        }
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void setRows(List<PresetResolver.ResolvedPreset> rows) {
        tableModel.setRows(rows);
    }

    @Override
    public int rowCount() {
        return tableModel.getRowCount();
    }

    @Override
    public PresetResolver.ResolvedPreset rowAt(int index) {
        return tableModel.getRow(index);
    }

    @Override
    public int selectedRow() {
        return presetTable.getSelectedRow();
    }

    @Override
    public void selectRow(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= tableModel.getRowCount()) {
            return;
        }
        presetTable.setRowSelectionInterval(rowIndex, rowIndex);
    }

    @Override
    public void setPresetActions(boolean deleteEnabled, boolean duplicateEnabled, boolean editEnabled, boolean exportEnabled, boolean moveUpEnabled, boolean moveDownEnabled) {
        deleteButton.setEnabled(!busy && deleteEnabled);
        duplicateButton.setEnabled(!busy && duplicateEnabled);
        editButton.setEnabled(!busy && editEnabled);
        exportButton.setEnabled(!busy && exportEnabled);
        moveUpButton.setEnabled(!busy && moveUpEnabled);
        moveDownButton.setEnabled(!busy && moveDownEnabled);
    }

    @Override
    public void setBusy(boolean busy) {
        this.busy = busy;
        Cursor cursor = busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null;

        panel.setCursor(cursor);
        presetTable.setCursor(cursor);
        addButton.setEnabled(!busy);
        loadButton.setEnabled(!busy);
        restoreDefaultsButton.setEnabled(!busy);
        presetTable.setEnabled(!busy);

        if (busy) {
            deleteButton.setEnabled(false);
            duplicateButton.setEnabled(false);
            editButton.setEnabled(false);
            exportButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
        }
    }

    @Override
    public void setEditorFormData(PresetFormData formData) {
        presetFormPanel.setFormData(formData);
    }

    @Override
    public PresetFormData getEditorFormData() {
        return presetFormPanel.getFormData();
    }

    @Override
    public void setEditorEnabled(boolean enabled) {
        presetFormPanel.setFormEnabled(enabled);
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
        if (enabled) {
            SwingUtilities.invokeLater(() -> {
                JDialog dialog = ensureEditorDialog();
                if (!editorDialog.isVisible()) {
                    dialog.pack();
                    dialog.setLocationRelativeTo(dialogOwner());
                    dialog.setVisible(true);
                } else {
                    dialog.toFront();
                    dialog.requestFocus();
                }
            });
            return;
        }

        if (editorDialog != null && editorDialog.isVisible()) {
            editorDialog.setVisible(false);
        }
    }

    @Override
    public void focusEditorNameField() {
        SwingUtilities.invokeLater(presetFormPanel::focusNameField);
    }

    @Override
    public void showValidationWarning(String message) {
        JOptionPane.showMessageDialog(dialogParentComponent(), message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public boolean confirmDelete(String presetName) {
        int confirm = JOptionPane.showConfirmDialog(dialogParentComponent(),
                "Delete preset \"" + presetName + "\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        return confirm == JOptionPane.YES_OPTION;
    }

    @Override
    public List<File> choosePresetFilesToLoad() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load presets");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        int selection = chooser.showOpenDialog(dialogParentComponent());
        if (selection != JFileChooser.APPROVE_OPTION) {
            return List.of();
        }

        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0) {
            File singleFile = chooser.getSelectedFile();
            return singleFile == null ? List.of() : List.of(singleFile);
        }
        return List.of(files);
    }

    @Override
    public File choosePresetFileToExport(String suggestedFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export preset");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        chooser.setSelectedFile(new File(defaultExportFileName(suggestedFileName)));
        int selection = chooser.showSaveDialog(dialogParentComponent());
        if (selection != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selectedFile = appendJsonExtension(chooser.getSelectedFile());
        if (selectedFile.exists()) {
            int overwrite = JOptionPane.showConfirmDialog(
                    dialogParentComponent(),
                    "Overwrite \"" + selectedFile.getName() + "\"?",
                    "Confirm overwrite",
                    JOptionPane.YES_NO_OPTION
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        return selectedFile;
    }

    @Override
    public List<PresetApplicationService.ImportPlanRow> resolveImportConflicts(List<PresetApplicationService.ImportPlanRow> rows) {
        JTable table = new JTable(new ImportPlanTableModel(rows));
        table.setFillsViewportHeight(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof PresetApplicationService.ImportAction
                        ? value.toString()
                        : "");
            }
        });
        JComboBox<PresetApplicationService.ImportAction> comboBox = new JComboBox<>(
                new PresetApplicationService.ImportAction[]{
                        PresetApplicationService.ImportAction.REPLACE,
                        PresetApplicationService.ImportAction.KEEP_BOTH
                }
        );
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(comboBox));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(700, Math.min(220, table.getRowHeight() * (rows.size() + 1) + 24)));

        int choice = JOptionPane.showConfirmDialog(
                dialogParentComponent(),
                scrollPane,
                "Resolve preset name conflicts",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        return choice == JOptionPane.OK_OPTION ? rows : null;
    }

    @Override
    public HotkeySettingsState getHotkeyState() {
        return new HotkeySettingsState(hotkeyEnabledCheckbox.isSelected(), hotkeyField.getText().trim());
    }

    @Override
    public void setHotkeyState(HotkeySettingsState state) {
        hotkeyEnabledCheckbox.setSelected(state.isEnabled());
        hotkeyField.setText(state.getHotkey());
        hotkeyField.setEnabled(state.isEnabled());
        hotkeyChangeButton.setEnabled(state.isEnabled());
    }

    private void onSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (listener != null) {
            listener.onSelectionChanged();
        }
    }

    private void notifyAdd() {
        if (listener != null) {
            listener.onAdd();
        }
    }

    private void notifyDelete() {
        if (listener != null) {
            listener.onDelete();
        }
    }

    private void notifyDuplicate() {
        if (listener != null) {
            listener.onDuplicate();
        }
    }

    private void notifyEdit() {
        if (listener != null) {
            listener.onEdit();
        }
    }

    private void notifyMoveUp() {
        if (listener != null) {
            listener.onMoveUp();
        }
    }

    private void notifyMoveDown() {
        if (listener != null) {
            listener.onMoveDown();
        }
    }

    private void notifyLoadPresets() {
        if (listener != null) {
            listener.onLoadPresets();
        }
    }

    private void notifyExportPreset() {
        if (listener != null) {
            listener.onExportPreset();
        }
    }

    private void notifyRestoreDefaults() {
        if (listener != null) {
            listener.onRestoreDefaults();
        }
    }

    private void notifySave() {
        if (listener != null) {
            listener.onSave();
        }
    }

    private void notifyCancel() {
        if (listener != null) {
            listener.onCancel();
        }
    }

    private void notifyHotkeyEnabledToggled(boolean enabled) {
        if (listener != null) {
            listener.onHotkeyEnabledToggled(enabled);
        }
    }

    private void notifyPresetEnabledToggled(int rowIndex, boolean enabled) {
        if (listener != null) {
            listener.onPresetEnabledToggled(rowIndex, enabled);
        }
    }

    private void notifyViewShown() {
        if (listener != null) {
            listener.onViewShown();
        }
    }

    private JDialog ensureEditorDialog() {
        Window owner = dialogOwner();
        if (editorDialog == null || (owner != null && editorDialog.getOwner() != owner)) {
            if (editorDialog != null) {
                editorDialog.dispose();
            }
            editorDialog = createEditorDialog(owner);
        }
        return editorDialog;
    }

    private JDialog createEditorDialog(Window owner) {
        JDialog dialog;
        if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog) owner, "Preset Editor", Dialog.ModalityType.DOCUMENT_MODAL);
        } else if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner, "Preset Editor", Dialog.ModalityType.DOCUMENT_MODAL);
        } else {
            dialog = new JDialog(requireSuiteFrame(), "Preset Editor", Dialog.ModalityType.DOCUMENT_MODAL);
        }
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setContentPane(editorPanel);
        dialog.setMinimumSize(new Dimension(820, 650));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                notifyCancel();
            }
        });
        dialog.getRootPane().setDefaultButton(saveButton);
        applyTheme(dialog);
        return dialog;
    }

    private void showHotkeyCaptureDialog() {
        String capturedHotkey = captureHotkey(hotkeyField.getText().trim());
        if (capturedHotkey == null) {
            return;
        }

        hotkeyField.setText(capturedHotkey);
        if (listener != null) {
            listener.onApplyHotkey();
        }
    }

    private String captureHotkey(String currentHotkey) {
        Window owner = dialogOwner();
        JDialog dialog = createHotkeyCaptureDialog(owner);
        hotkeyCaptureDialog = dialog;

        JTextField previewField = new JTextField(currentHotkey, 20);
        previewField.setEditable(false);
        previewField.setFocusable(false);
        previewField.setFont(UIManager.getFont("TextField.font"));

        JLabel noteLabel = new JLabel(primaryModifierNote());
        Color infoColor = UIManager.getColor("Label.disabledForeground");
        if (infoColor != null) {
            noteLabel.setForeground(infoColor);
        }

        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        AtomicReference<String> pendingHotkey = new AtomicReference<>(HotkeyDefinition.isValid(currentHotkey) ? currentHotkey : null);
        AtomicReference<String> appliedHotkey = new AtomicReference<>(null);
        applyButton.setEnabled(pendingHotkey.get() != null);

        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Press the new keyboard shortcut.");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewField.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(applyButton);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(cancelButton);

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(previewField);
        content.add(Box.createVerticalStrut(6));
        content.add(noteLabel);
        content.add(Box.createVerticalStrut(10));
        content.add(buttons);

        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(applyButton);
        dialog.pack();
        dialog.setMinimumSize(dialog.getPreferredSize());
        dialog.setLocationRelativeTo(dialogOwner());
        applyTheme(dialog);

        applyButton.addActionListener(e -> {
            appliedHotkey.set(pendingHotkey.get());
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        KeyEventDispatcher dispatcher = event -> {
            if (!dialog.isShowing() || !dialog.isActive() || event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }

            if (event.getModifiersEx() == 0 && event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                dialog.dispose();
                return true;
            }

            if (isModifierKey(event.getKeyCode())) {
                String preview = modifierPreview(event.getModifiersEx());
                previewField.setText(preview);
                noteLabel.setText(primaryModifierNote());
                applyButton.setEnabled(false);
                return true;
            }

            String shortcut = toShortcut(event);
            if (shortcut == null) {
                noteLabel.setText(invalidShortcutMessage());
                applyButton.setEnabled(false);
                return true;
            }

            pendingHotkey.set(shortcut);
            previewField.setText(shortcut);
            noteLabel.setText(primaryModifierNote());
            applyButton.setEnabled(true);
            return true;
        };

        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(dispatcher);
        try {
            SwingUtilities.invokeLater(() -> dialog.requestFocus());
            dialog.setVisible(true);
        } finally {
            if (hotkeyCaptureDialog == dialog) {
                hotkeyCaptureDialog = null;
            }
            focusManager.removeKeyEventDispatcher(dispatcher);
        }
        return appliedHotkey.get();
    }

    private JDialog createHotkeyCaptureDialog(Window owner) {
        JDialog dialog;
        if (owner instanceof Dialog) {
            dialog = new JDialog((Dialog) owner, "Record keyboard shortcut", Dialog.ModalityType.DOCUMENT_MODAL);
        } else if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner, "Record keyboard shortcut", Dialog.ModalityType.DOCUMENT_MODAL);
        } else {
            dialog = new JDialog(requireSuiteFrame(), "Record keyboard shortcut", Dialog.ModalityType.DOCUMENT_MODAL);
        }
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return dialog;
    }

    private void applyTheme(Component component) {
        if (themeApplier != null) {
            themeApplier.accept(component);
        }
    }

    private Component dialogParentComponent() {
        if (editorDialog != null && editorDialog.isVisible()) {
            return editorDialog;
        }
        return dialogOwner();
    }

    private Window dialogOwner() {
        if (windowForComponent != null) {
            Window owner = windowForComponent.apply(panel);
            if (owner != null) {
                return owner;
            }
        }
        return requireSuiteFrame();
    }

    private Frame requireSuiteFrame() {
        if (suiteFrameSupplier == null) {
            throw new IllegalStateException("Unable to resolve the Burp Suite frame.");
        }

        Frame suiteFrame = suiteFrameSupplier.get();
        if (suiteFrame == null) {
            throw new IllegalStateException("Unable to resolve the Burp Suite frame.");
        }
        return suiteFrame;
    }

    private void disposeDialogs() {
        if (hotkeyCaptureDialog != null) {
            hotkeyCaptureDialog.dispose();
            hotkeyCaptureDialog = null;
        }
        if (editorDialog != null) {
            editorDialog.dispose();
            editorDialog = null;
        }
    }

    private static String primaryModifierNote() {
        return "Use Ctrl with an additional key. Alt and Shift are optional.";
    }

    private static String invalidShortcutMessage() {
        return "That key combination is not supported by Burp. Try a different key.";
    }

    private static boolean isModifierKey(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
    }

    private static String modifierPreview(int modifiersEx) {
        List<String> parts = modifierParts(modifiersEx);
        return parts.isEmpty() ? "" : String.join("+", parts);
    }

    private static String toShortcut(KeyEvent event) {
        int modifiersEx = event.getModifiersEx();
        if (!hasPrimaryModifier(modifiersEx)) {
            return null;
        }

        String keyToken = keyToken(event.getKeyCode());
        if (keyToken == null) {
            return null;
        }

        List<String> parts = modifierParts(modifiersEx);
        parts.add(keyToken);
        String shortcut = String.join("+", parts);
        return HotkeyDefinition.isValid(shortcut) ? shortcut : null;
    }

    private static boolean hasPrimaryModifier(int modifiersEx) {
        return (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0
                || (isMacOs() && (modifiersEx & InputEvent.META_DOWN_MASK) != 0);
    }

    private static List<String> modifierParts(int modifiersEx) {
        List<String> parts = new ArrayList<>();
        if ((modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0
                || (isMacOs() && (modifiersEx & InputEvent.META_DOWN_MASK) != 0)) {
            parts.add("Ctrl");
        }

        if ((modifiersEx & InputEvent.ALT_DOWN_MASK) != 0) {
            parts.add("Alt");
        }
        if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0) {
            parts.add("Shift");
        }
        return parts;
    }

    private static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static String keyToken(int keyCode) {
        if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) ('A' + (keyCode - KeyEvent.VK_A)));
        }
        if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
            return String.valueOf((char) ('0' + (keyCode - KeyEvent.VK_0)));
        }
        if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F24) {
            return "F" + (keyCode - KeyEvent.VK_F1 + 1);
        }

        return switch (keyCode) {
            case KeyEvent.VK_ENTER -> "Enter";
            case KeyEvent.VK_SPACE -> "Space";
            case KeyEvent.VK_TAB -> "Tab";
            case KeyEvent.VK_BACK_SPACE -> "Backspace";
            case KeyEvent.VK_DELETE -> "Delete";
            case KeyEvent.VK_UP -> "Up";
            case KeyEvent.VK_DOWN -> "Down";
            case KeyEvent.VK_LEFT -> "Left";
            case KeyEvent.VK_RIGHT -> "Right";
            case KeyEvent.VK_HOME -> "Home";
            case KeyEvent.VK_END -> "End";
            case KeyEvent.VK_PAGE_UP -> "PageUp";
            case KeyEvent.VK_PAGE_DOWN -> "PageDown";
            case KeyEvent.VK_INSERT -> "Insert";
            case KeyEvent.VK_ESCAPE -> "Escape";
            case KeyEvent.VK_BACK_QUOTE -> "BackQuote";
            case KeyEvent.VK_MINUS -> "Minus";
            case KeyEvent.VK_EQUALS -> "Equals";
            case KeyEvent.VK_OPEN_BRACKET -> "OpenBracket";
            case KeyEvent.VK_CLOSE_BRACKET -> "CloseBracket";
            case KeyEvent.VK_BACK_SLASH -> "BackSlash";
            case KeyEvent.VK_SEMICOLON -> "Semicolon";
            case KeyEvent.VK_QUOTE -> "Quote";
            case KeyEvent.VK_COMMA -> "Comma";
            case KeyEvent.VK_PERIOD -> "Period";
            case KeyEvent.VK_SLASH -> "Slash";
            default -> {
                String keyText = KeyEvent.getKeyText(keyCode);
                if (keyText == null || keyText.isBlank() || keyText.toLowerCase(Locale.ROOT).contains("unknown")) {
                    yield null;
                }
                yield keyText.replace(" ", "");
            }
        };
    }

    private static Icon scaledNativeCheckboxIcon(int targetSize) {
        Icon raw = UIManager.getIcon("CheckBox.icon");
        if (raw == null || raw.getIconWidth() <= 0 || raw.getIconWidth() == targetSize) {
            return null;
        }

        return new Icon() {
            @Override
            public int getIconWidth() {
                return targetSize;
            }

            @Override
            public int getIconHeight() {
                return targetSize;
            }

            @Override
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                double scale = (double) targetSize / raw.getIconWidth();
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics2D.translate(x, y);
                graphics2D.scale(scale, scale);
                raw.paintIcon(component, graphics2D, 0, 0);
                graphics2D.dispose();
            }
        };
    }

    private static JPanel sectionPanel(String title, JComponent content) {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(sectionHeader(title), BorderLayout.NORTH);
        JPanel indentedContent = new JPanel(new BorderLayout());
        indentedContent.setOpaque(false);
        indentedContent.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        indentedContent.add(content, BorderLayout.CENTER);
        section.add(indentedContent, BorderLayout.CENTER);
        return section;
    }

    private static JLabel sectionHeader(String title) {
        JLabel label = new JLabel(title);
        Font baseFont = UIManager.getFont("Label.font");
        Font sourceFont = baseFont != null ? baseFont : label.getFont();
        label.setFont(sourceFont.deriveFont(Font.BOLD, sourceFont.getSize2D() + 2f));
        return label;
    }

    private static File appendJsonExtension(File file) {
        if (file == null || file.getName().toLowerCase().endsWith(".json")) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + ".json");
    }

    private static String defaultExportFileName(String presetName) {
        String sanitized = presetName == null ? "" : presetName.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
        if (sanitized.isEmpty()) {
            sanitized = "preset";
        }
        return "burp-copy-as-snippet-preset-" + sanitized + ".json";
    }

    private interface EnabledToggleListener {
        void onEnabledToggled(int rowIndex, boolean enabled);
    }

    private static class ImportPlanTableModel extends AbstractTableModel {
        private final List<PresetApplicationService.ImportPlanRow> rows;

        ImportPlanTableModel(List<PresetApplicationService.ImportPlanRow> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "File";
                case 1:
                    return "Preset";
                case 2:
                    return "Conflicts with";
                case 3:
                    return "Action";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? PresetApplicationService.ImportAction.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3 && rows.get(rowIndex).hasNameConflict();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PresetApplicationService.ImportPlanRow row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.getSourceName();
                case 1:
                    return row.getPreset().getName();
                case 2:
                    return row.getConflictingPresetName() != null ? row.getConflictingPresetName() : "";
                case 3:
                    return row.getAction();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != 3 || !(value instanceof PresetApplicationService.ImportAction)) {
                return;
            }
            rows.get(rowIndex).setAction((PresetApplicationService.ImportAction) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    private static class PresetTableModel extends AbstractTableModel {
        private final List<PresetResolver.ResolvedPreset> rows = new ArrayList<>();
        private EnabledToggleListener enabledToggleListener;

        void setRows(List<PresetResolver.ResolvedPreset> newRows) {
            rows.clear();
            rows.addAll(newRows);
            fireTableDataChanged();
        }

        PresetResolver.ResolvedPreset getRow(int index) {
            return rows.get(index);
        }

        void setEnabledToggleListener(EnabledToggleListener enabledToggleListener) {
            this.enabledToggleListener = enabledToggleListener;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Show";
                case 1:
                    return "Name";
                default:
                    return "";
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
            PresetResolver.ResolvedPreset row = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.getPreset().isEnabled();
                case 1:
                    return row.getPreset().getName();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != 0) {
                return;
            }
            PresetResolver.ResolvedPreset row = rows.get(rowIndex);
            boolean enabled = Boolean.TRUE.equals(value);
            row.getPreset().setEnabled(enabled);
            fireTableCellUpdated(rowIndex, columnIndex);
            if (enabledToggleListener != null) {
                enabledToggleListener.onEnabledToggled(rowIndex, enabled);
            }
        }
    }

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
                if (cachedIcon != null) {
                    setIcon(cachedIcon);
                }
            }
            return this;
        }
    }

    private static class NativeSizedBooleanEditor extends AbstractCellEditor implements TableCellEditor {
        private boolean value;
        private final NativeSizedBooleanRenderer renderer = new NativeSizedBooleanRenderer();

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            return true;
        }

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
