package com.copyassnippet;

import java.util.ArrayList;
import java.util.List;

final class PresetApplicationService {
    private final PresetStore presetStore;

    PresetApplicationService(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    List<PresetRow> listResolvedRows() {
        List<PresetRow> rows = new ArrayList<>();
        for (PresetResolver.ResolvedPreset resolvedPreset : presetStore.getResolvedPresetEntries()) {
            rows.add(new PresetRow(resolvedPreset.getPreset(), resolvedPreset.getScope()));
        }
        return rows;
    }

    void savePreset(Preset preset, PresetScope scope) {
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

    void removePreset(String presetId, PresetScope scope) {
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

    void clearAllSettings() {
        presetStore.resetAllSettings();
    }

    void setPresetOrder(List<String> orderIds) {
        presetStore.setPresetOrder(orderIds);
    }

    boolean isPresetNameTaken(String presetName, String excludedPresetId) {
        return presetStore.isPresetNameTaken(presetName, excludedPresetId);
    }

    void persistEnabledToggle(PresetRow row, boolean enabled) {
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
