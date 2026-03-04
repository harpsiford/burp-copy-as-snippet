package example.contextmenu;

import burp.api.montoya.persistence.PersistedList;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Preset {
    private String name;
    private List<String> headerRegexes;
    private List<String> cookieRegexes;
    private List<String> paramRegexes;
    private String template;
    private boolean enabled;

    public static final String DEFAULT_TEMPLATE =
            "HTTP request:\r\n```\r\n{{request}}\r\n```\r\n\r\nHTTP response:\r\n```\r\n{{response}}\r\n```";

    public Preset(String name, List<String> headerRegexes, List<String> cookieRegexes, List<String> paramRegexes, String template, boolean enabled) {
        this.name = name;
        this.headerRegexes = new ArrayList<>(headerRegexes);
        this.cookieRegexes = new ArrayList<>(cookieRegexes);
        this.paramRegexes = new ArrayList<>(paramRegexes);
        this.template = template;
        this.enabled = enabled;
    }

    public Preset(String name, List<String> headerRegexes, List<String> cookieRegexes, List<String> paramRegexes, String template) {
        this(name, headerRegexes, cookieRegexes, paramRegexes, template, true);
    }

    public static Preset createDefault() {
        List<String> defaultHeaders = List.of(
                "^Accept$",
                "^Accept-Language$",
                "^Accept-Encoding$",
                "^X-Pwnfox-Color$",
                "^Priority$",
                "^Te$",
                "^User-Agent$",
                "^X-Requested-With$",
                "^Vary$",
                "^Access-Control-Allow-Headers$",
                "^Access-Control-Allow-Methods$",
                "^X-Xss-Protection$",
                "^X-Content-Type-Options$",
                "^Access-Control-Expose-Headers$",
                "^Alt-Svc$",
                "^Via$",
                "^Server$",
                "^Sec-Ch-Ua$",
                "^Sec-Ch-Ua-Mobile$",
                "^Sec-Ch-Ua-Platform$",
                "^Upgrade-Insecure-Requests$",
                "^Sec-Fetch-Site$",
                "^Sec-Fetch-Mode$",
                "^Sec-Fetch-Dest$",
                "^Origin$",
                "^Referer$",
                "^Cf-Ray$",
                "^Trace-Id$",
                "^Baggage$",
                "^Sentry-Trace$"
        );

        List<String> defaultCookies = List.of(
                "^_[\\w_]*$",
                "^optimizely\\w+$",
                "^AMP_[\\w_]+$",
                "^ajs_\\w+_id$",
                "^GOOG\\w+$"
        );

        List<String> defaultParams = List.of(
                "^utm_\\w+$",
                "^fbclid$",
                "^gclid$",
                "^_[\\w_]*$"
        );

        return new Preset("Default", defaultHeaders, defaultCookies, defaultParams, DEFAULT_TEMPLATE);
    }

    // --- Getters and setters ---

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

    // --- PersistedObject serialization (for project-level storage) ---

    public void saveTo(PersistedObject obj) {
        obj.setString("name", name);
        obj.setString("template", template);
        obj.setString("enabled", String.valueOf(enabled));

        PersistedList<String> headerList = PersistedList.persistedStringList();
        headerList.addAll(headerRegexes);
        obj.setStringList("headerRegexes", headerList);

        PersistedList<String> cookieList = PersistedList.persistedStringList();
        cookieList.addAll(cookieRegexes);
        obj.setStringList("cookieRegexes", cookieList);

        PersistedList<String> paramList = PersistedList.persistedStringList();
        paramList.addAll(paramRegexes);
        obj.setStringList("paramRegexes", paramList);
    }

    public static Preset loadFrom(PersistedObject obj) {
        if (obj == null) return null;
        String name = obj.getString("name");
        if (name == null) return null;

        PersistedList<String> headers = obj.getStringList("headerRegexes");
        PersistedList<String> cookies = obj.getStringList("cookieRegexes");
        PersistedList<String> params = obj.getStringList("paramRegexes");
        String template = obj.getString("template");

        String enabledStr = obj.getString("enabled");
        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        return new Preset(
                name,
                headers != null ? new ArrayList<>(headers) : List.of(),
                cookies != null ? new ArrayList<>(cookies) : List.of(),
                params != null ? new ArrayList<>(params) : List.of(),
                template != null ? template : DEFAULT_TEMPLATE,
                enabled
        );
    }

    // --- Preferences serialization (for user-level storage, flat key-value) ---

    public void saveTo(Preferences prefs, String keyPrefix) {
        prefs.setString(keyPrefix + ".name", name);
        prefs.setString(keyPrefix + ".headerRegexes", String.join("\n", headerRegexes));
        prefs.setString(keyPrefix + ".cookieRegexes", String.join("\n", cookieRegexes));
        prefs.setString(keyPrefix + ".paramRegexes", String.join("\n", paramRegexes));
        prefs.setString(keyPrefix + ".template", template);
        prefs.setString(keyPrefix + ".enabled", String.valueOf(enabled));
    }

    public static Preset loadFrom(Preferences prefs, String keyPrefix) {
        String name = prefs.getString(keyPrefix + ".name");
        if (name == null) return null;

        String headersRaw = prefs.getString(keyPrefix + ".headerRegexes");
        String cookiesRaw = prefs.getString(keyPrefix + ".cookieRegexes");
        String paramsRaw = prefs.getString(keyPrefix + ".paramRegexes");
        String template = prefs.getString(keyPrefix + ".template");

        String enabledStr = prefs.getString(keyPrefix + ".enabled");
        boolean enabled = enabledStr == null || !"false".equals(enabledStr);

        return new Preset(
                name,
                headersRaw != null && !headersRaw.isEmpty() ? List.of(headersRaw.split("\n")) : List.of(),
                cookiesRaw != null && !cookiesRaw.isEmpty() ? List.of(cookiesRaw.split("\n")) : List.of(),
                paramsRaw != null && !paramsRaw.isEmpty() ? List.of(paramsRaw.split("\n")) : List.of(),
                template != null ? template : DEFAULT_TEMPLATE,
                enabled
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preset preset = (Preset) o;
        return Objects.equals(name, preset.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
