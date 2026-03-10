package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import java.util.ArrayList;
import java.util.List;

public final class UserSettingsV0 {
    static final int SCHEMA_VERSION = 0;

    private static final String USER_PRESET_PREFIX = "user.preset";
    private static final String USER_PRESET_IDS_KEY = "user.preset.ids";
    private static final String PRESET_ORDER_IDS_KEY = "preset.order.ids";
    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";
    private static final String BUILT_IN_DEFAULT_REMOVED_KEY = "builtin.default.removed";

    private UserSettingsV0() {
    }

    static boolean isDetected(Preferences preferences) {
        return preferences.getString(USER_PRESET_IDS_KEY) != null
                || preferences.getString(PRESET_ORDER_IDS_KEY) != null
                || preferences.getString(HOTKEY_ENABLED_KEY) != null
                || preferences.stringKeys().stream().anyMatch(key -> key.startsWith("user.preset."));
    }

    public static List<Preset> readPresets(Preferences preferences) {
        List<Preset> presets = new ArrayList<>();
        for (String presetId : parseMultiline(preferences.getString(USER_PRESET_IDS_KEY))) {
            Preset preset = loadPreset(preferences, userPresetKey(presetId), presetId);
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    public static List<String> readPresetOrder(Preferences preferences) {
        return parseMultiline(preferences.getString(PRESET_ORDER_IDS_KEY));
    }

    public static Boolean readHotkeyEnabled(Preferences preferences) {
        return parseBooleanString(preferences.getString(HOTKEY_ENABLED_KEY));
    }

    public static Boolean readBuiltInDefaultRemoved(Preferences preferences) {
        return parseBooleanString(preferences.getString(BUILT_IN_DEFAULT_REMOVED_KEY));
    }

    public static String readHotkeyString(Preferences preferences) {
        return preferences.getString(HOTKEY_STRING_KEY);
    }

    static void writePresets(Preferences preferences, List<Preset> presets) {
        clearPresetEntries(preferences);
        if (presets == null || presets.isEmpty()) {
            preferences.deleteString(USER_PRESET_IDS_KEY);
            return;
        }

        List<String> presetIds = new ArrayList<>();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());
            savePreset(preferences, userPresetKey(preset.getId()), preset);
        }
        preferences.setString(USER_PRESET_IDS_KEY, joinMultiline(presetIds));
    }

    static void writePresetOrder(Preferences preferences, List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            preferences.deleteString(PRESET_ORDER_IDS_KEY);
            return;
        }
        preferences.setString(PRESET_ORDER_IDS_KEY, joinMultiline(orderIds));
    }

    static void writeHotkeyEnabled(Preferences preferences, boolean enabled) {
        preferences.setString(HOTKEY_ENABLED_KEY, String.valueOf(enabled));
    }

    static void writeHotkeyString(Preferences preferences, String hotkey) {
        preferences.setString(HOTKEY_STRING_KEY, hotkey);
    }

    static void writeBuiltInDefaultRemoved(Preferences preferences, boolean removed) {
        preferences.setString(BUILT_IN_DEFAULT_REMOVED_KEY, String.valueOf(removed));
    }

    public static void clear(Preferences preferences) {
        clearPresetEntries(preferences);
        preferences.deleteString(USER_PRESET_IDS_KEY);
        preferences.deleteString(PRESET_ORDER_IDS_KEY);
        preferences.deleteString(HOTKEY_ENABLED_KEY);
        preferences.deleteString(HOTKEY_STRING_KEY);
        preferences.deleteString(BUILT_IN_DEFAULT_REMOVED_KEY);
    }

    private static Preset loadPreset(Preferences preferences, String keyPrefix, String presetId) {
        String id = preferences.getString(keyPrefix + ".id");
        String name = preferences.getString(keyPrefix + ".name");
        if (name == null) {
            return null;
        }

        String template = preferences.getString(keyPrefix + ".template");
        String replacement = preferences.getString(keyPrefix + ".replacementString");
        Boolean enabled = parseBooleanString(preferences.getString(keyPrefix + ".enabled"));

        return new Preset(
                id != null ? id : presetId,
                name,
                parseMultiline(preferences.getString(keyPrefix + ".headerRegexes")),
                parseMultiline(preferences.getString(keyPrefix + ".cookieRegexes")),
                parseMultiline(preferences.getString(keyPrefix + ".paramRegexes")),
                parseRules(parseMultiline(preferences.getString(keyPrefix + ".redactionRules"))),
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabled == null || enabled
        );
    }

    private static void savePreset(Preferences preferences, String keyPrefix, Preset preset) {
        preferences.setString(keyPrefix + ".id", preset.getId());
        preferences.setString(keyPrefix + ".name", preset.getName());
        preferences.setString(keyPrefix + ".headerRegexes", joinMultiline(preset.getHeaderRegexes()));
        preferences.setString(keyPrefix + ".cookieRegexes", joinMultiline(preset.getCookieRegexes()));
        preferences.setString(keyPrefix + ".paramRegexes", joinMultiline(preset.getParamRegexes()));
        preferences.setString(keyPrefix + ".template", preset.getTemplate());
        preferences.setString(keyPrefix + ".enabled", String.valueOf(preset.isEnabled()));
        preferences.setString(keyPrefix + ".replacementString", preset.getReplacementString());
        preferences.setString(keyPrefix + ".redactionRules", joinMultiline(serializeRules(preset.getRedactionRules())));
    }

    private static void clearPresetEntries(Preferences preferences) {
        for (String key : new ArrayList<>(preferences.stringKeys())) {
            if (key.startsWith(USER_PRESET_PREFIX + ".")) {
                preferences.deleteString(key);
            }
        }
    }

    private static String userPresetKey(String presetId) {
        return USER_PRESET_PREFIX + "." + presetId;
    }

    private static List<String> parseMultiline(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String value : raw.split("\\R")) {
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static String joinMultiline(Iterable<String> values) {
        if (values == null) {
            return "";
        }
        return String.join("\n", copyWithoutNulls(values));
    }

    private static Boolean parseBooleanString(String raw) {
        if ("true".equals(raw)) {
            return true;
        }
        if ("false".equals(raw)) {
            return false;
        }
        return null;
    }

    private static List<RedactionRule> parseRules(Iterable<String> rawRules) {
        List<RedactionRule> rules = new ArrayList<>();
        if (rawRules == null) {
            return rules;
        }

        for (String rawRule : rawRules) {
            if (rawRule == null || rawRule.isBlank()) {
                continue;
            }

            int separator = rawRule.indexOf(':');
            if (separator < 0) {
                continue;
            }

            String type = rawRule.substring(0, separator).trim();
            String pattern = rawRule.substring(separator + 1);
            try {
                rules.add(new RedactionRule(RedactionRule.Type.valueOf(type.toUpperCase()), pattern));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid legacy rules rather than failing the whole preset.
            }
        }
        return rules;
    }

    private static List<String> serializeRules(List<RedactionRule> rules) {
        List<String> serialized = new ArrayList<>();
        if (rules == null) {
            return serialized;
        }

        for (RedactionRule rule : rules) {
            if (rule == null || rule.getType() == null || rule.getPattern() == null) {
                continue;
            }
            serialized.add(rule.getType().name() + ":" + rule.getPattern());
        }
        return serialized;
    }

    private static List<String> copyWithoutNulls(Iterable<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
