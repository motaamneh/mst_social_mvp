package com.motaamneh.mstsocialmvp.verification.infrastructure;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationChallengeJpaRepository extends JpaRepository<VerificationChallengeEntity, UUID> {
    Optional<VerificationChallengeEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from VerificationChallengeEntity c where c.id = :id and c.tenantId = :tenantId")
    Optional<VerificationChallengeEntity> lockByIdAndTenantId(@Param("id") UUID id,
                                                                @Param("tenantId") UUID tenantId);
}
