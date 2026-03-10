package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class UserSettingsMigrator {
    private static final Set<Preferences> CHECKED_PREFERENCES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private UserSettingsMigrator() {
    }

    static void migrateIfNeeded(Preferences preferences) {
        synchronized (CHECKED_PREFERENCES) {
            if (CHECKED_PREFERENCES.contains(preferences)) {
                return;
            }

            SchemaVersion schemaVersion = detectSchemaVersion(preferences);
            if (schemaVersion != SchemaVersion.CURRENT) {
                migrate(preferences, schemaVersion);
            }

            CHECKED_PREFERENCES.add(preferences);
        }
    }

    private static void migrate(Preferences preferences, SchemaVersion schemaVersion) {
        SchemaVersion currentSchemaVersion = schemaVersion;
        while (currentSchemaVersion != SchemaVersion.CURRENT) {
            if (currentSchemaVersion == SchemaVersion.V0) {
                UserSettingsV0ToV1Migration.migrate(preferences);
            } else {
                throw new IllegalStateException(
                        "No migration path for user settings schema version: " + currentSchemaVersion
                );
            }
            currentSchemaVersion = detectSchemaVersion(preferences);
        }
    }

    private static SchemaVersion detectSchemaVersion(Preferences preferences) {
        Integer storedVersion = preferences.getInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY);
        if (storedVersion != null) {
            return SchemaVersion.fromPersistedVersion(storedVersion);
        } else {
            return SchemaVersion.V0;
        }
    }

    private enum SchemaVersion {
        V0(UserSettingsV0.SCHEMA_VERSION),
        CURRENT(UserSettingsV1.SCHEMA_VERSION);

        private final int persistedVersion;

        SchemaVersion(int persistedVersion) {
            this.persistedVersion = persistedVersion;
        }

        private static SchemaVersion fromPersistedVersion(int persistedVersion) {
            for (SchemaVersion version : values()) {
                if (version.persistedVersion == persistedVersion) {
                    return version;
                }
            }
            throw new IllegalStateException("Unsupported user settings schema version: " + persistedVersion);
        }
    }
}
