package com.copyassnippet.preset.storage;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsMigrationTest {
    @Test
    void migratesLegacyUserSettingsToCurrentSchema() {
        PreferencesFixture fixture = new PreferencesFixture();
        Preferences preferences = fixture.preferences();

        Preset preset = new Preset(
                "legacy-id",
                "Legacy",
                List.of("Authorization"),
                List.of("session"),
                List.of("token"),
                List.of(new RedactionRule(RedactionRule.Type.HEADER, "Authorization")),
                "MASKED",
                "template-body",
                false
        );

        UserSettingsV0.writePresets(preferences, List.of(preset));
        UserSettingsV0.writePresetOrder(preferences, List.of("legacy-id"));
        UserSettingsV0.writeHotkeyEnabled(preferences, true);
        UserSettingsV0.writeHotkeyString(preferences, "Ctrl+Alt+Shift+C");
        UserSettingsV0.writeBuiltInDefaultRemoved(preferences, true);

        UserSettings.migrate(preferences);

        assertEquals(UserSettings.SCHEMA_VERSION, preferences.getInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY));
        assertNull(preferences.getString("user.preset.ids"));
        assertNull(preferences.getString("preset.order.ids"));
        assertFalse(preferences.stringKeys().stream().anyMatch(key -> key.startsWith("user.preset.")));
        assertPresetEquals(preset, UserSettings.readPresets(preferences).get(0));
        assertEquals(List.of("legacy-id"), UserSettings.readPresetOrder(preferences));
        assertTrue(UserSettings.isHotkeyEnabled(preferences));
        assertEquals("Ctrl+Alt+Shift+C", UserSettings.readHotkeyString(preferences, PresetStore.DEFAULT_HOTKEY));
        assertFalse(UserSettings.isBuiltInDefaultRemoved(preferences));
        assertFalse(preferences.booleanKeys().contains("builtin.default.removed"));
    }

    @Test
    void migratesSparseLegacyPresetEntriesUsingDefaults() {
        PreferencesFixture fixture = new PreferencesFixture();
        Preferences preferences = fixture.preferences();

        preferences.setString("user.preset.ids", "legacy-id");
        preferences.setString("user.preset.legacy-id.name", "Legacy");
        preferences.setString("user.preset.legacy-id.headerRegexes", "Authorization\nX-Api-Key");
        preferences.setString("user.preset.legacy-id.cookieRegexes", "session");

        UserSettings.migrate(preferences);

        assertEquals(UserSettings.SCHEMA_VERSION, preferences.getInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY));

        List<Preset> presets = UserSettings.readPresets(preferences);
        assertEquals(1, presets.size());

        Preset migrated = presets.get(0);
        assertEquals("legacy-id", migrated.getId());
        assertEquals("Legacy", migrated.getName());
        assertEquals(List.of("Authorization", "X-Api-Key"), migrated.getHeaderRegexes());
        assertEquals(List.of("session"), migrated.getCookieRegexes());
        assertEquals(List.of(), migrated.getParamRegexes());
        assertEquals(List.of(), migrated.getRedactionRules());
        assertEquals(DefaultPresetFactory.DEFAULT_REPLACEMENT, migrated.getReplacementString());
        assertEquals(DefaultPresetFactory.DEFAULT_TEMPLATE, migrated.getTemplate());
        assertTrue(migrated.isEnabled());
    }

    @Test
    void presetStoreMigratesLegacySettingsOnConstruction() {
        PreferencesFixture fixture = new PreferencesFixture();
        Preferences preferences = fixture.preferences();

        UserSettingsV0.writePresets(preferences, List.of(new Preset(
                "legacy-id",
                "Legacy",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                DefaultPresetFactory.DEFAULT_REPLACEMENT,
                DefaultPresetFactory.DEFAULT_TEMPLATE,
                true
        )));
        UserSettingsV0.writeHotkeyString(preferences, "Ctrl+Alt+C");

        PresetStore presetStore = new PresetStore(montoyaApi(preferences));

        assertEquals(UserSettings.SCHEMA_VERSION, preferences.getInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY));
        assertEquals("Ctrl+Alt+C", presetStore.getHotkeyString());
        assertEquals(1, presetStore.getUserPresets().size());
        assertEquals("legacy-id", presetStore.getUserPresets().get(0).getId());
    }

    private static void assertPresetEquals(Preset expected, Preset actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getHeaderRegexes(), actual.getHeaderRegexes());
        assertEquals(expected.getCookieRegexes(), actual.getCookieRegexes());
        assertEquals(expected.getParamRegexes(), actual.getParamRegexes());
        assertEquals(toRuleStrings(expected.getRedactionRules()), toRuleStrings(actual.getRedactionRules()));
        assertEquals(expected.getReplacementString(), actual.getReplacementString());
        assertEquals(expected.getTemplate(), actual.getTemplate());
        assertEquals(expected.isEnabled(), actual.isEnabled());
    }

    private static List<String> toRuleStrings(List<RedactionRule> rules) {
        List<String> serialized = new ArrayList<>();
        for (RedactionRule rule : rules) {
            serialized.add(rule.getType() + ":" + rule.getPattern());
        }
        return serialized;
    }

    private static MontoyaApi montoyaApi(Preferences preferences) {
        Map<String, Object> persistenceMethods = new HashMap<>();
        persistenceMethods.put("preferences", preferences);
        persistenceMethods.put("extensionData", null);
        Persistence persistence = (Persistence) Proxy.newProxyInstance(
                Persistence.class.getClassLoader(),
                new Class[]{Persistence.class},
                new FixedReturnHandler(persistenceMethods)
        );

        return (MontoyaApi) Proxy.newProxyInstance(
                MontoyaApi.class.getClassLoader(),
                new Class[]{MontoyaApi.class},
                new FixedReturnHandler(Map.of("persistence", persistence))
        );
    }

    private static final class PreferencesFixture implements InvocationHandler {
        private final Map<String, String> strings = new HashMap<>();
        private final Map<String, Boolean> booleans = new HashMap<>();
        private final Map<String, Integer> integers = new HashMap<>();
        private final Preferences preferences = (Preferences) Proxy.newProxyInstance(
                Preferences.class.getClassLoader(),
                new Class[]{Preferences.class},
                this
        );

        private Preferences preferences() {
            return preferences;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getString".equals(name)) {
                return strings.get(args[0]);
            }
            if ("setString".equals(name)) {
                setValue(strings, args);
                return null;
            }
            if ("deleteString".equals(name)) {
                strings.remove(args[0]);
                return null;
            }
            if ("stringKeys".equals(name)) {
                return Set.copyOf(strings.keySet());
            }
            if ("getBoolean".equals(name)) {
                return booleans.get(args[0]);
            }
            if ("setBoolean".equals(name)) {
                booleans.put((String) args[0], (Boolean) args[1]);
                return null;
            }
            if ("deleteBoolean".equals(name)) {
                booleans.remove(args[0]);
                return null;
            }
            if ("booleanKeys".equals(name)) {
                return Set.copyOf(booleans.keySet());
            }
            if ("getInteger".equals(name)) {
                return integers.get(args[0]);
            }
            if ("setInteger".equals(name)) {
                integers.put((String) args[0], (Integer) args[1]);
                return null;
            }
            if ("deleteInteger".equals(name)) {
                integers.remove(args[0]);
                return null;
            }
            if ("integerKeys".equals(name)) {
                return Set.copyOf(integers.keySet());
            }
            if ("toString".equals(name)) {
                return "PreferencesFixture";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected Preferences call: " + method);
        }

        private static void setValue(Map<String, String> target, Object[] args) {
            String key = (String) args[0];
            String value = (String) args[1];
            if (value == null) {
                target.remove(key);
            } else {
                target.put(key, value);
            }
        }
    }

    private static final class FixedReturnHandler implements InvocationHandler {
        private final Map<String, Object> returnsByMethod;

        private FixedReturnHandler(Map<String, Object> returnsByMethod) {
            this.returnsByMethod = returnsByMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (returnsByMethod.containsKey(name)) {
                return returnsByMethod.get(name);
            }
            if ("toString".equals(name)) {
                return method.getDeclaringClass().getSimpleName() + "Proxy";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected proxy call: " + method);
        }
    }
}
