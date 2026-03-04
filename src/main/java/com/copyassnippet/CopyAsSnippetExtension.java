package com.copyassnippet;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.copyassnippet.hotkey.HotkeyManager;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;
import com.copyassnippet.ui.contextmenu.MyContextMenuItemsProvider;
import com.copyassnippet.ui.settings.MySettingsPanel;

public class CopyAsSnippetExtension implements BurpExtension
{
    @Override
    public void initialize(MontoyaApi api)
    {
        api.extension().setName("Copy as snippet");

        PresetStore presetStore = new PresetStore(api);
        CachingRedactionEngine redactionEngine = new CachingRedactionEngine();
        HotkeyManager hotkeyManager = new HotkeyManager(api, presetStore, redactionEngine);

        api.userInterface().registerContextMenuItemsProvider(new MyContextMenuItemsProvider(presetStore, redactionEngine));
        api.userInterface().registerSettingsPanel(new MySettingsPanel(presetStore, hotkeyManager));

        hotkeyManager.applyFromSettings();
    }
}
