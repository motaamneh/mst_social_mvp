package com.motaamneh.mstsocialmvp.tenant.application;

import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantApiKeyEntity;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantApiKeyJpaRepository;
import com.motaamneh.mstsocialmvp.verification.application.TenantLookup;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantApiKeyService {
    private final TenantApiKeyJpaRepository keys;
    private final TenantLookup tenants;
    private final Clock clock;

    public TenantApiKeyService(TenantApiKeyJpaRepository keys, TenantLookup tenants, Clock clock) {
        this.keys = keys;
        this.tenants = tenants;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> authenticate(String key) {
        if (!TenantApiKeys.isValid(key)) {
            return Optional.empty();
        }
        byte[] digest = TenantApiKeys.digest(key);
        Instant now = clock.instant();
        for (TenantApiKeyEntity stored : keys.findByKeyPrefixAndStatus(TenantApiKeys.prefix(key), "ACTIVE")) {
            if (MessageDigest.isEqual(digest, stored.getKeyDigest())
                    && stored.getRevokedAt() == null
                    && (stored.getExpiresAt() == null || now.isBefore(stored.getExpiresAt()))
                    && tenants.isActive(stored.getTenantId())) {
                return Optional.of(stored.getTenantId());
            }
        }
        return Optional.empty();
    }
}
