package com.motaamneh.mstsocialmvp.verification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motaamneh.mstsocialmvp.TestcontainersConfiguration;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantEntity;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantJpaRepository;
import com.motaamneh.mstsocialmvp.verification.domain.ChallengeStatus;
import com.motaamneh.mstsocialmvp.verification.domain.MarkerService;
import com.motaamneh.mstsocialmvp.verification.infrastructure.VerificationChallengeJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "mst.security.marker-hmac-key=test-only-marker-hmac-key-at-least-32-bytes")
class VerificationServiceIntegrationTest {
    @Autowired
    private VerificationService service;

    @Autowired
    private TenantJpaRepository tenants;

    @Autowired
    private VerificationChallengeJpaRepository challenges;

    @Autowired
    private MarkerService markers;

    @Test
    void createsChallengeWithDigestAndReturnsMarkerOnlyOnce() {
        UUID tenantId = UUID.randomUUID();
        tenants.saveAndFlush(new TenantEntity(tenantId, "Test tenant", "ACTIVE", Instant.now()));

        ChallengeCreated created = service.create(tenantId, "user-123", " @Alice_Example ");
        var stored = challenges.findByIdAndTenantId(created.verificationId(), tenantId).orElseThrow();
        ChallengeStatusView status = service.getStatus(tenantId, created.verificationId());

        assertThat(created.username()).isEqualTo("alice_example");
        assertThat(created.marker()).matches("mst_[A-Z2-7]{26}");
        assertThat(created.status()).isEqualTo(ChallengeStatus.PENDING);
        assertThat(markers.matches(created.marker(), stored.toDomain().markerDigest())).isTrue();
        assertThat(status.status()).isEqualTo(ChallengeStatus.PENDING);
        assertThat(status).hasNoNullFieldsOrProperties();
        assertThat(status.toString()).doesNotContain(created.marker());
        assertThatThrownBy(() -> service.getStatus(UUID.randomUUID(), created.verificationId()))
                .isInstanceOf(ChallengeNotFoundException.class);
    }
}
