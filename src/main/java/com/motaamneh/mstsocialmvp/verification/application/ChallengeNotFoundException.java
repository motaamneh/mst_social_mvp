package com.motaamneh.mstsocialmvp.verification.application;

public class ChallengeNotFoundException extends RuntimeException {
    public ChallengeNotFoundException() {
        super("Verification challenge not found");
    }
}
