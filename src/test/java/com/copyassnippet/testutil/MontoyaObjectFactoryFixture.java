package com.copyassnippet.testutil;

import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.internal.MontoyaObjectFactory;
import burp.api.montoya.internal.ObjectFactoryLocator;

import java.lang.reflect.Proxy;

public final class MontoyaObjectFactoryFixture {
    private MontoyaObjectFactoryFixture() {
    }

    public static void install() {
        if (ObjectFactoryLocator.FACTORY != null) {
            return;
        }

        ObjectFactoryLocator.FACTORY = (MontoyaObjectFactory) Proxy.newProxyInstance(
                MontoyaObjectFactory.class.getClassLoader(),
                new Class[]{MontoyaObjectFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "parameter" -> parameter((String) args[0], (String) args[1], (HttpParameterType) args[2]);
                    case "urlParameter" -> parameter((String) args[0], (String) args[1], HttpParameterType.URL);
                    case "bodyParameter" -> parameter((String) args[0], (String) args[1], HttpParameterType.BODY);
                    case "cookieParameter" -> parameter((String) args[0], (String) args[1], HttpParameterType.COOKIE);
                    default -> throw new UnsupportedOperationException("Unexpected factory call: " + method);
                }
        );
    }

    private static HttpParameter parameter(String name, String value, HttpParameterType type) {
        return (HttpParameter) Proxy.newProxyInstance(
                HttpParameter.class.getClassLoader(),
                new Class[]{HttpParameter.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "type" -> type;
                    case "name" -> name;
                    case "value" -> value;
                    case "toString" -> name + "=" + value;
                    case "hashCode" -> java.util.Objects.hash(name, value, type);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected HttpParameter call: " + method);
                }
        );
    }
}
