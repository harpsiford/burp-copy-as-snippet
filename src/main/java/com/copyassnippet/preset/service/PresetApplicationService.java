package com.copyassnippet.preset.service;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.storage.PresetStore;

import java.util.ArrayList;
import java.util.List;

public final class PresetApplicationService {
    private final PresetStore presetStore;

    public PresetApplicationService(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    public List<PresetResolver.ResolvedPreset> listResolvedPresets() {
        return presetStore.getResolvedPresetEntries();
    }

    public void savePreset(Preset preset) {
        if (Preset.BUILT_IN_ID.equals(preset.getId())) {
            presetStore.setBuiltInDefaultRemoved(false);
        }
        List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
        userList.removeIf(existing -> existing.getId().equals(preset.getId()));
        userList.add(preset);
        presetStore.setUserPresets(userList);
    }

    public void removePreset(String presetId) {
        if (Preset.BUILT_IN_ID.equals(presetId)) {
            presetStore.setBuiltInDefaultRemoved(true);
        }
        List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
        userList.removeIf(p -> p.getId().equals(presetId));
        presetStore.setUserPresets(userList);
    }

    public void clearAllSettings() {
        presetStore.resetAllSettings();
    }

    public void setPresetOrder(List<String> orderIds) {
        presetStore.setPresetOrder(orderIds);
    }

    public boolean isPresetNameTaken(String presetName, String excludedPresetId) {
        return presetStore.isPresetNameTaken(presetName, excludedPresetId);
    }

    public void persistEnabledToggle(PresetResolver.ResolvedPreset row, boolean enabled) {
        Preset preset = row.getPreset();
        preset.setEnabled(enabled);

        if (row.getScope().isBuiltIn()) {
            Preset userPreset = new Preset(
                    preset.getName(),
                    preset.getHeaderRegexes(),
                    preset.getCookieRegexes(),
                    preset.getParamRegexes(),
                    preset.getRedactionRules(),
                    preset.getReplacementString(),
                    preset.getTemplate(),
                    preset.isEnabled()
            );
            savePreset(userPreset);
            return;
        }

        savePreset(preset);
    }
}
