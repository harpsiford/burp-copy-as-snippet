package com.copyassnippet;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class RequestRedactor {
    private static final RedactionEngine DEFAULT_ENGINE = new CachingRedactionEngine();

    private final Preset preset;
    private final RedactionEngine redactionEngine;

    public RequestRedactor(Preset preset) {
        this(preset, DEFAULT_ENGINE);
    }

    RequestRedactor(Preset preset, RedactionEngine redactionEngine) {
        this.preset = preset;
        this.redactionEngine = redactionEngine;
    }

    public HttpRequest redact(HttpRequest request) {
        return redactionEngine.redactRequest(preset, request);
    }

    public HttpResponse redact(HttpResponse response) {
        return redactionEngine.redactResponse(preset, response);
    }

    public String format(HttpRequestResponse requestResponse) {
        return redactionEngine.format(preset, requestResponse);
    }
}
