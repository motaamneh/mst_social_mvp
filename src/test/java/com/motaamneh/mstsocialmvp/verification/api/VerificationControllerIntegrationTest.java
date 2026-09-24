package com.motaamneh.mstsocialmvp.verification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.motaamneh.mstsocialmvp.TestcontainersConfiguration;
import com.motaamneh.mstsocialmvp.tenant.application.TenantApiKeys;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantApiKeyEntity;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantApiKeyJpaRepository;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantEntity;
import com.motaamneh.mstsocialmvp.tenant.infrastructure.TenantJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(properties = "mst.security.marker-hmac-key=test-only-marker-hmac-key-at-least-32-bytes")
class VerificationControllerIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private TenantJpaRepository tenants;

    @Autowired
    private TenantApiKeyJpaRepository keys;

    @Test
    void bearerKeyControlsTenantAndStatusNeverReturnsMarker() throws Exception {
        String firstKey = "mst_test_" + "a".repeat(64);
        String secondKey = "mst_test_" + "b".repeat(64);
        addTenantWithKey(firstKey);
        addTenantWithKey(secondKey);

        mvc.perform(post("/api/v1/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"user-1\",\"username\":\"mothe.techguy\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/verifications/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer mst_test_" + "c".repeat(64)))
                .andExpect(status().isUnauthorized());

        String created = mvc.perform(post("/api/v1/verifications")
                        .header("Authorization", "Bearer " + firstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"user-1\",\"username\":\"mothe.techguy\"," +
                                "\"tenantId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("instagram"))
                .andExpect(jsonPath("$.username").value("mothe.techguy"))
                .andExpect(jsonPath("$.marker").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(created, "$.verificationId");
        mvc.perform(get("/api/v1/verifications/{id}", id)
                        .header("Authorization", "Bearer " + firstKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.marker").doesNotExist());

        mvc.perform(get("/api/v1/verifications/{id}", id)
                        .header("Authorization", "Bearer " + secondKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_FOUND"));

        mvc.perform(post("/api/v1/verifications/{id}/verify", id)
                        .header("Authorization", "Bearer " + secondKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHALLENGE_NOT_FOUND"));

        mvc.perform(post("/api/v1/verifications/{id}/verify", id)
                        .header("Authorization", "Bearer " + firstKey))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.result").value("PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(0));

        mvc.perform(post("/api/v1/verifications")
                        .header("Authorization", "Bearer " + firstKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"user-2\",\"username\":\"bad/name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INSTAGRAM_HANDLE"));
    }

    private UUID addTenantWithKey(String key) {
        UUID tenantId = UUID.randomUUID();
        tenants.saveAndFlush(new TenantEntity(tenantId, "Test tenant", "ACTIVE", Instant.now()));
        keys.saveAndFlush(new TenantApiKeyEntity(UUID.randomUUID(), tenantId, TenantApiKeys.prefix(key),
                TenantApiKeys.digest(key), "ACTIVE", Instant.now()));
        return tenantId;
    }
}
