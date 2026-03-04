package com.copyassnippet.preset.storage;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.service.PresetResolver;

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
    private static final String PROJECT_PRESET_IDS_RAW_KEY = "idsRaw";
    private static final String PROJECT_PRESET_NAMES_LIST_KEY_LEGACY = "names";

    private static final String PRESET_ORDER_IDS_KEY = "preset.order.ids";
    private static final String PRESET_ORDER_KEY_LEGACY = "preset.order";

    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";
    private static final String BUILT_IN_DEFAULT_REMOVED_KEY = "builtin.default.removed";

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
                Preset preset = PresetPreferencesSerializer.load(preferences, key, presetId);
                if (preset != null) {
                    result.add(preset);
                }
            }
            return result;
        }

        List<String> legacyNames = parseMultiline(preferences.getString(USER_PRESET_NAMES_KEY_LEGACY));
        for (String legacyName : legacyNames) {
            String key = userPresetKey(legacyName);
            Preset preset = PresetPreferencesSerializer.load(preferences, key, legacyUserPresetId(legacyName));
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
            PresetPreferencesSerializer.save(preferences, userPresetKey(preset.getId()), preset);
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

        List<String> presetIdsRaw = parseMultiline(container.getString(PROJECT_PRESET_IDS_RAW_KEY));
        if (!presetIdsRaw.isEmpty()) {
            loadProjectPresetsByIds(container, presetIdsRaw, result);
            if (!result.isEmpty()) {
                return result;
            }
        }

        PersistedList<String> presetIds = container.getStringList(PROJECT_PRESET_IDS_LIST_KEY);
        if (presetIds != null && !presetIds.isEmpty()) {
            loadProjectPresetsByIds(container, presetIds, result);
            if (!result.isEmpty()) {
                return result;
            }
        }

        PersistedList<String> legacyNames = container.getStringList(PROJECT_PRESET_NAMES_LIST_KEY_LEGACY);
        if (legacyNames == null) {
            return result;
        }

        for (String legacyName : legacyNames) {
            PersistedObject child = container.getChildObject(legacyName);
            Preset preset = PresetPersistedObjectSerializer.load(child, legacyProjectPresetId(legacyName));
            if (preset != null) {
                result.add(preset);
            }
        }

        return result;
    }

    public void setProjectPresets(List<Preset> presets) {
        PersistedObject existingContainer = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (existingContainer != null) {
            deleteProjectChildren(parseMultiline(existingContainer.getString(PROJECT_PRESET_IDS_RAW_KEY)), existingContainer);
            deleteProjectChildren(existingContainer.getStringList(PROJECT_PRESET_IDS_LIST_KEY), existingContainer);
            deleteProjectChildren(existingContainer.getStringList(PROJECT_PRESET_NAMES_LIST_KEY_LEGACY), existingContainer);
        }

        PersistedObject container = PersistedObject.persistedObject();
        List<String> presetIds = new ArrayList<>();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());
            PersistedObject child = PersistedObject.persistedObject();
            PresetPersistedObjectSerializer.save(child, preset);
            container.setChildObject(preset.getId(), child);
        }

        PersistedList<String> idsList = PersistedList.persistedStringList();
        for (String presetId : presetIds) {
            idsList.add(presetId);
        }
        container.setStringList(PROJECT_PRESET_IDS_LIST_KEY, idsList);
        container.setString(PROJECT_PRESET_IDS_RAW_KEY, String.join("\n", presetIds));

        extensionData.setChildObject(PROJECT_PRESETS_KEY, container);
    }

    public List<Preset> getResolvedPresets() {
        List<Preset> userPresets = getUserPresets();
        List<Preset> projectPresets = getProjectPresets();
        List<String> order = getPresetOrder(userPresets, projectPresets);
        boolean includeBuiltIn = shouldIncludeBuiltInDefault(userPresets, projectPresets);
        return presetResolver.resolvePresets(userPresets, projectPresets, order, includeBuiltIn);
    }

    public List<PresetResolver.ResolvedPreset> getResolvedPresetEntries() {
        List<Preset> userPresets = getUserPresets();
        List<Preset> projectPresets = getProjectPresets();
        List<String> order = getPresetOrder(userPresets, projectPresets);
        boolean includeBuiltIn = shouldIncludeBuiltInDefault(userPresets, projectPresets);
        return presetResolver.resolve(userPresets, projectPresets, order, includeBuiltIn);
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

        boolean includeBuiltIn = shouldIncludeBuiltInDefault(userPresets, projectPresets);
        List<PresetResolver.ResolvedPreset> resolvedPresets = presetResolver.resolve(userPresets, projectPresets, List.of(), includeBuiltIn);
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
        return val == null || val.isBlank() ? DEFAULT_HOTKEY : val;
    }

    public void setHotkeyString(String hotkey) {
        preferences.setString(HOTKEY_STRING_KEY, hotkey);
    }

    public boolean isBuiltInDefaultRemoved() {
        return "true".equals(preferences.getString(BUILT_IN_DEFAULT_REMOVED_KEY));
    }

    public void setBuiltInDefaultRemoved(boolean removed) {
        preferences.setString(BUILT_IN_DEFAULT_REMOVED_KEY, String.valueOf(removed));
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

    public void resetAllSettings() {
        clearUserPresetSettings();
        extensionData.deleteChildObject(PROJECT_PRESETS_KEY);

        preferences.deleteString(PRESET_ORDER_IDS_KEY);
        preferences.deleteString(PRESET_ORDER_KEY_LEGACY);
        preferences.deleteString(HOTKEY_ENABLED_KEY);
        preferences.deleteString(HOTKEY_STRING_KEY);
        preferences.deleteString(BUILT_IN_DEFAULT_REMOVED_KEY);
    }

    /**
     * Kept for compatibility with older code paths.
     * Intentionally no-op to avoid unexpected wipes during extension unload/reload.
     */
    @Deprecated
    public void clearAllSettings() {
        // no-op
    }

    private static String userPresetKey(String storageKeyPart) {
        return USER_PRESET_PREFIX + "." + storageKeyPart;
    }

    private boolean shouldIncludeBuiltInDefault(List<Preset> userPresets, List<Preset> projectPresets) {
        if (isBuiltInDefaultRemoved()) {
            return false;
        }
        return !containsBuiltInDefaultId(userPresets) && !containsBuiltInDefaultId(projectPresets);
    }

    private static boolean containsBuiltInDefaultId(List<Preset> presets) {
        for (Preset preset : presets) {
            if (Preset.BUILT_IN_ID.equals(preset.getId())) {
                return true;
            }
        }
        return false;
    }

    private void clearUserPresetSettings() {
        for (String key : new ArrayList<>(preferences.stringKeys())) {
            if (key.startsWith(USER_PRESET_PREFIX + ".")) {
                preferences.deleteString(key);
            }
        }
        preferences.deleteString(USER_PRESET_IDS_KEY);
        preferences.deleteString(USER_PRESET_NAMES_KEY_LEGACY);
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

    private static void deleteProjectChildren(List<String> keys, PersistedObject container) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            container.deleteChildObject(key);
        }
    }

    private static void loadProjectPresetsByIds(PersistedObject container, Iterable<String> presetIds, List<Preset> target) {
        for (String presetId : presetIds) {
            PersistedObject child = container.getChildObject(presetId);
            Preset preset = PresetPersistedObjectSerializer.load(child, presetId);
            if (preset != null) {
                target.add(preset);
            }
        }
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
