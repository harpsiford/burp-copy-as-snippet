package com.copyassnippet.ui.settings;

import burp.api.montoya.ui.settings.SettingsPanel;
import com.copyassnippet.hotkey.HotkeyManager;
import com.copyassnippet.hotkey.HotkeySettingsService;
import com.copyassnippet.preset.service.PresetApplicationService;
import com.copyassnippet.preset.storage.PresetStore;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class MySettingsPanel implements SettingsPanel {
    private final PresetStore presetStore;
    private final HotkeyManager hotkeyManager;
    private final Consumer<Component> themeApplier;
    private SwingSettingsView view;

    public MySettingsPanel(
            PresetStore presetStore,
            HotkeyManager hotkeyManager,
            Consumer<Component> themeApplier
    ) {
        this.presetStore = presetStore;
        this.hotkeyManager = hotkeyManager;
        this.themeApplier = themeApplier;
    }

    @Override
    public JPanel uiComponent() {
        if (view == null) {
            view = new SwingSettingsView(themeApplier);
            PresetApplicationService presetService = new PresetApplicationService(presetStore);
            HotkeySettingsService hotkeySettingsService = new HotkeySettingsService(presetStore, hotkeyManager);
            new SettingsPresenter(view, presetService, hotkeySettingsService);
        }
        return view.uiComponent();
    }

    public void dispose() {
        if (view != null) {
            view.dispose();
        }
    }
}
