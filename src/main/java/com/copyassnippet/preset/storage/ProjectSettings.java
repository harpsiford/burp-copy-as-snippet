package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.PersistedObject;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

public final class ProjectSettings {
    public static final int SCHEMA_VERSION = 1;

    private static final TypeReference<List<StoredPreset>> PRESET_LIST_TYPE = new TypeReference<>() {
    };

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

        String rawJson = container.getString(PresetStorageSchema.PROJECT_PRESETS_DATA_KEY);
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

    public static void writePresets(PersistedObject extensionData, List<Preset> presets) {
        extensionData.deleteChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (presets == null || presets.isEmpty()) {
            return;
        }

        PersistedObject container = PersistedObject.persistedObject();
        container.setInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY, SCHEMA_VERSION);

        List<StoredPreset> serialized = new ArrayList<>();
        for (Preset preset : presets) {
            serialized.add(StoredPreset.fromPreset(preset));
        }

        container.setString(PresetStorageSchema.PROJECT_PRESETS_DATA_KEY, PresetSerializationHelper.writeJson(serialized));
        extensionData.setChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY, container);
    }

    public static void clear(PersistedObject extensionData) {
        extensionData.deleteChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
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
