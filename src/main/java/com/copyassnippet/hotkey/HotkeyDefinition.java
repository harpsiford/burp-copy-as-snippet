package com.copyassnippet.hotkey;

import burp.api.montoya.ui.hotkey.HotKey;

import java.util.ArrayList;
import java.util.List;

public final class HotkeyDefinition {
    public static final String DISPLAY_NAME = "Copy as Snippet";

    private HotkeyDefinition() {
    }

    public static HotKey hotKey(String shortcut) {
        return HotKey.hotKey(DISPLAY_NAME, canonicalShortcut(shortcut));
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
        return "Ctrl+Shift+C";
    }

    public static String canonicalShortcut(String shortcut) {
        if (shortcut == null || shortcut.isBlank()) {
            return "";
        }

        String[] rawParts = shortcut.trim().split("\\+");
        List<String> parts = new ArrayList<>();
        for (String rawPart : rawParts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            if ("cmd".equalsIgnoreCase(part) || "command".equalsIgnoreCase(part)) {
                parts.add("Ctrl");
            } else {
                parts.add(part);
            }
        }
        return String.join("+", parts);
    }
}
