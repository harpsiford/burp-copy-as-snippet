package com.copyassnippet;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RegexValidation {

    private RegexValidation() {
    }

    public static String firstValidationError(
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            List<RedactionRule> redactionRules) {
        String invalid = firstInvalid(headerRegexes, Pattern.CASE_INSENSITIVE, true);
        if (invalid != null) {
            return "Invalid header regex: " + invalid;
        }
        invalid = firstInvalid(cookieRegexes, 0, true);
        if (invalid != null) {
            return "Invalid cookie regex: " + invalid;
        }
        invalid = firstInvalid(paramRegexes, 0, true);
        if (invalid != null) {
            return "Invalid param regex: " + invalid;
        }

        for (RedactionRule rule : redactionRules) {
            String pattern = rule.getPattern();
            int flags = rule.getType().patternFlags();
            String label = rule.getType().label();
            if (!isValid(RegexUtil.anchorRegex(pattern), flags)) {
                return "Invalid " + label + " redaction rule regex: " + pattern;
            }
        }

        return null;
    }

    private static String firstInvalid(List<String> regexes, int flags, boolean anchored) {
        for (String regex : regexes) {
            String candidate = anchored ? RegexUtil.anchorRegex(regex) : regex;
            if (!isValid(candidate, flags)) {
                return regex;
            }
        }
        return null;
    }

    private static boolean isValid(String regex, int flags) {
        try {
            Pattern.compile(regex, flags);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
