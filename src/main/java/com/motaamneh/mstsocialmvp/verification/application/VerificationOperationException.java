package com.motaamneh.mstsocialmvp.verification.application;

public class VerificationOperationException extends RuntimeException {
    private final String code;

    public VerificationOperationException(String code) {
        super(code);
        this.code = code;
    }

    public String code() { return code; }
}
