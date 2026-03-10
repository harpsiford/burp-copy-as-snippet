package com.copyassnippet.testutil;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;

import java.util.List;
import java.util.regex.Pattern;

public final class TestArbitraries {
    private static final String SAFE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";

    private TestArbitraries() {
    }

    public static Arbitrary<String> token(String prefix) {
        return Arbitraries.strings()
                .withChars(SAFE_CHARS)
                .ofMinLength(1)
                .ofMaxLength(12)
                .map(value -> prefix + value);
    }

    public static Arbitrary<String> multilineFreeText(String prefix) {
        return Arbitraries.strings()
                .withChars(SAFE_CHARS + " .:/")
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(value -> prefix + value.trim().replace('\n', ' ').replace('\r', ' '));
    }

    public static Arbitrary<String> exactRegex() {
        return token("")
                .map(Pattern::quote);
    }

    public static Arbitrary<List<String>> uniqueTokens(String prefix, int maxSize) {
        return token(prefix)
                .list()
                .uniqueElements()
                .ofMinSize(0)
                .ofMaxSize(maxSize);
    }
}
