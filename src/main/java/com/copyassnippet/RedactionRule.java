package com.copyassnippet;

public class RedactionRule {

    public enum Type {
        REGEX, COOKIE, HEADER, PARAM;

        public String displayName() {
            switch (this) {
                case REGEX: return "Regex";
                case COOKIE: return "Cookie";
                case HEADER: return "Header";
                case PARAM: return "Param";
                default: return name();
            }
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

    /** Serializes to {@code "TYPE:pattern"} for flat storage. */
    public String toSerializedString() {
        return type.name() + ":" + pattern;
    }

    /**
     * Parses a string produced by {@link #toSerializedString()}.
     * Returns null if the string is blank or malformed.
     */
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
