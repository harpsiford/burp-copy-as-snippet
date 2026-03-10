package com.copyassnippet.ui.settings;

import com.copyassnippet.hotkey.HotkeySettingsService;
import com.copyassnippet.hotkey.HotkeySettingsState;
import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.service.DefaultPresetFactory;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.service.PresetResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class SettingsPresenter implements SettingsView.Listener {
    private final SettingsView view;
    private final PresetApplicationService presetService;
    private final HotkeySettingsService hotkeySettingsService;

    private int editingRow = -1;
    private boolean addingNew = false;

    SettingsPresenter(
            SettingsView view,
            PresetApplicationService presetService,
            HotkeySettingsService hotkeySettingsService
    ) {
        this.view = view;
        this.presetService = presetService;
        this.hotkeySettingsService = hotkeySettingsService;

        this.view.setListener(this);
        this.view.setHotkeyState(hotkeySettingsService.currentSettings());

        clearEditor();
        view.setEditorEnabled(false);
        view.setPresetActions(false, false, false, false, false, false);
    }

    @Override
    public void onAdd() {
        addingNew = true;
        editingRow = -1;
        view.setEditorFormData(PresetFormData.forNewPreset());
        view.setEditorEnabled(true);
        view.focusEditorNameField();
    }

    @Override
    public void onDelete() {
        int selectedRow = selectedRowForRowAction();
        if (selectedRow < 0) {
            view.showValidationWarning("Select a preset to delete.");
            return;
        }

        PresetResolver.ResolvedPreset row = view.rowAt(selectedRow);
        if (!view.confirmDelete(row.getPreset().getName())) {
            return;
        }

        presetService.removePreset(row.getPreset().getId());
        onCancel();
        reloadTable();
    }

    @Override
    public void onDuplicate() {
        int selectedRow = selectedRowForRowAction();
        if (selectedRow < 0) {
            view.showValidationWarning("Select a preset to duplicate.");
            return;
        }

        PresetResolver.ResolvedPreset row = view.rowAt(selectedRow);
        addingNew = true;
        editingRow = -1;
        try {
            view.setEditorFormData(
                    PresetFormData.fromPreset(row.getPreset())
                            .withName(row.getPreset().getName() + " (copy)")
                            .withoutPresetId()
            );
            view.setEditorEnabled(true);
            view.focusEditorNameField();
        } catch (RuntimeException exception) {
            addingNew = false;
            view.showValidationWarning("Unable to duplicate this preset because some saved values are invalid.");
        }
    }

    @Override
    public void onEdit() {
        int selectedRow = selectedRowForRowAction();
        if (selectedRow < 0) {
            view.showValidationWarning("Select a preset to edit.");
            return;
        }

        PresetResolver.ResolvedPreset row = view.rowAt(selectedRow);
        addingNew = false;
        editingRow = selectedRow;
        try {
            view.setEditorFormData(PresetFormData.fromPreset(row.getPreset()));
            view.setEditorEnabled(true);
            view.focusEditorNameField();
        } catch (RuntimeException exception) {
            editingRow = -1;
            view.showValidationWarning("Unable to edit this preset because some saved values are invalid.");
        }
    }

    @Override
    public void onMoveUp() {
        int selectedRow = view.selectedRow();
        if (selectedRow <= 0) {
            return;
        }

        swapAndPersistOrder(selectedRow, selectedRow - 1);
        view.selectRow(selectedRow - 1);
    }

    @Override
    public void onMoveDown() {
        int selectedRow = view.selectedRow();
        if (selectedRow < 0 || selectedRow >= view.rowCount() - 1) {
            return;
        }

        swapAndPersistOrder(selectedRow, selectedRow + 1);
        view.selectRow(selectedRow + 1);
    }

    @Override
    public void onLoadPresets() {
        List<File> files = view.choosePresetFilesToLoad();
        if (files.isEmpty()) {
            return;
        }

        try {
            List<PresetApplicationService.ImportPlanRow> importPlan = presetService.loadImportPlan(
                    files.stream().map(File::toPath).collect(Collectors.toList())
            );
            if (presetService.hasImportConflicts(importPlan)) {
                importPlan = view.resolveImportConflicts(importPlan);
                if (importPlan == null) {
                    return;
                }
            }

            List<String> importedPresetIds = presetService.importPresets(importPlan);
            onCancel();
            reloadTable();
            if (!importedPresetIds.isEmpty()) {
                reselectPreset(importedPresetIds.get(importedPresetIds.size() - 1));
            }
        } catch (IllegalStateException exception) {
            view.showValidationWarning(exception.getMessage());
        }
    }

    @Override
    public void onExportPreset() {
        int selectedRow = selectedRowForRowAction();
        if (selectedRow < 0) {
            view.showValidationWarning("Select a preset to export.");
            return;
        }

        PresetResolver.ResolvedPreset row = view.rowAt(selectedRow);
        File file = view.choosePresetFileToExport(row.getPreset().getName());
        if (file == null) {
            return;
        }

        try {
            presetService.exportPreset(row.getPreset(), file.toPath());
        } catch (IllegalStateException exception) {
            view.showValidationWarning(exception.getMessage());
        }
    }

    @Override
    public void onRestoreDefaults() {
        presetService.clearAllSettings();
        presetService.savePreset(DefaultPresetFactory.createBuiltInPreset());
        hotkeySettingsService.applyFromStore();
        view.setHotkeyState(hotkeySettingsService.currentSettings());
        onCancel();
        reloadTable();
    }

    @Override
    public void onSave() {
        PresetFormData formData = view.getEditorFormData();
        PresetFormData effectiveFormData = addingNew ? formData.withoutPresetId() : formData;
        String validationError = effectiveFormData.firstValidationError();
        if (validationError != null) {
            view.showValidationWarning(validationError);
            return;
        }

        if (presetService.isPresetNameTaken(effectiveFormData.getName(), effectiveFormData.getPresetId())) {
            view.showValidationWarning("A preset named \"" + effectiveFormData.getName().trim() + "\" already exists.");
            return;
        }

        boolean enabled = true;
        if (editingRow >= 0) {
            enabled = view.rowAt(editingRow).getPreset().isEnabled();
        }

        Preset preset = effectiveFormData.toPreset(enabled);
        String savedPresetId = preset.getId();
        presetService.savePreset(preset);
        addingNew = false;
        editingRow = -1;
        view.setEditorEnabled(false);
        reloadTable();
        reselectPreset(savedPresetId);
    }

    @Override
    public void onCancel() {
        addingNew = false;
        editingRow = -1;
        view.setEditorEnabled(false);
        clearEditor();
    }

    @Override
    public void onApplyHotkey() {
        HotkeySettingsState state = view.getHotkeyState();
        if (state.isEnabled() && state.getHotkey().isEmpty()) {
            view.showValidationWarning("Shortcut cannot be empty.");
            return;
        }

        hotkeySettingsService.apply(state);
    }

    @Override
    public void onHotkeyEnabledToggled(boolean enabled) {
        hotkeySettingsService.setEnabled(enabled);
    }

    @Override
    public void onSelectionChanged() {
        if (addingNew) {
            return;
        }

        int selectedRow = view.selectedRow();
        if (selectedRow < 0) {
            view.setPresetActions(false, false, false, false, false, false);
            clearEditor();
            view.setEditorEnabled(false);
            return;
        }

        editingRow = selectedRow;

        view.setPresetActions(
                true,
                true,
                true,
                true,
                selectedRow > 0,
                selectedRow < view.rowCount() - 1
        );
    }

    @Override
    public void onPresetEnabledToggled(int rowIndex, boolean enabled) {
        if (rowIndex < 0 || rowIndex >= view.rowCount()) {
            return;
        }

        PresetResolver.ResolvedPreset row = view.rowAt(rowIndex);
        presetService.persistEnabledToggle(row, enabled);
        if (row.getScope().isBuiltIn()) {
            reloadTable();
            if (rowIndex < view.rowCount()) {
                view.selectRow(rowIndex);
            }
        }
    }

    @Override
    public void onViewShown() {
        reloadTable();
    }

    private void swapAndPersistOrder(int from, int to) {
        List<String> order = new ArrayList<>();
        for (int index = 0; index < view.rowCount(); index++) {
            order.add(view.rowAt(index).getPreset().getId());
        }

        String movedPresetId = order.remove(from);
        order.add(to, movedPresetId);
        presetService.setPresetOrder(order);
        reloadTable();
    }

    private void reloadTable() {
        view.setRows(presetService.listResolvedPresets());
    }

    private void clearEditor() {
        view.setEditorFormData(PresetFormData.empty());
    }

    private void reselectPreset(String presetId) {
        for (int index = 0; index < view.rowCount(); index++) {
            if (view.rowAt(index).getPreset().getId().equals(presetId)) {
                view.selectRow(index);
                break;
            }
        }
    }

    private int selectedRowForRowAction() {
        int selectedRow = view.selectedRow();
        if (selectedRow >= 0) {
            return selectedRow;
        }

        if (editingRow >= 0 && editingRow < view.rowCount()) {
            return editingRow;
        }

        return -1;
    }
}
