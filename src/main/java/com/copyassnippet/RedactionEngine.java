package com.copyassnippet;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public interface RedactionEngine {
    HttpRequest redactRequest(Preset preset, HttpRequest request);

    HttpResponse redactResponse(Preset preset, HttpResponse response);

    String format(Preset preset, HttpRequestResponse requestResponse);
}
