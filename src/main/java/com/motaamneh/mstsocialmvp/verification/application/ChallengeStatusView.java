package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.time.Instant;
import java.util.UUID;

/** Application result without the one-time marker. */
public final class ChallengeStatusView {
    private final UUID verificationId;
    private final String username;
    private final ChallengeStatus status;
    private final Instant expiresAt;

    public ChallengeStatusView(UUID verificationId, String username,
                               ChallengeStatus status, Instant expiresAt) {
        this.verificationId = verificationId;
        this.username = username;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID verificationId() { return verificationId; }
    public String username() { return username; }
    public ChallengeStatus status() { return status; }
    public Instant expiresAt() { return expiresAt; }
}
