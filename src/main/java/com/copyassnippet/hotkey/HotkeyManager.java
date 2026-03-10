package com.copyassnippet.hotkey;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyManager {
    private static final Logger LOGGER = Logger.getLogger(HotkeyManager.class.getName());

    private final MontoyaApi api;
    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;
    private Registration currentRegistration;

    public HotkeyManager(MontoyaApi api, PresetStore presetStore, CachingRedactionEngine redactionEngine) {
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
        try {
            currentRegistration = api.userInterface().registerHotKeyHandler(
                    HotKeyContext.HTTP_MESSAGE_EDITOR,
                    HotkeyDefinition.hotKey(hotkeyString),
                    new HotkeyHandler(presetStore, redactionEngine)
            );
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to register hotkey \"" + hotkeyString + "\".", exception);
            currentRegistration = null;
        }
    }

    private void deregister() {
        if (currentRegistration != null && currentRegistration.isRegistered()) {
            currentRegistration.deregister();
        }
        currentRegistration = null;
    }
}
