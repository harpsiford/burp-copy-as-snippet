package com.copyassnippet;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.hotkey.HotKeyEvent;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class HotkeyHandler implements HotKeyHandler {

    private final PresetStore presetStore;

    public HotkeyHandler(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    @Override
    public void handle(HotKeyEvent event) {
        if (event.messageEditorRequestResponse().isEmpty()) return;

        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().get().requestResponse();

        Preset preset = getFirstEnabledPreset();
        if (preset == null) return;

        RequestRedactor redactor = new RequestRedactor(preset);
        String result = redactor.format(requestResponse);

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception e) {
            // Clipboard access denied or unavailable — silently ignore
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
