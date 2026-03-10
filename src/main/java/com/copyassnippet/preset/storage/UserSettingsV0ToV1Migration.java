package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;

import java.util.List;

final class UserSettingsV0ToV1Migration {
    private UserSettingsV0ToV1Migration() {
    }

    static void migrate(Preferences preferences) {
        List<Preset> presets = UserSettingsV0.readPresets(preferences);
        List<String> presetOrder = UserSettingsV0.readPresetOrder(preferences);
        Boolean hotkeyEnabled = UserSettingsV0.readHotkeyEnabled(preferences);
        String hotkeyString = UserSettingsV0.readHotkeyString(preferences);

        UserSettingsV0.clear(preferences);

        UserSettingsV1.writePresets(preferences, presets);
        UserSettingsV1.writePresetOrder(preferences, presetOrder);

        if (hotkeyEnabled != null) {
            UserSettingsV1.writeHotkeyEnabled(preferences, hotkeyEnabled);
        }
        if (hotkeyString != null) {
            UserSettingsV1.writeHotkeyString(preferences, hotkeyString);
        }
    }
}
