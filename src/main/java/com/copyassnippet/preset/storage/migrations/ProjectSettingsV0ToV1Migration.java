package com.copyassnippet.preset.storage.migrations;

import burp.api.montoya.persistence.PersistedObject;
import com.copyassnippet.preset.storage.ProjectSettings;
import com.copyassnippet.preset.storage.ProjectSettingsV0;

import java.util.List;

final class ProjectSettingsV0ToV1Migration {
    private ProjectSettingsV0ToV1Migration() {
    }

    static void migrate(PersistedObject extensionData) {
        List<com.copyassnippet.preset.model.Preset> presets = ProjectSettingsV0.readPresets(extensionData);
        ProjectSettingsV0.clear(extensionData);
        ProjectSettings.writePresets(extensionData, presets);
    }
}
