package com.copyassnippet;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;


public class RequestRedactor {

    private final List<Pattern> headerPatterns;
    private final List<Pattern> cookiePatterns;
    private final List<Pattern> paramPatterns;

    private final List<Pattern> redactCookiePatterns;
    private final List<Pattern> redactHeaderPatterns;
    private final List<Pattern> redactParamPatterns;
    private final List<Pattern> redactRegexPatterns;

    private final String replacementString;
    private final String template;

    private static String anchorRegex(String r) {
        String s = r.startsWith("^") ? r : "^" + r;
        return s.endsWith("$") ? s : s + "$";
    }

    private static Pattern tryCompile(String regex, int flags) {
        try {
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    public RequestRedactor(Preset preset) {
        List<Pattern> hPat = new ArrayList<>();
        for (String r : preset.getHeaderRegexes()) {
            Pattern p = tryCompile(anchorRegex(r), Pattern.CASE_INSENSITIVE);
            if (p != null) hPat.add(p);
        }
        this.headerPatterns = hPat;

        List<Pattern> cPat = new ArrayList<>();
        for (String r : preset.getCookieRegexes()) {
            Pattern p = tryCompile(anchorRegex(r), 0);
            if (p != null) cPat.add(p);
        }
        this.cookiePatterns = cPat;

        List<Pattern> pPat = new ArrayList<>();
        for (String r : preset.getParamRegexes()) {
            Pattern p = tryCompile(anchorRegex(r), 0);
            if (p != null) pPat.add(p);
        }
        this.paramPatterns = pPat;

        this.replacementString = preset.getReplacementString();

        List<Pattern> rCookie = new ArrayList<>();
        List<Pattern> rHeader = new ArrayList<>();
        List<Pattern> rParam = new ArrayList<>();
        List<Pattern> rRegex = new ArrayList<>();
        for (RedactionRule rule : preset.getRedactionRules()) {
            switch (rule.getType()) {
                case COOKIE: { Pattern p = tryCompile(anchorRegex(rule.getPattern()), 0); if (p != null) rCookie.add(p); break; }
                case HEADER: { Pattern p = tryCompile(anchorRegex(rule.getPattern()), Pattern.CASE_INSENSITIVE); if (p != null) rHeader.add(p); break; }
                case PARAM:  { Pattern p = tryCompile(anchorRegex(rule.getPattern()), 0); if (p != null) rParam.add(p); break; }
                case REGEX:  { Pattern p = tryCompile(rule.getPattern(), Pattern.DOTALL); if (p != null) rRegex.add(p); break; }
            }
        }
        this.redactCookiePatterns = rCookie;
        this.redactHeaderPatterns = rHeader;
        this.redactParamPatterns = rParam;
        this.redactRegexPatterns = rRegex;

        this.template = preset.getTemplate();
    }

    public HttpRequest redact(HttpRequest request) {
        if (request.hasHeader("Cookie")) {
            List<String> newCookies = Arrays.stream(request.headerValue("Cookie").split(";"))
                    .map(String::trim)
                    .map(s -> s.split("=", 2))
                    .filter(a -> cookiePatterns.stream().noneMatch(p -> p.matcher(a[0]).matches()))
                    .map(a -> (a.length > 1) ? (a[0] + "=" + a[1]) : a[0])
                    .collect(Collectors.toList());

            if (newCookies.isEmpty()) {
                request = request.withRemovedHeader("Cookie");
            } else {
                request = request.withUpdatedHeader("Cookie", String.join("; ", newCookies));
            }
        }

        List<HttpHeader> headersToRemove = request.headers().stream()
                .filter(h -> headerPatterns.stream().anyMatch(p -> p.matcher(h.name()).matches()))
                .collect(Collectors.toList());
        request = request.withRemovedHeaders(headersToRemove);

        if (!paramPatterns.isEmpty()) {
            List<ParsedHttpParameter> paramsToRemove = request.parameters().stream()
                    .filter(p -> p.type() == HttpParameterType.URL
                              || p.type() == HttpParameterType.BODY
                              || p.type() == HttpParameterType.JSON)
                    .filter(p -> paramPatterns.stream().anyMatch(pat -> pat.matcher(p.name()).matches()))
                    .collect(Collectors.toList());
            if (!paramsToRemove.isEmpty()) {
                request = request.withRemovedParameters(paramsToRemove);
            }
        }

        if (!redactCookiePatterns.isEmpty() && request.hasHeader("Cookie")) {
            List<String> redactedCookies = Arrays.stream(request.headerValue("Cookie").split(";"))
                    .map(String::trim)
                    .map(s -> s.split("=", 2))
                    .map(a -> {
                        if (a.length > 1 && redactCookiePatterns.stream().anyMatch(p -> p.matcher(a[0]).matches())) {
                            return a[0] + "=" + replacementString;
                        }
                        return (a.length > 1) ? (a[0] + "=" + a[1]) : a[0];
                    })
                    .collect(Collectors.toList());
            request = request.withUpdatedHeader("Cookie", String.join("; ", redactedCookies));
        }

        if (!redactHeaderPatterns.isEmpty()) {
            for (HttpHeader h : new ArrayList<>(request.headers())) {
                if (redactHeaderPatterns.stream().anyMatch(p -> p.matcher(h.name()).matches())) {
                    request = request.withUpdatedHeader(h.name(), replacementString);
                }
            }
        }

        if (!redactParamPatterns.isEmpty()) {
            List<ParsedHttpParameter> paramsToRedact = request.parameters().stream()
                    .filter(p -> p.type() == HttpParameterType.URL
                              || p.type() == HttpParameterType.BODY
                              || p.type() == HttpParameterType.JSON)
                    .filter(p -> redactParamPatterns.stream().anyMatch(pat -> pat.matcher(p.name()).matches()))
                    .collect(Collectors.toList());
            for (ParsedHttpParameter param : paramsToRedact) {
                request = request.withRemovedParameters(List.of(param));
                request = request.withParameter(HttpParameter.parameter(param.name(), replacementString, param.type()));
            }
        }

        return request;
    }

    public HttpResponse redact(HttpResponse response) {
        List<HttpHeader> headersToRemove = response.headers().stream()
                .filter(h -> headerPatterns.stream().anyMatch(p -> p.matcher(h.name()).matches()))
                .collect(Collectors.toList());
        response = response.withRemovedHeaders(headersToRemove);

        if (!redactHeaderPatterns.isEmpty()) {
            for (HttpHeader h : new ArrayList<>(response.headers())) {
                if (redactHeaderPatterns.stream().anyMatch(p -> p.matcher(h.name()).matches())) {
                    response = response.withUpdatedHeader(h.name(), replacementString);
                }
            }
        }

        // Remove all Set-Cookie headers first, then re-add each one individually (withUpdatedHeader only updates the first occurrence, breaking multi-value headers)
        if (!redactCookiePatterns.isEmpty()) {
            List<HttpHeader> setCookieHeaders = response.headers().stream()
                    .filter(h -> h.name().equalsIgnoreCase("Set-Cookie"))
                    .collect(Collectors.toList());
            if (!setCookieHeaders.isEmpty()) {
                response = response.withRemovedHeaders(setCookieHeaders);
                for (HttpHeader h : setCookieHeaders) {
                    String cookieLine = h.value();
                    // Set-Cookie: name=value; attributes...
                    int eq = cookieLine.indexOf('=');
                    int semi = cookieLine.indexOf(';');
                    String newValue = cookieLine;
                    if (eq > 0) {
                        String cookieName = cookieLine.substring(0, eq).trim();
                        if (redactCookiePatterns.stream().anyMatch(p -> p.matcher(cookieName).matches())) {
                            String rest = semi >= 0 ? cookieLine.substring(semi) : "";
                            newValue = cookieName + "=" + replacementString + rest;
                        }
                    }
                    response = response.withAddedHeader(h.name(), newValue);
                }
            }
        }

        return response;
    }

    private String applyRegexRedactions(String text) {
        for (Pattern p : redactRegexPatterns) {
            Matcher m = p.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                int groupCount = m.groupCount();
                if (groupCount == 0) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacementString));
                } else {
                    // Replace all capturing groups; preserve non-group text within the match.
                    // Skip groups that start before `pos` (i.e. nested inside an already-replaced group).
                    StringBuilder rep = new StringBuilder();
                    int pos = m.start();
                    for (int i = 1; i <= groupCount; i++) {
                        if (m.group(i) != null && m.start(i) >= pos) {
                            rep.append(text, pos, m.start(i));
                            rep.append(replacementString);
                            pos = m.end(i);
                        }
                    }
                    rep.append(text, pos, m.end()); // text after last group, still within the match
                    m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
                }
            }
            m.appendTail(sb);
            text = sb.toString();
        }
        return text;
    }

    public String format(HttpRequestResponse requestResponse) {
        String redactedRequest = applyRegexRedactions(redact(requestResponse.request()).toString());

        String responseBlock;
        if (requestResponse.response() != null) {
            responseBlock = applyRegexRedactions(redact(requestResponse.response()).toString());
        } else {
            responseBlock = "No response was received.";
        }

        return template
                .replace("{{request}}", redactedRequest)
                .replace("{{response}}", responseBlock);
    }
}
