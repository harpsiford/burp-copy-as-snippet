package com.copyassnippet;

final class PresetRow {
    private final Preset preset;
    private final PresetScope scope;

    PresetRow(Preset preset, PresetScope scope) {
        this.preset = preset;
        this.scope = scope;
    }

    Preset getPreset() {
        return preset;
    }

    PresetScope getScope() {
        return scope;
    }
}
