package com.copyassnippet;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class ContextMenu implements BurpExtension
{
    @Override
    public void initialize(MontoyaApi api)
    {
        api.extension().setName("Copy as snippet");

        PresetStore presetStore = new PresetStore(api);
        RedactionEngine redactionEngine = new CachingRedactionEngine();
        HotkeyManager hotkeyManager = new HotkeyManager(api, presetStore, redactionEngine);

        api.userInterface().registerContextMenuItemsProvider(new MyContextMenuItemsProvider(presetStore, redactionEngine));
        api.userInterface().registerSettingsPanel(new MySettingsPanel(presetStore, hotkeyManager));

        hotkeyManager.applyFromSettings();
    }
}
