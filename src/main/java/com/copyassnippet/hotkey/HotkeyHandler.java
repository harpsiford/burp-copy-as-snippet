package com.copyassnippet.hotkey;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.hotkey.HotKeyEvent;
import burp.api.montoya.ui.hotkey.HotKeyHandler;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class HotkeyHandler implements HotKeyHandler {
    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;
    private final Logging logging;

    public HotkeyHandler(PresetStore presetStore, CachingRedactionEngine redactionEngine, Logging logging) {
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
        this.logging = logging;
    }

    @Override
    public void handle(HotKeyEvent event) {
        HttpRequestResponse requestResponse = requestResponseFor(event);
        if (requestResponse == null) {
            return;
        }

        Preset preset = getFirstEnabledPreset();
        if (preset == null) {
            return;
        }

        String result = redactionEngine.format(preset, requestResponse);

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception e) {
            logging.logToError("Failed to copy snippet to clipboard from hotkey.", e);
        }
    }

    private static HttpRequestResponse requestResponseFor(HotKeyEvent event) {
        if (event.messageEditorRequestResponse().isPresent()) {
            return event.messageEditorRequestResponse().get().requestResponse();
        }

        if (!event.selectedRequestResponses().isEmpty()) {
            return event.selectedRequestResponses().get(0);
        }

        return null;
    }

    private Preset getFirstEnabledPreset() {
        List<Preset> presets = presetStore.getResolvedPresets();
        for (Preset p : presets) {
            if (p.isEnabled()) return p;
        }
        return null;
    }
}
