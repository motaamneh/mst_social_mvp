package com.motaamneh.mstsocialmvp.verification.api;

import com.motaamneh.mstsocialmvp.verification.application.ChallengeCreated;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateVerificationResponse(UUID verificationId, String provider, String username,
                                         String marker, ChallengeStatus status, Instant expiresAt) {
    public static CreateVerificationResponse from(ChallengeCreated created) {
        return new CreateVerificationResponse(created.verificationId(), "instagram", created.username(),
                created.marker(), created.status(), created.expiresAt());
    }
}
