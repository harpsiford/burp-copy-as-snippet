package com.copyassnippet.ui.settings;

import com.copyassnippet.hotkey.HotkeySettingsService;
import com.copyassnippet.hotkey.HotkeySettingsState;
import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.service.DefaultPresetFactory;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.service.PresetResolver;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

final class SettingsPresenter implements SettingsView.Listener {
    private final SettingsView view;
    private final PresetApplicationService presetService;
    private final HotkeySettingsService hotkeySettingsService;
    private final Executor backgroundExecutor;

    private int editingRow = -1;
    private boolean addingNew = false;
    private boolean busy = false;

    SettingsPresenter(
            SettingsView view,
            PresetApplicationService presetService,
            HotkeySettingsService hotkeySettingsService,
            Executor backgroundExecutor
    ) {
        this.view = view;
        this.presetService = presetService;
        this.hotkeySettingsService = hotkeySettingsService;
        this.backgroundExecutor = backgroundExecutor;

        this.view.setListener(this);
        this.view.setHotkeyState(hotkeySettingsService.currentSettings());
        this.view.setBusy(false);

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

        setBusy(true);
        List<java.nio.file.Path> importPaths = files.stream().map(File::toPath).collect(Collectors.toList());
        backgroundExecutor.execute(() -> loadPresetsInBackground(importPaths));
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

        Preset preset = row.getPreset();
        setBusy(true);
        backgroundExecutor.execute(() -> exportPresetInBackground(preset, file));
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
        refreshPresetActions();
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

    private void setBusy(boolean busy) {
        this.busy = busy;
        view.setBusy(busy);
        refreshPresetActions();
    }

    private void refreshPresetActions() {
        if (busy || addingNew) {
            view.setPresetActions(false, false, false, false, false, false);
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

    private void loadPresetsInBackground(List<java.nio.file.Path> importPaths) {
        try {
            List<PresetApplicationService.ImportPlanRow> importPlan = presetService.loadImportPlan(importPaths);
            if (Thread.currentThread().isInterrupted()) {
                runOnEdt(() -> setBusy(false));
                return;
            }

            if (presetService.hasImportConflicts(importPlan)) {
                List<PresetApplicationService.ImportPlanRow> currentImportPlan = importPlan;
                List<PresetApplicationService.ImportPlanRow> resolvedPlan = callOnEdt(
                        () -> view.resolveImportConflicts(currentImportPlan)
                );
                if (resolvedPlan == null || Thread.currentThread().isInterrupted()) {
                    runOnEdt(() -> setBusy(false));
                    return;
                }
                importPlan = resolvedPlan;
            }

            List<String> importedPresetIds = presetService.importPresets(importPlan);
            if (Thread.currentThread().isInterrupted()) {
                runOnEdt(() -> setBusy(false));
                return;
            }

            runOnEdt(() -> {
                onCancel();
                reloadTable();
                if (!importedPresetIds.isEmpty()) {
                    reselectPreset(importedPresetIds.get(importedPresetIds.size() - 1));
                }
                setBusy(false);
            });
        } catch (IllegalStateException exception) {
            runOnEdt(() -> {
                setBusy(false);
                view.showValidationWarning(exception.getMessage());
            });
        }
    }

    private void exportPresetInBackground(Preset preset, File file) {
        try {
            presetService.exportPreset(preset, file.toPath());
            if (Thread.currentThread().isInterrupted()) {
                runOnEdt(() -> setBusy(false));
                return;
            }

            runOnEdt(() -> setBusy(false));
        } catch (IllegalStateException exception) {
            runOnEdt(() -> {
                setBusy(false);
                view.showValidationWarning(exception.getMessage());
            });
        }
    }

    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeLater(action);
    }

    private static <T> T callOnEdt(EdtSupplier<T> supplier) {
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }

        FutureTask<T> task = new FutureTask<>(supplier::get);
        SwingUtilities.invokeLater(task);
        try {
            return task.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Failed while waiting for the settings UI.", cause);
        }
    }

    @FunctionalInterface
    private interface EdtSupplier<T> {
        T get();
    }
}
