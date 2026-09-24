package com.motaamneh.mstsocialmvp.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SearchApiBioProviderTest {
    private HttpServer server;
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> query = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/search", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void observesBioUsingDocumentedFieldsAndBearerAuth() {
        responseBody = "{\"search_metadata\":{\"status\":\"Success\",\"created_at\":\""
                + Instant.now() + "\"},\"profile\":{\"username\":\"mothe.techguy\","
                + "\"bio\":\"Hello mst_ABC\",\"is_private\":true}}";
        BioObservation observation = adapter().observe(InstagramHandle.parse("mothe.techguy"));

        assertThat(observation.status()).isEqualTo(BioObservation.Status.AVAILABLE);
        assertThat(observation.biography()).isEqualTo("Hello mst_ABC");
        assertThat(observation.canonicalHandle()).isEqualTo("mothe.techguy");
        assertThat(observation.evidenceMethod()).isEqualTo("SEARCHAPI_INSTAGRAM_PROFILE");
        assertThat(auth.get()).isEqualTo("Bearer test-search-key");
        assertThat(query.get()).isEqualTo("engine=instagram_profile&username=mothe.techguy");
    }

    @Test
    void doesNotMistakeUnavailablePrivateBioOrProviderErrorForMarkerMismatch() {
        responseBody = "{\"search_metadata\":{\"status\":\"Success\",\"created_at\":\""
                + Instant.now() + "\"},\"profile\":{\"username\":\"private_user\",\"is_private\":true}}";
        assertThat(adapter().observe(InstagramHandle.parse("private_user")).status())
                .isEqualTo(BioObservation.Status.INACCESSIBLE);

        responseStatus = 429;
        assertThat(adapter().observe(InstagramHandle.parse("private_user")).status())
                .isEqualTo(BioObservation.Status.UNAVAILABLE);
        responseStatus = 200;
        responseBody = "{\"profile\":{\"username\":\"private_user\",\"bio\":\"mst_ABC\"}}";
        assertThat(adapter().observe(InstagramHandle.parse("private_user")).status())
                .isEqualTo(BioObservation.Status.UNAVAILABLE);
    }

    @Test
    void rejectsMissingKeyAndUntrustedEndpoint() {
        assertThatThrownBy(() -> new SearchApiBioProvider("", new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchApiBioProvider("test", new ObjectMapper(),
                URI.create("https://evil.example/api/v1/search"), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SearchApiBioProvider adapter() {
        URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/search");
        return new SearchApiBioProvider("test-search-key", new ObjectMapper(), endpoint, true);
    }
}
