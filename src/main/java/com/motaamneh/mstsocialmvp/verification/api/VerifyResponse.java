package com.motaamneh.mstsocialmvp.verification.api;

import com.motaamneh.mstsocialmvp.verification.application.VerificationOutcome;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import java.util.UUID;

public record VerifyResponse(UUID verificationId, ChallengeStatus status, String result, int attemptCount) {
    static VerifyResponse from(VerificationOutcome outcome) {
        return new VerifyResponse(outcome.verificationId(), outcome.status(), outcome.result(),
                outcome.attemptCount());
    }
}
