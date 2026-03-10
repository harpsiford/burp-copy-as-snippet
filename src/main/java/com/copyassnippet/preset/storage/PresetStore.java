package com.copyassnippet.preset.storage;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.service.PresetResolver;
import com.copyassnippet.preset.storage.migrations.PresetStoreMigrator;

import java.util.ArrayList;
import java.util.List;

public class PresetStore {
    public static final String DEFAULT_HOTKEY = "Ctrl+Shift+C";

    private final Preferences preferences;
    private final PersistedObject extensionData;
    private final PresetResolver presetResolver;
    private List<Preset> cachedUserPresets;
    private List<Preset> cachedProjectPresets;
    private List<String> cachedPresetOrder;
    private List<Preset> cachedResolvedPresets;
    private Boolean cachedBuiltInDefaultRemoved;

    public PresetStore(MontoyaApi api) {
        this(api, new PresetResolver());
    }

    PresetStore(MontoyaApi api, PresetResolver presetResolver) {
        this.preferences = api.persistence().preferences();
        this.extensionData = api.persistence().extensionData();
        this.presetResolver = presetResolver;
        PresetStoreMigrator.migrate(preferences, extensionData);
    }

    public List<Preset> getUserPresets() {
        if (cachedUserPresets == null) {
            cachedUserPresets = UserSettings.readPresets(preferences);
        }
        return copyPresets(cachedUserPresets);
    }

    public void setUserPresets(List<Preset> presets) {
        UserSettings.writePresets(preferences, presets);
        cachedUserPresets = copyPresets(presets);
        invalidateResolvedPresetCache();
    }

    public List<Preset> getProjectPresets() {
        if (cachedProjectPresets == null) {
            cachedProjectPresets = ProjectSettings.readPresets(extensionData);
        }
        return copyPresets(cachedProjectPresets);
    }

    public void setProjectPresets(List<Preset> presets) {
        ProjectSettings.writePresets(extensionData, presets);
        cachedProjectPresets = copyPresets(presets);
        invalidateResolvedPresetCache();
    }

    public List<Preset> getResolvedPresets() {
        if (cachedResolvedPresets == null) {
            List<Preset> userPresets = getUserPresets();
            List<Preset> projectPresets = getProjectPresets();
            List<String> order = getPresetOrder();
            boolean includeBuiltIn = shouldIncludeBuiltInDefault(userPresets, projectPresets);
            cachedResolvedPresets = presetResolver.resolvePresets(userPresets, projectPresets, order, includeBuiltIn);
        }
        return copyPresets(cachedResolvedPresets);
    }

    public List<PresetResolver.ResolvedPreset> getResolvedPresetEntries() {
        List<Preset> userPresets = getUserPresets();
        List<Preset> projectPresets = getProjectPresets();
        List<String> order = getPresetOrder();
        boolean includeBuiltIn = shouldIncludeBuiltInDefault(userPresets, projectPresets);
        return presetResolver.resolve(userPresets, projectPresets, order, includeBuiltIn);
    }

    public List<String> getPresetOrder() {
        if (cachedPresetOrder == null) {
            cachedPresetOrder = UserSettings.readPresetOrder(preferences);
        }
        return new ArrayList<>(cachedPresetOrder);
    }

    public void setPresetOrder(List<String> orderIds) {
        UserSettings.writePresetOrder(preferences, orderIds);
        cachedPresetOrder = new ArrayList<>(orderIds);
        invalidateResolvedPresetCache();
    }

    public boolean isHotkeyEnabled() {
        return UserSettings.isHotkeyEnabled(preferences);
    }

    public void setHotkeyEnabled(boolean enabled) {
        UserSettings.writeHotkeyEnabled(preferences, enabled);
    }

    public String getHotkeyString() {
        return UserSettings.readHotkeyString(preferences, DEFAULT_HOTKEY);
    }

    public void setHotkeyString(String hotkey) {
        UserSettings.writeHotkeyString(preferences, hotkey);
    }

    public boolean isBuiltInDefaultRemoved() {
        if (cachedBuiltInDefaultRemoved == null) {
            cachedBuiltInDefaultRemoved = UserSettings.isBuiltInDefaultRemoved(preferences);
        }
        return cachedBuiltInDefaultRemoved;
    }

    public void setBuiltInDefaultRemoved(boolean removed) {
        UserSettings.writeBuiltInDefaultRemoved(preferences, removed);
        cachedBuiltInDefaultRemoved = removed;
        invalidateResolvedPresetCache();
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
        preferences.deleteInteger(PresetStorageSchema.USER_SETTINGS_SCHEMA_VERSION_KEY);
        UserSettings.clear(preferences);
        ProjectSettings.clear(extensionData);
        cachedUserPresets = List.of();
        cachedProjectPresets = List.of();
        cachedPresetOrder = List.of();
        cachedBuiltInDefaultRemoved = false;
        invalidateResolvedPresetCache();
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

    private void invalidateResolvedPresetCache() {
        cachedResolvedPresets = null;
    }

    private static List<Preset> copyPresets(List<Preset> presets) {
        List<Preset> copies = new ArrayList<>();
        for (Preset preset : presets) {
            copies.add(copyPreset(preset));
        }
        return copies;
    }

    private static Preset copyPreset(Preset preset) {
        return new Preset(
                preset.getId(),
                preset.getName(),
                new ArrayList<>(preset.getHeaderRegexes()),
                new ArrayList<>(preset.getCookieRegexes()),
                new ArrayList<>(preset.getParamRegexes()),
                copyRedactionRules(preset.getRedactionRules()),
                preset.getReplacementString(),
                preset.getTemplate(),
                preset.isEnabled()
        );
    }

    private static List<com.copyassnippet.preset.model.RedactionRule> copyRedactionRules(
            List<com.copyassnippet.preset.model.RedactionRule> rules
    ) {
        List<com.copyassnippet.preset.model.RedactionRule> copies = new ArrayList<>();
        for (com.copyassnippet.preset.model.RedactionRule rule : rules) {
            copies.add(new com.copyassnippet.preset.model.RedactionRule(rule.getType(), rule.getPattern()));
        }
        return copies;
    }
}
