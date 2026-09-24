package com.motaamneh.mstsocialmvp.tenant.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public final class TenantApiKeys {
    private static final Pattern FORMAT = Pattern.compile("mst_(test|live)_[0-9a-f]{64}");
    private static final int PREFIX_LENGTH = 21;

    private TenantApiKeys() {
    }

    public static boolean isValid(String key) {
        return key != null && FORMAT.matcher(key).matches();
    }

    public static String prefix(String key) {
        if (!isValid(key)) {
            throw new IllegalArgumentException("Invalid tenant API key format");
        }
        return key.substring(0, PREFIX_LENGTH);
    }

    public static byte[] digest(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
