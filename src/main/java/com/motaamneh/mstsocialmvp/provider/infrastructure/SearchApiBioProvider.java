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
import java.time.Instant;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "mst.instagram-provider.mode", havingValue = "searchapi")
public class SearchApiBioProvider implements BioProvider {
    private static final Logger log = LoggerFactory.getLogger(SearchApiBioProvider.class);
    private static final URI SEARCH_ENDPOINT = URI.create("https://www.searchapi.io/api/v1/search");
    private static final int MAX_RESPONSE_BYTES = 524_288;
    private final String apiKey;
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final HttpClient client;

    @Autowired
    public SearchApiBioProvider(@Value("${mst.searchapi.api-key:}") String apiKey, ObjectMapper mapper) {
        this(apiKey, mapper, SEARCH_ENDPOINT, false);
    }

    // A loopback-only endpoint is permitted in tests; Spring always uses the fixed HTTPS endpoint.
    SearchApiBioProvider(String apiKey, ObjectMapper mapper, URI endpoint, boolean allowLoopbackHttp) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("\n") || apiKey.contains("\r")) {
            throw new IllegalArgumentException("MST_SEARCHAPI_API_KEY is required for SearchAPI mode");
        }
        boolean productionEndpoint = SEARCH_ENDPOINT.equals(endpoint);
        boolean testEndpoint = allowLoopbackHttp && "http".equalsIgnoreCase(endpoint.getScheme())
                && ("127.0.0.1".equals(endpoint.getHost()) || "localhost".equalsIgnoreCase(endpoint.getHost()))
                && "/api/v1/search".equals(endpoint.getPath())
                && endpoint.getQuery() == null && endpoint.getFragment() == null;
        if (!productionEndpoint && !testEndpoint) {
            throw new IllegalArgumentException("SearchAPI endpoint must be the documented HTTPS endpoint");
        }
        this.apiKey = apiKey;
        this.mapper = mapper;
        this.endpoint = endpoint;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public BioObservation observe(InstagramHandle handle) {
        URI requestUri = URI.create(endpoint + "?engine=instagram_profile&username=" + handle.value());
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                // SearchAPI's error status codes are not profile-specific; never treat auth/quota errors as a mismatch.
                if (response.statusCode() != 200) {
                    log.warn("SearchAPI biography request returned HTTP {}", response.statusCode());
                    return BioObservation.unavailable();
                }
                byte[] bytes = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) {
                    log.warn("SearchAPI biography response exceeded size limit");
                    return BioObservation.unavailable();
                }
                return parse(bytes);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("SearchAPI biography request was interrupted");
            return BioObservation.unavailable();
        } catch (IOException | RuntimeException exception) {
            log.warn("SearchAPI biography request failed: {}", exception.getClass().getSimpleName());
            return BioObservation.unavailable();
        }
    }

    private BioObservation parse(byte[] bytes) {
        JsonNode root = mapper.readTree(bytes);
        JsonNode metadata = root == null ? null : root.get("search_metadata");
        String createdAt = text(metadata, "created_at");
        String searchStatus = text(metadata, "status");
        if (createdAt == null || (searchStatus != null && !"Success".equals(searchStatus))) {
            log.warn("SearchAPI biography response had missing or unsuccessful metadata");
            return BioObservation.unavailable();
        }
        Instant observedAt = Instant.parse(createdAt);
        JsonNode profile = root.get("profile");
        if (profile == null || !profile.isObject()) {
            log.warn("SearchAPI biography response had no profile object");
            return BioObservation.unavailable();
        }
        String username = text(profile, "username");
        String biography = text(profile, "bio");
        if (username == null) {
            log.warn("SearchAPI biography response had no username");
            return BioObservation.unavailable();
        }
        if (biography == null) {
            JsonNode privateFlag = profile.get("is_private");
            log.warn("SearchAPI biography response had no bio field; private={}",
                    privateFlag != null && privateFlag.isBoolean() && privateFlag.asBoolean());
            return privateFlag != null && privateFlag.isBoolean() && privateFlag.asBoolean()
                    ? new BioObservation(BioObservation.Status.INACCESSIBLE, null, null, username,
                            "SEARCHAPI_INSTAGRAM_PROFILE", "THIRD_PARTY_OBSERVATION", observedAt)
                    : BioObservation.unavailable();
        }
        if (biography.length() > 4096) {
            log.warn("SearchAPI biography response exceeded biography length limit");
            return BioObservation.unavailable();
        }
        return new BioObservation(BioObservation.Status.AVAILABLE, biography, null, username,
                "SEARCHAPI_INSTAGRAM_PROFILE", "THIRD_PARTY_OBSERVATION", observedAt);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
