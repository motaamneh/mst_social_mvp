package com.motaamneh.mstsocialmvp.verification.infrastructure;

import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import com.motaamneh.mstsocialmvp.verification.domain.VerificationChallenge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_challenge")
public class VerificationChallengeEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subject_id", nullable = false, length = 200)
    private String subjectId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "claimed_handle", nullable = false, length = 30)
    private String claimedHandle;

    @Column(name = "normalized_handle", nullable = false, length = 30)
    private String normalizedHandle;

    @Column(name = "marker_digest", nullable = false)
    private byte[] markerDigest;

    @Column(name = "marker_key_version", nullable = false)
    private int markerKeyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verifying_at")
    private Instant verifyingAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected VerificationChallengeEntity() {
    }

    public static VerificationChallengeEntity fromDomain(VerificationChallenge challenge) {
        VerificationChallengeEntity entity = new VerificationChallengeEntity();
        entity.id = challenge.id();
        entity.tenantId = challenge.tenantId();
        entity.subjectId = challenge.subjectId();
        entity.provider = "instagram";
        entity.claimedHandle = challenge.handle().value();
        entity.normalizedHandle = challenge.handle().value();
        entity.markerDigest = challenge.markerDigest();
        entity.markerKeyVersion = challenge.markerKeyVersion();
        entity.status = challenge.status();
        entity.attemptCount = challenge.attemptCount();
        entity.createdAt = challenge.createdAt();
        entity.expiresAt = challenge.expiresAt();
        return entity;
    }

    public VerificationChallenge toDomain() {
        return new VerificationChallenge(id, tenantId, subjectId, new InstagramHandle(normalizedHandle),
                markerDigest, markerKeyVersion, status, attemptCount, createdAt, expiresAt);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getSubjectId() { return subjectId; }
    public String getNormalizedHandle() { return normalizedHandle; }
    public byte[] getMarkerDigest() { return markerDigest.clone(); }
    public ChallengeStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getVerifyingAt() { return verifyingAt; }
    public void startVerification(Instant at) { status = ChallengeStatus.VERIFYING; verifyingAt = at; }
    public void finishVerification(ChallengeStatus newStatus, int attempts, Instant verifiedAt) {
        status = newStatus;
        attemptCount = attempts;
        this.verifiedAt = verifiedAt;
        verifyingAt = null;
    }
}
