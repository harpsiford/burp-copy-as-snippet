package com.copyassnippet.preset.model;

public enum PresetScope {
    USER("User"),
    PROJECT("Project"),
    BUILT_IN("Built-in");

    public static final PresetScope[] EDITABLE_VALUES = {USER, PROJECT};

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

    public PresetScope toEditableScope() {
        return this == PROJECT ? PROJECT : USER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
