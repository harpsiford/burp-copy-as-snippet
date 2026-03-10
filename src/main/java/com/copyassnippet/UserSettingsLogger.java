package com.copyassnippet;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class UserSettingsLogger {
    private UserSettingsLogger() {
    }

    public static void logCurrentSettings(Logging logging, Preferences preferences) {
        logging.logToOutput("Copy as snippet persisted user settings:");

        List<String> lines = new ArrayList<>();
        appendEntries(lines, "string", preferences.stringKeys(), preferences::getString);
        appendEntries(lines, "boolean", preferences.booleanKeys(), preferences::getBoolean);
        appendEntries(lines, "integer", preferences.integerKeys(), preferences::getInteger);
        appendEntries(lines, "byte", preferences.byteKeys(), preferences::getByte);
        appendEntries(lines, "short", preferences.shortKeys(), preferences::getShort);
        appendEntries(lines, "long", preferences.longKeys(), preferences::getLong);

        if (lines.isEmpty()) {
            logging.logToOutput("  (none)");
            return;
        }

        for (String line : lines) {
            logging.logToOutput("  " + line);
        }
    }

    private static void appendEntries(
            List<String> target,
            String type,
            Set<String> keys,
            Function<String, Object> valueReader
    ) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        List<String> sortedKeys = new ArrayList<>(keys);
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            target.add(type + " " + key + " = " + valueReader.apply(key));
        }
    }
}
