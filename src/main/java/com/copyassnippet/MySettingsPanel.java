package com.copyassnippet;

import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;

public class MySettingsPanel implements SettingsPanel {
    private final SwingSettingsView view;

    public MySettingsPanel(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.view = new SwingSettingsView();

        PresetApplicationService presetService = new PresetApplicationService(presetStore);
        HotkeySettingsService hotkeySettingsService = new HotkeySettingsService(presetStore, hotkeyManager);

        new SettingsPresenter(view, presetService, hotkeySettingsService);
    }

    @Override
    public JPanel uiComponent() {
        return view.uiComponent();
    }
}
