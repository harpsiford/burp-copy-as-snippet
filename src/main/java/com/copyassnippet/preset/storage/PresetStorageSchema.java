package com.copyassnippet.preset.storage;

public final class PresetStorageSchema {
    public static final String USER_SETTINGS_SCHEMA_VERSION_KEY = "user.settings.schemaVersion";
    public static final int CURRENT_USER_SCHEMA_VERSION = UserSettings.SCHEMA_VERSION;

    public static final String PROJECT_PRESETS_KEY = "project.presets";
    public static final String PROJECT_PRESET_SCHEMA_VERSION_KEY = "schemaVersion";
    public static final String PROJECT_PRESETS_DATA_KEY = "data";
    public static final int CURRENT_PROJECT_SCHEMA_VERSION = ProjectSettings.SCHEMA_VERSION;

    private PresetStorageSchema() {
    }
}
