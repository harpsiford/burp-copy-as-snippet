package com.copyassnippet.redaction.processor;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.redaction.plan.RedactionPlan;
import com.copyassnippet.redaction.plan.RedactionPlanCompiler;
import com.copyassnippet.testutil.HttpFixtures;
import com.copyassnippet.testutil.TestArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseRedactionPropertyTest {
    private static final String REMOVE_HEADER_NAME = "Server";
    private static final String KEEP_HEADER_NAME = "X-Keep";
    private static final String REDACT_HEADER_NAME = "Authorization";
    private static final String KEEP_COOKIE_NAME = "keepCookie";
    private static final String REDACT_COOKIE_NAME = "sessionid";

    private final ResponseRedactionProcessor processor = new ResponseRedactionProcessor();
    private final RedactionPlanCompiler compiler = new RedactionPlanCompiler();

    private record ResponseScenario(
            String removeHeaderValue,
            String keepHeaderValue,
            String redactHeaderValue,
            String keepCookieValue,
            String redactCookieValue,
            String replacement
    ) {
    }

    @Property(tries = 45)
    void responseRedactionPreservesStructureWhileHidingSecrets(@ForAll("responseScenarios") ResponseScenario scenario) {
        HttpResponse response = buildResponse(scenario);
        RedactionPlan plan = compiler.compile(presetFor(scenario));

        HttpResponse redacted = processor.redact(response, plan);
        HttpResponse redactedAgain = processor.redact(redacted, plan);

        assertFalse(redacted.hasHeader(REMOVE_HEADER_NAME));
        assertEquals(scenario.keepHeaderValue(), redacted.headerValue(KEEP_HEADER_NAME));
        assertEquals(scenario.replacement(), redacted.headerValue(REDACT_HEADER_NAME));

        List<HttpHeader> setCookies = redacted.headers().stream()
                .filter(header -> header.name().equalsIgnoreCase("Set-Cookie"))
                .toList();
        assertEquals(2, setCookies.size());

        String keepSetCookie = setCookies.stream()
                .map(HttpHeader::value)
                .filter(value -> value.startsWith(KEEP_COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();
        assertTrue(keepSetCookie.contains(KEEP_COOKIE_NAME + "=" + scenario.keepCookieValue()));
        assertTrue(keepSetCookie.contains("Path=/"));

        String redactSetCookie = setCookies.stream()
                .map(HttpHeader::value)
                .filter(value -> value.startsWith(REDACT_COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow();
        assertTrue(redactSetCookie.contains(REDACT_COOKIE_NAME + "=" + scenario.replacement()));
        assertTrue(redactSetCookie.contains("HttpOnly"));
        assertFalse(redactSetCookie.contains(scenario.redactCookieValue()));

        String result = redacted.toString();
        assertFalse(result.contains(scenario.removeHeaderValue()));
        assertFalse(result.contains(scenario.redactHeaderValue()));
        assertFalse(result.contains(scenario.redactCookieValue()));
        assertTrue(result.contains(scenario.keepHeaderValue()));
        assertTrue(result.contains(scenario.keepCookieValue()));
        assertEquals(redacted.toString(), redactedAgain.toString());
    }

    @Provide
    Arbitrary<ResponseScenario> responseScenarios() {
        return Combinators.combine(
                TestArbitraries.token("remove-header-"),
                TestArbitraries.token("keep-header-"),
                TestArbitraries.token("redact-header-"),
                TestArbitraries.token("keep-cookie-"),
                TestArbitraries.token("redact-cookie-"),
                TestArbitraries.token("MASK-")
        ).as(ResponseScenario::new);
    }

    private static Preset presetFor(ResponseScenario scenario) {
        return new Preset(
                "preset-id",
                "preset",
                List.of(Pattern.quote(REMOVE_HEADER_NAME)),
                List.of(),
                List.of(),
                List.of(
                        new RedactionRule(RedactionRule.Type.HEADER, Pattern.quote(REDACT_HEADER_NAME)),
                        new RedactionRule(RedactionRule.Type.COOKIE, Pattern.quote(REDACT_COOKIE_NAME))
                ),
                scenario.replacement(),
                "{{request}}\n{{response}}",
                true
        );
    }

    private static HttpResponse buildResponse(ResponseScenario scenario) {
        return HttpFixtures.response(
                (short) 200,
                List.of(
                        HttpFixtures.header(REMOVE_HEADER_NAME, scenario.removeHeaderValue()),
                        HttpFixtures.header(KEEP_HEADER_NAME, scenario.keepHeaderValue()),
                        HttpFixtures.header(REDACT_HEADER_NAME, scenario.redactHeaderValue()),
                        HttpFixtures.header("Set-Cookie", KEEP_COOKIE_NAME + "=" + scenario.keepCookieValue() + "; Path=/; Secure"),
                        HttpFixtures.header("Set-Cookie", REDACT_COOKIE_NAME + "=" + scenario.redactCookieValue() + "; Path=/; HttpOnly")
                ),
                "ok"
        );
    }
}
