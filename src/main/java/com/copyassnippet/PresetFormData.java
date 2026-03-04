package com.copyassnippet;

import java.util.ArrayList;
import java.util.List;

final class PresetFormData {
    private final String presetId;
    private final String name;
    private final PresetScope scope;
    private final List<String> headerRegexes;
    private final List<String> cookieRegexes;
    private final List<String> paramRegexes;
    private final String replacementString;
    private final List<RedactionRule> redactionRules;
    private final String template;

    PresetFormData(
            String presetId,
            String name,
            PresetScope scope,
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            String replacementString,
            List<RedactionRule> redactionRules,
            String template) {
        this.presetId = presetId;
        this.name = name != null ? name : "";
        this.scope = scope != null ? scope : PresetScope.USER;
        this.headerRegexes = new ArrayList<>(headerRegexes);
        this.cookieRegexes = new ArrayList<>(cookieRegexes);
        this.paramRegexes = new ArrayList<>(paramRegexes);
        this.replacementString = replacementString != null ? replacementString : DefaultPresetFactory.DEFAULT_REPLACEMENT;
        this.redactionRules = new ArrayList<>(redactionRules);
        this.template = template != null ? template : "";
    }

    String getPresetId() {
        return presetId;
    }

    String getName() {
        return name;
    }

    PresetScope getScope() {
        return scope;
    }

    List<String> getHeaderRegexes() {
        return new ArrayList<>(headerRegexes);
    }

    List<String> getCookieRegexes() {
        return new ArrayList<>(cookieRegexes);
    }

    List<String> getParamRegexes() {
        return new ArrayList<>(paramRegexes);
    }

    String getReplacementString() {
        return replacementString;
    }

    List<RedactionRule> getRedactionRules() {
        return new ArrayList<>(redactionRules);
    }

    String getTemplate() {
        return template;
    }

    PresetFormData withName(String newName) {
        return new PresetFormData(
                presetId,
                newName,
                scope,
                headerRegexes,
                cookieRegexes,
                paramRegexes,
                replacementString,
                redactionRules,
                template
        );
    }

    PresetFormData withoutPresetId() {
        return new PresetFormData(
                null,
                name,
                scope,
                headerRegexes,
                cookieRegexes,
                paramRegexes,
                replacementString,
                redactionRules,
                template
        );
    }

    static PresetFormData forNewPreset() {
        return fromPreset(DefaultPresetFactory.createBuiltInPreset(), PresetScope.USER)
                .withName("")
                .withoutPresetId();
    }

    static PresetFormData empty() {
        return new PresetFormData(
                null,
                "",
                PresetScope.USER,
                List.of(),
                List.of(),
                List.of(),
                DefaultPresetFactory.DEFAULT_REPLACEMENT,
                List.of(),
                ""
        );
    }

    static PresetFormData fromPreset(Preset preset, PresetScope scope) {
        return new PresetFormData(
                preset.getId(),
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

    String firstValidationError() {
        if (name.trim().isEmpty()) {
            return "Name cannot be empty.";
        }

        return RegexValidation.firstValidationError(
                getHeaderRegexes(),
                getCookieRegexes(),
                getParamRegexes(),
                getRedactionRules()
        );
    }

    Preset toPreset(boolean enabled) {
        return new Preset(
                presetId,
                name.trim(),
                getHeaderRegexes(),
                getCookieRegexes(),
                getParamRegexes(),
                getRedactionRules(),
                replacementString,
                template,
                enabled
        );
    }
}
