package com.copyassnippet;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NewPresetDialog {

    private final PresetStore presetStore;

    public NewPresetDialog(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    public void show(Component parent) {
        PresetFormPanel formPanel = new PresetFormPanel();
        formPanel.setFormData(PresetFormMapper.forNewPreset());
        formPanel.setPreferredSize(new Dimension(800, 600));

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent, formPanel, "Create new preset",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            PresetFormData formData = formPanel.getFormData();
            String validationError = PresetFormMapper.firstValidationError(formData);
            if (validationError != null) {
                JOptionPane.showMessageDialog(parent, validationError,
                        "Validation", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (presetStore.isPresetNameTaken(formData.getName(), formData.getPresetId())) {
                JOptionPane.showMessageDialog(
                        parent,
                        "A preset named \"" + formData.getName().trim() + "\" already exists.",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            Preset preset = PresetFormMapper.toPreset(formData, true);
            PresetScope scope = formData.getScope();

            if (scope == PresetScope.PROJECT) {
                List<Preset> list = new ArrayList<>(presetStore.getProjectPresets());
                list.removeIf(existing -> existing.getId().equals(preset.getId()));
                list.add(preset);
                presetStore.setProjectPresets(list);
            } else {
                List<Preset> list = new ArrayList<>(presetStore.getUserPresets());
                list.removeIf(existing -> existing.getId().equals(preset.getId()));
                list.add(preset);
                presetStore.setUserPresets(list);
            }
            return;
        }
    }
}
