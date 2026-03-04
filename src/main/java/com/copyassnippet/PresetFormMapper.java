package com.copyassnippet;

import java.util.List;

final class PresetFormMapper {

    private PresetFormMapper() {
    }

    static PresetFormData forNewPreset() {
        return fromPreset(Preset.createDefault(), PresetScope.USER).withName("");
    }

    static PresetFormData empty() {
        return new PresetFormData(
                "",
                PresetScope.USER,
                List.of(),
                List.of(),
                List.of(),
                Preset.DEFAULT_REPLACEMENT,
                List.of(),
                ""
        );
    }

    static PresetFormData fromPreset(Preset preset, PresetScope scope) {
        return new PresetFormData(
                preset.getName(),
                scope,
                preset.getHeaderRegexes(),
                preset.getCookieRegexes(),
                preset.getParamRegexes(),
                preset.getReplacementString(),
                preset.getRedactionRules(),
                preset.getTemplate()
        );
    }

    static String firstValidationError(PresetFormData formData) {
        if (formData.getName().trim().isEmpty()) {
            return "Name cannot be empty.";
        }

        return RegexValidation.firstValidationError(
                formData.getHeaderRegexes(),
                formData.getCookieRegexes(),
                formData.getParamRegexes(),
                formData.getRedactionRules()
        );
    }

    static Preset toPreset(PresetFormData formData, boolean enabled) {
        return new Preset(
                formData.getName().trim(),
                formData.getHeaderRegexes(),
                formData.getCookieRegexes(),
                formData.getParamRegexes(),
                formData.getRedactionRules(),
                formData.getReplacementString(),
                formData.getTemplate(),
                enabled
        );
    }
}
