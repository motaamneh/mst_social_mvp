package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.util.UUID;

public record VerificationOutcome(UUID verificationId, ChallengeStatus status, String result, int attemptCount) {}
