package com.copyassnippet;

import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.Preferences;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSettingsLoggerTest {
    @Test
    void logsAllDiscoveredSettingKeysAndValues() {
        PreferencesFixture preferencesFixture = new PreferencesFixture();
        LoggingFixture loggingFixture = new LoggingFixture();

        preferencesFixture.strings.put("preset.order.json", "[\"a\"]");
        preferencesFixture.strings.put("user.presets.json", "[{\"id\":\"a\"}]");
        preferencesFixture.booleans.put("hotkey.enabled", true);
        preferencesFixture.integers.put("user.settings.schemaVersion", 1);
        preferencesFixture.bytes.put("byte.setting", (byte) 7);
        preferencesFixture.shorts.put("short.setting", (short) 12);
        preferencesFixture.longs.put("long.setting", 99L);

        UserSettingsLogger.logCurrentSettings(loggingFixture.logging(), preferencesFixture.preferences());

        assertEquals(List.of(
                "Copy as snippet persisted user settings:",
                "  string preset.order.json = [\"a\"]",
                "  string user.presets.json = [{\"id\":\"a\"}]",
                "  boolean hotkey.enabled = true",
                "  integer user.settings.schemaVersion = 1",
                "  byte byte.setting = 7",
                "  short short.setting = 12",
                "  long long.setting = 99"
        ), loggingFixture.messages);
    }

    @Test
    void logsWhenNoSettingsExist() {
        PreferencesFixture preferencesFixture = new PreferencesFixture();
        LoggingFixture loggingFixture = new LoggingFixture();

        UserSettingsLogger.logCurrentSettings(loggingFixture.logging(), preferencesFixture.preferences());

        assertEquals(List.of(
                "Copy as snippet persisted user settings:",
                "  (none)"
        ), loggingFixture.messages);
    }

    private static final class PreferencesFixture implements InvocationHandler {
        private final Map<String, String> strings = new HashMap<>();
        private final Map<String, Boolean> booleans = new HashMap<>();
        private final Map<String, Integer> integers = new HashMap<>();
        private final Map<String, Byte> bytes = new HashMap<>();
        private final Map<String, Short> shorts = new HashMap<>();
        private final Map<String, Long> longs = new HashMap<>();
        private final Preferences preferences = (Preferences) Proxy.newProxyInstance(
                Preferences.class.getClassLoader(),
                new Class[]{Preferences.class},
                this
        );

        private Preferences preferences() {
            return preferences;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getString".equals(name)) {
                return strings.get(args[0]);
            }
            if ("stringKeys".equals(name)) {
                return Set.copyOf(strings.keySet());
            }
            if ("getBoolean".equals(name)) {
                return booleans.get(args[0]);
            }
            if ("booleanKeys".equals(name)) {
                return Set.copyOf(booleans.keySet());
            }
            if ("getInteger".equals(name)) {
                return integers.get(args[0]);
            }
            if ("integerKeys".equals(name)) {
                return Set.copyOf(integers.keySet());
            }
            if ("getByte".equals(name)) {
                return bytes.get(args[0]);
            }
            if ("byteKeys".equals(name)) {
                return Set.copyOf(bytes.keySet());
            }
            if ("getShort".equals(name)) {
                return shorts.get(args[0]);
            }
            if ("shortKeys".equals(name)) {
                return Set.copyOf(shorts.keySet());
            }
            if ("getLong".equals(name)) {
                return longs.get(args[0]);
            }
            if ("longKeys".equals(name)) {
                return Set.copyOf(longs.keySet());
            }
            if ("toString".equals(name)) {
                return "PreferencesFixture";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected Preferences call: " + method);
        }
    }

    private static final class LoggingFixture implements InvocationHandler {
        private final List<String> messages = new ArrayList<>();
        private final Logging logging = (Logging) Proxy.newProxyInstance(
                Logging.class.getClassLoader(),
                new Class[]{Logging.class},
                this
        );

        private Logging logging() {
            return logging;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("logToOutput".equals(name)) {
                messages.add(String.valueOf(args[0]));
                return null;
            }
            if ("toString".equals(name)) {
                return "LoggingFixture";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected Logging call: " + method);
        }
    }
}
