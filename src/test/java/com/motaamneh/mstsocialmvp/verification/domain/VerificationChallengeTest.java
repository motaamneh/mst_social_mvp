package com.motaamneh.mstsocialmvp.verification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationChallengeTest {
    @Test
    void expiresAtDeadlineAndProtectsDigestFromMutation() {
        Instant created = Instant.parse("2026-09-24T12:00:00Z");
        byte[] digest = new byte[32];
        VerificationChallenge challenge = new VerificationChallenge(UUID.randomUUID(), UUID.randomUUID(),
                "subject-1", InstagramHandle.parse("alice"), digest, 1, ChallengeStatus.PENDING,
                0, created, created.plusSeconds(900));
        digest[0] = 1;
        challenge.markerDigest()[1] = 1;

        assertThat(challenge.markerDigest()).containsOnly((byte) 0);
        assertThat(challenge.isExpired(created.plusSeconds(899))).isFalse();
        assertThat(challenge.isExpired(created.plusSeconds(900))).isTrue();
    }
}
