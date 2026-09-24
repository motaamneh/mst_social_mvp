package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import java.util.UUID;

public interface VerificationState {
    VerificationClaim claim(UUID tenantId, UUID verificationId);
    VerificationOutcome complete(VerificationClaim claim, BioObservation observation, boolean markerFound);
}
