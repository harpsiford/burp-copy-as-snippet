package com.copyassnippet.preset.service;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.PresetScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PresetResolver {

    public static class ResolvedPreset {
        private final Preset preset;
        private final PresetScope scope;

        ResolvedPreset(Preset preset, PresetScope scope) {
            this.preset = preset;
            this.scope = scope;
        }

        public Preset getPreset() {
            return preset;
        }

        public PresetScope getScope() {
            return scope;
        }
    }

    public List<ResolvedPreset> resolve(List<Preset> userPresets, List<String> order, boolean includeBuiltIn) {
        Map<String, ResolvedPreset> merged = new LinkedHashMap<>();

        if (includeBuiltIn) {
            Preset builtIn = DefaultPresetFactory.createBuiltInPreset();
            merged.put(builtIn.getName(), new ResolvedPreset(builtIn, PresetScope.BUILT_IN));
        }

        for (Preset preset : userPresets) {
            merged.put(preset.getName(), new ResolvedPreset(preset, PresetScope.USER));
        }

        return applyOrder(new ArrayList<>(merged.values()), order);
    }

    public List<Preset> resolvePresets(List<Preset> userPresets, List<String> order, boolean includeBuiltIn) {
        List<Preset> result = new ArrayList<>();
        for (ResolvedPreset resolvedPreset : resolve(userPresets, order, includeBuiltIn)) {
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
