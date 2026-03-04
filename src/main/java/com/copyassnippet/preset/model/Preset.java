package com.copyassnippet.preset.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Preset {
    public static final String BUILT_IN_ID = "builtin-default";

    private final String id;
    private String name;
    private List<String> headerRegexes;
    private List<String> cookieRegexes;
    private List<String> paramRegexes;
    private List<RedactionRule> redactionRules;
    private String replacementString;
    private String template;
    private boolean enabled;

    public Preset(
            String id,
            String name,
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            List<RedactionRule> redactionRules,
            String replacementString,
            String template,
            boolean enabled) {
        this.id = normalizeId(id);
        this.name = name;
        this.headerRegexes = new ArrayList<>(headerRegexes);
        this.cookieRegexes = new ArrayList<>(cookieRegexes);
        this.paramRegexes = new ArrayList<>(paramRegexes);
        this.redactionRules = new ArrayList<>(redactionRules);
        this.replacementString = replacementString != null ? replacementString : "";
        this.template = template != null ? template : "";
        this.enabled = enabled;
    }

    public Preset(
            String name,
            List<String> headerRegexes,
            List<String> cookieRegexes,
            List<String> paramRegexes,
            List<RedactionRule> redactionRules,
            String replacementString,
            String template,
            boolean enabled) {
        this(null, name, headerRegexes, cookieRegexes, paramRegexes, redactionRules, replacementString, template, enabled);
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getHeaderRegexes() {
        return headerRegexes;
    }

    public void setHeaderRegexes(List<String> headerRegexes) {
        this.headerRegexes = new ArrayList<>(headerRegexes);
    }

    public List<String> getCookieRegexes() {
        return cookieRegexes;
    }

    public void setCookieRegexes(List<String> cookieRegexes) {
        this.cookieRegexes = new ArrayList<>(cookieRegexes);
    }

    public List<String> getParamRegexes() {
        return paramRegexes;
    }

    public void setParamRegexes(List<String> paramRegexes) {
        this.paramRegexes = new ArrayList<>(paramRegexes);
    }

    public List<RedactionRule> getRedactionRules() {
        return redactionRules;
    }

    public void setRedactionRules(List<RedactionRule> redactionRules) {
        this.redactionRules = new ArrayList<>(redactionRules);
    }

    public String getReplacementString() {
        return replacementString;
    }

    public void setReplacementString(String replacementString) {
        this.replacementString = replacementString;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Preset preset = (Preset) o;
        return Objects.equals(id, preset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
