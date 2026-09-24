package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import com.motaamneh.mstsocialmvp.verification.application.TenantLookup;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTenantLookup implements TenantLookup {
    private final TenantJpaRepository repository;

    public JpaTenantLookup(TenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isActive(UUID tenantId) {
        return repository.existsByIdAndStatus(tenantId, "ACTIVE");
    }
}
