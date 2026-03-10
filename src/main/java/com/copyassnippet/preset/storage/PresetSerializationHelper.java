package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.PersistedList;
import com.copyassnippet.preset.model.RedactionRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

final class PresetSerializationHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private PresetSerializationHelper() {
    }

    static List<String> parseMultiline(String raw) {
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

    static String joinMultiline(Iterable<String> values) {
        if (values == null) {
            return "";
        }
        return String.join("\n", copyWithoutNulls(values));
    }

    static PersistedList<String> toPersistedStringList(Iterable<String> values) {
        PersistedList<String> list = PersistedList.persistedStringList();
        for (String value : copyWithoutNulls(values)) {
            list.add(value);
        }
        return list;
    }

    static <T> T readJson(String rawJson, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(rawJson, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse preset settings JSON.", exception);
        }
    }

    static String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize preset settings JSON.", exception);
        }
    }

    static Boolean parseBooleanString(String raw) {
        if ("true".equals(raw)) {
            return true;
        }
        if ("false".equals(raw)) {
            return false;
        }
        return null;
    }

    static List<RedactionRule> parseRulesV0(Iterable<String> rawRules) {
        List<RedactionRule> rules = new ArrayList<>();
        if (rawRules == null) {
            return rules;
        }

        for (String rawRule : rawRules) {
            if (rawRule == null || rawRule.isBlank()) {
                continue;
            }

            int separator = rawRule.indexOf(':');
            if (separator < 0) {
                continue;
            }

            String type = rawRule.substring(0, separator).trim();
            String pattern = rawRule.substring(separator + 1);
            try {
                rules.add(new RedactionRule(RedactionRule.Type.valueOf(type.toUpperCase()), pattern));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid legacy rules rather than failing the whole preset.
            }
        }
        return rules;
    }

    static List<String> serializeLegacyRules(List<RedactionRule> rules) {
        List<String> serialized = new ArrayList<>();
        if (rules == null) {
            return serialized;
        }

        for (RedactionRule rule : rules) {
            if (rule == null || rule.getType() == null || rule.getPattern() == null) {
                continue;
            }
            serialized.add(rule.getType().name() + ":" + rule.getPattern());
        }
        return serialized;
    }

    static List<String> copyWithoutNulls(Iterable<String> values) {
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
