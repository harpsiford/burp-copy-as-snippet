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
import java.util.concurrent.Executor;

public class HotkeyHandler implements HotKeyHandler {
    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;
    private final Executor backgroundExecutor;
    private final Logging logging;

    public HotkeyHandler(
            PresetStore presetStore,
            CachingRedactionEngine redactionEngine,
            Executor backgroundExecutor,
            Logging logging
    ) {
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
        this.backgroundExecutor = backgroundExecutor;
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

        try {
            backgroundExecutor.execute(() -> copySnippet(preset, requestResponse));
        } catch (RuntimeException exception) {
            logging.logToError("Failed to queue snippet copy from hotkey.", exception);
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

    private void copySnippet(Preset preset, HttpRequestResponse requestResponse) {
        String result = redactionEngine.format(preset, requestResponse);

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception exception) {
            logging.logToError("Failed to copy snippet to clipboard from hotkey.", exception);
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
