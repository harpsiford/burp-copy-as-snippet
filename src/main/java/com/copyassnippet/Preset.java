package com.copyassnippet;

import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static final String DEFAULT_TEMPLATE =
            "HTTP request:\r\n```\r\n{{request}}\r\n```\r\n\r\nHTTP response:\r\n```\r\n{{response}}\r\n```";
    public static final String DEFAULT_REPLACEMENT = "REDACTED";

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
        this.replacementString = replacementString != null ? replacementString : DEFAULT_REPLACEMENT;
        this.template = template;
        this.enabled = enabled;
    }

    public Preset(String name, List<String> headerRegexes, List<String> cookieRegexes, List<String> paramRegexes,
                  List<RedactionRule> redactionRules, String replacementString, String template, boolean enabled) {
        this(null, name, headerRegexes, cookieRegexes, paramRegexes, redactionRules, replacementString, template, enabled);
    }

    public Preset(String name, List<String> headerRegexes, List<String> cookieRegexes, List<String> paramRegexes,
                  String template, boolean enabled) {
        this(name, headerRegexes, cookieRegexes, paramRegexes, List.of(), DEFAULT_REPLACEMENT, template, enabled);
    }

    public Preset(String name, List<String> headerRegexes, List<String> cookieRegexes, List<String> paramRegexes,
                  String template) {
        this(name, headerRegexes, cookieRegexes, paramRegexes, List.of(), DEFAULT_REPLACEMENT, template, true);
    }

    public static Preset createDefault() {
        List<String> defaultHeaders = List.of(
                "Accept",
                "Accept-Language",
                "Accept-Encoding",
                "X-Pwnfox-Color",
                "Priority",
                "Te",
                "User-Agent",
                "X-Requested-With",
                "Vary",
                "Access-Control-Allow-Headers",
                "Access-Control-Allow-Methods",
                "X-Xss-Protection",
                "X-Content-Type-Options",
                "Access-Control-Expose-Headers",
                "Alt-Svc",
                "Via",
                "Server",
                "Sec-Ch-Ua",
                "Sec-Ch-Ua-Mobile",
                "Sec-Ch-Ua-Platform",
                "Upgrade-Insecure-Requests",
                "Sec-Fetch-Site",
                "Sec-Fetch-Mode",
                "Sec-Fetch-Dest",
                "Origin",
                "Referer",
                "Cf-Ray",
                "Trace-Id",
                "Baggage",
                "Sentry-Trace"
        );

        List<String> defaultCookies = List.of(
                "_[\\w_]*",
                "optimizely\\w+",
                "AMP_[\\w_]+",
                "ajs_\\w+_id",
                "GOOG\\w+"
        );

        List<String> defaultParams = List.of(
                "utm_\\w+",
                "fbclid",
                "gclid",
                "_[\\w_]*"
        );

        List<RedactionRule> defaultRedactions = List.of(
                new RedactionRule(RedactionRule.Type.REGEX, "eyJ[\\w-]+\\.eyJ[\\w-]+\\.([\\w-]+)"),
                new RedactionRule(RedactionRule.Type.COOKIE, "PHPSESSID"),
                new RedactionRule(RedactionRule.Type.COOKIE, "JSESSIONID"),
                new RedactionRule(RedactionRule.Type.COOKIE, "ASP\\.NET_SessionId"),
                new RedactionRule(RedactionRule.Type.COOKIE, "\\.ASPXAUTH"),
                new RedactionRule(RedactionRule.Type.COOKIE, "sessionid"),
                new RedactionRule(RedactionRule.Type.COOKIE, "laravel_session"),
                new RedactionRule(RedactionRule.Type.COOKIE, "connect\\.sid"),
                new RedactionRule(RedactionRule.Type.COOKIE, "CFID"),
                new RedactionRule(RedactionRule.Type.COOKIE, "CFTOKEN"),
                new RedactionRule(RedactionRule.Type.COOKIE, "S?SESS\\w+"),
                new RedactionRule(RedactionRule.Type.COOKIE, "wordpress_logged_in_\\w+"),
                new RedactionRule(RedactionRule.Type.COOKIE, "ci_session"),
                new RedactionRule(RedactionRule.Type.COOKIE, "AWSALB"),
                new RedactionRule(RedactionRule.Type.COOKIE, "AWSALBCORS"),
                new RedactionRule(RedactionRule.Type.HEADER, "Authorization"),
                new RedactionRule(RedactionRule.Type.HEADER, "X-Authorization")
        );

        return new Preset(BUILT_IN_ID, "Default", defaultHeaders, defaultCookies, defaultParams,
                defaultRedactions, DEFAULT_REPLACEMENT, DEFAULT_TEMPLATE, true);
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getHeaderRegexes() { return headerRegexes; }
    public void setHeaderRegexes(List<String> headerRegexes) { this.headerRegexes = new ArrayList<>(headerRegexes); }

    public List<String> getCookieRegexes() { return cookieRegexes; }
    public void setCookieRegexes(List<String> cookieRegexes) { this.cookieRegexes = new ArrayList<>(cookieRegexes); }

    public List<String> getParamRegexes() { return paramRegexes; }
    public void setParamRegexes(List<String> paramRegexes) { this.paramRegexes = new ArrayList<>(paramRegexes); }

    public List<RedactionRule> getRedactionRules() { return redactionRules; }
    public void setRedactionRules(List<RedactionRule> redactionRules) { this.redactionRules = new ArrayList<>(redactionRules); }

    public String getReplacementString() { return replacementString; }
    public void setReplacementString(String replacementString) { this.replacementString = replacementString; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void saveTo(PersistedObject obj) {
        obj.setString("id", id);
        obj.setString("name", name);
        obj.setString("template", template);
        obj.setString("enabled", String.valueOf(enabled));
        obj.setString("replacementString", replacementString);

        PersistedList<String> headerList = PersistedList.persistedStringList();
        headerList.addAll(headerRegexes);
        obj.setStringList("headerRegexes", headerList);

        PersistedList<String> cookieList = PersistedList.persistedStringList();
        cookieList.addAll(cookieRegexes);
        obj.setStringList("cookieRegexes", cookieList);

        PersistedList<String> paramList = PersistedList.persistedStringList();
        paramList.addAll(paramRegexes);
        obj.setStringList("paramRegexes", paramList);

        PersistedList<String> ruleList = PersistedList.persistedStringList();
        for (RedactionRule r : redactionRules) ruleList.add(r.toSerializedString());
        obj.setStringList("redactionRules", ruleList);
    }

    public static Preset loadFrom(PersistedObject obj) {
        return loadFrom(obj, null);
    }

    public static Preset loadFrom(PersistedObject obj, String fallbackId) {
        if (obj == null) return null;
        String name = obj.getString("name");
        if (name == null) return null;
        String id = obj.getString("id");

        PersistedList<String> headers = obj.getStringList("headerRegexes");
        PersistedList<String> cookies = obj.getStringList("cookieRegexes");
        PersistedList<String> params = obj.getStringList("paramRegexes");
        PersistedList<String> rulesRaw = obj.getStringList("redactionRules");
        String template = obj.getString("template");
        String replacement = obj.getString("replacementString");

        String enabledStr = obj.getString("enabled");
        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        List<RedactionRule> rules = new ArrayList<>();
        if (rulesRaw != null) {
            for (String s : rulesRaw) {
                RedactionRule r = RedactionRule.fromSerializedString(s);
                if (r != null) rules.add(r);
            }
        }

        return new Preset(
                id != null ? id : fallbackId,
                name,
                headers != null ? new ArrayList<>(headers) : List.of(),
                cookies != null ? new ArrayList<>(cookies) : List.of(),
                params != null ? new ArrayList<>(params) : List.of(),
                rules,
                replacement != null ? replacement : DEFAULT_REPLACEMENT,
                template != null ? template : DEFAULT_TEMPLATE,
                enabled
        );
    }

    public void saveTo(Preferences prefs, String keyPrefix) {
        prefs.setString(keyPrefix + ".id", id);
        prefs.setString(keyPrefix + ".name", name);
        prefs.setString(keyPrefix + ".headerRegexes", String.join("\n", headerRegexes));
        prefs.setString(keyPrefix + ".cookieRegexes", String.join("\n", cookieRegexes));
        prefs.setString(keyPrefix + ".paramRegexes", String.join("\n", paramRegexes));
        prefs.setString(keyPrefix + ".template", template);
        prefs.setString(keyPrefix + ".enabled", String.valueOf(enabled));
        prefs.setString(keyPrefix + ".replacementString", replacementString);
        prefs.setString(keyPrefix + ".redactionRules",
                redactionRules.stream().map(RedactionRule::toSerializedString).collect(Collectors.joining("\n")));
    }

    public static Preset loadFrom(Preferences prefs, String keyPrefix) {
        return loadFrom(prefs, keyPrefix, null);
    }

    public static Preset loadFrom(Preferences prefs, String keyPrefix, String fallbackId) {
        String id = prefs.getString(keyPrefix + ".id");
        String name = prefs.getString(keyPrefix + ".name");
        if (name == null) return null;

        String headersRaw = prefs.getString(keyPrefix + ".headerRegexes");
        String cookiesRaw = prefs.getString(keyPrefix + ".cookieRegexes");
        String paramsRaw = prefs.getString(keyPrefix + ".paramRegexes");
        String template = prefs.getString(keyPrefix + ".template");
        String replacement = prefs.getString(keyPrefix + ".replacementString");
        String rulesRaw = prefs.getString(keyPrefix + ".redactionRules");

        String enabledStr = prefs.getString(keyPrefix + ".enabled");
        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        List<RedactionRule> rules = new ArrayList<>();
        if (rulesRaw != null && !rulesRaw.isBlank()) {
            for (String line : rulesRaw.split("\n")) {
                RedactionRule r = RedactionRule.fromSerializedString(line);
                if (r != null) rules.add(r);
            }
        }

        return new Preset(
                id != null ? id : fallbackId,
                name,
                headersRaw != null && !headersRaw.isEmpty() ? List.of(headersRaw.split("\n")) : List.of(),
                cookiesRaw != null && !cookiesRaw.isEmpty() ? List.of(cookiesRaw.split("\n")) : List.of(),
                paramsRaw != null && !paramsRaw.isEmpty() ? List.of(paramsRaw.split("\n")) : List.of(),
                rules,
                replacement != null ? replacement : DEFAULT_REPLACEMENT,
                template != null ? template : DEFAULT_TEMPLATE,
                enabled
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preset preset = (Preset) o;
        return Objects.equals(id, preset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
