package com.copyassnippet;

final class HotkeySettingsState {
    private final boolean enabled;
    private final String hotkey;

    HotkeySettingsState(boolean enabled, String hotkey) {
        this.enabled = enabled;
        this.hotkey = hotkey != null ? hotkey : "";
    }

    boolean isEnabled() {
        return enabled;
    }

    String getHotkey() {
        return hotkey;
    }
}
