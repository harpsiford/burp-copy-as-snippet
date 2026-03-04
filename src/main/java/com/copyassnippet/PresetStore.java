package com.copyassnippet;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.List;


public class PresetStore {
    private static final String USER_PRESET_PREFIX = "user.preset";
    private static final String USER_PRESET_NAMES_KEY = "user.preset.names";
    private static final String PROJECT_PRESETS_KEY = "project.presets";

    private static final String PRESET_ORDER_KEY = "preset.order";

    private static final String HOTKEY_ENABLED_KEY = "hotkey.enabled";
    private static final String HOTKEY_STRING_KEY = "hotkey.string";

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
        String namesRaw = preferences.getString(USER_PRESET_NAMES_KEY);
        if (namesRaw == null || namesRaw.isEmpty()) return result;

        for (String name : namesRaw.split("\n")) {
            String key = USER_PRESET_PREFIX + "." + name;
            Preset p = Preset.loadFrom(preferences, key);
            if (p != null) result.add(p);
        }
        return result;
    }

    public void setUserPresets(List<Preset> presets) {
        String oldNamesRaw = preferences.getString(USER_PRESET_NAMES_KEY);
        if (oldNamesRaw != null && !oldNamesRaw.isEmpty()) {
            for (String name : oldNamesRaw.split("\n")) {
                String key = USER_PRESET_PREFIX + "." + name;
                preferences.setString(key + ".name", null);
                preferences.setString(key + ".headerRegexes", null);
                preferences.setString(key + ".cookieRegexes", null);
                preferences.setString(key + ".template", null);
            }
        }

        List<String> names = new ArrayList<>();
        for (Preset p : presets) {
            names.add(p.getName());
            String key = USER_PRESET_PREFIX + "." + p.getName();
            p.saveTo(preferences, key);
        }
        preferences.setString(USER_PRESET_NAMES_KEY, String.join("\n", names));
    }

    public List<Preset> getProjectPresets() {
        List<Preset> result = new ArrayList<>();
        PersistedObject container = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (container == null) return result;

        PersistedList<String> names = container.getStringList("names");
        if (names == null) return result;

        for (String name : names) {
            PersistedObject child = container.getChildObject(name);
            Preset p = Preset.loadFrom(child);
            if (p != null) result.add(p);
        }
        return result;
    }

    public void setProjectPresets(List<Preset> presets) {
        PersistedObject container = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (container != null) {
            PersistedList<String> oldNames = container.getStringList("names");
            if (oldNames != null) {
                for (String name : oldNames) {
                    container.deleteChildObject(name);
                }
            }
        }

        container = PersistedObject.persistedObject();
        List<String> names = new ArrayList<>();
        for (Preset p : presets) {
            names.add(p.getName());
            PersistedObject child = PersistedObject.persistedObject();
            p.saveTo(child);
            container.setChildObject(p.getName(), child);
        }

        PersistedList<String> namesList = PersistedList.persistedStringList();
        namesList.addAll(names);
        container.setStringList("names", namesList);

        extensionData.setChildObject(PROJECT_PRESETS_KEY, container);
    }

    public List<Preset> getResolvedPresets() {
        return presetResolver.resolvePresets(getUserPresets(), getProjectPresets(), getPresetOrder());
    }

    List<PresetResolver.ResolvedPreset> getResolvedPresetEntries() {
        return presetResolver.resolve(getUserPresets(), getProjectPresets(), getPresetOrder());
    }

    public List<String> getPresetOrder() {
        String raw = preferences.getString(PRESET_ORDER_KEY);
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String name : raw.split("\n")) {
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }

    public void setPresetOrder(List<String> order) {
        preferences.setString(PRESET_ORDER_KEY, String.join("\n", order));
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
}
