package com.motaamneh.mstsocialmvp.verification.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateVerificationRequest {
    @NotBlank
    @Size(max = 200)
    private String subjectId;

    @NotBlank
    @Size(max = 32)
    private String username;

    public CreateVerificationRequest() {
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
