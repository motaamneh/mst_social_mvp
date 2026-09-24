package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantApiKeyJpaRepository extends JpaRepository<TenantApiKeyEntity, UUID> {
    List<TenantApiKeyEntity> findByKeyPrefixAndStatus(String keyPrefix, String status);
}
