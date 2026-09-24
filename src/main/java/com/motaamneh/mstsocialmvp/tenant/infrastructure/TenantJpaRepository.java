package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {
    boolean existsByIdAndStatus(UUID id, String status);
}
