package com.motaamneh.mstsocialmvp.verification.api;

import com.motaamneh.mstsocialmvp.verification.application.ChallengeStatusView;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.time.Instant;
import java.util.UUID;

public record VerificationStatusResponse(UUID verificationId, String provider, String username,
                                         ChallengeStatus status, Instant expiresAt) {
    public static VerificationStatusResponse from(ChallengeStatusView view) {
        return new VerificationStatusResponse(view.verificationId(), "instagram", view.username(),
                view.status(), view.expiresAt());
    }
}
