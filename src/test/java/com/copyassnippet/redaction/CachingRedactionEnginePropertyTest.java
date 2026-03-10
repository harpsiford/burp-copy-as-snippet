package com.copyassnippet.redaction;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.testutil.HttpFixtures;
import com.copyassnippet.testutil.TestArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingRedactionEnginePropertyTest {
    private static final String HEADER_NAME = "Authorization";
    private static final String COOKIE_NAME = "sessionid";

    private record EngineScenario(
            String headerSecret,
            String cookieSecret,
            String bodySecret,
            String firstReplacement,
            String secondReplacement
    ) {
    }

    @Property(tries = 40)
    void cachedPlansRefreshWhenPresetContentsChange(@ForAll("engineScenarios") EngineScenario scenario) {
        CachingRedactionEngine engine = new CachingRedactionEngine();
        HttpRequestResponse requestResponse = messagePair(scenario);

        Preset firstPreset = preset("shared-id", scenario.firstReplacement(), scenario.bodySecret());
        Preset secondPreset = preset("shared-id", scenario.secondReplacement(), scenario.bodySecret());

        String firstResult = engine.format(firstPreset, requestResponse);
        String secondResult = engine.format(secondPreset, requestResponse);

        assertTrue(firstResult.contains(scenario.firstReplacement()));
        assertTrue(secondResult.contains(scenario.secondReplacement()));
        assertFalse(secondResult.contains(scenario.firstReplacement()));
        assertFalse(secondResult.contains(scenario.headerSecret()));
        assertFalse(secondResult.contains(scenario.cookieSecret()));
        assertFalse(secondResult.contains(scenario.bodySecret()));
    }

    @Provide
    Arbitrary<EngineScenario> engineScenarios() {
        return Combinators.combine(
                TestArbitraries.token("header-secret-"),
                TestArbitraries.token("cookie-secret-"),
                TestArbitraries.token("body-secret-"),
                TestArbitraries.token("MASK-A-"),
                TestArbitraries.token("MASK-B-")
        ).as(EngineScenario::new).filter(scenario -> !scenario.firstReplacement().equals(scenario.secondReplacement()));
    }

    private static Preset preset(String id, String replacement, String bodySecret) {
        return new Preset(
                id,
                "preset",
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new RedactionRule(RedactionRule.Type.HEADER, Pattern.quote(HEADER_NAME)),
                        new RedactionRule(RedactionRule.Type.COOKIE, Pattern.quote(COOKIE_NAME)),
                        new RedactionRule(RedactionRule.Type.REGEX, ".*(" + Pattern.quote(bodySecret) + ").*")
                ),
                replacement,
                "{{request}}\n{{response}}",
                true
        );
    }

    private static HttpRequestResponse messagePair(EngineScenario scenario) {
        HttpRequest request = HttpFixtures.request(
                "POST",
                "/demo",
                List.of(
                        HttpFixtures.header(HEADER_NAME, scenario.headerSecret()),
                        HttpFixtures.header("Cookie", COOKIE_NAME + "=" + scenario.cookieSecret())
                ),
                List.of(HttpFixtures.parameter("bodySecret", scenario.bodySecret(), burp.api.montoya.http.message.params.HttpParameterType.BODY))
        );
        HttpResponse response = HttpFixtures.response(
                (short) 200,
                List.of(HttpFixtures.header("Set-Cookie", COOKIE_NAME + "=" + scenario.cookieSecret() + "; Path=/")),
                "bodySecret=" + scenario.bodySecret()
        );
        return HttpFixtures.requestResponse(request, response);
    }
}
