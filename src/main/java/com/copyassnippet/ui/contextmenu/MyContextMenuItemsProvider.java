package com.copyassnippet.ui.contextmenu;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MyContextMenuItemsProvider implements ContextMenuItemsProvider
{
    private static final Logger LOGGER = Logger.getLogger(MyContextMenuItemsProvider.class.getName());
    private final PresetStore presetStore;
    private final CachingRedactionEngine redactionEngine;
    private final Executor backgroundExecutor;

    public MyContextMenuItemsProvider(
            PresetStore presetStore,
            CachingRedactionEngine redactionEngine,
            Executor backgroundExecutor
    )
    {
        this.presetStore = presetStore;
        this.redactionEngine = redactionEngine;
        this.backgroundExecutor = backgroundExecutor;
    }

    private List<HttpRequestResponse> getRequestResponses(ContextMenuEvent event) {
        if (event.messageEditorRequestResponse().isPresent()) {
            return Collections.singletonList(event.messageEditorRequestResponse().get().requestResponse());
        }
        return event.selectedRequestResponses();
    }

    private void copyWithPreset(ContextMenuEvent event, Preset preset) {
        try {
            List<HttpRequestResponse> requestResponses = List.copyOf(getRequestResponses(event));
            backgroundExecutor.execute(() -> copySnippet(preset, requestResponses));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to queue snippet copy from context menu.", exception);
        }
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event)
    {
        JMenu submenu = new JMenu("Copy as snippet");

        List<Preset> presets = presetStore.getResolvedPresets();
        for (Preset preset : presets) {
            if (!preset.isEnabled()) continue;
            JMenuItem item = new JMenuItem(preset.getName());
            item.addActionListener(l -> copyWithPreset(event, preset));
            submenu.add(item);
        }

        List<Component> menuItemList = new ArrayList<>();
        menuItemList.add(submenu);
        return menuItemList;
    }

    private void copySnippet(Preset preset, List<HttpRequestResponse> requestResponses) {
        StringBuilder report = new StringBuilder();
        for (HttpRequestResponse rr : requestResponses) {
            if (report.length() > 0) {
                report.append("\r\n");
            }
            report.append(redactionEngine.format(preset, rr));
        }

        String result = report.toString().replaceAll("[\r\n]+$", "");
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to copy snippet to clipboard from context menu.", exception);
        }
    }
}
