package example.contextmenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages preset persistence at two levels:
 * - User-level: stored via Preferences with prefixed keys (persists across projects)
 * - Project-level: stored via extensionData() PersistedObject child objects (per-project)
 *
 * When resolving presets, project-level presets override user-level ones with the same name.
 * A built-in "Default" preset is always available as a fallback.
 */
public class PresetStore {
    private static final String USER_PRESET_PREFIX = "user.preset";
    private static final String USER_PRESET_NAMES_KEY = "user.preset.names";
    private static final String PROJECT_PRESETS_KEY = "project.presets";

    private final Preferences preferences;
    private final PersistedObject extensionData;

    public PresetStore(MontoyaApi api) {
        this.preferences = api.persistence().preferences();
        this.extensionData = api.persistence().extensionData();
    }

    // --- User-level presets (flat Preferences with prefixed keys) ---

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
        // Clear old entries
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

        // Write new entries
        List<String> names = new ArrayList<>();
        for (Preset p : presets) {
            names.add(p.getName());
            String key = USER_PRESET_PREFIX + "." + p.getName();
            p.saveTo(preferences, key);
        }
        preferences.setString(USER_PRESET_NAMES_KEY, String.join("\n", names));
    }

    // --- Project-level presets (PersistedObject with child objects) ---

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
        // Clear old child objects
        PersistedObject container = extensionData.getChildObject(PROJECT_PRESETS_KEY);
        if (container != null) {
            PersistedList<String> oldNames = container.getStringList("names");
            if (oldNames != null) {
                for (String name : oldNames) {
                    container.deleteChildObject(name);
                }
            }
        }

        // Create fresh container and write new entries
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

    // --- Merged view ---

    /**
     * Returns the merged list of presets. Project-level presets override user-level
     * ones with the same name. If no presets exist at all, returns the built-in Default.
     */
    public List<Preset> getResolvedPresets() {
        Map<String, Preset> merged = new LinkedHashMap<>();

        // Start with built-in default
        Preset builtIn = Preset.createDefault();
        merged.put(builtIn.getName(), builtIn);

        // Layer user-level presets (can override built-in Default)
        for (Preset p : getUserPresets()) {
            merged.put(p.getName(), p);
        }

        // Layer project-level presets (override user-level with same name)
        for (Preset p : getProjectPresets()) {
            merged.put(p.getName(), p);
        }

        return new ArrayList<>(merged.values());
    }
}
