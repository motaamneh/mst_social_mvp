CREATE TABLE tenant (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE tenant_api_key (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    key_prefix VARCHAR(32) NOT NULL,
    key_digest BYTEA NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX idx_tenant_api_key_prefix ON tenant_api_key(key_prefix);

CREATE TABLE verification_challenge (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    subject_id VARCHAR(200) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    claimed_handle VARCHAR(30) NOT NULL,
    normalized_handle VARCHAR(30) NOT NULL,
    marker_digest BYTEA NOT NULL,
    marker_key_version INTEGER NOT NULL CHECK (marker_key_version > 0),
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT chk_challenge_expiry CHECK (expires_at > created_at)
);
CREATE INDEX idx_challenge_tenant_subject ON verification_challenge(tenant_id, subject_id);
CREATE INDEX idx_challenge_expiry ON verification_challenge(status, expires_at);

CREATE TABLE verification_attempt (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL REFERENCES verification_challenge(id),
    result VARCHAR(30) NOT NULL,
    evidence_method VARCHAR(50),
    assurance_level VARCHAR(30),
    provider_account_id VARCHAR(200),
    canonical_handle VARCHAR(30),
    provider_error_code VARCHAR(80),
    retryable BOOLEAN NOT NULL DEFAULT FALSE,
    observed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_attempt_challenge ON verification_attempt(challenge_id, created_at);

CREATE TABLE social_identity (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    subject_id VARCHAR(200) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_account_id VARCHAR(200),
    canonical_handle VARCHAR(30) NOT NULL,
    evidence_method VARCHAR(50) NOT NULL,
    assurance_level VARCHAR(30) NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL,
    reverify_after TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_active_provider_account
    ON social_identity(tenant_id, provider, provider_account_id)
    WHERE revoked_at IS NULL AND provider_account_id IS NOT NULL;
CREATE UNIQUE INDEX uq_active_provider_handle_without_id
    ON social_identity(tenant_id, provider, canonical_handle)
    WHERE revoked_at IS NULL AND provider_account_id IS NULL;

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    actor_type VARCHAR(30) NOT NULL,
    actor_id VARCHAR(200) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL,
    safe_metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX idx_audit_tenant_time ON audit_event(tenant_id, occurred_at);
