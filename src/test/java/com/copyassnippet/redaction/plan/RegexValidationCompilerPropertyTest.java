package com.copyassnippet.redaction.plan;

import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.testutil.PresetArbitraries;
import com.copyassnippet.testutil.TestArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegexValidationCompilerPropertyTest {
    private final RedactionPlanCompiler compiler = new RedactionPlanCompiler();

    private record InvalidRegexSeed(
            String id,
            String name,
            List<String> headers,
            List<String> cookies,
            List<String> params,
            List<RedactionRule> rules,
            String invalidRegex,
            int targetBucket
    ) {
    }

    private record InvalidRegexCase(Preset preset) {
    }

    @Property(tries = 60)
    void validationAcceptanceMatchesCompiledPatternCounts(@ForAll("validPresets") Preset preset) {
        PresetFormData formData = PresetFormData.fromPreset(preset);
        assertNull(formData.firstValidationError());

        RedactionPlan plan = compiler.compile(preset);
        assertEquals(preset.getHeaderRegexes().size(), plan.headerPatterns().size());
        assertEquals(preset.getCookieRegexes().size(), plan.cookiePatterns().size());
        assertEquals(preset.getParamRegexes().size(), plan.paramPatterns().size());
        assertEquals(ruleCount(preset, RedactionRule.Type.HEADER), plan.redactHeaderPatterns().size());
        assertEquals(ruleCount(preset, RedactionRule.Type.COOKIE), plan.redactCookiePatterns().size());
        assertEquals(ruleCount(preset, RedactionRule.Type.PARAM), plan.redactParamPatterns().size());
        assertEquals(ruleCount(preset, RedactionRule.Type.REGEX), plan.redactRegexPatterns().size());
    }

    @Property(tries = 40)
    void invalidRegexesAreRejectedWithoutCrashingCompilation(@ForAll("invalidRegexCases") InvalidRegexCase invalidRegexCase) {
        Preset preset = invalidRegexCase.preset();

        String validationError = PresetFormData.fromPreset(preset).firstValidationError();
        assertNotNull(validationError);
        assertDoesNotThrow(() -> compiler.compile(preset));
    }

    @Provide
    Arbitrary<Preset> validPresets() {
        return PresetArbitraries.preset();
    }

    @Provide
    Arbitrary<InvalidRegexCase> invalidRegexCases() {
        Arbitrary<String> invalidRegex = net.jqwik.api.Arbitraries.of("(", "[", "[a-", "a{", "foo(bar");
        Arbitrary<RedactionRule.Type> ruleType = net.jqwik.api.Arbitraries.of(RedactionRule.Type.values());

        return Combinators.combine(
                TestArbitraries.token("id-"),
                TestArbitraries.multilineFreeText("Preset "),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2),
                PresetArbitraries.redactionRule().list().ofMinSize(0).ofMaxSize(2),
                invalidRegex,
                net.jqwik.api.Arbitraries.integers().between(0, 3)
        ).as(InvalidRegexSeed::new).flatMap(seed -> ruleType.map(targetRuleType -> {
            List<String> headerPatterns = new ArrayList<>(seed.headers());
            List<String> cookiePatterns = new ArrayList<>(seed.cookies());
            List<String> paramPatterns = new ArrayList<>(seed.params());
            List<RedactionRule> redactionRules = new ArrayList<>(seed.rules());

            switch (seed.targetBucket()) {
                case 0:
                    headerPatterns.add(seed.invalidRegex());
                    break;
                case 1:
                    cookiePatterns.add(seed.invalidRegex());
                    break;
                case 2:
                    paramPatterns.add(seed.invalidRegex());
                    break;
                default:
                    redactionRules.add(new RedactionRule(targetRuleType, seed.invalidRegex()));
                    break;
            }

            return new InvalidRegexCase(new Preset(
                    seed.id(),
                    seed.name(),
                    headerPatterns,
                    cookiePatterns,
                    paramPatterns,
                    redactionRules,
                    "MASK",
                    "{{request}}\n{{response}}",
                    true
            ));
        }));
    }

    private static int ruleCount(Preset preset, RedactionRule.Type type) {
        int count = 0;
        for (RedactionRule rule : preset.getRedactionRules()) {
            if (rule.getType() == type) {
                count++;
            }
        }
        return count;
    }
}
