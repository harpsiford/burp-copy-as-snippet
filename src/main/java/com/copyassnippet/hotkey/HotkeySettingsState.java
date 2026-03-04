package com.copyassnippet.hotkey;

public final class HotkeySettingsState {
    private final boolean enabled;
    private final String hotkey;

    public HotkeySettingsState(boolean enabled, String hotkey) {
        this.enabled = enabled;
        this.hotkey = hotkey != null ? hotkey : "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHotkey() {
        return hotkey;
    }
}
