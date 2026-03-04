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
        this.replacementString = replacementString != null ? replacementString : Preset.DEFAULT_REPLACEMENT;
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
}
