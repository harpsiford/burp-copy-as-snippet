package com.copyassnippet.redaction.processor;

import com.copyassnippet.redaction.plan.RedactionPlan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexBodyRedactionProcessor {

    public String redact(String text, RedactionPlan plan) {
        for (Pattern pattern : plan.redactRegexPatterns()) {
            Matcher matcher = pattern.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                int groupCount = matcher.groupCount();
                if (groupCount == 0) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(plan.replacementString()));
                } else {
                    // Replace all capturing groups while preserving text outside those groups.
                    StringBuilder replacement = new StringBuilder();
                    int pos = matcher.start();
                    for (int i = 1; i <= groupCount; i++) {
                        if (matcher.group(i) != null && matcher.start(i) >= pos) {
                            replacement.append(text, pos, matcher.start(i));
                            replacement.append(plan.replacementString());
                            pos = matcher.end(i);
                        }
                    }
                    replacement.append(text, pos, matcher.end());
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
                }
            }
            matcher.appendTail(sb);
            text = sb.toString();
        }
        return text;
    }
}
