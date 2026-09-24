package com.motaamneh.mstsocialmvp.verification.domain;

import java.time.Instant;
import java.util.UUID;

public record VerificationChallenge(
        UUID id,
        UUID tenantId,
        String subjectId,
        InstagramHandle handle,
        byte[] markerDigest,
        int markerKeyVersion,
        ChallengeStatus status,
        int attemptCount,
        Instant createdAt,
        Instant expiresAt
) {
    public VerificationChallenge {
        if (id == null || tenantId == null || subjectId == null || subjectId.isBlank()
                || handle == null || markerDigest == null || markerDigest.length != 32
                || markerKeyVersion < 1 || status == null || attemptCount < 0
                || createdAt == null || expiresAt == null || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Invalid verification challenge");
        }
        markerDigest = markerDigest.clone();
    }

    @Override
    public byte[] markerDigest() {
        return markerDigest.clone();
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
