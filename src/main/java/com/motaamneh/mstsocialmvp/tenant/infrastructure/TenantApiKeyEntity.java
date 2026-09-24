package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_api_key")
public class TenantApiKeyEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key_prefix", nullable = false, length = 32)
    private String keyPrefix;

    @Column(name = "key_digest", nullable = false)
    private byte[] keyDigest;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TenantApiKeyEntity() {
    }

    public TenantApiKeyEntity(UUID id, UUID tenantId, String keyPrefix, byte[] keyDigest,
                              String status, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.keyPrefix = keyPrefix;
        this.keyDigest = keyDigest.clone();
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public byte[] getKeyDigest() {
        return keyDigest.clone();
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
