package com.copyassnippet.preset.service;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.testutil.PresetArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetResolverPropertyTest {
    private final PresetResolver resolver = new PresetResolver();

    private record ResolverScenario(List<Preset> userPresets, List<String> order, boolean includeBuiltIn) {
    }

    @Property(tries = 75)
    void resolveNeverDropsUnorderedPresetsOrDuplicatesNames(@ForAll("resolverScenarios") ResolverScenario scenario) {
        List<PresetResolver.ResolvedPreset> resolved = resolver.resolve(
                scenario.userPresets(),
                scenario.order(),
                scenario.includeBuiltIn()
        );

        List<String> resolvedNames = resolved.stream().map(row -> row.getPreset().getName()).toList();
        assertEquals(new HashSet<>(resolvedNames).size(), resolvedNames.size());

        List<Preset> baseline = new ArrayList<>();
        if (scenario.includeBuiltIn()) {
            baseline.add(DefaultPresetFactory.createBuiltInPreset());
        }
        baseline.addAll(scenario.userPresets());

        Set<String> orderedIds = new HashSet<>(scenario.order());
        List<String> expectedUnorderedIds = baseline.stream()
                .map(Preset::getId)
                .filter(id -> !orderedIds.contains(id))
                .toList();
        List<String> actualUnorderedIds = resolved.stream()
                .map(row -> row.getPreset().getId())
                .filter(id -> !orderedIds.contains(id))
                .toList();
        assertEquals(expectedUnorderedIds, actualUnorderedIds);

        Set<String> baselineIds = baseline.stream().map(Preset::getId).collect(java.util.stream.Collectors.toSet());
        assertTrue(resolved.stream().map(row -> row.getPreset().getId()).allMatch(baselineIds::contains));
    }

    @Provide
    Arbitrary<ResolverScenario> resolverScenarios() {
        return PresetArbitraries.uniquePresets(5).flatMap(userPresets ->
                Combinators.combine(
                        orderFor(userPresets),
                        net.jqwik.api.Arbitraries.of(true, false)
                ).as((order, includeBuiltIn) -> new ResolverScenario(userPresets, order, includeBuiltIn))
        );
    }

    private static Arbitrary<List<String>> orderFor(List<Preset> presets) {
        List<String> candidateIds = new ArrayList<>();
        candidateIds.add("unknown-id");
        candidateIds.add("unknown-id-2");
        candidateIds.add(Preset.BUILT_IN_ID);
        for (Preset preset : presets) {
            candidateIds.add(preset.getId());
        }
        return net.jqwik.api.Arbitraries.of(candidateIds).list().ofMinSize(0).ofMaxSize(candidateIds.size() + 1);
    }
}
