package com.motaamneh.mstsocialmvp.verification.infrastructure;

import com.motaamneh.mstsocialmvp.verification.application.VerificationStore;
import com.motaamneh.mstsocialmvp.verification.domain.VerificationChallenge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVerificationStore implements VerificationStore {
    private final VerificationChallengeJpaRepository repository;

    public JpaVerificationStore(VerificationChallengeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(VerificationChallenge challenge) {
        repository.save(VerificationChallengeEntity.fromDomain(challenge));
    }

    @Override
    public Optional<VerificationChallenge> findByIdAndTenantId(UUID id, UUID tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(VerificationChallengeEntity::toDomain);
    }
}
