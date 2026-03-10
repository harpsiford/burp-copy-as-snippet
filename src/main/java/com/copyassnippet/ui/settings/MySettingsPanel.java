package com.copyassnippet.ui.settings;

import burp.api.montoya.ui.settings.SettingsPanel;
import com.copyassnippet.hotkey.HotkeyManager;
import com.copyassnippet.hotkey.HotkeySettingsService;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.storage.PresetStore;

import javax.swing.*;

public class MySettingsPanel implements SettingsPanel {
    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;
    private SwingSettingsView view;

    public MySettingsPanel(PresetStore presetStore, HotkeyManager hotkeyManager) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;
    }

    @Override
    public JPanel uiComponent() {
        if (view == null) {
            view = new SwingSettingsView();
            PresetApplicationService presetService = new PresetApplicationService(presetStore);
            HotkeySettingsService hotkeySettingsService = new HotkeySettingsService(presetStore, hotkeyManager);
            new SettingsPresenter(view, presetService, hotkeySettingsService);
        }
        return view.uiComponent();
    }
}
