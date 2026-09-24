package com.motaamneh.mstsocialmvp.provider.infrastructure;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.provider.application.BioProvider;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Adapter for a trusted biography service; this is not an Instagram scraping client. */
@Component
@ConditionalOnProperty(name = "mst.instagram-provider.mode", havingValue = "http")
public class HttpBioProvider implements BioProvider {
    private static final int MAX_RESPONSE_BYTES = 16_384;
    private final URI baseUri;
    private final String token;
    private final ObjectMapper mapper;
    private final HttpClient client;

    @Autowired
    public HttpBioProvider(@Value("${mst.instagram-provider.base-url:}") String baseUrl,
                           @Value("${mst.instagram-provider.allowed-host:}") String allowedHost,
                           @Value("${mst.instagram-provider.token:}") String token,
                           ObjectMapper mapper) {
        this(baseUrl, allowedHost, token, mapper, false);
    }

    // Tests may use a loopback HTTP fixture; the Spring-created production adapter never can.
    HttpBioProvider(String baseUrl, String allowedHost, String token, ObjectMapper mapper,
                    boolean allowLoopbackHttp) {
        if (token == null || token.isBlank() || token.contains("\n") || token.contains("\r")) {
            throw new IllegalArgumentException("Instagram provider token is required");
        }
        URI parsed = URI.create(baseUrl);
        boolean secure = "https".equalsIgnoreCase(parsed.getScheme());
        boolean testLoopback = allowLoopbackHttp && "http".equalsIgnoreCase(parsed.getScheme())
                && ("127.0.0.1".equals(parsed.getHost()) || "localhost".equalsIgnoreCase(parsed.getHost()));
        if ((!secure && !testLoopback) || parsed.getHost() == null || allowedHost == null
                || !parsed.getHost().equalsIgnoreCase(allowedHost) || parsed.getUserInfo() != null
                || parsed.getQuery() != null || parsed.getFragment() != null
                || !parsed.getRawPath().matches("[A-Za-z0-9/_-]*")) {
            throw new IllegalArgumentException("Instagram provider URL must use an allowlisted HTTPS host");
        }
        this.baseUri = parsed;
        this.token = token;
        this.mapper = mapper;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public BioObservation observe(InstagramHandle handle) {
        URI endpoint = URI.create(baseUri.toASCIIString()
                + (baseUri.toASCIIString().endsWith("/") ? "" : "/") + "profiles/" + handle.value());
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                if (response.statusCode() == 404) {
                    return new BioObservation(BioObservation.Status.NOT_FOUND, null, null, null,
                            null, null, null);
                }
                if (response.statusCode() == 403) {
                    return new BioObservation(BioObservation.Status.INACCESSIBLE, null, null, null,
                            null, null, null);
                }
                if (response.statusCode() != 200) {
                    return BioObservation.unavailable();
                }
                byte[] bytes = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) {
                    return BioObservation.unavailable();
                }
                JsonNode json = mapper.readTree(bytes);
                String username = text(json, "username");
                String biography = text(json, "biography");
                String observedAt = text(json, "observedAt");
                String accountId = text(json, "accountId");
                if (username == null || biography == null || observedAt == null
                        || biography.length() > 4096 || (accountId != null && accountId.length() > 200)) {
                    return BioObservation.unavailable();
                }
                return new BioObservation(BioObservation.Status.AVAILABLE, biography, accountId, username,
                        "EXTERNAL_BIO_PROVIDER", "PROVIDER_REPORTED", Instant.parse(observedAt));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return BioObservation.unavailable();
        } catch (IOException | RuntimeException exception) {
            return BioObservation.unavailable();
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
