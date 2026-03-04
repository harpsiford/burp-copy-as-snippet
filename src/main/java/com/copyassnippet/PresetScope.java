package com.copyassnippet;

enum PresetScope {
    USER("User"),
    PROJECT("Project"),
    BUILT_IN("Built-in");

    static final PresetScope[] EDITABLE_VALUES = {USER, PROJECT};

    private final String displayName;

    PresetScope(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    boolean isBuiltIn() {
        return this == BUILT_IN;
    }

    PresetScope toEditableScope() {
        return this == PROJECT ? PROJECT : USER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
