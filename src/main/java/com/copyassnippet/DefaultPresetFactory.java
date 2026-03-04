package com.copyassnippet;

import java.util.List;

final class DefaultPresetFactory {
    static final String DEFAULT_TEMPLATE =
            "HTTP request:\r\n```\r\n{{request}}\r\n```\r\n\r\nHTTP response:\r\n```\r\n{{response}}\r\n```";
    static final String DEFAULT_REPLACEMENT = "REDACTED";

    private DefaultPresetFactory() {
    }

    static Preset createBuiltInPreset() {
        return new Preset(
                Preset.BUILT_IN_ID,
                "Default",
                defaultHeaders(),
                defaultCookies(),
                defaultParams(),
                defaultRedactions(),
                DEFAULT_REPLACEMENT,
                DEFAULT_TEMPLATE,
                true
        );
    }

    private static List<String> defaultHeaders() {
        return List.of(
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
    }

    private static List<String> defaultCookies() {
        return List.of(
                "_[\\w_]*",
                "optimizely\\w+",
                "AMP_[\\w_]+",
                "ajs_\\w+_id",
                "GOOG\\w+"
        );
    }

    private static List<String> defaultParams() {
        return List.of(
                "utm_\\w+",
                "fbclid",
                "gclid",
                "_[\\w_]*"
        );
    }

    private static List<RedactionRule> defaultRedactions() {
        return List.of(
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
    }
}
