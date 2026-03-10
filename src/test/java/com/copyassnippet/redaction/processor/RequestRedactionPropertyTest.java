package com.copyassnippet.redaction.processor;

import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.redaction.plan.RedactionPlan;
import com.copyassnippet.redaction.plan.RedactionPlanCompiler;
import com.copyassnippet.testutil.HttpFixtures;
import com.copyassnippet.testutil.MontoyaObjectFactoryFixture;
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

class RequestRedactionPropertyTest {
    private static final String REMOVE_HEADER_NAME = "X-Remove-Header";
    private static final String KEEP_HEADER_NAME = "X-Keep-Header";
    private static final String REDACT_HEADER_NAME = "Authorization";
    private static final String REMOVE_COOKIE_NAME = "removeCookie";
    private static final String KEEP_COOKIE_NAME = "keepCookie";
    private static final String REDACT_COOKIE_NAME = "sessionid";

    private final RequestRedactionProcessor processor = new RequestRedactionProcessor();
    private final RedactionPlanCompiler compiler = new RedactionPlanCompiler();

    private record RequestSeed(
            HttpParameterType parameterType,
            String keepHeaderValue,
            String removeHeaderValue,
            String redactHeaderValue,
            String keepCookieValue,
            String removeCookieValue,
            String redactCookieValue,
            String keepParamName
    ) {
    }

    private record RequestScenario(
            HttpParameterType parameterType,
            String keepHeaderValue,
            String removeHeaderValue,
            String redactHeaderValue,
            String keepCookieValue,
            String removeCookieValue,
            String redactCookieValue,
            String keepParamName,
            String keepParamValue,
            String removeParamName,
            String removeParamValue,
            String redactParamName,
            String redactParamValue,
            String replacement
    ) {
    }

    @Property(tries = 45)
    void requestRedactionIsSelectiveStableAndLeakFree(@ForAll("requestScenarios") RequestScenario scenario) {
        MontoyaObjectFactoryFixture.install();
        HttpRequest request = buildRequest(scenario);
        RedactionPlan plan = compiler.compile(presetFor(scenario));

        HttpRequest redacted = processor.redact(request, plan);
        HttpRequest redactedAgain = processor.redact(redacted, plan);

        assertFalse(redacted.hasHeader(REMOVE_HEADER_NAME));
        assertEquals(scenario.keepHeaderValue(), redacted.headerValue(KEEP_HEADER_NAME));
        assertEquals(scenario.replacement(), redacted.headerValue(REDACT_HEADER_NAME));

        String cookieHeader = redacted.headerValue("Cookie");
        assertFalse(cookieHeader.contains(REMOVE_COOKIE_NAME + "="));
        assertTrue(cookieHeader.contains(KEEP_COOKIE_NAME + "=" + scenario.keepCookieValue()));
        assertTrue(cookieHeader.contains(REDACT_COOKIE_NAME + "=" + scenario.replacement()));

        assertEquals(scenario.keepParamValue(), redacted.parameterValue(scenario.keepParamName(), scenario.parameterType()));
        assertFalse(redacted.hasParameter(scenario.removeParamName(), scenario.parameterType()));
        assertEquals(scenario.replacement(), redacted.parameterValue(scenario.redactParamName(), scenario.parameterType()));

        String result = redacted.toString();
        assertFalse(result.contains(scenario.removeHeaderValue()));
        assertFalse(result.contains(scenario.redactHeaderValue()));
        assertFalse(result.contains(scenario.removeCookieValue()));
        assertFalse(result.contains(scenario.redactCookieValue()));
        assertFalse(result.contains(scenario.removeParamValue()));
        assertFalse(result.contains(scenario.redactParamValue()));
        assertTrue(result.contains(scenario.keepHeaderValue()));
        assertTrue(result.contains(scenario.keepCookieValue()));
        assertTrue(result.contains(scenario.keepParamValue()));
        assertEquals(redacted.toString(), redactedAgain.toString());
    }

    @Provide
    Arbitrary<RequestScenario> requestScenarios() {
        return Combinators.combine(
                net.jqwik.api.Arbitraries.of(HttpParameterType.URL, HttpParameterType.BODY, HttpParameterType.JSON),
                TestArbitraries.token("keep-header-"),
                TestArbitraries.token("remove-header-"),
                TestArbitraries.token("redact-header-"),
                TestArbitraries.token("keep-cookie-"),
                TestArbitraries.token("remove-cookie-"),
                TestArbitraries.token("redact-cookie-"),
                TestArbitraries.token("keepParam")
        ).as(RequestSeed::new).flatMap(seed ->
                Combinators.combine(
                        TestArbitraries.token("keep-value-"),
                        TestArbitraries.token("removeParam"),
                        TestArbitraries.token("remove-value-"),
                        TestArbitraries.token("redactParam"),
                        TestArbitraries.token("redact-value-"),
                        TestArbitraries.token("MASK-")
                ).as((keepParamValue, removeParamName, removeParamValue, redactParamName, redactParamValue, replacement) ->
                        new RequestScenario(
                                seed.parameterType(),
                                seed.keepHeaderValue(),
                                seed.removeHeaderValue(),
                                seed.redactHeaderValue(),
                                seed.keepCookieValue(),
                                seed.removeCookieValue(),
                                seed.redactCookieValue(),
                                seed.keepParamName(),
                                keepParamValue,
                                removeParamName,
                                removeParamValue,
                                redactParamName,
                                redactParamValue,
                                replacement
                        ))
        );
    }

    private static Preset presetFor(RequestScenario scenario) {
        return new Preset(
                "preset-id",
                "preset",
                List.of(Pattern.quote(REMOVE_HEADER_NAME)),
                List.of(Pattern.quote(REMOVE_COOKIE_NAME)),
                List.of(Pattern.quote(scenario.removeParamName())),
                List.of(
                        new RedactionRule(RedactionRule.Type.HEADER, Pattern.quote(REDACT_HEADER_NAME)),
                        new RedactionRule(RedactionRule.Type.COOKIE, Pattern.quote(REDACT_COOKIE_NAME)),
                        new RedactionRule(RedactionRule.Type.PARAM, Pattern.quote(scenario.redactParamName()))
                ),
                scenario.replacement(),
                "{{request}}\n{{response}}",
                true
        );
    }

    private static HttpRequest buildRequest(RequestScenario scenario) {
        List<HttpFixtures.HeaderValue> headers = List.of(
                HttpFixtures.header(REMOVE_HEADER_NAME, scenario.removeHeaderValue()),
                HttpFixtures.header(KEEP_HEADER_NAME, scenario.keepHeaderValue()),
                HttpFixtures.header(REDACT_HEADER_NAME, scenario.redactHeaderValue()),
                HttpFixtures.header(
                        "Cookie",
                        REMOVE_COOKIE_NAME + "=" + scenario.removeCookieValue()
                                + "; " + KEEP_COOKIE_NAME + "=" + scenario.keepCookieValue()
                                + "; " + REDACT_COOKIE_NAME + "=" + scenario.redactCookieValue()
                )
        );
        List<HttpFixtures.ParameterValue> parameters = List.of(
                HttpFixtures.parameter(scenario.keepParamName(), scenario.keepParamValue(), scenario.parameterType()),
                HttpFixtures.parameter(scenario.removeParamName(), scenario.removeParamValue(), scenario.parameterType()),
                HttpFixtures.parameter(scenario.redactParamName(), scenario.redactParamValue(), scenario.parameterType())
        );
        return HttpFixtures.request("POST", "/demo", headers, parameters);
    }
}
