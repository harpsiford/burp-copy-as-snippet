package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import java.util.ArrayList;
import java.util.List;

public final class ProjectSettingsV0 {
    public static final int SCHEMA_VERSION = 0;

    private static final String PRESET_IDS_LIST_KEY = "ids";
    private static final String PRESET_IDS_RAW_KEY = "idsRaw";

    private ProjectSettingsV0() {
    }

    public static boolean isDetected(PersistedObject extensionData) {
        PersistedObject container = extensionData.getChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        return container != null && container.getInteger(PresetStorageSchema.PROJECT_PRESET_SCHEMA_VERSION_KEY) == null;
    }

    public static List<Preset> readPresets(PersistedObject extensionData) {
        PersistedObject container = extensionData.getChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
        if (container == null) {
            return List.of();
        }

        List<Preset> presets = new ArrayList<>();
        for (String presetId : readPresetIds(container)) {
            Preset preset = loadPreset(container.getChildObject(presetId), presetId);
            if (preset != null) {
                presets.add(preset);
            }
        }
        return presets;
    }

    public static void writePresets(PersistedObject extensionData, List<Preset> presets) {
        clear(extensionData);
        if (presets == null || presets.isEmpty()) {
            return;
        }

        PersistedObject container = PersistedObject.persistedObject();
        List<String> presetIds = new ArrayList<>();
        for (Preset preset : presets) {
            presetIds.add(preset.getId());

            PersistedObject presetObject = PersistedObject.persistedObject();
            savePreset(presetObject, preset);
            container.setChildObject(preset.getId(), presetObject);
        }

        PersistedList<String> idsList = PersistedList.persistedStringList();
        for (String presetId : presetIds) {
            idsList.add(presetId);
        }
        container.setStringList(PRESET_IDS_LIST_KEY, idsList);
        container.setString(PRESET_IDS_RAW_KEY, PresetSerializationHelper.joinMultiline(presetIds));
        extensionData.setChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY, container);
    }

    public static void clear(PersistedObject extensionData) {
        extensionData.deleteChildObject(PresetStorageSchema.PROJECT_PRESETS_KEY);
    }

    private static List<String> readPresetIds(PersistedObject container) {
        List<String> presetIds = PresetSerializationHelper.parseMultiline(container.getString(PRESET_IDS_RAW_KEY));
        if (!presetIds.isEmpty()) {
            return presetIds;
        }

        PersistedList<String> storedIds = container.getStringList(PRESET_IDS_LIST_KEY);
        if (storedIds == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String storedId : storedIds) {
            if (storedId != null && !storedId.isEmpty()) {
                result.add(storedId);
            }
        }
        return result;
    }

    private static Preset loadPreset(PersistedObject object, String presetId) {
        if (object == null) {
            return null;
        }

        String id = object.getString("id");
        String name = object.getString("name");
        if (name == null) {
            return null;
        }

        String template = object.getString("template");
        String replacement = object.getString("replacementString");
        String enabledRaw = object.getString("enabled");

        return new Preset(
                id != null ? id : presetId,
                name,
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("headerRegexes")),
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("cookieRegexes")),
                PresetSerializationHelper.copyWithoutNulls(object.getStringList("paramRegexes")),
                PresetSerializationHelper.parseRulesV0(object.getStringList("redactionRules")),
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabledRaw == null || !"false".equals(enabledRaw)
        );
    }

    private static void savePreset(PersistedObject object, Preset preset) {
        object.setString("id", preset.getId());
        object.setString("name", preset.getName());
        object.setString("template", preset.getTemplate());
        object.setString("enabled", String.valueOf(preset.isEnabled()));
        object.setString("replacementString", preset.getReplacementString());
        object.setStringList("headerRegexes", toPersistedStringList(preset.getHeaderRegexes()));
        object.setStringList("cookieRegexes", toPersistedStringList(preset.getCookieRegexes()));
        object.setStringList("paramRegexes", toPersistedStringList(preset.getParamRegexes()));
        object.setStringList(
                "redactionRules",
                toPersistedStringList(PresetSerializationHelper.serializeLegacyRules(preset.getRedactionRules()))
        );
    }

    private static PersistedList<String> toPersistedStringList(Iterable<String> values) {
        PersistedList<String> list = PersistedList.persistedStringList();
        for (String value : PresetSerializationHelper.copyWithoutNulls(values)) {
            list.add(value);
        }
        return list;
    }
}
