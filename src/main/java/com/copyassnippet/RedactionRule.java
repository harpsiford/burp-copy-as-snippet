package com.copyassnippet;

import java.util.regex.Pattern;

public class RedactionRule {

    public enum Type {
        REGEX("Regex", "regex", "regex redaction rule", Pattern.DOTALL),
        COOKIE("Cookie", "cookie", "cookie redaction rule", 0),
        HEADER("Header", "header", "header redaction rule", Pattern.CASE_INSENSITIVE),
        PARAM("Param", "param", "param redaction rule", 0);

        private final String displayName;
        private final String label;
        private final String context;
        private final int patternFlags;

        Type(String displayName, String label, String context, int patternFlags) {
            this.displayName = displayName;
            this.label = label;
            this.context = context;
            this.patternFlags = patternFlags;
        }

        public String displayName() {
            return displayName;
        }

        public String label() {
            return label;
        }

        public String redactionContext() {
            return context;
        }

        public int patternFlags() {
            return patternFlags;
        }

        public static Type fromDisplayName(String s) {
            for (Type t : values()) {
                if (t.displayName().equalsIgnoreCase(s) || t.name().equalsIgnoreCase(s)) return t;
            }
            return REGEX;
        }
    }

    private Type type;
    private String pattern;

    public RedactionRule(Type type, String pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String toSerializedString() {
        return type.name() + ":" + pattern;
    }

    public static RedactionRule fromSerializedString(String s) {
        if (s == null || s.isBlank()) return null;
        int colon = s.indexOf(':');
        if (colon < 0) return null;
        String typePart = s.substring(0, colon).trim();
        String patternPart = s.substring(colon + 1);
        Type type;
        try {
            type = Type.valueOf(typePart.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new RedactionRule(type, patternPart);
    }
}
