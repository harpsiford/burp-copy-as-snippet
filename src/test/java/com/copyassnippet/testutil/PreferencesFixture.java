package com.copyassnippet.testutil;

import burp.api.montoya.persistence.Preferences;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PreferencesFixture implements InvocationHandler {
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

    public Preferences preferences() {
        return preferences;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("getString".equals(name)) {
            return strings.get(args[0]);
        }
        if ("setString".equals(name)) {
            setValue(strings, args);
            return null;
        }
        if ("deleteString".equals(name)) {
            strings.remove(args[0]);
            return null;
        }
        if ("stringKeys".equals(name)) {
            return Set.copyOf(strings.keySet());
        }
        if ("getBoolean".equals(name)) {
            return booleans.get(args[0]);
        }
        if ("setBoolean".equals(name)) {
            booleans.put((String) args[0], (Boolean) args[1]);
            return null;
        }
        if ("deleteBoolean".equals(name)) {
            booleans.remove(args[0]);
            return null;
        }
        if ("booleanKeys".equals(name)) {
            return Set.copyOf(booleans.keySet());
        }
        if ("getInteger".equals(name)) {
            return integers.get(args[0]);
        }
        if ("setInteger".equals(name)) {
            integers.put((String) args[0], (Integer) args[1]);
            return null;
        }
        if ("deleteInteger".equals(name)) {
            integers.remove(args[0]);
            return null;
        }
        if ("integerKeys".equals(name)) {
            return Set.copyOf(integers.keySet());
        }
        if ("getByte".equals(name)) {
            return bytes.get(args[0]);
        }
        if ("setByte".equals(name)) {
            bytes.put((String) args[0], (Byte) args[1]);
            return null;
        }
        if ("deleteByte".equals(name)) {
            bytes.remove(args[0]);
            return null;
        }
        if ("byteKeys".equals(name)) {
            return Set.copyOf(bytes.keySet());
        }
        if ("getShort".equals(name)) {
            return shorts.get(args[0]);
        }
        if ("setShort".equals(name)) {
            shorts.put((String) args[0], (Short) args[1]);
            return null;
        }
        if ("deleteShort".equals(name)) {
            shorts.remove(args[0]);
            return null;
        }
        if ("shortKeys".equals(name)) {
            return Set.copyOf(shorts.keySet());
        }
        if ("getLong".equals(name)) {
            return longs.get(args[0]);
        }
        if ("setLong".equals(name)) {
            longs.put((String) args[0], (Long) args[1]);
            return null;
        }
        if ("deleteLong".equals(name)) {
            longs.remove(args[0]);
            return null;
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

    private static void setValue(Map<String, String> target, Object[] args) {
        String key = (String) args[0];
        String value = (String) args[1];
        if (value == null) {
            target.remove(key);
        } else {
            target.put(key, value);
        }
    }
}
