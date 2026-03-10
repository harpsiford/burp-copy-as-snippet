package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;

import java.util.List;

public final class UserSettings {
    public static final int SCHEMA_VERSION = UserSettingsV1.SCHEMA_VERSION;

    private UserSettings() {
    }

    public static void migrate(Preferences preferences) {
        UserSettingsMigrator.migrateIfNeeded(preferences);
    }

    public static List<Preset> readPresets(Preferences preferences) {
        return UserSettingsV1.readPresets(preferences);
    }

    public static void writePresets(Preferences preferences, List<Preset> presets) {
        UserSettingsV1.writePresets(preferences, presets);
    }

    public static List<String> readPresetOrder(Preferences preferences) {
        return UserSettingsV1.readPresetOrder(preferences);
    }

    public static void writePresetOrder(Preferences preferences, List<String> orderIds) {
        UserSettingsV1.writePresetOrder(preferences, orderIds);
    }

    public static boolean isHotkeyEnabled(Preferences preferences) {
        return UserSettingsV1.isHotkeyEnabled(preferences);
    }

    public static void writeHotkeyEnabled(Preferences preferences, boolean enabled) {
        UserSettingsV1.writeHotkeyEnabled(preferences, enabled);
    }

    public static String readHotkeyString(Preferences preferences, String defaultHotkey) {
        return UserSettingsV1.readHotkeyString(preferences, defaultHotkey);
    }

    public static String readStoredHotkeyString(Preferences preferences) {
        return UserSettingsV1.readStoredHotkeyString(preferences);
    }

    public static void writeHotkeyString(Preferences preferences, String hotkey) {
        UserSettingsV1.writeHotkeyString(preferences, hotkey);
    }

    public static boolean isBuiltInDefaultRemoved(Preferences preferences) {
        return UserSettingsV1.isBuiltInDefaultRemoved(preferences);
    }

    public static void writeBuiltInDefaultRemoved(Preferences preferences, boolean removed) {
        UserSettingsV1.writeBuiltInDefaultRemoved(preferences, removed);
    }

    public static void clear(Preferences preferences) {
        UserSettingsV1.clear(preferences);
        UserSettingsV0.clear(preferences);
        preferences.deleteInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY);
    }

    public static String writePresetFile(Preset preset) {
        return UserSettingsV1.writePresetFile(preset);
    }

    public static Preset readPresetFile(String rawJson) {
        return UserSettingsV1.readPresetFile(rawJson);
    }
}
