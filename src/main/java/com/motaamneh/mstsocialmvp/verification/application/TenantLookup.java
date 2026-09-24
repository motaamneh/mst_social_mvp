package com.motaamneh.mstsocialmvp.verification.application;

import java.util.UUID;

public interface TenantLookup {
    boolean isActive(UUID tenantId);
}
