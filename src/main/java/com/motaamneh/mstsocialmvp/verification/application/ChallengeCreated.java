package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.time.Instant;
import java.util.UUID;

/** Application result; the API layer decides how to expose it. */
public final class ChallengeCreated {
    private final UUID verificationId;
    private final String username;
    private final String marker;
    private final ChallengeStatus status;
    private final Instant expiresAt;

    public ChallengeCreated(UUID verificationId, String username, String marker,
                            ChallengeStatus status, Instant expiresAt) {
        this.verificationId = verificationId;
        this.username = username;
        this.marker = marker;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID verificationId() { return verificationId; }
    public String username() { return username; }
    public String marker() { return marker; }
    public ChallengeStatus status() { return status; }
    public Instant expiresAt() { return expiresAt; }
}
