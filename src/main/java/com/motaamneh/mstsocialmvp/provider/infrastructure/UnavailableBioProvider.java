package com.motaamneh.mstsocialmvp.provider.infrastructure;

import com.motaamneh.mstsocialmvp.provider.application.BioObservation;
import com.motaamneh.mstsocialmvp.provider.application.BioProvider;
import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@ConditionalOnProperty(name = "mst.instagram-provider.mode", havingValue = "disabled", matchIfMissing = true)
public class UnavailableBioProvider implements BioProvider {
    @Override
    public BioObservation observe(InstagramHandle handle) {
        return BioObservation.unavailable();
    }
}
