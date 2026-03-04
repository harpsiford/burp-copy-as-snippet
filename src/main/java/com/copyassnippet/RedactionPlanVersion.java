package com.copyassnippet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class RedactionPlanVersion {
    private final List<String> headerRegexes;
    private final List<String> cookieRegexes;
    private final List<String> paramRegexes;
    private final List<RuleVersion> redactionRules;
    private final String replacementString;
    private final String template;

    private RedactionPlanVersion(
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            List<RuleVersion> redactionRules,
            String replacementString,
            String template) {
        this.headerRegexes = headerRegexes;
        this.cookieRegexes = cookieRegexes;
        this.paramRegexes = paramRegexes;
        this.redactionRules = redactionRules;
        this.replacementString = replacementString;
        this.template = template;
    }

    static RedactionPlanVersion fromPreset(Preset preset) {
        List<RuleVersion> rules = new ArrayList<>();
        for (RedactionRule rule : preset.getRedactionRules()) {
            rules.add(new RuleVersion(rule.getType(), rule.getPattern()));
        }

        return new RedactionPlanVersion(
                List.copyOf(preset.getHeaderRegexes()),
                List.copyOf(preset.getCookieRegexes()),
                List.copyOf(preset.getParamRegexes()),
                List.copyOf(rules),
                preset.getReplacementString(),
                preset.getTemplate()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedactionPlanVersion that = (RedactionPlanVersion) o;
        return Objects.equals(headerRegexes, that.headerRegexes)
                && Objects.equals(cookieRegexes, that.cookieRegexes)
                && Objects.equals(paramRegexes, that.paramRegexes)
                && Objects.equals(redactionRules, that.redactionRules)
                && Objects.equals(replacementString, that.replacementString)
                && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headerRegexes, cookieRegexes, paramRegexes, redactionRules, replacementString, template);
    }

    private static final class RuleVersion {
        private final RedactionRule.Type type;
        private final String pattern;

        private RuleVersion(RedactionRule.Type type, String pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RuleVersion that = (RuleVersion) o;
            return type == that.type && Objects.equals(pattern, that.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, pattern);
        }
    }
}
