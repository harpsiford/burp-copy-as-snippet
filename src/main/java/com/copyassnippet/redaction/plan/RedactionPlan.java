package com.copyassnippet.redaction.plan;

import java.util.List;
import java.util.regex.Pattern;

public final class RedactionPlan {
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

    public List<Pattern> headerPatterns() {
        return headerPatterns;
    }

    public List<Pattern> cookiePatterns() {
        return cookiePatterns;
    }

    public List<Pattern> paramPatterns() {
        return paramPatterns;
    }

    public List<Pattern> redactCookiePatterns() {
        return redactCookiePatterns;
    }

    public List<Pattern> redactHeaderPatterns() {
        return redactHeaderPatterns;
    }

    public List<Pattern> redactParamPatterns() {
        return redactParamPatterns;
    }

    public List<Pattern> redactRegexPatterns() {
        return redactRegexPatterns;
    }

    public String replacementString() {
        return replacementString;
    }

    public String template() {
        return template;
    }
}
