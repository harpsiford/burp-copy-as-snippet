package com.copyassnippet;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.hotkey.HotKeyContext;

public class HotkeyManager {

    private final MontoyaApi api;
    private final PresetStore presetStore;
    private final RedactionEngine redactionEngine;
    private Registration currentRegistration;

    public HotkeyManager(MontoyaApi api, PresetStore presetStore) {
        this(api, presetStore, new CachingRedactionEngine());
    }

    HotkeyManager(MontoyaApi api, PresetStore presetStore, RedactionEngine redactionEngine) {
        this.api = api;
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
    }

    public void applyFromSettings() {
        deregister();
        if (presetStore.isHotkeyEnabled()) {
            register(presetStore.getHotkeyString());
        }
    }

    private void register(String hotkeyString) {
        currentRegistration = api.userInterface().registerHotKeyHandler(
                HotKeyContext.HTTP_MESSAGE_EDITOR,
                hotkeyString,
                new HotkeyHandler(presetStore, redactionEngine)
        );
    }

    private void deregister() {
        if (currentRegistration != null && currentRegistration.isRegistered()) {
            currentRegistration.deregister();
        }
        currentRegistration = null;
    }
}
