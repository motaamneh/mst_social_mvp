package com.motaamneh.mstsocialmvp.verification.application;

import com.motaamneh.mstsocialmvp.verification.domain.VerificationChallenge;
import java.util.Optional;
import java.util.UUID;

public interface VerificationStore {
    void save(VerificationChallenge challenge);

    Optional<VerificationChallenge> findByIdAndTenantId(UUID id, UUID tenantId);
}
