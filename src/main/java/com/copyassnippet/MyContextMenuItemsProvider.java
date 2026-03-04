package com.copyassnippet;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MyContextMenuItemsProvider implements ContextMenuItemsProvider
{
    private final MontoyaApi api;
    private final PresetStore presetStore;

    public MyContextMenuItemsProvider(MontoyaApi api, PresetStore presetStore)
    {
        this.api = api;
        this.presetStore = presetStore;
    }

    private List<HttpRequestResponse> getRequestResponses(ContextMenuEvent event) {
        if (event.messageEditorRequestResponse().isPresent()) {
            return Collections.singletonList(event.messageEditorRequestResponse().get().requestResponse());
        }
        return event.selectedRequestResponses();
    }

    private void copyWithPreset(ContextMenuEvent event, Preset preset) {
        List<HttpRequestResponse> requestResponses = getRequestResponses(event);
        RequestRedactor redactor = new RequestRedactor(preset);

        StringBuilder report = new StringBuilder();
        for (HttpRequestResponse rr : requestResponses) {
            if (report.length() > 0) {
                report.append("\r\n");
            }
            report.append(redactor.format(rr));
        }

        String result = report.toString().replaceAll("[\r\n]+$", "");
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
        } catch (Exception e) {
            // Clipboard access denied or unavailable — silently ignore
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

        submenu.addSeparator();

        JMenuItem createNew = new JMenuItem("Create new preset\u2026");
        createNew.addActionListener(l -> {
            new NewPresetDialog(presetStore).show(submenu);
        });
        submenu.add(createNew);

        List<Component> menuItemList = new ArrayList<>();
        menuItemList.add(submenu);
        return menuItemList;
    }
}
