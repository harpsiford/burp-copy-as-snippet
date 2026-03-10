package com.copyassnippet.hotkey;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;

import java.util.concurrent.Executor;

public class HotkeyManager {
    private final MontoyaApi api;
    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;
    private final Executor backgroundExecutor;
    private final Logging logging;
    private Registration currentRegistration;

    public HotkeyManager(
            MontoyaApi api,
            PresetStore presetStore,
            CachingRedactionEngine redactionEngine,
            Executor backgroundExecutor
    ) {
        this.api = api;
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
        this.backgroundExecutor = backgroundExecutor;
        this.logging = api.logging();
    }

    public void applyFromSettings() {
        deregister();
        if (presetStore.isHotkeyEnabled()) {
            register(presetStore.getHotkeyString());
        }
    }

    public void shutdown() {
        deregister();
    }

    private void register(String hotkeyString) {
        try {
            currentRegistration = api.userInterface().registerHotKeyHandler(
                    HotKeyContext.HTTP_MESSAGE_EDITOR,
                    HotkeyDefinition.hotKey(hotkeyString),
                    new HotkeyHandler(presetStore, redactionEngine, backgroundExecutor, logging)
            );
        } catch (RuntimeException exception) {
            logging.logToError("Failed to register hotkey \"" + hotkeyString + "\".", exception);
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
