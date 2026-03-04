package com.copyassnippet.hotkey;

import com.copyassnippet.preset.storage.PresetStore;

public final class HotkeySettingsService {
    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;

    public HotkeySettingsService(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;
    }

    public HotkeySettingsState currentSettings() {
        return new HotkeySettingsState(presetStore.isHotkeyEnabled(), presetStore.getHotkeyString());
    }

    public void apply(HotkeySettingsState state) {
        presetStore.setHotkeyEnabled(state.isEnabled());
        presetStore.setHotkeyString(state.getHotkey());
        hotkeyManager.applyFromSettings();
    }

    public void setEnabled(boolean enabled) {
        presetStore.setHotkeyEnabled(enabled);
        hotkeyManager.applyFromSettings();
    }

    public void applyFromStore() {
        hotkeyManager.applyFromSettings();
    }
}
