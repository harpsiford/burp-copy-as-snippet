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

            Preset preset = PresetFormMapper.toPreset(formData, true);
            String scope = formData.getScope();
            String name = preset.getName();

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
}
