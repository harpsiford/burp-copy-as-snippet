package com.copyassnippet.testutil;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HttpFixtures {
    private HttpFixtures() {
    }

    public static HeaderValue header(String name, String value) {
        return new HeaderValue(name, value);
    }

    public static ParameterValue parameter(String name, String value, HttpParameterType type) {
        return new ParameterValue(name, value, type);
    }

    public static HttpRequest request(String method, String path, List<HeaderValue> headers, List<ParameterValue> parameters) {
        return (HttpRequest) Proxy.newProxyInstance(
                HttpRequest.class.getClassLoader(),
                new Class[]{HttpRequest.class},
                new RequestHandler(new RequestState(method, path, List.copyOf(headers), List.copyOf(parameters)))
        );
    }

    public static HttpResponse response(short statusCode, List<HeaderValue> headers, String body) {
        return (HttpResponse) Proxy.newProxyInstance(
                HttpResponse.class.getClassLoader(),
                new Class[]{HttpResponse.class},
                new ResponseHandler(new ResponseState(statusCode, List.copyOf(headers), body))
        );
    }

    public static HttpRequestResponse requestResponse(HttpRequest request, HttpResponse response) {
        return (HttpRequestResponse) Proxy.newProxyInstance(
                HttpRequestResponse.class.getClassLoader(),
                new Class[]{HttpRequestResponse.class},
                new RequestResponseHandler(request, response)
        );
    }

    public record HeaderValue(String name, String value) {
    }

    public record ParameterValue(String name, String value, HttpParameterType type) {
    }

    private record RequestState(String method, String path, List<HeaderValue> headers, List<ParameterValue> parameters) {
    }

    private record ResponseState(short statusCode, List<HeaderValue> headers, String body) {
    }

    private static final class RequestHandler implements InvocationHandler {
        private final RequestState state;

        private RequestHandler(RequestState state) {
            this.state = state;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "hasHeader":
                    return hasHeader((String) args[0], state.headers());
                case "headerValue":
                    return headerValue((String) args[0], state.headers());
                case "headers":
                    return headerProxies(state.headers());
                case "parameters":
                    return parameterProxies(state.parameters());
                case "withRemovedHeader":
                    return request(state.method(), state.path(), removeHeaders(state.headers(), List.of((String) args[0])), state.parameters());
                case "withUpdatedHeader":
                    return request(state.method(), state.path(), updateHeader(state.headers(), (String) args[0], (String) args[1]), state.parameters());
                case "withRemovedHeaders":
                    return request(state.method(), state.path(), removeHeaders(state.headers(), headerNames((List<?>) args[0])), state.parameters());
                case "withRemovedParameters":
                    return request(state.method(), state.path(), state.headers(), removeParameters(state.parameters(), (List<?>) args[0]));
                case "withParameter":
                    return request(state.method(), state.path(), state.headers(), addParameter(state.parameters(), (HttpParameter) args[0]));
                case "hasParameter":
                    return hasParameter((String) args[0], (HttpParameterType) args[1], state.parameters());
                case "parameterValue":
                    return parameterValue((String) args[0], (HttpParameterType) args[1], state.parameters());
                case "toString":
                    return requestToString(state);
                case "method":
                    return state.method();
                case "path":
                    return state.path();
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException("Unexpected HttpRequest call: " + method);
            }
        }
    }

    private static final class ResponseHandler implements InvocationHandler {
        private final ResponseState state;

        private ResponseHandler(ResponseState state) {
            this.state = state;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "hasHeader":
                    return hasHeader((String) args[0], state.headers());
                case "headerValue":
                    return headerValue((String) args[0], state.headers());
                case "headers":
                    return headerProxies(state.headers());
                case "withRemovedHeaders":
                    return response(state.statusCode(), removeHeaders(state.headers(), headerNames((List<?>) args[0])), state.body());
                case "withUpdatedHeader":
                    return response(state.statusCode(), updateHeader(state.headers(), (String) args[0], (String) args[1]), state.body());
                case "withAddedHeader":
                    return response(state.statusCode(), addHeader(state.headers(), (String) args[0], (String) args[1]), state.body());
                case "withBody":
                    return response(state.statusCode(), state.headers(), (String) args[0]);
                case "statusCode":
                    return state.statusCode();
                case "toString":
                    return responseToString(state);
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException("Unexpected HttpResponse call: " + method);
            }
        }
    }

    private static final class RequestResponseHandler implements InvocationHandler {
        private final HttpRequest request;
        private final HttpResponse response;

        private RequestResponseHandler(HttpRequest request, HttpResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "request":
                    return request;
                case "response":
                    return response;
                case "hasResponse":
                    return response != null;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    throw new UnsupportedOperationException("Unexpected HttpRequestResponse call: " + method);
            }
        }
    }

    private static List<HttpHeader> headerProxies(List<HeaderValue> headers) {
        List<HttpHeader> proxies = new ArrayList<>();
        for (HeaderValue header : headers) {
            proxies.add((HttpHeader) Proxy.newProxyInstance(
                    HttpHeader.class.getClassLoader(),
                    new Class[]{HttpHeader.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "name" -> header.name();
                        case "value" -> header.value();
                        case "toString" -> header.name() + ": " + header.value();
                        case "hashCode" -> Objects.hash(header.name(), header.value());
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException("Unexpected HttpHeader call: " + method);
                    }
            ));
        }
        return proxies;
    }

    private static List<ParsedHttpParameter> parameterProxies(List<ParameterValue> parameters) {
        List<ParsedHttpParameter> proxies = new ArrayList<>();
        for (ParameterValue parameter : parameters) {
            proxies.add((ParsedHttpParameter) Proxy.newProxyInstance(
                    ParsedHttpParameter.class.getClassLoader(),
                    new Class[]{ParsedHttpParameter.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "type" -> parameter.type();
                        case "name" -> parameter.name();
                        case "value" -> parameter.value();
                        case "nameOffsets", "valueOffsets" -> null;
                        case "hashCode" -> Objects.hash(parameter.name(), parameter.value(), parameter.type());
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException("Unexpected ParsedHttpParameter call: " + method);
                    }
            ));
        }
        return proxies;
    }

    private static boolean hasHeader(String name, List<HeaderValue> headers) {
        return headers.stream().anyMatch(header -> header.name().equalsIgnoreCase(name));
    }

    private static String headerValue(String name, List<HeaderValue> headers) {
        return headers.stream()
                .filter(header -> header.name().equalsIgnoreCase(name))
                .map(HeaderValue::value)
                .findFirst()
                .orElse(null);
    }

    private static List<String> headerNames(List<?> headers) {
        List<String> names = new ArrayList<>();
        for (Object header : headers) {
            if (header instanceof HttpHeader httpHeader) {
                names.add(httpHeader.name());
            } else if (header instanceof String value) {
                names.add(value);
            }
        }
        return names;
    }

    private static List<HeaderValue> removeHeaders(List<HeaderValue> headers, List<String> namesToRemove) {
        return headers.stream()
                .filter(header -> namesToRemove.stream().noneMatch(name -> header.name().equalsIgnoreCase(name)))
                .toList();
    }

    private static List<HeaderValue> updateHeader(List<HeaderValue> headers, String name, String value) {
        List<HeaderValue> updated = new ArrayList<>();
        boolean replaced = false;
        for (HeaderValue header : headers) {
            if (header.name().equalsIgnoreCase(name)) {
                updated.add(new HeaderValue(header.name(), value));
                replaced = true;
            } else {
                updated.add(header);
            }
        }
        if (!replaced) {
            updated.add(new HeaderValue(name, value));
        }
        return updated;
    }

    private static List<HeaderValue> addHeader(List<HeaderValue> headers, String name, String value) {
        List<HeaderValue> updated = new ArrayList<>(headers);
        updated.add(new HeaderValue(name, value));
        return updated;
    }

    private static boolean hasParameter(String name, HttpParameterType type, List<ParameterValue> parameters) {
        return parameters.stream().anyMatch(parameter -> parameter.name().equals(name) && parameter.type() == type);
    }

    private static String parameterValue(String name, HttpParameterType type, List<ParameterValue> parameters) {
        return parameters.stream()
                .filter(parameter -> parameter.name().equals(name) && parameter.type() == type)
                .map(ParameterValue::value)
                .findFirst()
                .orElse(null);
    }

    private static List<ParameterValue> removeParameters(List<ParameterValue> parameters, List<?> paramsToRemove) {
        return parameters.stream()
                .filter(parameter -> paramsToRemove.stream().noneMatch(candidate -> parameterMatches(parameter, candidate)))
                .toList();
    }

    private static boolean parameterMatches(ParameterValue parameter, Object candidate) {
        if (!(candidate instanceof HttpParameter httpParameter)) {
            return false;
        }
        return parameter.name().equals(httpParameter.name())
                && parameter.type() == httpParameter.type()
                && Objects.equals(parameter.value(), httpParameter.value());
    }

    private static List<ParameterValue> addParameter(List<ParameterValue> parameters, HttpParameter parameter) {
        List<ParameterValue> updated = new ArrayList<>(parameters);
        updated.add(new ParameterValue(parameter.name(), parameter.value(), parameter.type()));
        return updated;
    }

    private static String requestToString(RequestState state) {
        StringBuilder builder = new StringBuilder();
        builder.append(state.method()).append(" ").append(state.path()).append(" HTTP/1.1\r\n");
        for (HeaderValue header : state.headers()) {
            builder.append(header.name()).append(": ").append(header.value()).append("\r\n");
        }
        builder.append("\r\n");
        for (ParameterValue parameter : state.parameters()) {
            builder.append(parameter.type().name().toLowerCase(Locale.ROOT))
                    .append(" ")
                    .append(parameter.name())
                    .append("=")
                    .append(parameter.value())
                    .append("\r\n");
        }
        return builder.toString();
    }

    private static String responseToString(ResponseState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("HTTP/1.1 ").append(state.statusCode()).append(" OK\r\n");
        for (HeaderValue header : state.headers()) {
            builder.append(header.name()).append(": ").append(header.value()).append("\r\n");
        }
        builder.append("\r\n");
        if (state.body() != null) {
            builder.append(state.body());
        }
        return builder.toString();
    }
}
