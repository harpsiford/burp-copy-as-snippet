package com.copyassnippet;

import javax.swing.*;
import java.util.List;

interface SettingsView {

    interface Listener {
        void onAdd();

        void onDelete();

        void onDuplicate();

        void onEdit();

        void onMoveUp();

        void onMoveDown();

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

    void setRows(List<PresetRow> rows);

    int rowCount();

    PresetRow rowAt(int index);

    int selectedRow();

    void selectRow(int rowIndex);

    void setPresetActions(boolean deleteEnabled, boolean duplicateEnabled, boolean editEnabled, boolean moveUpEnabled, boolean moveDownEnabled);

    void setEditorFormData(PresetFormData formData);

    PresetFormData getEditorFormData();

    void setEditorEnabled(boolean enabled);

    void focusEditorNameField();

    void showValidationWarning(String message);

    boolean confirmDelete(String presetName);

    HotkeySettingsState getHotkeyState();

    void setHotkeyState(HotkeySettingsState state);
}
