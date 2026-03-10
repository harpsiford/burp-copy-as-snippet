package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

public final class UserSettings {
    public static final int SCHEMA_VERSION = 1;

    private static final String USER_PRESETS_JSON_KEY = "user.presets.json";
    private static final String PRESET_ORDER_JSON_KEY = "preset.order.json";
    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";
    private static final String BUILT_IN_DEFAULT_REMOVED_KEY = "builtin.default.removed";
    private static final TypeReference<List<StoredPreset>> PRESET_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private UserSettings() {
    }

    public static List<Preset> readPresets(Preferences preferences) {
        String rawJson = preferences.getString(USER_PRESETS_JSON_KEY);
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }

        List<StoredPreset> storedPresets = PresetSerializationHelper.readJson(rawJson, PRESET_LIST_TYPE);
        List<Preset> presets = new ArrayList<>();
        for (StoredPreset storedPreset : storedPresets) {
            Preset preset = storedPreset.toPreset();
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    public static void writePresets(Preferences preferences, List<Preset> presets) {
        if (presets == null || presets.isEmpty()) {
            preferences.deleteString(USER_PRESETS_JSON_KEY);
        } else {
            List<StoredPreset> serialized = new ArrayList<>();
            for (Preset preset : presets) {
                serialized.add(StoredPreset.fromPreset(preset));
            }
            preferences.setString(USER_PRESETS_JSON_KEY, PresetSerializationHelper.writeJson(serialized));
        }
        markCurrent(preferences);
    }

    public static List<String> readPresetOrder(Preferences preferences) {
        String rawJson = preferences.getString(PRESET_ORDER_JSON_KEY);
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        return PresetSerializationHelper.copyWithoutNulls(
                PresetSerializationHelper.readJson(rawJson, STRING_LIST_TYPE)
        );
    }

    public static void writePresetOrder(Preferences preferences, List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            preferences.deleteString(PRESET_ORDER_JSON_KEY);
        } else {
            preferences.setString(
                    PRESET_ORDER_JSON_KEY,
                    PresetSerializationHelper.writeJson(PresetSerializationHelper.copyWithoutNulls(orderIds))
            );
        }
        markCurrent(preferences);
    }

    public static boolean isHotkeyEnabled(Preferences preferences) {
        return Boolean.TRUE.equals(preferences.getBoolean(HOTKEY_ENABLED_KEY));
    }

    public static void writeHotkeyEnabled(Preferences preferences, boolean enabled) {
        preferences.setBoolean(HOTKEY_ENABLED_KEY, enabled);
        preferences.deleteString(HOTKEY_ENABLED_KEY);
        markCurrent(preferences);
    }

    public static String readHotkeyString(Preferences preferences, String defaultHotkey) {
        String hotkey = readStoredHotkeyString(preferences);
        return hotkey == null || hotkey.isBlank() ? defaultHotkey : hotkey;
    }

    public static String readStoredHotkeyString(Preferences preferences) {
        return preferences.getString(HOTKEY_STRING_KEY);
    }

    public static void writeHotkeyString(Preferences preferences, String hotkey) {
        preferences.setString(HOTKEY_STRING_KEY, hotkey);
        markCurrent(preferences);
    }

    public static boolean isBuiltInDefaultRemoved(Preferences preferences) {
        return Boolean.TRUE.equals(preferences.getBoolean(BUILT_IN_DEFAULT_REMOVED_KEY));
    }

    public static void writeBuiltInDefaultRemoved(Preferences preferences, boolean removed) {
        preferences.setBoolean(BUILT_IN_DEFAULT_REMOVED_KEY, removed);
        preferences.deleteString(BUILT_IN_DEFAULT_REMOVED_KEY);
        markCurrent(preferences);
    }

    public static void clear(Preferences preferences) {
        preferences.deleteString(USER_PRESETS_JSON_KEY);
        preferences.deleteString(PRESET_ORDER_JSON_KEY);
        preferences.deleteBoolean(HOTKEY_ENABLED_KEY);
        preferences.deleteString(HOTKEY_ENABLED_KEY);
        preferences.deleteString(HOTKEY_STRING_KEY);
        preferences.deleteBoolean(BUILT_IN_DEFAULT_REMOVED_KEY);
        preferences.deleteString(BUILT_IN_DEFAULT_REMOVED_KEY);
    }

    private static void markCurrent(Preferences preferences) {
        preferences.setInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY, SCHEMA_VERSION);
    }

    private static final class StoredPreset {
        public String id;
        public String name;
        public List<String> headerRegexes = List.of();
        public List<String> cookieRegexes = List.of();
        public List<String> paramRegexes = List.of();
        public List<StoredRedactionRule> redactionRules = List.of();
        public String replacementString = DefaultPresetFactory.DEFAULT_REPLACEMENT;
        public String template = DefaultPresetFactory.DEFAULT_TEMPLATE;
        public boolean enabled = true;

        private static StoredPreset fromPreset(Preset preset) {
            StoredPreset storedPreset = new StoredPreset();
            storedPreset.id = preset.getId();
            storedPreset.name = preset.getName();
            storedPreset.headerRegexes = PresetSerializationHelper.copyWithoutNulls(preset.getHeaderRegexes());
            storedPreset.cookieRegexes = PresetSerializationHelper.copyWithoutNulls(preset.getCookieRegexes());
            storedPreset.paramRegexes = PresetSerializationHelper.copyWithoutNulls(preset.getParamRegexes());
            storedPreset.redactionRules = StoredRedactionRule.fromRules(preset.getRedactionRules());
            storedPreset.replacementString = preset.getReplacementString();
            storedPreset.template = preset.getTemplate();
            storedPreset.enabled = preset.isEnabled();
            return storedPreset;
        }

        private Preset toPreset() {
            if (id == null || id.isBlank() || name == null) {
                return null;
            }

            return new Preset(
                    id,
                    name,
                    PresetSerializationHelper.copyWithoutNulls(headerRegexes),
                    PresetSerializationHelper.copyWithoutNulls(cookieRegexes),
                    PresetSerializationHelper.copyWithoutNulls(paramRegexes),
                    StoredRedactionRule.toRules(redactionRules),
                    replacementString != null ? replacementString : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                    template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                    enabled
            );
        }
    }

    private static final class StoredRedactionRule {
        public String type;
        public String pattern;

        private static List<StoredRedactionRule> fromRules(List<RedactionRule> rules) {
            List<StoredRedactionRule> serialized = new ArrayList<>();
            if (rules == null) {
                return serialized;
            }

            for (RedactionRule rule : rules) {
                if (rule == null || rule.getType() == null || rule.getPattern() == null) {
                    continue;
                }

                StoredRedactionRule storedRule = new StoredRedactionRule();
                storedRule.type = rule.getType().name();
                storedRule.pattern = rule.getPattern();
                serialized.add(storedRule);
            }
            return serialized;
        }

        private static List<RedactionRule> toRules(List<StoredRedactionRule> storedRules) {
            if (storedRules == null) {
                return List.of();
            }

            List<RedactionRule> rules = new ArrayList<>();
            for (StoredRedactionRule storedRule : storedRules) {
                if (storedRule == null || storedRule.type == null || storedRule.pattern == null) {
                    continue;
                }

                try {
                    rules.add(new RedactionRule(
                            RedactionRule.Type.valueOf(storedRule.type.toUpperCase()),
                            storedRule.pattern
                    ));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid stored rule types rather than failing the whole settings payload.
                }
            }
            return rules;
        }
    }
}
