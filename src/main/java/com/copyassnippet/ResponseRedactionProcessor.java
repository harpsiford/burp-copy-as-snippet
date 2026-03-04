package com.copyassnippet;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class ResponseRedactionProcessor {

    HttpResponse redact(HttpResponse response, RedactionPlan plan) {
        List<HttpHeader> headersToRemove = response.headers().stream()
                .filter(h -> plan.headerPatterns().stream().anyMatch(p -> p.matcher(h.name()).matches()))
                .collect(Collectors.toList());
        response = response.withRemovedHeaders(headersToRemove);

        if (!plan.redactHeaderPatterns().isEmpty()) {
            for (HttpHeader h : new ArrayList<>(response.headers())) {
                if (plan.redactHeaderPatterns().stream().anyMatch(p -> p.matcher(h.name()).matches())) {
                    response = response.withUpdatedHeader(h.name(), plan.replacementString());
                }
            }
        }

        // Remove all Set-Cookie headers first, then re-add each one individually.
        if (!plan.redactCookiePatterns().isEmpty()) {
            List<HttpHeader> setCookieHeaders = response.headers().stream()
                    .filter(h -> h.name().equalsIgnoreCase("Set-Cookie"))
                    .collect(Collectors.toList());
            if (!setCookieHeaders.isEmpty()) {
                response = response.withRemovedHeaders(setCookieHeaders);
                for (HttpHeader header : setCookieHeaders) {
                    String cookieLine = header.value();
                    int eq = cookieLine.indexOf('=');
                    int semi = cookieLine.indexOf(';');
                    String newValue = cookieLine;
                    if (eq > 0) {
                        String cookieName = cookieLine.substring(0, eq).trim();
                        if (plan.redactCookiePatterns().stream().anyMatch(p -> p.matcher(cookieName).matches())) {
                            String rest = semi >= 0 ? cookieLine.substring(semi) : "";
                            newValue = cookieName + "=" + plan.replacementString() + rest;
                        }
                    }
                    response = response.withAddedHeader(header.name(), newValue);
                }
            }
        }

        return response;
    }
}
