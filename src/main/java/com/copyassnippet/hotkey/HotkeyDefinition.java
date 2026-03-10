package com.copyassnippet.hotkey;

import burp.api.montoya.ui.hotkey.HotKey;

import java.util.Locale;

public final class HotkeyDefinition {
    public static final String DISPLAY_NAME = "Copy as Snippet";

    private HotkeyDefinition() {
    }

    public static HotKey hotKey(String shortcut) {
        return HotKey.hotKey(DISPLAY_NAME, shortcut);
    }

    public static boolean isValid(String shortcut) {
        if (shortcut == null || shortcut.isBlank()) {
            return false;
        }

        try {
            hotKey(shortcut);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static String defaultShortcut() {
        return usesCommandModifier() ? "Cmd+Shift+C" : "Ctrl+Shift+C";
    }

    public static boolean usesCommandModifier() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("mac");
    }
}
