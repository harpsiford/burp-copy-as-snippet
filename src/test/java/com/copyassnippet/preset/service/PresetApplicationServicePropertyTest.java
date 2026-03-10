package com.copyassnippet.preset.service;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.storage.PresetStore;
import com.copyassnippet.testutil.MontoyaFixtures;
import com.copyassnippet.testutil.PresetArbitraries;
import com.copyassnippet.testutil.PreferencesFixture;
import com.copyassnippet.testutil.TestArbitraries;
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

class PresetApplicationServicePropertyTest {
    private record ImportScenario(
            List<Preset> existingPresets,
            List<String> existingOrder,
            List<PresetApplicationService.ImportPlanRow> rows,
            List<String> replacedIds
    ) {
    }

    @Property(tries = 60)
    void importMaintainsAConsistentStore(@ForAll("importScenarios") ImportScenario scenario) {
        PreferencesFixture fixture = new PreferencesFixture();
        PresetStore store = new PresetStore(MontoyaFixtures.montoyaApi(fixture.preferences()));
        store.setBuiltInDefaultRemoved(true);
        store.setUserPresets(scenario.existingPresets());
        store.setPresetOrder(scenario.existingOrder());

        PresetApplicationService service = new PresetApplicationService(store);
        List<String> importedIds = service.importPresets(copyRows(scenario.rows()));

        List<Preset> finalUserPresets = store.getUserPresets();
        List<String> finalOrder = store.getPresetOrder();
        Set<String> finalIds = finalUserPresets.stream().map(Preset::getId).collect(java.util.stream.Collectors.toSet());
        List<String> finalNames = finalUserPresets.stream().map(Preset::getName).toList();

        assertEquals(new HashSet<>(finalNames).size(), finalNames.size());
        assertTrue(finalOrder.stream().allMatch(finalIds::contains));
        assertEquals(new HashSet<>(finalOrder).size(), finalOrder.size());

        for (String importedId : importedIds) {
            assertTrue(finalUserPresets.stream().filter(preset -> preset.getId().equals(importedId)).count() <= 1L);
            assertTrue(finalOrder.stream().filter(importedId::equals).count() <= 1L);
        }

        for (String replacedId : scenario.replacedIds()) {
            assertTrue(finalUserPresets.stream().noneMatch(preset -> preset.getId().equals(replacedId)));
            assertTrue(finalOrder.stream().noneMatch(replacedId::equals));
        }
    }

    @Provide
    Arbitrary<ImportScenario> importScenarios() {
        return PresetArbitraries.uniquePresets(4).flatMap(existingPresets -> {
            List<String> existingNames = existingPresets.stream().map(Preset::getName).toList();
            Arbitrary<List<String>> existingOrder = orderFor(existingPresets);
            Arbitrary<List<String>> importedNames = importedNames(existingNames);
            Arbitrary<List<Preset>> importedPresets = importedNames.flatMap(this::importedPresetsForNames);

            return Combinators.combine(existingOrder, importedPresets).as((order, imported) -> {
                List<PresetApplicationService.ImportPlanRow> rows = new ArrayList<>();
                List<String> replacedIds = new ArrayList<>();
                for (Preset preset : imported) {
                    Preset conflicting = existingPresets.stream()
                            .filter(existing -> existing.getName().equals(preset.getName()))
                            .findFirst()
                            .orElse(null);

                    PresetApplicationService.ImportAction action;
                    if (conflicting == null) {
                        action = PresetApplicationService.ImportAction.ADD;
                    } else if (Math.abs(preset.getId().hashCode()) % 2 == 0) {
                        action = PresetApplicationService.ImportAction.REPLACE;
                        replacedIds.add(conflicting.getId());
                    } else {
                        action = PresetApplicationService.ImportAction.KEEP_BOTH;
                    }

                    rows.add(new PresetApplicationService.ImportPlanRow(
                            preset.getName() + ".json",
                            preset,
                            conflicting != null ? conflicting.getId() : null,
                            conflicting != null ? conflicting.getName() : null,
                            action
                    ));
                }
                return new ImportScenario(existingPresets, order, rows, replacedIds);
            });
        });
    }

    private Arbitrary<List<String>> importedNames(List<String> existingNames) {
        Arbitrary<List<String>> newNames = TestArbitraries.multilineFreeText("Imported ")
                .list()
                .ofMinSize(1)
                .ofMaxSize(4);
        if (existingNames.isEmpty()) {
            return newNames;
        }

        Arbitrary<String> nameSource = net.jqwik.api.Arbitraries.oneOf(
                net.jqwik.api.Arbitraries.of(existingNames),
                TestArbitraries.multilineFreeText("Imported ")
        );
        return nameSource.list().ofMinSize(1).ofMaxSize(4);
    }

    private Arbitrary<List<Preset>> importedPresetsForNames(List<String> names) {
        return Combinators.combine(
                TestArbitraries.token("imported-id-").list().uniqueElements().ofMinSize(names.size()).ofMaxSize(names.size()),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2).list().ofMinSize(names.size()).ofMaxSize(names.size()),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2).list().ofMinSize(names.size()).ofMaxSize(names.size()),
                TestArbitraries.exactRegex().list().ofMinSize(0).ofMaxSize(2).list().ofMinSize(names.size()).ofMaxSize(names.size()),
                PresetArbitraries.redactionRule().list().ofMinSize(0).ofMaxSize(3).list().ofMinSize(names.size()).ofMaxSize(names.size()),
                TestArbitraries.multilineFreeText("MASK-").list().ofMinSize(names.size()).ofMaxSize(names.size()),
                net.jqwik.api.Arbitraries.just("{{request}}\n{{response}}").list().ofMinSize(names.size()).ofMaxSize(names.size()),
                net.jqwik.api.Arbitraries.of(true, false).list().ofMinSize(names.size()).ofMaxSize(names.size())
        ).as((ids, headers, cookies, params, rules, replacements, templates, enabledFlags) -> {
            List<Preset> presets = new ArrayList<>();
            for (int index = 0; index < names.size(); index++) {
                presets.add(new Preset(
                        ids.get(index),
                        names.get(index),
                        headers.get(index),
                        cookies.get(index),
                        params.get(index),
                        rules.get(index),
                        replacements.get(index),
                        templates.get(index),
                        enabledFlags.get(index)
                ));
            }
            return presets;
        });
    }

    private static Arbitrary<List<String>> orderFor(List<Preset> presets) {
        List<String> ids = presets.stream().map(Preset::getId).toList();
        if (ids.isEmpty()) {
            return net.jqwik.api.Arbitraries.just(List.of());
        }
        return net.jqwik.api.Arbitraries.of(ids).list().uniqueElements().ofMinSize(0).ofMaxSize(ids.size());
    }

    private static List<PresetApplicationService.ImportPlanRow> copyRows(List<PresetApplicationService.ImportPlanRow> rows) {
        List<PresetApplicationService.ImportPlanRow> copies = new ArrayList<>();
        for (PresetApplicationService.ImportPlanRow row : rows) {
            copies.add(new PresetApplicationService.ImportPlanRow(
                    row.getSourceName(),
                    row.getPreset(),
                    row.getConflictingPresetId(),
                    row.getConflictingPresetName(),
                    row.getAction()
            ));
        }
        return copies;
    }
}
