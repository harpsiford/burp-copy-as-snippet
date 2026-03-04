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
        for (String headerRegex : preset.getHeaderRegexes()) {
            if (headerRegex != null) {
                headerList.add(headerRegex);
            }
        }
        object.setStringList("headerRegexes", headerList);

        PersistedList<String> cookieList = PersistedList.persistedStringList();
        for (String cookieRegex : preset.getCookieRegexes()) {
            if (cookieRegex != null) {
                cookieList.add(cookieRegex);
            }
        }
        object.setStringList("cookieRegexes", cookieList);

        PersistedList<String> paramList = PersistedList.persistedStringList();
        for (String paramRegex : preset.getParamRegexes()) {
            if (paramRegex != null) {
                paramList.add(paramRegex);
            }
        }
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
                if (serialized == null) {
                    continue;
                }
                RedactionRule rule = RedactionRule.fromSerializedString(serialized);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return new Preset(
                id != null ? id : fallbackId,
                name,
                sanitizedStrings(headers),
                sanitizedStrings(cookies),
                sanitizedStrings(params),
                rules,
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabled
        );
    }

    private static List<String> sanitizedStrings(Iterable<String> values) {
        if (values == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
