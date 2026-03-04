package com.copyassnippet;

import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;

public class MySettingsPanel implements SettingsPanel {
    private final SwingSettingsView view;
    private final SettingsPresenter presenter;

    public MySettingsPanel(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.view = new SwingSettingsView();

        PresetApplicationService presetService = new PresetApplicationService(presetStore);
        HotkeySettingsService hotkeySettingsService = new HotkeySettingsService(presetStore, hotkeyManager);

        this.presenter = new SettingsPresenter(view, presetService, hotkeySettingsService);
    }

    @Override
    public JPanel uiComponent() {
        return view.uiComponent();
    }
}
