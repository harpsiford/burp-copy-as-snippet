package com.copyassnippet.preset.storage.migrations;

import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.storage.ProjectSettings;
import com.copyassnippet.preset.storage.PresetStorageSchema;
import com.copyassnippet.preset.storage.ProjectSettingsV0;
import com.copyassnippet.preset.storage.UserSettings;
import com.copyassnippet.preset.storage.UserSettingsV0;

import java.util.Map;
import java.util.function.Consumer;

public final class PresetStoreMigrator {
    private static final Map<UserSchemaVersion, Consumer<Preferences>> USER_MIGRATIONS = Map.of(
            UserSchemaVersion.V0, UserSettingsV0ToV1Migration::migrate
    );
    private static final Map<ProjectSchemaVersion, Consumer<PersistedObject>> PROJECT_MIGRATIONS = Map.of(
            ProjectSchemaVersion.V0, ProjectSettingsV0ToV1Migration::migrate
    );

    private PresetStoreMigrator() {
    }

    public static void migrate(Preferences preferences, PersistedObject extensionData) {
        migrateUserSettings(preferences);
        migrateProjectSettings(extensionData);
    }

    private static void migrateUserSettings(Preferences preferences) {
        UserSchemaVersion schemaVersion = detectUserSchemaVersion(preferences);
        while (schemaVersion != UserSchemaVersion.CURRENT) {
            Consumer<Preferences> migration = USER_MIGRATIONS.get(schemaVersion);
            if (migration == null) {
                throw new IllegalStateException("No migration path for user settings schema version: " + schemaVersion);
            }
            migration.accept(preferences);
            schemaVersion = detectUserSchemaVersion(preferences);
        }
    }

    private static void migrateProjectSettings(PersistedObject extensionData) {
        ProjectSchemaVersion schemaVersion = detectProjectSchemaVersion(extensionData);
        while (schemaVersion != ProjectSchemaVersion.CURRENT) {
            Consumer<PersistedObject> migration = PROJECT_MIGRATIONS.get(schemaVersion);
            if (migration == null) {
                throw new IllegalStateException("No migration path for project settings schema version: " + schemaVersion);
            }
            migration.accept(extensionData);
            schemaVersion = detectProjectSchemaVersion(extensionData);
        }
    }

    private static UserSchemaVersion detectUserSchemaVersion(Preferences preferences) {
        Integer storedVersion = preferences.getInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY);
        if (storedVersion != null) {
            return UserSchemaVersion.fromPersistedVersion(storedVersion);
        }

        if (UserSettings.hasStructuredData(preferences)) {
            return UserSchemaVersion.CURRENT;
        }

        if (UserSettingsV0.isDetected(preferences)) {
            return UserSchemaVersion.V0;
        }

        return UserSchemaVersion.CURRENT;
    }

    private static ProjectSchemaVersion detectProjectSchemaVersion(PersistedObject extensionData) {
        PersistedObject container = extensionData.getChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (container == null) {
            return ProjectSchemaVersion.CURRENT;
        }

        Integer storedVersion = container.getInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY);
        if (storedVersion != null) {
            if (storedVersion == ProjectSettings.SCHEMA_VERSION) {
                return ProjectSchemaVersion.CURRENT;
            }
            throw new IllegalStateException("Unsupported project settings schema version: " + storedVersion);
        }

        if (ProjectSettingsV0.isDetected(extensionData)) {
            return ProjectSchemaVersion.V0;
        }

        return ProjectSchemaVersion.CURRENT;
    }

    private enum UserSchemaVersion {
        V0(UserSettingsV0.SCHEMA_VERSION),
        CURRENT(UserSettings.SCHEMA_VERSION);

        private final int persistedVersion;

        UserSchemaVersion(int persistedVersion) {
            this.persistedVersion = persistedVersion;
        }

        private static UserSchemaVersion fromPersistedVersion(int persistedVersion) {
            for (UserSchemaVersion version : values()) {
                if (version.persistedVersion == persistedVersion) {
                    return version;
                }
            }
            throw new IllegalStateException("Unsupported user settings schema version: " + persistedVersion);
        }
    }

    private enum ProjectSchemaVersion {
        V0(ProjectSettingsV0.SCHEMA_VERSION),
        CURRENT(ProjectSettings.SCHEMA_VERSION);

        private final int persistedVersion;

        ProjectSchemaVersion(int persistedVersion) {
            this.persistedVersion = persistedVersion;
        }
    }
}
