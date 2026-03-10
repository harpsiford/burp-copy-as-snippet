package com.copyassnippet.preset.storage;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.testutil.MontoyaFixtures;
import com.copyassnippet.testutil.PresetArbitraries;
import com.copyassnippet.testutil.PreferencesFixture;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.ArrayList;
import java.util.List;

import static com.copyassnippet.testutil.TestAssertions.assertPresetSemantics;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PresetStorePropertyTest {
    @Property(tries = 50)
    void returnedPresetsAreDefensiveCopies(@ForAll("nonEmptyPresets") List<Preset> presets) {
        PreferencesFixture fixture = new PreferencesFixture();
        PresetStore store = new PresetStore(MontoyaFixtures.montoyaApi(fixture.preferences()));
        store.setBuiltInDefaultRemoved(true);
        store.setUserPresets(presets);
        store.setPresetOrder(presets.stream().map(Preset::getId).toList());

        List<Preset> userPresets = store.getUserPresets();
        mutate(userPresets);
        assertPresetListsEqual(presets, store.getUserPresets());

        List<Preset> resolvedPresets = store.getResolvedPresets();
        mutate(resolvedPresets);
        assertPresetListsEqual(presets, store.getResolvedPresets());
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<List<Preset>> nonEmptyPresets() {
        return PresetArbitraries.uniquePresets(4).filter(presets -> !presets.isEmpty());
    }

    private static void mutate(List<Preset> presets) {
        presets.add(new Preset(
                "extra-id",
                "extra",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "MASK",
                "{{request}}",
                true
        ));

        Preset first = presets.get(0);
        first.setName(first.getName() + "-changed");
        first.getHeaderRegexes().add("new-header");
        first.getCookieRegexes().add("new-cookie");
        first.getParamRegexes().add("new-param");
        List<RedactionRule> rules = new ArrayList<>(first.getRedactionRules());
        rules.add(new RedactionRule(RedactionRule.Type.HEADER, "Authorization"));
        first.setRedactionRules(rules);
        first.setReplacementString("CHANGED");
        first.setTemplate("changed");
        first.setEnabled(!first.isEnabled());
    }

    private static void assertPresetListsEqual(List<Preset> expected, List<Preset> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertPresetSemantics(expected.get(index), actual.get(index));
        }
    }
}
