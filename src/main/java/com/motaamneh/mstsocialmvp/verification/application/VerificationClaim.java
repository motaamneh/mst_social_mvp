package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import java.time.Instant;
import java.util.UUID;

public record VerificationClaim(UUID id, UUID tenantId, String subjectId, InstagramHandle handle,
                                byte[] markerDigest, Instant createdAt, Instant leaseStartedAt) {
    public VerificationClaim {
        markerDigest = markerDigest.clone();
    }
    @Override public byte[] markerDigest() { return markerDigest.clone(); }
}
