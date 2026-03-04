package com.copyassnippet;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyEvent;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyHandler implements HotKeyHandler {
    private static final Logger LOGGER = Logger.getLogger(HotkeyHandler.class.getName());

    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;

    HotkeyHandler(PresetStore presetStore, CachingRedactionEngine redactionEngine) {
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
    }

    @Override
    public void handle(HotKeyEvent event) {
        if (event.messageEditorRequestResponse().isEmpty()) return;

        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().get().requestResponse();

        Preset preset = getFirstEnabledPreset();
        if (preset == null) return;

        String result = redactionEngine.format(preset, requestResponse);

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to copy snippet to clipboard from hotkey.", e);
        }
    }

    private Preset getFirstEnabledPreset() {
        List<Preset> presets = presetStore.getResolvedPresets();
        for (Preset p : presets) {
            if (p.isEnabled()) return p;
        }
        return null;
    }
}
