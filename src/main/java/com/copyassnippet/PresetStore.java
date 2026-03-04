package com.copyassnippet;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PresetStore {
    private static final String USER_PRESET_PREFIX = "user.preset";
    private static final String USER_PRESET_IDS_KEY = "user.preset.ids";
    private static final String USER_PRESET_NAMES_KEY_LEGACY = "user.preset.names";

    private static final String PROJECT_PRESETS_KEY = "project.presets";
    private static final String PROJECT_PRESET_IDS_LIST_KEY = "ids";
    private static final String PROJECT_PRESET_NAMES_LIST_KEY_LEGACY = "names";

    private static final String PRESET_ORDER_IDS_KEY = "preset.order.ids";
    private static final String PRESET_ORDER_KEY_LEGACY = "preset.order";

    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";

    private static final String[] USER_PRESET_FIELD_SUFFIXES = {
            ".id",
            ".name",
            ".headerRegexes",
            ".cookieRegexes",
            ".paramRegexes",
            ".template",
            ".enabled",
            ".replacementString",
            ".redactionRules"
    };

    public static final String DEFAULT_HOTKEY = "Ctrl+Shift+C";

    private final Preferences preferences;
    private final PersistedObject extensionData;
    private final PresetResolver presetResolver;

    public PresetStore(MontoyaApi api) {
        this(api, new PresetResolver());
    }

    PresetStore(MontoyaApi api, PresetResolver presetResolver) {
        this.preferences = api.persistence().preferences();
        this.extensionData = api.persistence().extensionData();
        this.presetResolver = presetResolver;
    }

    public List<Preset> getUserPresets() {
        List<Preset> result = new ArrayList<>();
        List<String> presetIds = parseMultiline(preferences.getString(USER_PRESET_IDS_KEY));
        if (!presetIds.isEmpty()) {
            for (String presetId : presetIds) {
                String key = userPresetKey(presetId);
                Preset preset = Preset.loadFrom(preferences, key, presetId);
                if (preset != null) {
                    result.add(preset);
                }
            }
            return result;
        }

        List<String> legacyNames = parseMultiline(preferences.getString(USER_PRESET_NAMES_KEY_LEGACY));
        for (String legacyName : legacyNames) {
            String key = userPresetKey(legacyName);
            Preset preset = Preset.loadFrom(preferences, key, legacyUserPresetId(legacyName));
            if (preset != null) {
                result.add(preset);
            }
        }
        return result;
    }

    public void setUserPresets(List<Preset> presets) {
        for (String presetId : parseMultiline(preferences.getString(USER_PRESET_IDS_KEY))) {
            clearUserPresetEntry(userPresetKey(presetId));
        }

        for (String legacyName : parseMultiline(preferences.getString(USER_PRESET_NAMES_KEY_LEGACY))) {
            clearUserPresetEntry(userPresetKey(legacyName));
        }

        List<String> presetIds = new ArrayList<>();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());
            preset.saveTo(preferences, userPresetKey(preset.getId()));
        }

        preferences.setString(USER_PRESET_IDS_KEY, String.join("\n", presetIds));
        preferences.setString(USER_PRESET_NAMES_KEY_LEGACY, null);
    }

    public List<Preset> getProjectPresets() {
        List<Preset> result = new ArrayList<>();
        PersistedObject container = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (container == null) {
            return result;
        }

        PersistedList<String> presetIds = container.getStringList(PROJECT_PRESET_IDS_LIST_KEY);
        if (presetIds != null && !presetIds.isEmpty()) {
            for (String presetId : presetIds) {
                PersistedObject child = container.getChildObject(presetId);
                Preset preset = Preset.loadFrom(child, presetId);
                if (preset != null) {
                    result.add(preset);
                }
            }
            return result;
        }

        PersistedList<String> legacyNames = container.getStringList(PROJECT_PRESET_NAMES_LIST_KEY_LEGACY);
        if (legacyNames == null) {
            return result;
        }

        for (String legacyName : legacyNames) {
            PersistedObject child = container.getChildObject(legacyName);
            Preset preset = Preset.loadFrom(child, legacyProjectPresetId(legacyName));
            if (preset != null) {
                result.add(preset);
            }
        }

        return result;
    }

    public void setProjectPresets(List<Preset> presets) {
        PersistedObject existingContainer = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (existingContainer != null) {
            deleteProjectChildren(existingContainer.getStringList(PROJECT_PRESET_IDS_LIST_KEY), existingContainer);
            deleteProjectChildren(existingContainer.getStringList(PROJECT_PRESET_NAMES_LIST_KEY_LEGACY), existingContainer);
        }

        PersistedObject container = PersistedObject.persistedObject();
        List<String> presetIds = new ArrayList<>();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());
            PersistedObject child = PersistedObject.persistedObject();
            preset.saveTo(child);
            container.setChildObject(preset.getId(), child);
        }

        PersistedList<String> idsList = PersistedList.persistedStringList();
        idsList.addAll(presetIds);
        container.setStringList(PROJECT_PRESET_IDS_LIST_KEY, idsList);

        extensionData.setChildObject(PROJECT_PRESETS_KEY, container);
    }

    public List<Preset> getResolvedPresets() {
        List<Preset> userPresets = getUserPresets();
        List<Preset> projectPresets = getProjectPresets();
        List<String> order = getPresetOrder(userPresets, projectPresets);
        return presetResolver.resolvePresets(userPresets, projectPresets, order);
    }

    List<PresetResolver.ResolvedPreset> getResolvedPresetEntries() {
        List<Preset> userPresets = getUserPresets();
        List<Preset> projectPresets = getProjectPresets();
        List<String> order = getPresetOrder(userPresets, projectPresets);
        return presetResolver.resolve(userPresets, projectPresets, order);
    }

    public List<String> getPresetOrder() {
        return getPresetOrder(getUserPresets(), getProjectPresets());
    }

    private List<String> getPresetOrder(List<Preset> userPresets, List<Preset> projectPresets) {
        List<String> orderIds = parseMultiline(preferences.getString(PRESET_ORDER_IDS_KEY));
        if (!orderIds.isEmpty()) {
            return orderIds;
        }

        List<String> legacyOrderNames = parseMultiline(preferences.getString(PRESET_ORDER_KEY_LEGACY));
        if (legacyOrderNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<PresetResolver.ResolvedPreset> resolvedPresets = presetResolver.resolve(userPresets, projectPresets, List.of());
        Map<String, String> idByName = new LinkedHashMap<>();
        for (PresetResolver.ResolvedPreset resolvedPreset : resolvedPresets) {
            idByName.put(resolvedPreset.getPreset().getName(), resolvedPreset.getPreset().getId());
        }

        List<String> migratedOrderIds = new ArrayList<>();
        for (String presetName : legacyOrderNames) {
            String presetId = idByName.get(presetName);
            if (presetId != null) {
                migratedOrderIds.add(presetId);
            }
        }

        if (!migratedOrderIds.isEmpty()) {
            preferences.setString(PRESET_ORDER_IDS_KEY, String.join("\n", migratedOrderIds));
        }

        return migratedOrderIds;
    }

    public void setPresetOrder(List<String> orderIds) {
        preferences.setString(PRESET_ORDER_IDS_KEY, String.join("\n", orderIds));
        preferences.setString(PRESET_ORDER_KEY_LEGACY, null);
    }

    public boolean isHotkeyEnabled() {
        String val = preferences.getString(HOTKEY_ENABLED_KEY);
        return "true".equals(val);
    }

    public void setHotkeyEnabled(boolean enabled) {
        preferences.setString(HOTKEY_ENABLED_KEY, String.valueOf(enabled));
    }

    public String getHotkeyString() {
        String val = preferences.getString(HOTKEY_STRING_KEY);
        return val != null ? val : DEFAULT_HOTKEY;
    }

    public void setHotkeyString(String hotkey) {
        preferences.setString(HOTKEY_STRING_KEY, hotkey);
    }

    public boolean isPresetNameTaken(String presetName, String excludedPresetId) {
        String candidate = presetName != null ? presetName.trim() : "";
        if (candidate.isEmpty()) {
            return false;
        }

        for (PresetResolver.ResolvedPreset resolvedPreset : getResolvedPresetEntries()) {
            Preset preset = resolvedPreset.getPreset();
            if (preset.getName().equals(candidate) && !preset.getId().equals(excludedPresetId)) {
                return true;
            }
        }

        return false;
    }

    private static String userPresetKey(String storageKeyPart) {
        return USER_PRESET_PREFIX + "." + storageKeyPart;
    }

    private void clearUserPresetEntry(String keyPrefix) {
        for (String fieldSuffix : USER_PRESET_FIELD_SUFFIXES) {
            preferences.setString(keyPrefix + fieldSuffix, null);
        }
    }

    private static void deleteProjectChildren(PersistedList<String> keys, PersistedObject container) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            container.deleteChildObject(key);
        }
    }

    private static List<String> parseMultiline(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }

        for (String value : raw.split("\n")) {
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static String legacyUserPresetId(String legacyName) {
        return deterministicLegacyId("user:" + legacyName);
    }

    private static String legacyProjectPresetId(String legacyName) {
        return deterministicLegacyId("project:" + legacyName);
    }

    private static String deterministicLegacyId(String seed) {
        return "legacy-" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
