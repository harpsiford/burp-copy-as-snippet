package com.copyassnippet.testutil;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TestAssertions {
    private TestAssertions() {
    }

    public static void assertPresetSemantics(Preset expected, Preset actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getHeaderRegexes(), actual.getHeaderRegexes());
        assertEquals(expected.getCookieRegexes(), actual.getCookieRegexes());
        assertEquals(expected.getParamRegexes(), actual.getParamRegexes());
        assertEquals(ruleStrings(expected.getRedactionRules()), ruleStrings(actual.getRedactionRules()));
        assertEquals(expected.getReplacementString(), actual.getReplacementString());
        assertEquals(expected.getTemplate(), actual.getTemplate());
        assertEquals(expected.isEnabled(), actual.isEnabled());
    }

    private static List<String> ruleStrings(List<RedactionRule> rules) {
        List<String> serialized = new ArrayList<>();
        for (RedactionRule rule : rules) {
            serialized.add(rule.getType() + ":" + rule.getPattern());
        }
        return serialized;
    }
}
