package com.motaamneh.mstsocialmvp.verification.infrastructure;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.verification.application.ChallengeNotFoundException;
import com.motaamneh.mstsocialmvp.verification.application.VerificationClaim;
import com.motaamneh.mstsocialmvp.verification.application.VerificationOperationException;
import com.motaamneh.mstsocialmvp.verification.application.VerificationOutcome;
import com.motaamneh.mstsocialmvp.verification.application.VerificationState;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaVerificationState implements VerificationState {
    private static final Duration VERIFY_LEASE = Duration.ofSeconds(30);
    private final VerificationChallengeJpaRepository challenges;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int maximumAttempts;

    public JpaVerificationState(VerificationChallengeJpaRepository challenges, JdbcTemplate jdbc, Clock clock,
                                @Value("${mst.verification.maximum-attempts}") int maximumAttempts) {
        this.challenges = challenges;
        this.jdbc = jdbc;
        this.clock = clock;
        this.maximumAttempts = maximumAttempts;
    }

    @Override
    @Transactional(noRollbackFor = VerificationOperationException.class)
    public VerificationClaim claim(UUID tenantId, UUID verificationId) {
        VerificationChallengeEntity challenge = challenges.lockByIdAndTenantId(verificationId, tenantId)
                .orElseThrow(ChallengeNotFoundException::new);
        Instant now = clock.instant();
        if (challenge.getStatus() == ChallengeStatus.VERIFIED) {
            throw new VerificationOperationException("CHALLENGE_ALREADY_VERIFIED");
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            if (challenge.getStatus() == ChallengeStatus.PENDING || challenge.getStatus() == ChallengeStatus.VERIFYING) {
                challenge.finishVerification(ChallengeStatus.EXPIRED, challenge.getAttemptCount(), null);
            }
            throw new VerificationOperationException("CHALLENGE_EXPIRED");
        }
        if (challenge.getStatus() == ChallengeStatus.LOCKED || challenge.getAttemptCount() >= maximumAttempts) {
            challenge.finishVerification(ChallengeStatus.LOCKED, challenge.getAttemptCount(), null);
            throw new VerificationOperationException("CHALLENGE_LOCKED");
        }
        if (challenge.getStatus() == ChallengeStatus.VERIFYING && challenge.getVerifyingAt() != null
                && challenge.getVerifyingAt().plus(VERIFY_LEASE).isAfter(now)) {
            throw new VerificationOperationException("CHALLENGE_IN_PROGRESS");
        }
        if (challenge.getStatus() != ChallengeStatus.PENDING && challenge.getStatus() != ChallengeStatus.VERIFYING) {
            throw new VerificationOperationException("CHALLENGE_NOT_PENDING");
        }
        challenge.startVerification(now);
        return new VerificationClaim(challenge.getId(), tenantId, challenge.getSubjectId(),
                new InstagramHandle(challenge.getNormalizedHandle()), challenge.getMarkerDigest(),
                challenge.getCreatedAt(), now);
    }

    @Override
    @Transactional
    public VerificationOutcome complete(VerificationClaim claim, BioObservation observation, boolean markerFound) {
        VerificationChallengeEntity challenge = challenges.lockByIdAndTenantId(claim.id(), claim.tenantId())
                .orElseThrow(ChallengeNotFoundException::new);
        if (challenge.getStatus() != ChallengeStatus.VERIFYING
                || !claim.leaseStartedAt().equals(challenge.getVerifyingAt())) {
            throw new VerificationOperationException("CHALLENGE_IN_PROGRESS");
        }
        Instant now = clock.instant();
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.finishVerification(ChallengeStatus.EXPIRED, challenge.getAttemptCount(), null);
            return new VerificationOutcome(claim.id(), ChallengeStatus.EXPIRED, "CHALLENGE_EXPIRED",
                    challenge.getAttemptCount());
        }

        String result;
        ChallengeStatus status;
        int attempts = challenge.getAttemptCount();
        if (observation.status() == BioObservation.Status.UNAVAILABLE) {
            result = "PROVIDER_UNAVAILABLE";
            status = ChallengeStatus.PENDING;
        } else {
            attempts++;
            result = switch (observation.status()) {
                case NOT_FOUND -> "PROFILE_NOT_FOUND";
                case INACCESSIBLE -> "PROFILE_INACCESSIBLE";
                case AVAILABLE -> markerFound ? "VERIFIED" : "MARKER_NOT_FOUND";
                case UNAVAILABLE -> throw new IllegalStateException("Handled above");
            };
            status = attempts >= maximumAttempts ? ChallengeStatus.LOCKED : ChallengeStatus.PENDING;
            if (markerFound && observation.status() == BioObservation.Status.AVAILABLE) {
                if (observation.evidenceMethod() == null || observation.assuranceLevel() == null) {
                    throw new IllegalArgumentException("Provider evidence metadata is required");
                }
                int linked = jdbc.update("""
                        INSERT INTO social_identity (id, tenant_id, subject_id, provider, provider_account_id,
                          canonical_handle, evidence_method, assurance_level, verified_at)
                        VALUES (?, ?, ?, 'instagram', ?, ?, ?, ?, ?)
                        ON CONFLICT DO NOTHING
                        """, UUID.randomUUID(), claim.tenantId(), claim.subjectId(), observation.accountId(),
                        claim.handle().value(), observation.evidenceMethod(), observation.assuranceLevel(),
                        Timestamp.from(now));
                if (linked == 0) {
                    result = "IDENTITY_ALREADY_LINKED";
                } else {
                    status = ChallengeStatus.VERIFIED;
                }
            }
        }
        challenge.finishVerification(status, attempts, status == ChallengeStatus.VERIFIED ? now : null);
        jdbc.update("""
                INSERT INTO verification_attempt (id, challenge_id, result, evidence_method, assurance_level,
                  provider_account_id, canonical_handle, provider_error_code, retryable, observed_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), claim.id(), result, observation.evidenceMethod(),
                observation.assuranceLevel(), observation.accountId(), observation.canonicalHandle(),
                observation.status() == BioObservation.Status.UNAVAILABLE ? "PROVIDER_UNAVAILABLE" : null,
                observation.status() == BioObservation.Status.UNAVAILABLE,
                observation.observedAt() == null ? null : Timestamp.from(observation.observedAt()), Timestamp.from(now));
        return new VerificationOutcome(claim.id(), status, result, attempts);
    }
}
