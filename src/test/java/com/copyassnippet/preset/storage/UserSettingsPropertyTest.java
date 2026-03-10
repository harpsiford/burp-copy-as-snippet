package com.copyassnippet.preset.storage;

import burp.api.montoya.persistence.Preferences;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.testutil.PresetArbitraries;
import com.copyassnippet.testutil.PreferencesFixture;
import com.copyassnippet.testutil.TestArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;

import static com.copyassnippet.testutil.TestAssertions.assertPresetSemantics;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSettingsPropertyTest {
    private record PersistedState(
            List<Preset> presets,
            List<String> order,
            boolean hotkeyEnabled,
            String hotkeyString,
            boolean builtInDefaultRemoved
    ) {
    }

    @Property(tries = 75)
    void v1RoundTripsSettingsWithoutSemanticLoss(@ForAll("persistedStates") PersistedState state) {
        Preferences preferences = new PreferencesFixture().preferences();

        UserSettingsV1.writePresets(preferences, state.presets());
        UserSettingsV1.writePresetOrder(preferences, state.order());
        UserSettingsV1.writeHotkeyEnabled(preferences, state.hotkeyEnabled());
        UserSettingsV1.writeHotkeyString(preferences, state.hotkeyString());
        UserSettingsV1.writeBuiltInDefaultRemoved(preferences, state.builtInDefaultRemoved());

        assertPresetListsEqual(state.presets(), UserSettings.readPresets(preferences));
        assertEquals(state.order(), UserSettings.readPresetOrder(preferences));
        assertEquals(state.hotkeyEnabled(), UserSettings.isHotkeyEnabled(preferences));
        assertEquals(state.hotkeyString(), UserSettings.readStoredHotkeyString(preferences));
        assertEquals(state.builtInDefaultRemoved(), UserSettings.isBuiltInDefaultRemoved(preferences));
    }

    @Property(tries = 75)
    void migratingLegacySettingsPreservesSemanticState(@ForAll("persistedStates") PersistedState state) {
        Preferences preferences = new PreferencesFixture().preferences();

        UserSettingsV0.writePresets(preferences, state.presets());
        UserSettingsV0.writePresetOrder(preferences, state.order());
        UserSettingsV0.writeHotkeyEnabled(preferences, state.hotkeyEnabled());
        UserSettingsV0.writeHotkeyString(preferences, state.hotkeyString());
        UserSettingsV0.writeBuiltInDefaultRemoved(preferences, state.builtInDefaultRemoved());

        UserSettings.migrate(preferences);

        assertPresetListsEqual(state.presets(), UserSettings.readPresets(preferences));
        assertEquals(state.order(), UserSettings.readPresetOrder(preferences));
        assertEquals(state.hotkeyEnabled(), UserSettings.isHotkeyEnabled(preferences));
        assertEquals(state.hotkeyString(), UserSettings.readHotkeyString(preferences, PresetStore.DEFAULT_HOTKEY));
        assertEquals(state.builtInDefaultRemoved(), UserSettings.isBuiltInDefaultRemoved(preferences));
    }

    @Property(tries = 50)
    void migratingLegacySettingsIsIdempotent(@ForAll("persistedStates") PersistedState state) {
        Preferences preferences = new PreferencesFixture().preferences();

        UserSettingsV0.writePresets(preferences, state.presets());
        UserSettingsV0.writePresetOrder(preferences, state.order());
        UserSettingsV0.writeHotkeyEnabled(preferences, state.hotkeyEnabled());
        UserSettingsV0.writeHotkeyString(preferences, state.hotkeyString());
        UserSettingsV0.writeBuiltInDefaultRemoved(preferences, state.builtInDefaultRemoved());

        UserSettings.migrate(preferences);
        PersistedState afterFirstMigration = snapshot(preferences);

        UserSettings.migrate(preferences);
        PersistedState afterSecondMigration = snapshot(preferences);

        assertStatesEqual(afterFirstMigration, afterSecondMigration);
    }

    @Provide
    Arbitrary<PersistedState> persistedStates() {
        return PresetArbitraries.uniquePresets(4).flatMap(presets ->
                Combinators.combine(
                        orderFor(presets),
                        net.jqwik.api.Arbitraries.of(true, false),
                        net.jqwik.api.Arbitraries.oneOf(
                                net.jqwik.api.Arbitraries.just("Ctrl+Shift+C"),
                                TestArbitraries.multilineFreeText("Ctrl+")
                        ),
                        net.jqwik.api.Arbitraries.of(true, false)
                ).as((order, hotkeyEnabled, hotkeyString, builtInDefaultRemoved) ->
                        new PersistedState(presets, order, hotkeyEnabled, hotkeyString, builtInDefaultRemoved)
                )
        );
    }

    private static Arbitrary<List<String>> orderFor(List<Preset> presets) {
        List<String> candidateIds = new ArrayList<>();
        for (Preset preset : presets) {
            candidateIds.add(preset.getId());
        }
        candidateIds.add("unknown-order-a");
        candidateIds.add("unknown-order-b");
        return net.jqwik.api.Arbitraries.of(candidateIds).list().ofMinSize(0).ofMaxSize(candidateIds.size() + 1);
    }

    private static PersistedState snapshot(Preferences preferences) {
        return new PersistedState(
                UserSettings.readPresets(preferences),
                UserSettings.readPresetOrder(preferences),
                UserSettings.isHotkeyEnabled(preferences),
                UserSettings.readHotkeyString(preferences, PresetStore.DEFAULT_HOTKEY),
                UserSettings.isBuiltInDefaultRemoved(preferences)
        );
    }

    private static void assertStatesEqual(PersistedState expected, PersistedState actual) {
        assertPresetListsEqual(expected.presets(), actual.presets());
        assertEquals(expected.order(), actual.order());
        assertEquals(expected.hotkeyEnabled(), actual.hotkeyEnabled());
        assertEquals(expected.hotkeyString(), actual.hotkeyString());
        assertEquals(expected.builtInDefaultRemoved(), actual.builtInDefaultRemoved());
    }

    private static void assertPresetListsEqual(List<Preset> expected, List<Preset> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertPresetSemantics(expected.get(index), actual.get(index));
        }
    }
}
