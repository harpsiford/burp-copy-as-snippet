package example.contextmenu;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Standalone utility that redacts HTTP requests/responses using a Preset's
 * header and cookie regex lists, then formats the result using the preset's template.
 */
public class RequestRedactor {

    private final List<Pattern> headerPatterns;
    private final List<Pattern> cookiePatterns;
    private final String template;

    public RequestRedactor(Preset preset) {
        this.headerPatterns = preset.getHeaderRegexes().stream()
                .map(r -> Pattern.compile(r, Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
        this.cookiePatterns = preset.getCookieRegexes().stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        this.template = preset.getTemplate();
    }

    public HttpRequest redact(HttpRequest request) {
        if (request.hasHeader("Authorization")) {
            request = request.withUpdatedHeader("Authorization",
                    request.headerValue("Authorization").replaceAll(" .+$", " [REDACTED]"));
        }
        if (request.hasHeader("X-Authorization")) {
            request = request.withUpdatedHeader("X-Authorization",
                    request.headerValue("X-Authorization").replaceAll(" .+$", " [REDACTED]"));
        }
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
        return request.withRemovedHeaders(headersToRemove);
    }

    public HttpResponse redact(HttpResponse response) {
        List<HttpHeader> headersToRemove = response.headers().stream()
                .filter(h -> headerPatterns.stream().anyMatch(p -> p.matcher(h.name()).matches()))
                .collect(Collectors.toList());
        return response.withRemovedHeaders(headersToRemove);
    }

    public String format(HttpRequestResponse requestResponse) {
        String redactedRequest = redact(requestResponse.request()).toString();

        String responseBlock;
        if (requestResponse.response() != null) {
            responseBlock = redact(requestResponse.response()).toString();
        } else {
            responseBlock = "No response was received.";
        }

        return template
                .replace("{{request}}", redactedRequest)
                .replace("{{response}}", responseBlock);
    }
}
