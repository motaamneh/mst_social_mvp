package com.motaamneh.mstsocialmvp.provider.application;

import com.motaamneh.mstsocialmvp.verification.domain.InstagramHandle;

public interface BioProvider {
    BioObservation observe(InstagramHandle handle);
}
