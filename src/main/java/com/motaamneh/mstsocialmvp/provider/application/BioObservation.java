package com.motaamneh.mstsocialmvp.provider.application;

import java.time.Instant;

public record BioObservation(Status status, String biography, String accountId, String canonicalHandle,
                             String evidenceMethod, String assuranceLevel, Instant observedAt) {
    public enum Status { AVAILABLE, NOT_FOUND, INACCESSIBLE, UNAVAILABLE }

    public static BioObservation unavailable() {
        return new BioObservation(Status.UNAVAILABLE, null, null, null, null, null, null);
    }
}
