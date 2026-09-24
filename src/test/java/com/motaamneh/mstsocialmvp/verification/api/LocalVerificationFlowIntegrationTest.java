package com.motaamneh.mstsocialmvp.verification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.motaamneh.mstsocialmvp.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "MST_LOCAL_API_KEY=mst_test_eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
        "mst.security.marker-hmac-key=test-only-marker-hmac-key-at-least-32-bytes"
})
class LocalVerificationFlowIntegrationTest {
    private static final String KEY = "Bearer mst_test_" + "e".repeat(64);

    @Autowired private MockMvc mvc;

    @Test
    void mismatchThenMatchCreatesIdentityAndMarkerDoesNotLeak() throws Exception {
        String username = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String created = mvc.perform(post("/api/v1/verifications")
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"user-1\",\"username\":\"" + username + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.verificationId");
        String marker = JsonPath.read(created, "$.marker");

        mvc.perform(put("/api/v1/local/instagram-profiles/{username}", username)
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"biography\":\"Just a normal bio\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/verifications/{id}/verify", id).header("Authorization", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("MARKER_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(1));

        mvc.perform(put("/api/v1/local/instagram-profiles/{username}", username)
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"biography\":\"My profile " + marker + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/verifications/{id}/verify", id).header("Authorization", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("VERIFIED"))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.attemptCount").value(2));
        String body = mvc.perform(get("/api/v1/verifications/{id}", id).header("Authorization", KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.marker").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(marker);
        mvc.perform(post("/api/v1/verifications/{id}/verify", id).header("Authorization", KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_ALREADY_VERIFIED"));

        String second = mvc.perform(post("/api/v1/verifications")
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"different-user\",\"username\":\"" + username + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String secondId = JsonPath.read(second, "$.verificationId");
        String secondMarker = JsonPath.read(second, "$.marker");
        mvc.perform(put("/api/v1/local/instagram-profiles/{username}", username)
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"biography\":\"My profile " + secondMarker + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/verifications/{id}/verify", secondId).header("Authorization", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("IDENTITY_ALREADY_LINKED"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void fiveMissingMarkersLockChallenge() throws Exception {
        String username = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String created = mvc.perform(post("/api/v1/verifications")
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":\"user-2\",\"username\":\"" + username + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.verificationId");
        mvc.perform(put("/api/v1/local/instagram-profiles/{username}", username)
                        .header("Authorization", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"biography\":\"No marker\"}"))
                .andExpect(status().isNoContent());
        for (int attempt = 1; attempt <= 5; attempt++) {
            mvc.perform(post("/api/v1/verifications/{id}/verify", id).header("Authorization", KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attemptCount").value(attempt))
                    .andExpect(jsonPath("$.status").value(attempt == 5 ? "LOCKED" : "PENDING"));
        }
        mvc.perform(post("/api/v1/verifications/{id}/verify", id).header("Authorization", KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHALLENGE_LOCKED"));
    }
}
