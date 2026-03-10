package com.copyassnippet.preset.storage.migrations;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.storage.PresetStorageSchema;
import com.copyassnippet.preset.storage.UserSettings;
import com.copyassnippet.preset.storage.UserSettingsV0;

import java.util.List;

final class UserSettingsV0ToV1Migration {
    private UserSettingsV0ToV1Migration() {
    }

    static void migrate(Preferences preferences) {
        List<com.copyassnippet.preset.model.Preset> presets = UserSettingsV0.readPresets(preferences);
        List<String> presetOrder = UserSettingsV0.readPresetOrder(preferences);
        Boolean hotkeyEnabled = UserSettingsV0.readHotkeyEnabled(preferences);
        String hotkeyString = UserSettingsV0.readHotkeyString(preferences);
        Boolean builtInDefaultRemoved = UserSettingsV0.readBuiltInDefaultRemoved(preferences);

        UserSettingsV0.clear(preferences);

        UserSettings.writePresets(preferences, presets);
        UserSettings.writePresetOrder(preferences, presetOrder);

        if (hotkeyEnabled != null) {
            UserSettings.writeHotkeyEnabled(preferences, hotkeyEnabled);
        }
        if (hotkeyString != null) {
            UserSettings.writeHotkeyString(preferences, hotkeyString);
        }
        if (builtInDefaultRemoved != null) {
            UserSettings.writeBuiltInDefaultRemoved(preferences, builtInDefaultRemoved);
        }

        preferences.setInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY, UserSettings.SCHEMA_VERSION);
    }
}
