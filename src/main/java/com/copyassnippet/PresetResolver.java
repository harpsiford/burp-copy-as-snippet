package com.copyassnippet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PresetResolver {

    static class ResolvedPreset {
        private final Preset preset;
        private final PresetScope scope;

        ResolvedPreset(Preset preset, PresetScope scope) {
            this.preset = preset;
            this.scope = scope;
        }

        Preset getPreset() {
            return preset;
        }

        PresetScope getScope() {
            return scope;
        }
    }

    List<ResolvedPreset> resolve(List<Preset> userPresets, List<Preset> projectPresets, List<String> order) {
        Map<String, ResolvedPreset> merged = new LinkedHashMap<>();

        Preset builtIn = DefaultPresetFactory.createBuiltInPreset();
        merged.put(builtIn.getName(), new ResolvedPreset(builtIn, PresetScope.BUILT_IN));

        for (Preset preset : userPresets) {
            merged.put(preset.getName(), new ResolvedPreset(preset, PresetScope.USER));
        }

        for (Preset preset : projectPresets) {
            merged.put(preset.getName(), new ResolvedPreset(preset, PresetScope.PROJECT));
        }

        return applyOrder(new ArrayList<>(merged.values()), order);
    }

    List<Preset> resolvePresets(List<Preset> userPresets, List<Preset> projectPresets, List<String> order) {
        List<Preset> result = new ArrayList<>();
        for (ResolvedPreset resolvedPreset : resolve(userPresets, projectPresets, order)) {
            result.add(resolvedPreset.getPreset());
        }
        return result;
    }

    private static List<ResolvedPreset> applyOrder(List<ResolvedPreset> presets, List<String> order) {
        if (order == null || order.isEmpty()) {
            return presets;
        }

        Map<String, ResolvedPreset> remaining = new LinkedHashMap<>();
        for (ResolvedPreset preset : presets) {
            remaining.put(preset.getPreset().getId(), preset);
        }

        List<ResolvedPreset> ordered = new ArrayList<>();
        for (String presetId : order) {
            ResolvedPreset preset = remaining.remove(presetId);
            if (preset != null) {
                ordered.add(preset);
            }
        }
        ordered.addAll(remaining.values());
        return ordered;
    }
}
