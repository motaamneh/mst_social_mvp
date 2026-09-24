package com.motaamneh.mstsocialmvp.verification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class MarkerServiceTest {
    private final MarkerService markers = new MarkerService(
            new SecureRandom(), "test-only-marker-hmac-key-at-least-32-bytes");

    @Test
    void generatesBase32MarkerAndMatchesOnlyItsDigest() {
        String marker = markers.generate();
        byte[] digest = markers.digest(marker);

        assertThat(marker).matches("mst_[A-Z2-7]{26}");
        assertThat(digest).hasSize(32);
        assertThat(markers.matches(marker, digest)).isTrue();
        assertThat(markers.matches(markers.generate(), digest)).isFalse();
    }
}
