package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.provider.application.BioProvider;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import com.motaamneh.mstsocialmvp.verification.domain.MarkerService;
import com.motaamneh.mstsocialmvp.verification.domain.VerificationChallenge;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {
    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final Pattern MARKER = Pattern.compile("(?<![A-Za-z0-9_])mst_[A-Z2-7]{26}(?![A-Za-z0-9_])");
    private final VerificationStore store;
    private final TenantLookup tenants;
    private final MarkerService markers;
    private final Clock clock;
    private final Duration challengeTtl;
    private final BioProvider provider;
    private final VerificationState state;

    public VerificationService(VerificationStore store, TenantLookup tenants, MarkerService markers,
                               Clock clock, BioProvider provider, VerificationState state,
                               @Value("${mst.verification.challenge-ttl}") Duration challengeTtl) {
        if (challengeTtl == null || challengeTtl.isNegative() || challengeTtl.isZero()) {
            throw new IllegalArgumentException("Challenge TTL must be positive");
        }
        this.store = store;
        this.tenants = tenants;
        this.markers = markers;
        this.clock = clock;
        this.challengeTtl = challengeTtl;
        this.provider = provider;
        this.state = state;
    }

    @Transactional
    public ChallengeCreated create(UUID tenantId, String subjectId, String username) {
        if (tenantId == null || !tenants.isActive(tenantId)) {
            throw new IllegalArgumentException("Active tenant is required");
        }
        if (subjectId == null || subjectId.isBlank() || subjectId.length() > 200) {
            throw new IllegalArgumentException("Subject ID must contain 1 to 200 characters");
        }
        InstagramHandle handle = InstagramHandle.parse(username);
        String marker = markers.generate();
        Instant now = clock.instant();
        VerificationChallenge challenge = new VerificationChallenge(
                UUID.randomUUID(), tenantId, subjectId, handle, markers.digest(marker), 1,
                ChallengeStatus.PENDING, 0, now, now.plus(challengeTtl));
        store.save(challenge);
        return new ChallengeCreated(challenge.id(), handle.value(), marker, challenge.status(), challenge.expiresAt());
    }

    @Transactional(readOnly = true)
    public ChallengeStatusView getStatus(UUID tenantId, UUID verificationId) {
        VerificationChallenge challenge = store.findByIdAndTenantId(verificationId, tenantId)
                .orElseThrow(ChallengeNotFoundException::new);
        ChallengeStatus status = challenge.status() == ChallengeStatus.PENDING && challenge.isExpired(clock.instant())
                ? ChallengeStatus.EXPIRED : challenge.status();
        return new ChallengeStatusView(challenge.id(), challenge.handle().value(), status, challenge.expiresAt());
    }

    public VerificationOutcome verify(UUID tenantId, UUID verificationId) {
        VerificationClaim claim = state.claim(tenantId, verificationId);
        log.info("Verification {} observing biography with {}", verificationId,
                provider.getClass().getSimpleName());
        BioObservation observation;
        try {
            observation = provider.observe(claim.handle());
            if (observation == null || observation.status() == null) {
                observation = BioObservation.unavailable();
            }
        } catch (RuntimeException exception) {
            log.warn("Verification {} provider observation failed: {}", verificationId,
                    exception.getClass().getSimpleName());
            observation = BioObservation.unavailable();
        }
        boolean found = false;
        if (observation.status() == BioObservation.Status.AVAILABLE) {
            if (observation.biography() == null || observation.biography().length() > 4096
                    || observation.observedAt() == null || observation.observedAt().isAfter(clock.instant())
                    || observation.observedAt().isBefore(claim.createdAt())
                    || observation.canonicalHandle() == null) {
                observation = BioObservation.unavailable();
            } else {
                try {
                    if (!claim.handle().equals(InstagramHandle.parse(observation.canonicalHandle()))) {
                        observation = BioObservation.unavailable();
                    } else {
                        Matcher candidates = MARKER.matcher(observation.biography());
                        while (candidates.find()) {
                            if (markers.matches(candidates.group(), claim.markerDigest())) {
                                found = true;
                                break;
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    observation = BioObservation.unavailable();
                }
            }
        }
        VerificationOutcome outcome = state.complete(claim, observation, found);
        log.info("Verification {} observation={} result={} status={} attempts={}", verificationId,
                observation.status(), outcome.result(), outcome.status(), outcome.attemptCount());
        return outcome;
    }
}
