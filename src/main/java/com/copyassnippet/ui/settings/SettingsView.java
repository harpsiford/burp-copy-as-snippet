package com.copyassnippet.ui.settings;

import com.copyassnippet.hotkey.HotkeySettingsState;
import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.service.PresetResolver;

import javax.swing.*;
import java.io.File;
import java.util.List;

interface SettingsView {

    interface Listener {
        void onAdd();

        void onDelete();

        void onDuplicate();

        void onEdit();

        void onMoveUp();

        void onMoveDown();

        void onLoadPresets();

        void onExportPreset();

        void onRestoreDefaults();

        void onSave();

        void onCancel();

        void onApplyHotkey();

        void onHotkeyEnabledToggled(boolean enabled);

        void onSelectionChanged();

        void onPresetEnabledToggled(int rowIndex, boolean enabled);

        void onViewShown();
    }

    JPanel uiComponent();

    void setListener(Listener listener);

    void setRows(List<PresetResolver.ResolvedPreset> rows);

    int rowCount();

    PresetResolver.ResolvedPreset rowAt(int index);

    int selectedRow();

    void selectRow(int rowIndex);

    void setPresetActions(boolean deleteEnabled, boolean duplicateEnabled, boolean editEnabled, boolean exportEnabled, boolean moveUpEnabled, boolean moveDownEnabled);

    void setEditorFormData(PresetFormData formData);

    PresetFormData getEditorFormData();

    void setEditorEnabled(boolean enabled);

    void focusEditorNameField();

    void showValidationWarning(String message);

    boolean confirmDelete(String presetName);

    List<File> choosePresetFilesToLoad();

    File choosePresetFileToExport(String suggestedFileName);

    List<PresetApplicationService.ImportPlanRow> resolveImportConflicts(List<PresetApplicationService.ImportPlanRow> rows);

    HotkeySettingsState getHotkeyState();

    void setHotkeyState(HotkeySettingsState state);
}
