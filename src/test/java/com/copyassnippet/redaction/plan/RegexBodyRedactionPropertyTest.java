package com.copyassnippet.redaction.plan;

import com.copyassnippet.redaction.processor.RegexBodyRedactionProcessor;
import com.copyassnippet.testutil.TestArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegexBodyRedactionPropertyTest {
    private final RegexBodyRedactionProcessor processor = new RegexBodyRedactionProcessor();

    private record UngroupedScenario(String before, String target, String after, String replacement) {
    }

    private record GroupedScenario(
            String before,
            String left,
            String firstSecret,
            String middle,
            String secondSecret,
            String right,
            String after,
            String replacement
    ) {
    }

    @Property(tries = 50)
    void noGroupRegexesOnlyReplaceTheMatchedSpan(@ForAll("ungroupedScenarios") UngroupedScenario scenario) {
        String text = scenario.before() + scenario.target() + scenario.after();
        RedactionPlan plan = new RedactionPlan(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(Pattern.compile(Pattern.quote(scenario.target()))),
                scenario.replacement(),
                "{{request}}"
        );

        String result = processor.redact(text, plan);
        assertEquals(scenario.before() + scenario.replacement() + scenario.after(), result);
    }

    @Property(tries = 50)
    void groupedRegexesPreserveNonMatchingText(@ForAll("groupedScenarios") GroupedScenario scenario) {
        String text = scenario.before()
                + scenario.left()
                + scenario.firstSecret()
                + scenario.middle()
                + scenario.secondSecret()
                + scenario.right()
                + scenario.after();

        Pattern pattern = Pattern.compile(
                Pattern.quote(scenario.left())
                        + "(" + Pattern.quote(scenario.firstSecret()) + ")"
                        + Pattern.quote(scenario.middle())
                        + "(" + Pattern.quote(scenario.secondSecret()) + ")"
                        + Pattern.quote(scenario.right())
        );

        RedactionPlan plan = new RedactionPlan(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(pattern),
                scenario.replacement(),
                "{{request}}"
        );

        String result = processor.redact(text, plan);
        assertEquals(
                scenario.before()
                        + scenario.left()
                        + scenario.replacement()
                        + scenario.middle()
                        + scenario.replacement()
                        + scenario.right()
                        + scenario.after(),
                result
        );
    }

    @Provide
    Arbitrary<UngroupedScenario> ungroupedScenarios() {
        return Combinators.combine(
                TestArbitraries.token("before-"),
                TestArbitraries.token("target-"),
                TestArbitraries.token("after-"),
                TestArbitraries.token("MASK-")
        ).as(UngroupedScenario::new);
    }

    @Provide
    Arbitrary<GroupedScenario> groupedScenarios() {
        return Combinators.combine(
                TestArbitraries.token("before-"),
                TestArbitraries.token("left-"),
                TestArbitraries.token("secret-a-"),
                TestArbitraries.token("mid-"),
                TestArbitraries.token("secret-b-"),
                TestArbitraries.token("right-"),
                TestArbitraries.token("after-"),
                TestArbitraries.token("MASK-")
        ).as(GroupedScenario::new);
    }
}
