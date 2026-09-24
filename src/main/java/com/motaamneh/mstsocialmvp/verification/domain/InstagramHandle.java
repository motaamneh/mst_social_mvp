package com.motaamneh.mstsocialmvp.verification.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record InstagramHandle(String value) {
    private static final Pattern VALID = Pattern.compile("[a-z0-9._]{1,30}");

    public InstagramHandle {
        if (value == null || !VALID.matcher(value).matches()
                || value.startsWith(".") || value.endsWith(".") || value.contains("..")) {
            throw new InvalidInstagramHandleException();
        }
    }

    public static InstagramHandle parse(String input) {
        if (input == null) {
            throw new InvalidInstagramHandleException();
        }
        String normalized = input.trim();
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        return new InstagramHandle(normalized.toLowerCase(Locale.ROOT));
    }
}
