package com.motaamneh.mstsocialmvp.verification.domain;

public class InvalidInstagramHandleException extends IllegalArgumentException {
    public InvalidInstagramHandleException() {
        super("Invalid Instagram username");
    }
}
