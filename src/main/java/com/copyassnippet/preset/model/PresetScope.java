package com.copyassnippet.preset.model;

public enum PresetScope {
    USER("User"),
    BUILT_IN("Built-in");

    private final String displayName;

    PresetScope(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isBuiltIn() {
        return this == BUILT_IN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
