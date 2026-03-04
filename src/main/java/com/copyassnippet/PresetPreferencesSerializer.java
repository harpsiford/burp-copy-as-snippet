package com.copyassnippet;

import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class PresetPreferencesSerializer {

    private PresetPreferencesSerializer() {
    }

    static void save(Preferences prefs, String keyPrefix, Preset preset) {
        prefs.setString(keyPrefix + ".id", preset.getId());
        prefs.setString(keyPrefix + ".name", preset.getName());
        prefs.setString(keyPrefix + ".headerRegexes", joinSanitizedLines(preset.getHeaderRegexes()));
        prefs.setString(keyPrefix + ".cookieRegexes", joinSanitizedLines(preset.getCookieRegexes()));
        prefs.setString(keyPrefix + ".paramRegexes", joinSanitizedLines(preset.getParamRegexes()));
        prefs.setString(keyPrefix + ".template", preset.getTemplate());
        prefs.setString(keyPrefix + ".enabled", String.valueOf(preset.isEnabled()));
        prefs.setString(keyPrefix + ".replacementString", preset.getReplacementString());
        prefs.setString(keyPrefix + ".redactionRules",
                preset.getRedactionRules().stream()
                        .map(RedactionRule::toSerializedString)
                        .collect(Collectors.joining("\n")));
    }

    static Preset load(Preferences prefs, String keyPrefix, String fallbackId) {
        String id = prefs.getString(keyPrefix + ".id");
        String name = prefs.getString(keyPrefix + ".name");
        if (name == null) {
            return null;
        }

        String headersRaw = prefs.getString(keyPrefix + ".headerRegexes");
        String cookiesRaw = prefs.getString(keyPrefix + ".cookieRegexes");
        String paramsRaw = prefs.getString(keyPrefix + ".paramRegexes");
        String template = prefs.getString(keyPrefix + ".template");
        String replacement = prefs.getString(keyPrefix + ".replacementString");
        String rulesRaw = prefs.getString(keyPrefix + ".redactionRules");

        String enabledStr = prefs.getString(keyPrefix + ".enabled");
        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        List<RedactionRule> rules = new ArrayList<>();
        if (rulesRaw != null && !rulesRaw.isBlank()) {
            for (String line : rulesRaw.split("\\n")) {
                RedactionRule rule = RedactionRule.fromSerializedString(line);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return new Preset(
                id != null ? id : fallbackId,
                name,
                splitLines(headersRaw),
                splitLines(cookiesRaw),
                splitLines(paramsRaw),
                rules,
                replacement != null ? replacement : DefaultPresetFactory.DEFAULT_REPLACEMENT,
                template != null ? template : DefaultPresetFactory.DEFAULT_TEMPLATE,
                enabled
        );
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String line : value.split("\\R")) {
            if (line != null && !line.isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    private static String joinSanitizedLines(List<String> values) {
        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            if (value != null) {
                sanitized.add(value);
            }
        }
        return String.join("\n", sanitized);
    }
}
