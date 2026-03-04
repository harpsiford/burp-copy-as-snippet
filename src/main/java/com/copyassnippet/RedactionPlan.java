package com.copyassnippet;

import java.util.List;
import java.util.regex.Pattern;

final class RedactionPlan {
    private final List<Pattern> headerPatterns;
    private final List<Pattern> cookiePatterns;
    private final List<Pattern> paramPatterns;
    private final List<Pattern> redactCookiePatterns;
    private final List<Pattern> redactHeaderPatterns;
    private final List<Pattern> redactParamPatterns;
    private final List<Pattern> redactRegexPatterns;
    private final String replacementString;
    private final String template;

    RedactionPlan(
            List<Pattern> headerPatterns,
            List<Pattern> cookiePatterns,
            List<Pattern> paramPatterns,
            List<Pattern> redactCookiePatterns,
            List<Pattern> redactHeaderPatterns,
            List<Pattern> redactParamPatterns,
            List<Pattern> redactRegexPatterns,
            String replacementString,
            String template) {
        this.headerPatterns = List.copyOf(headerPatterns);
        this.cookiePatterns = List.copyOf(cookiePatterns);
        this.paramPatterns = List.copyOf(paramPatterns);
        this.redactCookiePatterns = List.copyOf(redactCookiePatterns);
        this.redactHeaderPatterns = List.copyOf(redactHeaderPatterns);
        this.redactParamPatterns = List.copyOf(redactParamPatterns);
        this.redactRegexPatterns = List.copyOf(redactRegexPatterns);
        this.replacementString = replacementString;
        this.template = template;
    }

    List<Pattern> headerPatterns() {
        return headerPatterns;
    }

    List<Pattern> cookiePatterns() {
        return cookiePatterns;
    }

    List<Pattern> paramPatterns() {
        return paramPatterns;
    }

    List<Pattern> redactCookiePatterns() {
        return redactCookiePatterns;
    }

    List<Pattern> redactHeaderPatterns() {
        return redactHeaderPatterns;
    }

    List<Pattern> redactParamPatterns() {
        return redactParamPatterns;
    }

    List<Pattern> redactRegexPatterns() {
        return redactRegexPatterns;
    }

    String replacementString() {
        return replacementString;
    }

    String template() {
        return template;
    }
}
