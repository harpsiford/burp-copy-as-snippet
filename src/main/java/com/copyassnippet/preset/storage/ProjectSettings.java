package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProjectSettings {
    public static final int SCHEMA_VERSION = 1;

    private static final String PRESET_IDS_LIST_KEY = "ids";
    private static final String REDACTION_RULES_KEY = "redactionRules";
    private static final String RULE_TYPE_KEY = "type";
    private static final String RULE_PATTERN_KEY = "pattern";

    private ProjectSettings() {
    }

    public static boolean isDetected(PersistedObject extensionData) {
        PersistedObject container = extensionData.getChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (container == null) {
            return false;
        }

        Integer schemaVersion = container.getInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY);
        return schemaVersion != null && schemaVersion == SCHEMA_VERSION;
    }

    public static List<Preset> readPresets(PersistedObject extensionData) {
        PersistedObject container = extensionData.getChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (container == null) {
            return List.of();
        }

        Integer schemaVersion = container.getInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY);
        if (schemaVersion == null || schemaVersion != SCHEMA_VERSION) {
            return List.of();
        }

        PersistedList<String> presetIds = container.getStringList(PRESET_IDS_LIST_KEY);
        if (presetIds == null || presetIds.isEmpty()) {
            return List.of();
        }

        List<Preset> presets = new ArrayList<>();
        for (String presetId : presetIds) {
            Preset preset = loadPreset(container.getChildObject(presetId));
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    public static void writePresets(PersistedObject extensionData, List<Preset> presets) {
        extensionData.deleteChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (presets == null || presets.isEmpty()) {
            return;
        }

        PersistedObject container = PersistedObject.persistedObject();
        container.setInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY, SCHEMA_VERSION);

        PersistedList<String> presetIds = PersistedList.persistedStringList();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());

            PersistedObject presetObject = PersistedObject.persistedObject();
            savePreset(presetObject, preset);
            container.setChildObject(preset.getId(), presetObject);
        }

        container.setStringList(PRESET_IDS_LIST_KEY, presetIds);
        extensionData.setChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY, container);
    }

    public static void clear(PersistedObject extensionData) {
        extensionData.deleteChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
    }

    private static void savePreset(PersistedObject object, Preset preset) {
        object.setString("id", preset.getId());
        object.setString("name", preset.getName());
        object.setString("template", preset.getTemplate());
        object.setBoolean("enabled", preset.isEnabled());
        object.setString("replacementString", preset.getReplacementString());
        object.setStringList("headerRegexes", PresetSerializationHelper.toPersistedStringList(preset.getHeaderRegexes()));
        object.setStringList("cookieRegexes", PresetSerializationHelper.toPersistedStringList(preset.getCookieRegexes()));
        object.setStringList("paramRegexes", PresetSerializationHelper.toPersistedStringList(preset.getParamRegexes()));

        PersistedObject rulesContainer = PersistedObject.persistedObject();
        int index = 0;
        for (RedactionRule rule : preset.getRedactionRules()) {
            if (rule == null || rule.getType() == null || rule.getPattern() == null) {
                continue;
            }

            PersistedObject ruleObject = PersistedObject.persistedObject();
            ruleObject.setString(RULE_TYPE_KEY, rule.getType().name());
            ruleObject.setString(RULE_PATTERN_KEY, rule.getPattern());
            rulesContainer.setChildObject(String.valueOf(index), ruleObject);
            index++;
        }
        object.setChildObject(REDACTION_RULES_KEY, rulesContainer);
    }

    private static Preset loadPreset(PersistedObject object) {
        if (object == null) {
            return null;
        }

        String name = object.getString("name");
        String id = object.getString("id");
        if (id == null || name == null) {
            return null;
        }

        String template = object.getString("template");
        String replacement = object.getString("replacementString");
        Boolean enabledValue = object.getBoolean("enabled");

        return new Preset(
                id,
                name,
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("headerRegexes")),
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("cookieRegexes")),
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("paramRegexes")),
                loadRules(object.getChildObject(REDACTION_RULES_KEY)),
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabledValue == null || enabledValue
        );
    }

    private static List<RedactionRule> loadRules(PersistedObject rulesContainer) {
        if (rulesContainer == null) {
            return List.of();
        }

        List<String> childKeys = new ArrayList<>(rulesContainer.childObjectKeys());
        childKeys.sort(Comparator.comparingInt(ProjectSettings::ruleKeyOrder));

        List<RedactionRule> rules = new ArrayList<>();
        for (String childKey : childKeys) {
            PersistedObject ruleObject = rulesContainer.getChildObject(childKey);
            if (ruleObject == null) {
                continue;
            }

            String type = ruleObject.getString(RULE_TYPE_KEY);
            String pattern = ruleObject.getString(RULE_PATTERN_KEY);
            if (type == null || pattern == null) {
                continue;
            }

            try {
                rules.add(new RedactionRule(RedactionRule.Type.valueOf(type.toUpperCase()), pattern));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid stored rules rather than failing the entire preset.
            }
        }

        return rules;
    }

    private static int ruleKeyOrder(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
