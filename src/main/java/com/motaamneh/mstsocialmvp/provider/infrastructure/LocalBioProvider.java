package com.motaamneh.mstsocialmvp.provider.infrastructure;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.provider.application.BioProvider;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(name = "mst.instagram-provider.mode", havingValue = "fake")
public class LocalBioProvider implements BioProvider {
    private final Map<String, String> biographies = new ConcurrentHashMap<>();
    private final Clock clock;

    public LocalBioProvider(Clock clock) {
        this.clock = clock;
    }

    public void setBiography(InstagramHandle handle, String biography) {
        biographies.put(handle.value(), biography);
    }

    @Override
    public BioObservation observe(InstagramHandle handle) {
        String biography = biographies.get(handle.value());
        return biography == null
                ? new BioObservation(BioObservation.Status.NOT_FOUND, null, null, null,
                        "LOCAL_FAKE_BIO", "TEST_ONLY", clock.instant())
                : new BioObservation(BioObservation.Status.AVAILABLE, biography, null, handle.value(),
                        "LOCAL_FAKE_BIO", "TEST_ONLY", clock.instant());
    }
}
