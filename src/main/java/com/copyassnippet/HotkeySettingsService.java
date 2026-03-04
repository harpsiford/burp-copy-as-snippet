package com.copyassnippet;

final class HotkeySettingsService {
    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;

    HotkeySettingsService(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;
    }

    HotkeySettingsState currentSettings() {
        return new HotkeySettingsState(presetStore.isHotkeyEnabled(), presetStore.getHotkeyString());
    }

    void apply(HotkeySettingsState state) {
        presetStore.setHotkeyEnabled(state.isEnabled());
        presetStore.setHotkeyString(state.getHotkey());
        hotkeyManager.applyFromSettings();
    }

    void setEnabled(boolean enabled) {
        presetStore.setHotkeyEnabled(enabled);
        hotkeyManager.applyFromSettings();
    }

    void applyFromStore() {
        hotkeyManager.applyFromSettings();
    }
}
