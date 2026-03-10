package com.copyassnippet.testutil;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Persistence;
import burp.api.montoya.persistence.Preferences;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public final class MontoyaFixtures {
    private MontoyaFixtures() {
    }

    public static MontoyaApi montoyaApi(Preferences preferences) {
        Map<String, Object> persistenceMethods = new HashMap<>();
        persistenceMethods.put("preferences", preferences);
        persistenceMethods.put("extensionData", null);
        Persistence persistence = (Persistence) Proxy.newProxyInstance(
                Persistence.class.getClassLoader(),
                new Class[]{Persistence.class},
                new FixedReturnHandler(persistenceMethods)
        );

        return (MontoyaApi) Proxy.newProxyInstance(
                MontoyaApi.class.getClassLoader(),
                new Class[]{MontoyaApi.class},
                new FixedReturnHandler(Map.of("persistence", persistence))
        );
    }

    private static final class FixedReturnHandler implements InvocationHandler {
        private final Map<String, Object> returnsByMethod;

        private FixedReturnHandler(Map<String, Object> returnsByMethod) {
            this.returnsByMethod = returnsByMethod;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (returnsByMethod.containsKey(name)) {
                return returnsByMethod.get(name);
            }
            if ("toString".equals(name)) {
                return method.getDeclaringClass().getSimpleName() + "Proxy";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected proxy call: " + method);
        }
    }
}
