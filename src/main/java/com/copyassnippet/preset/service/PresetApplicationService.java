package com.copyassnippet.preset.service;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.PresetScope;
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

    public void savePreset(Preset preset, PresetScope scope) {
        if (Preset.BUILT_IN_ID.equals(preset.getId())) {
            presetStore.setBuiltInDefaultRemoved(false);
        }
        switch (scope.toEditableScope()) {
            case PROJECT:
                List<Preset> projectList = new ArrayList<>(presetStore.getProjectPresets());
                projectList.removeIf(existing -> existing.getId().equals(preset.getId()));
                projectList.add(preset);
                presetStore.setProjectPresets(projectList);
                break;
            case USER:
                List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
                userList.removeIf(existing -> existing.getId().equals(preset.getId()));
                userList.add(preset);
                presetStore.setUserPresets(userList);
                break;
            default:
                throw new IllegalStateException("Unexpected editable scope: " + scope);
        }
    }

    public void removePreset(String presetId, PresetScope scope) {
        if (Preset.BUILT_IN_ID.equals(presetId)) {
            presetStore.setBuiltInDefaultRemoved(true);
        }
        switch (scope.toEditableScope()) {
            case PROJECT:
                List<Preset> projectList = new ArrayList<>(presetStore.getProjectPresets());
                projectList.removeIf(p -> p.getId().equals(presetId));
                presetStore.setProjectPresets(projectList);
                break;
            case USER:
                List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
                userList.removeIf(p -> p.getId().equals(presetId));
                presetStore.setUserPresets(userList);
                break;
            default:
                throw new IllegalStateException("Unexpected editable scope: " + scope);
        }
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
            savePreset(userPreset, PresetScope.USER);
            return;
        }

        savePreset(preset, row.getScope());
    }
}
