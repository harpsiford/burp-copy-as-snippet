package com.copyassnippet.redaction.processor;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.copyassnippet.redaction.plan.RedactionPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RequestRedactionProcessor {

    public HttpRequest redact(HttpRequest request, RedactionPlan plan) {
        if (request.hasHeader("Cookie")) {
            List<String> newCookies = Arrays.stream(request.headerValue("Cookie").split(";"))
                    .map(String::trim)
                    .map(s -> s.split("=", 2))
                    .filter(a -> plan.cookiePatterns().stream().noneMatch(p -> p.matcher(a[0]).matches()))
                    .map(a -> (a.length > 1) ? (a[0] + "=" + a[1]) : a[0])
                    .collect(Collectors.toList());

            if (newCookies.isEmpty()) {
                request = request.withRemovedHeader("Cookie");
            } else {
                request = request.withUpdatedHeader("Cookie", String.join("; ", newCookies));
            }
        }

        List<HttpHeader> headersToRemove = request.headers().stream()
                .filter(h -> plan.headerPatterns().stream().anyMatch(p -> p.matcher(h.name()).matches()))
                .collect(Collectors.toList());
        request = request.withRemovedHeaders(headersToRemove);

        if (!plan.paramPatterns().isEmpty()) {
            List<ParsedHttpParameter> paramsToRemove = request.parameters().stream()
                    .filter(RequestRedactionProcessor::isRedactableParameter)
                    .filter(p -> plan.paramPatterns().stream().anyMatch(pat -> pat.matcher(p.name()).matches()))
                    .collect(Collectors.toList());
            if (!paramsToRemove.isEmpty()) {
                request = request.withRemovedParameters(paramsToRemove);
            }
        }

        if (!plan.redactCookiePatterns().isEmpty() && request.hasHeader("Cookie")) {
            List<String> redactedCookies = Arrays.stream(request.headerValue("Cookie").split(";"))
                    .map(String::trim)
                    .map(s -> s.split("=", 2))
                    .map(a -> {
                        if (a.length > 1 && plan.redactCookiePatterns().stream().anyMatch(p -> p.matcher(a[0]).matches())) {
                            return a[0] + "=" + plan.replacementString();
                        }
                        return (a.length > 1) ? (a[0] + "=" + a[1]) : a[0];
                    })
                    .collect(Collectors.toList());
            request = request.withUpdatedHeader("Cookie", String.join("; ", redactedCookies));
        }

        if (!plan.redactHeaderPatterns().isEmpty()) {
            for (HttpHeader h : new ArrayList<>(request.headers())) {
                if (plan.redactHeaderPatterns().stream().anyMatch(p -> p.matcher(h.name()).matches())) {
                    request = request.withUpdatedHeader(h.name(), plan.replacementString());
                }
            }
        }

        if (!plan.redactParamPatterns().isEmpty()) {
            List<ParsedHttpParameter> paramsToRedact = request.parameters().stream()
                    .filter(RequestRedactionProcessor::isRedactableParameter)
                    .filter(p -> plan.redactParamPatterns().stream().anyMatch(pat -> pat.matcher(p.name()).matches()))
                    .collect(Collectors.toList());
            for (ParsedHttpParameter param : paramsToRedact) {
                request = request.withRemovedParameters(List.of(param));
                request = request.withParameter(HttpParameter.parameter(
                        param.name(),
                        plan.replacementString(),
                        param.type())
                );
            }
        }

        return request;
    }

    private static boolean isRedactableParameter(ParsedHttpParameter parameter) {
        return parameter.type() == HttpParameterType.URL
                || parameter.type() == HttpParameterType.BODY
                || parameter.type() == HttpParameterType.JSON;
    }
}
