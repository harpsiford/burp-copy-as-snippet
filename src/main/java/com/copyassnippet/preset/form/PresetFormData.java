package com.copyassnippet.preset.form;

import com.copyassnippet.preset.model.Preset;
import com.copyassnippet.preset.model.RedactionRule;
import com.copyassnippet.preset.service.DefaultPresetFactory;

import java.util.ArrayList;
import java.util.List;

public final class PresetFormData {
    private final String presetId;
    private final String name;
    private final List<String> headerRegexes;
    private final List<String> cookieRegexes;
    private final List<String> paramRegexes;
    private final String replacementString;
    private final List<RedactionRule> redactionRules;
    private final String template;

    public PresetFormData(
            String presetId,
            String name,
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            String replacementString,
            List<RedactionRule> redactionRules,
            String template) {
        this.presetId = presetId;
        this.name = name != null ? name : "";
        this.headerRegexes = new ArrayList<>(headerRegexes);
        this.cookieRegexes = new ArrayList<>(cookieRegexes);
        this.paramRegexes = new ArrayList<>(paramRegexes);
        this.replacementString = replacementString != null ? replacementString : DefaultPresetFactory.DEFAULT_REPLACEMENT;
        this.redactionRules = new ArrayList<>(redactionRules);
        this.template = template != null ? template : "";
    }

    public String getPresetId() {
        return presetId;
    }

    public String getName() {
        return name;
    }

    public List<String> getHeaderRegexes() {
        return new ArrayList<>(headerRegexes);
    }

    public List<String> getCookieRegexes() {
        return new ArrayList<>(cookieRegexes);
    }

    public List<String> getParamRegexes() {
        return new ArrayList<>(paramRegexes);
    }

    public String getReplacementString() {
        return replacementString;
    }

    public List<RedactionRule> getRedactionRules() {
        return new ArrayList<>(redactionRules);
    }

    public String getTemplate() {
        return template;
    }

    public PresetFormData withName(String newName) {
        return new PresetFormData(
                presetId,
                newName,
                headerRegexes,
                cookieRegexes,
                paramRegexes,
                replacementString,
                redactionRules,
                template
        );
    }

    public PresetFormData withoutPresetId() {
        return new PresetFormData(
                null,
                name,
                headerRegexes,
                cookieRegexes,
                paramRegexes,
                replacementString,
                redactionRules,
                template
        );
    }

    public static PresetFormData forNewPreset() {
        return fromPreset(DefaultPresetFactory.createBuiltInPreset())
                .withName("")
                .withoutPresetId();
    }

    public static PresetFormData empty() {
        return new PresetFormData(
                null,
                "",
                List.of(),
                List.of(),
                List.of(),
                DefaultPresetFactory.DEFAULT_REPLACEMENT,
                List.of(),
                ""
        );
    }

    public static PresetFormData fromPreset(Preset preset) {
        return new PresetFormData(
                preset.getId(),
                preset.getName(),
                preset.getHeaderRegexes(),
                preset.getCookieRegexes(),
                preset.getParamRegexes(),
                preset.getReplacementString(),
                preset.getRedactionRules(),
                preset.getTemplate()
        );
    }

    public String firstValidationError() {
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

    public Preset toPreset(boolean enabled) {
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
