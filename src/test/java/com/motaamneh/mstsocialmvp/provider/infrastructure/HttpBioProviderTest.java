package com.motaamneh.mstsocialmvp.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class HttpBioProviderTest {
    private HttpServer server;
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/profiles/", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            path.set(exchange.getRequestURI().getPath());
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
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
    void readsBoundedBiographyFromTrustedHandler() {
        body.set("{\"username\":\"mothe.techguy\",\"biography\":\"Hello\",\"accountId\":\"ig-123\","
                + "\"observedAt\":\"" + Instant.now() + "\"}");
        BioObservation observation = adapter().observe(InstagramHandle.parse("mothe.techguy"));

        assertThat(observation.status()).isEqualTo(BioObservation.Status.AVAILABLE);
        assertThat(observation.biography()).isEqualTo("Hello");
        assertThat(observation.accountId()).isEqualTo("ig-123");
        assertThat(observation.evidenceMethod()).isEqualTo("EXTERNAL_BIO_PROVIDER");
        assertThat(auth.get()).isEqualTo("Bearer secret-token");
        assertThat(path.get()).isEqualTo("/v1/profiles/mothe.techguy");
    }

    @Test
    void mapsUnavailableAndInaccessibleWithoutTrustingBadResponses() {
        body.set("{}");
        status = 404;
        assertThat(adapter().observe(InstagramHandle.parse("someone")).status())
                .isEqualTo(BioObservation.Status.NOT_FOUND);
        status = 403;
        assertThat(adapter().observe(InstagramHandle.parse("someone")).status())
                .isEqualTo(BioObservation.Status.INACCESSIBLE);
        status = 302;
        assertThat(adapter().observe(InstagramHandle.parse("someone")).status())
                .isEqualTo(BioObservation.Status.UNAVAILABLE);
        status = 200;
        assertThat(adapter().observe(InstagramHandle.parse("someone")).status())
                .isEqualTo(BioObservation.Status.UNAVAILABLE);
        body.set("x".repeat(16_385));
        assertThat(adapter().observe(InstagramHandle.parse("someone")).status())
                .isEqualTo(BioObservation.Status.UNAVAILABLE);
    }

    @Test
    void rejectsUntrustedOrInsecureConfiguration() {
        assertThatThrownBy(() -> new HttpBioProvider("http://example.com/v1", "example.com", "secret",
                new ObjectMapper())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpBioProvider("https://example.com/v1", "other.example", "secret",
                new ObjectMapper())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpBioProvider("https://example.com/v1", "example.com", "",
                new ObjectMapper())).isInstanceOf(IllegalArgumentException.class);
    }

    private HttpBioProvider adapter() {
        return new HttpBioProvider("http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                "127.0.0.1", "secret-token", new ObjectMapper(), true);
    }
}
