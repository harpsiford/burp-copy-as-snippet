package com.copyassnippet;

import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;

import java.util.ArrayList;
import java.util.List;

final class PresetPersistedObjectSerializer {

    private PresetPersistedObjectSerializer() {
    }

    static void save(PersistedObject object, Preset preset) {
        object.setString("id", preset.getId());
        object.setString("name", preset.getName());
        object.setString("template", preset.getTemplate());
        object.setString("enabled", String.valueOf(preset.isEnabled()));
        object.setString("replacementString", preset.getReplacementString());

        PersistedList<String> headerList = PersistedList.persistedStringList();
        headerList.addAll(preset.getHeaderRegexes());
        object.setStringList("headerRegexes", headerList);

        PersistedList<String> cookieList = PersistedList.persistedStringList();
        cookieList.addAll(preset.getCookieRegexes());
        object.setStringList("cookieRegexes", cookieList);

        PersistedList<String> paramList = PersistedList.persistedStringList();
        paramList.addAll(preset.getParamRegexes());
        object.setStringList("paramRegexes", paramList);

        PersistedList<String> ruleList = PersistedList.persistedStringList();
        for (RedactionRule rule : preset.getRedactionRules()) {
            ruleList.add(rule.toSerializedString());
        }
        object.setStringList("redactionRules", ruleList);
    }

    static Preset load(PersistedObject object, String fallbackId) {
        if (object == null) {
            return null;
        }

        String name = object.getString("name");
        if (name == null) {
            return null;
        }

        String id = object.getString("id");
        String template = object.getString("template");
        String replacement = object.getString("replacementString");
        String enabledStr = object.getString("enabled");

        PersistedList<String> headers = object.getStringList("headerRegexes");
        PersistedList<String> cookies = object.getStringList("cookieRegexes");
        PersistedList<String> params = object.getStringList("paramRegexes");
        PersistedList<String> rulesRaw = object.getStringList("redactionRules");

        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        List<RedactionRule> rules = new ArrayList<>();
        if (rulesRaw != null) {
            for (String serialized : rulesRaw) {
                RedactionRule rule = RedactionRule.fromSerializedString(serialized);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return new Preset(
                id != null ? id : fallbackId,
                name,
                headers != null ? new ArrayList<>(headers) : List.of(),
                cookies != null ? new ArrayList<>(cookies) : List.of(),
                params != null ? new ArrayList<>(params) : List.of(),
                rules,
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabled
        );
    }
}
