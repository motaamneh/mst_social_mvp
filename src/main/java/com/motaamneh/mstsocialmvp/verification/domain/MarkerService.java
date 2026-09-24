package com.motaamneh.mstsocialmvp.verification.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class MarkerService {
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private final SecureRandom random;
    private final SecretKeySpec key;

    public MarkerService(SecureRandom random, String secret) {
        if (random == null || secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Marker HMAC key must be at least 32 bytes");
        }
        this.random = random;
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generate() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder result = new StringBuilder("mst_");
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                result.append(BASE32[(buffer >>> bits) & 31]);
            }
        }
        if (bits > 0) {
            result.append(BASE32[(buffer << (5 - bits)) & 31]);
        }
        return result.toString();
    }

    public byte[] digest(String marker) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(marker.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate marker digest", exception);
        }
    }

    public boolean matches(String candidate, byte[] expectedDigest) {
        return expectedDigest != null && MessageDigest.isEqual(digest(candidate), expectedDigest);
    }
}
