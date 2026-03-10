package com.copyassnippet;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.logging.Logging;
import com.copyassnippet.hotkey.HotkeyManager;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.redaction.CachingRedactionEngine;
import com.copyassnippet.ui.contextmenu.MyContextMenuItemsProvider;
import com.copyassnippet.ui.settings.MySettingsPanel;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CopyAsSnippetExtension implements BurpExtension
{
    @Override
    public void initialize(MontoyaApi api)
    {
        api.extension().setName("Copy as snippet");

        PresetStore presetStore = new PresetStore(api);
        UserSettingsLogger.logCurrentSettings(api.logging(), api.persistence().preferences());
        CachingRedactionEngine redactionEngine = new CachingRedactionEngine();
        ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "copy-as-snippet-worker");
            thread.setDaemon(true);
            return thread;
        });
        Executor backgroundWorker = backgroundExecutor;
        HotkeyManager hotkeyManager = new HotkeyManager(api, presetStore, redactionEngine, backgroundWorker);
        MySettingsPanel settingsPanel = new MySettingsPanel(
                presetStore,
                hotkeyManager,
                backgroundExecutor,
                api.userInterface()::applyThemeToComponent,
                api.userInterface().swingUtils()::windowForComponent,
                api.userInterface().swingUtils()::suiteFrame
        );

        Registration contextMenuRegistration = api.userInterface().registerContextMenuItemsProvider(
                new MyContextMenuItemsProvider(presetStore, redactionEngine, backgroundWorker)
        );
        Registration settingsPanelRegistration = api.userInterface().registerSettingsPanel(settingsPanel);

        hotkeyManager.applyFromSettings();
        api.extension().registerUnloadingHandler(
                new ExtensionCleanup(
                        api.logging(),
                        hotkeyManager,
                        settingsPanel,
                        backgroundExecutor,
                        contextMenuRegistration,
                        settingsPanelRegistration
                )
        );
    }

    private static final class ExtensionCleanup implements ExtensionUnloadingHandler
    {
        private final Logging logging;
        private final HotkeyManager hotkeyManager;
        private final MySettingsPanel settingsPanel;
        private final ExecutorService backgroundExecutor;
        private final Registration contextMenuRegistration;
        private final Registration settingsPanelRegistration;

        private ExtensionCleanup(
                Logging logging,
                HotkeyManager hotkeyManager,
                MySettingsPanel settingsPanel,
                ExecutorService backgroundExecutor,
                Registration contextMenuRegistration,
                Registration settingsPanelRegistration
        ) {
            this.logging = logging;
            this.hotkeyManager = hotkeyManager;
            this.settingsPanel = settingsPanel;
            this.backgroundExecutor = backgroundExecutor;
            this.contextMenuRegistration = contextMenuRegistration;
            this.settingsPanelRegistration = settingsPanelRegistration;
        }

        @Override
        public void extensionUnloaded()
        {
            cleanup("settings dialogs", settingsPanel::dispose);
            cleanup("hotkey handler", hotkeyManager::shutdown);
            cleanup("background executor", () -> backgroundExecutor.shutdownNow());
            deregister("context menu items provider", contextMenuRegistration);
            deregister("settings panel", settingsPanelRegistration);
        }

        private void deregister(String name, Registration registration) {
            cleanup(name, () -> {
                if (registration != null && registration.isRegistered()) {
                    registration.deregister();
                }
            });
        }

        private void cleanup(String name, Runnable action) {
            try {
                action.run();
            } catch (RuntimeException exception) {
                logging.logToError("Failed to release " + name + " while unloading Copy as snippet.", exception);
            }
        }
    }
}
