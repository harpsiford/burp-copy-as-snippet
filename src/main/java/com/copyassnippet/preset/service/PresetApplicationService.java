package com.copyassnippet.preset.service;

import com.copyassnippet.preset.form.PresetFormData;
import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.storage.UserSettings;
import com.copyassnippet.preset.storage.PresetStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PresetApplicationService {
    private static final class ReplacementSlot {
        private final int userPresetIndex;
        private final int orderIndex;

        private ReplacementSlot(int userPresetIndex, int orderIndex) {
            this.userPresetIndex = userPresetIndex;
            this.orderIndex = orderIndex;
        }
    }

    public enum ImportAction {
        ADD("Add"),
        REPLACE("Replace"),
        KEEP_BOTH("Store both");

        private final String label;

        ImportAction(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class ImportPlanRow {
        private final String sourceName;
        private final Preset preset;
        private final String conflictingPresetId;
        private final String conflictingPresetName;
        private ImportAction action;

        public ImportPlanRow(
                String sourceName,
                Preset preset,
                String conflictingPresetId,
                String conflictingPresetName,
                ImportAction action
        ) {
            this.sourceName = sourceName;
            this.preset = preset;
            this.conflictingPresetId = conflictingPresetId;
            this.conflictingPresetName = conflictingPresetName;
            this.action = action;
        }

        public String getSourceName() {
            return sourceName;
        }

        public Preset getPreset() {
            return preset;
        }

        public String getConflictingPresetId() {
            return conflictingPresetId;
        }

        public String getConflictingPresetName() {
            return conflictingPresetName;
        }

        public boolean hasNameConflict() {
            return conflictingPresetId != null;
        }

        public ImportAction getAction() {
            return action;
        }

        public void setAction(ImportAction action) {
            this.action = action;
        }
    }

    private final PresetStore presetStore;

    public PresetApplicationService(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    public List<PresetResolver.ResolvedPreset> listResolvedPresets() {
        return presetStore.getResolvedPresetEntries();
    }

    public void savePreset(Preset preset) {
        if (Preset.BUILT_IN_ID.equals(preset.getId())) {
            presetStore.setBuiltInDefaultRemoved(false);
        }
        List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
        userList.removeIf(existing -> existing.getId().equals(preset.getId()));
        userList.add(preset);
        presetStore.setUserPresets(userList);
    }

    public void removePreset(String presetId) {
        if (Preset.BUILT_IN_ID.equals(presetId)) {
            presetStore.setBuiltInDefaultRemoved(true);
        }
        List<Preset> userList = new ArrayList<>(presetStore.getUserPresets());
        userList.removeIf(p -> p.getId().equals(presetId));
        presetStore.setUserPresets(userList);
    }

    public void clearAllSettings() {
        presetStore.resetAllSettings();
    }

    public void setPresetOrder(List<String> orderIds) {
        presetStore.setPresetOrder(orderIds);
    }

    public boolean isPresetNameTaken(String presetName, String excludedPresetId) {
        return presetStore.isPresetNameTaken(presetName, excludedPresetId);
    }

    public void persistEnabledToggle(PresetResolver.ResolvedPreset row, boolean enabled) {
        Preset preset = row.getPreset();
        preset.setEnabled(enabled);

        if (row.getScope().isBuiltIn()) {
            Preset userPreset = new Preset(
                    preset.getName(),
                    preset.getHeaderRegexes(),
                    preset.getCookieRegexes(),
                    preset.getParamRegexes(),
                    preset.getRedactionRules(),
                    preset.getReplacementString(),
                    preset.getTemplate(),
                    preset.isEnabled()
            );
            savePreset(userPreset);
            return;
        }

        savePreset(preset);
    }

    public void exportPreset(Preset preset, Path destination) {
        try {
            Files.writeString(destination, UserSettings.writePresetFile(preset), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to export preset to \"" + destination.getFileName() + "\".", exception);
        }
    }

    public List<ImportPlanRow> loadImportPlan(List<Path> files) {
        Map<String, PresetResolver.ResolvedPreset> existingByName = new LinkedHashMap<>();
        for (PresetResolver.ResolvedPreset resolvedPreset : listResolvedPresets()) {
            existingByName.put(resolvedPreset.getPreset().getName(), resolvedPreset);
        }

        List<ImportPlanRow> rows = new ArrayList<>();
        for (Path file : files) {
            Preset preset = readImportedPreset(file);
            PresetResolver.ResolvedPreset existing = existingByName.get(preset.getName());
            rows.add(new ImportPlanRow(
                    file.getFileName().toString(),
                    preset,
                    existing != null ? existing.getPreset().getId() : null,
                    existing != null ? existing.getPreset().getName() : null,
                    existing != null ? ImportAction.KEEP_BOTH : ImportAction.ADD
            ));
        }
        return rows;
    }

    public boolean hasImportConflicts(List<ImportPlanRow> rows) {
        for (ImportPlanRow row : rows) {
            if (row.hasNameConflict()) {
                return true;
            }
        }
        return false;
    }

    public List<String> importPresets(List<ImportPlanRow> rows) {
        List<Preset> workingUserPresets = new ArrayList<>(presetStore.getUserPresets());
        List<String> workingOrder = new ArrayList<>(presetStore.getPresetOrder());
        Map<String, PresetResolver.ResolvedPreset> existingByName = new LinkedHashMap<>();
        Map<String, String> occupantByName = new LinkedHashMap<>();
        for (PresetResolver.ResolvedPreset resolvedPreset : listResolvedPresets()) {
            existingByName.put(resolvedPreset.getPreset().getName(), resolvedPreset);
            occupantByName.put(resolvedPreset.getPreset().getName(), resolvedPreset.getPreset().getId());
        }

        List<String> importedPresetIds = new ArrayList<>();
        for (ImportPlanRow row : rows) {
            Preset importedPreset = row.getPreset();
            String originalName = importedPreset.getName();
            ReplacementSlot replacementSlot = null;

            if (row.hasNameConflict() && row.getAction() == ImportAction.REPLACE) {
                PresetResolver.ResolvedPreset existing = existingByName.get(originalName);
                if (existing != null && Objects.equals(existing.getPreset().getId(), row.getConflictingPresetId())) {
                    replacementSlot = removeExistingPreset(existing, workingUserPresets, workingOrder);
                    if (Objects.equals(occupantByName.get(originalName), existing.getPreset().getId())) {
                        occupantByName.remove(originalName);
                    }
                }
            }

            String finalName = nextAvailableName(originalName, occupantByName);
            Preset finalPreset = copyPreset(importedPreset, finalName);
            if (replacementSlot != null
                    && replacementSlot.userPresetIndex >= 0
                    && replacementSlot.userPresetIndex <= workingUserPresets.size()) {
                workingUserPresets.add(replacementSlot.userPresetIndex, finalPreset);
            } else {
                workingUserPresets.add(finalPreset);
            }
            if (replacementSlot != null
                    && replacementSlot.orderIndex >= 0
                    && replacementSlot.orderIndex <= workingOrder.size()) {
                workingOrder.add(replacementSlot.orderIndex, finalPreset.getId());
            } else {
                workingOrder.add(finalPreset.getId());
            }
            occupantByName.put(finalName, finalPreset.getId());
            importedPresetIds.add(finalPreset.getId());
        }

        presetStore.setUserPresets(workingUserPresets);
        presetStore.setPresetOrder(workingOrder);
        return importedPresetIds;
    }

    private Preset readImportedPreset(Path file) {
        try {
            Preset importedPreset = UserSettings.readPresetFile(Files.readString(file, StandardCharsets.UTF_8));
            String validationError = PresetFormData.fromPreset(importedPreset).withoutPresetId().firstValidationError();
            if (validationError != null) {
                throw new IllegalStateException(validationError);
            }
            return importedPreset;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read \"" + file.getFileName() + "\".", exception);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Unable to load \"" + file.getFileName() + "\": " + exception.getMessage(), exception);
        }
    }

    private static ReplacementSlot removeExistingPreset(
            PresetResolver.ResolvedPreset existing,
            List<Preset> workingUserPresets,
            List<String> workingOrder
    ) {
        int orderInsertIndex = indexOfPresetId(workingOrder, existing.getPreset().getId());
        if (existing.getScope().isBuiltIn()) {
            if (orderInsertIndex >= 0) {
                workingOrder.remove(orderInsertIndex);
            }
            return new ReplacementSlot(-1, orderInsertIndex);
        }

        List<String> removedIds = new ArrayList<>();
        int userInsertIndex = -1;
        for (Preset preset : workingUserPresets) {
            if (preset.getName().equals(existing.getPreset().getName())) {
                if (userInsertIndex < 0) {
                    userInsertIndex = workingUserPresets.indexOf(preset);
                }
                removedIds.add(preset.getId());
            }
        }
        workingUserPresets.removeIf(preset -> preset.getName().equals(existing.getPreset().getName()));
        if (orderInsertIndex < 0 && !removedIds.isEmpty()) {
            orderInsertIndex = indexOfPresetId(workingOrder, removedIds.get(0));
        }
        workingOrder.removeIf(removedIds::contains);
        return new ReplacementSlot(userInsertIndex, orderInsertIndex);
    }

    private static int indexOfPresetId(List<String> order, String presetId) {
        for (int index = 0; index < order.size(); index++) {
            if (Objects.equals(order.get(index), presetId)) {
                return index;
            }
        }
        return -1;
    }

    private static String nextAvailableName(String baseName, Map<String, String> occupantByName) {
        if (!occupantByName.containsKey(baseName)) {
            return baseName;
        }

        int suffix = 2;
        while (occupantByName.containsKey(baseName + " " + suffix)) {
            suffix++;
        }
        return baseName + " " + suffix;
    }

    private static Preset copyPreset(Preset preset, String name) {
        return new Preset(
                preset.getId(),
                name,
                preset.getHeaderRegexes(),
                preset.getCookieRegexes(),
                preset.getParamRegexes(),
                preset.getRedactionRules(),
                preset.getReplacementString(),
                preset.getTemplate(),
                preset.isEnabled()
        );
    }
}
