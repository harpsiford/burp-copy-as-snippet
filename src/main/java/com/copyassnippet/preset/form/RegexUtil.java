package com.copyassnippet.preset.form;

public final class RegexUtil {

    private RegexUtil() {
    }

    public static String anchorRegex(String regex) {
        String startAnchored = regex.startsWith("^") ? regex : "^" + regex;
        return startAnchored.endsWith("$") ? startAnchored : startAnchored + "$";
    }
}
