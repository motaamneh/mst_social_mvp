package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.motaamneh.mstsocialmvp.TestcontainersConfiguration;
import com.motaamneh.mstsocialmvp.tenant.application.TenantApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
@SpringBootTest(properties = {
        "MST_LOCAL_API_KEY=mst_test_dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
        "mst.security.marker-hmac-key=test-only-marker-hmac-key-at-least-32-bytes"
})
class LocalTenantBootstrapIntegrationTest {
    @Autowired
    private TenantApiKeyService keys;

    @Test
    void bootstrapCreatesUsableLocalTenantKey() {
        assertThat(keys.authenticate("mst_test_" + "d".repeat(64))).isPresent();
    }
}
