package com.copyassnippet.testutil;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Combinators;

import java.util.ArrayList;
import java.util.List;

public final class PresetArbitraries {
    private record PresetSeed(
            String id,
            String name,
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            List<RedactionRule> redactionRules,
            String replacement,
            String template
    ) {
    }

    private record PresetListSeed(
            List<String> ids,
            List<String> names,
            List<List<String>> headers,
            List<List<String>> cookies,
            List<List<String>> params,
            List<List<RedactionRule>> rules,
            List<String> replacements,
            List<String> templates
    ) {
    }

    private PresetArbitraries() {
    }

    public static Arbitrary<RedactionRule> redactionRule() {
        return Combinators.combine(
                Arbitraries.of(RedactionRule.Type.values()),
                TestArbitraries.exactRegex()
        ).as(RedactionRule::new);
    }

    public static Arbitrary<Preset> preset() {
        return Combinators.combine(
                TestArbitraries.token("preset-id-"),
                TestArbitraries.multilineFreeText("Preset "),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3),
                redactionRule().list().ofMinSize(0).ofMaxSize(4),
                TestArbitraries.multilineFreeText("MASK-"),
                Arbitraries.oneOf(
                        Arbitraries.just("{{request}}\n{{response}}"),
                        TestArbitraries.multilineFreeText("template-")
                )
        ).as(PresetSeed::new).flatMap(seed ->
                Arbitraries.of(true, false).map(enabled -> new Preset(
                        seed.id(),
                        seed.name(),
                        seed.headerRegexes(),
                        seed.cookieRegexes(),
                        seed.paramRegexes(),
                        seed.redactionRules(),
                        seed.replacement(),
                        seed.template(),
                        enabled
                ))
        );
    }

    public static Arbitrary<List<Preset>> uniquePresets(int maxSize) {
        return Arbitraries.integers().between(0, maxSize).flatMap(size ->
                Combinators.combine(
                        TestArbitraries.token("preset-id-").list().uniqueElements().ofMinSize(size).ofMaxSize(size),
                        TestArbitraries.multilineFreeText("Preset ").list().uniqueElements().ofMinSize(size).ofMaxSize(size),
                        TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3).list().ofMinSize(size).ofMaxSize(size),
                        TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3).list().ofMinSize(size).ofMaxSize(size),
                        TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(3).list().ofMinSize(size).ofMaxSize(size),
                        redactionRule().list().ofMinSize(0).ofMaxSize(4).list().ofMinSize(size).ofMaxSize(size),
                        TestArbitraries.multilineFreeText("MASK-").list().ofMinSize(size).ofMaxSize(size),
                        Arbitraries.oneOf(
                                Arbitraries.just("{{request}}\n{{response}}"),
                                TestArbitraries.multilineFreeText("template-")
                        ).list().ofMinSize(size).ofMaxSize(size)
                ).as(PresetListSeed::new).flatMap(seed ->
                        Arbitraries.of(true, false).list().ofMinSize(size).ofMaxSize(size).map(enabledFlags -> {
                    List<Preset> presets = new ArrayList<>();
                    for (int index = 0; index < size; index++) {
                        presets.add(new Preset(
                                seed.ids().get(index),
                                seed.names().get(index),
                                seed.headers().get(index),
                                seed.cookies().get(index),
                                seed.params().get(index),
                                seed.rules().get(index),
                                seed.replacements().get(index),
                                seed.templates().get(index),
                                enabledFlags.get(index)
                        ));
                    }
                    return presets;
                }))
        );
    }
}
