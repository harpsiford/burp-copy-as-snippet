package com.copyassnippet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class RedactionPlanCompiler {
    private static final Logger LOGGER = Logger.getLogger(RedactionPlanCompiler.class.getName());

    RedactionPlan compile(Preset preset) {
        List<Pattern> headerPatterns = compilePatterns(
                preset.getHeaderRegexes(),
                Pattern.CASE_INSENSITIVE,
                "header regex"
        );

        List<Pattern> cookiePatterns = compilePatterns(
                preset.getCookieRegexes(),
                0,
                "cookie regex"
        );

        List<Pattern> paramPatterns = compilePatterns(
                preset.getParamRegexes(),
                0,
                "param regex"
        );

        List<Pattern> redactCookiePatterns = new ArrayList<>();
        List<Pattern> redactHeaderPatterns = new ArrayList<>();
        List<Pattern> redactParamPatterns = new ArrayList<>();
        List<Pattern> redactRegexPatterns = new ArrayList<>();

        for (RedactionRule rule : preset.getRedactionRules()) {
            List<Pattern> target;
            switch (rule.getType()) {
                case COOKIE:
                    target = redactCookiePatterns;
                    break;
                case HEADER:
                    target = redactHeaderPatterns;
                    break;
                case PARAM:
                    target = redactParamPatterns;
                    break;
                case REGEX:
                default:
                    target = redactRegexPatterns;
                    break;
            }

            Pattern compiled = tryCompile(
                    RegexUtil.anchorRegex(rule.getPattern()),
                    rule.getType().patternFlags(),
                    rule.getType().redactionContext()
            );
            if (compiled != null) {
                target.add(compiled);
            }
        }

        return new RedactionPlan(
                headerPatterns,
                cookiePatterns,
                paramPatterns,
                redactCookiePatterns,
                redactHeaderPatterns,
                redactParamPatterns,
                redactRegexPatterns,
                preset.getReplacementString(),
                preset.getTemplate()
        );
    }

    private static List<Pattern> compilePatterns(List<String> source, int flags, String context) {
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String regex : source) {
            Pattern compiled = tryCompile(RegexUtil.anchorRegex(regex), flags, context);
            if (compiled != null) {
                compiledPatterns.add(compiled);
            }
        }
        return compiledPatterns;
    }

    private static Pattern tryCompile(String regex, int flags, String context) {
        try {
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            LOGGER.log(Level.WARNING, "Ignoring invalid regex for " + context + ": " + regex, e);
            return null;
        }
    }
}
