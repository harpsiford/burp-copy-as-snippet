package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import java.util.ArrayList;
import java.util.List;

public final class UserSettingsV0 {
    public static final int SCHEMA_VERSION = 0;

    private static final String USER_PRESET_PREFIX = "user.preset";
    private static final String USER_PRESET_IDS_KEY = "user.preset.ids";
    private static final String PRESET_ORDER_IDS_KEY = "preset.order.ids";
    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";
    private static final String BUILT_IN_DEFAULT_REMOVED_KEY = "builtin.default.removed";

    private UserSettingsV0() {
    }

    public static boolean isDetected(Preferences preferences) {
        return preferences.getString(USER_PRESET_IDS_KEY) != null
                || preferences.getString(PRESET_ORDER_IDS_KEY) != null
                || preferences.getString(HOTKEY_ENABLED_KEY) != null
                || preferences.getString(HOTKEY_STRING_KEY) != null
                || preferences.getString(BUILT_IN_DEFAULT_REMOVED_KEY) != null;
    }

    public static List<Preset> readPresets(Preferences preferences) {
        List<Preset> presets = new ArrayList<>();
        for (String presetId : PresetSerializationHelper.parseMultiline(preferences.getString(USER_PRESET_IDS_KEY))) {
            Preset preset = loadPreset(preferences, userPresetKey(presetId), presetId);
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    public static List<String> readPresetOrder(Preferences preferences) {
        return PresetSerializationHelper.parseMultiline(preferences.getString(PRESET_ORDER_IDS_KEY));
    }

    public static Boolean readHotkeyEnabled(Preferences preferences) {
        return PresetSerializationHelper.parseBooleanString(preferences.getString(HOTKEY_ENABLED_KEY));
    }

    public static Boolean readBuiltInDefaultRemoved(Preferences preferences) {
        return PresetSerializationHelper.parseBooleanString(preferences.getString(BUILT_IN_DEFAULT_REMOVED_KEY));
    }

    public static String readHotkeyString(Preferences preferences) {
        return preferences.getString(HOTKEY_STRING_KEY);
    }

    public static void writePresets(Preferences preferences, List<Preset> presets) {
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
        preferences.setString(USER_PRESET_IDS_KEY, PresetSerializationHelper.joinMultiline(presetIds));
    }

    public static void writePresetOrder(Preferences preferences, List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            preferences.deleteString(PRESET_ORDER_IDS_KEY);
            return;
        }
        preferences.setString(PRESET_ORDER_IDS_KEY, PresetSerializationHelper.joinMultiline(orderIds));
    }

    public static void writeHotkeyEnabled(Preferences preferences, boolean enabled) {
        preferences.setString(HOTKEY_ENABLED_KEY, String.valueOf(enabled));
    }

    public static void writeHotkeyString(Preferences preferences, String hotkey) {
        preferences.setString(HOTKEY_STRING_KEY, hotkey);
    }

    public static void writeBuiltInDefaultRemoved(Preferences preferences, boolean removed) {
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
        Boolean enabled = PresetSerializationHelper.parseBooleanString(preferences.getString(keyPrefix + ".enabled"));

        return new Preset(
                id != null ? id : presetId,
                name,
                PresetSerializationHelper.parseMultiline(preferences.getString(keyPrefix + ".headerRegexes")),
                PresetSerializationHelper.parseMultiline(preferences.getString(keyPrefix + ".cookieRegexes")),
                PresetSerializationHelper.parseMultiline(preferences.getString(keyPrefix + ".paramRegexes")),
                PresetSerializationHelper.parseRulesV0(
                        PresetSerializationHelper.parseMultiline(preferences.getString(keyPrefix + ".redactionRules"))
                ),
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabled == null || enabled
        );
    }

    private static void savePreset(Preferences preferences, String keyPrefix, Preset preset) {
        preferences.setString(keyPrefix + ".id", preset.getId());
        preferences.setString(keyPrefix + ".name", preset.getName());
        preferences.setString(
                keyPrefix + ".headerRegexes",
                PresetSerializationHelper.joinMultiline(preset.getHeaderRegexes())
        );
        preferences.setString(
                keyPrefix + ".cookieRegexes",
                PresetSerializationHelper.joinMultiline(preset.getCookieRegexes())
        );
        preferences.setString(
                keyPrefix + ".paramRegexes",
                PresetSerializationHelper.joinMultiline(preset.getParamRegexes())
        );
        preferences.setString(keyPrefix + ".template", preset.getTemplate());
        preferences.setString(keyPrefix + ".enabled", String.valueOf(preset.isEnabled()));
        preferences.setString(keyPrefix + ".replacementString", preset.getReplacementString());
        preferences.setString(
                keyPrefix + ".redactionRules",
                PresetSerializationHelper.joinMultiline(
                        PresetSerializationHelper.serializeLegacyRules(preset.getRedactionRules())
                )
        );
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
}
