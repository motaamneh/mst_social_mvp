package com.motaamneh.mstsocialmvp.verification.api;

import com.motaamneh.mstsocialmvp.verification.application.VerificationService;
import com.motaamneh.mstsocialmvp.verification.application.VerificationOutcome;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationController {
    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateVerificationResponse> create(@Valid @RequestBody CreateVerificationRequest request,
                                                              Authentication authentication) {
        UUID tenantId = (UUID) authentication.getPrincipal();
        var result = CreateVerificationResponse.from(
                service.create(tenantId, request.getSubjectId(), request.getUsername()));
        return ResponseEntity.created(URI.create("/api/v1/verifications/" + result.verificationId())).body(result);
    }

    @GetMapping("/{verificationId}")
    public VerificationStatusResponse status(@PathVariable UUID verificationId, Authentication authentication) {
        UUID tenantId = (UUID) authentication.getPrincipal();
        return VerificationStatusResponse.from(service.getStatus(tenantId, verificationId));
    }

    @PostMapping("/{verificationId}/verify")
    public ResponseEntity<VerifyResponse> verify(@PathVariable UUID verificationId, Authentication authentication) {
        UUID tenantId = (UUID) authentication.getPrincipal();
        VerificationOutcome outcome = service.verify(tenantId, verificationId);
        HttpStatus httpStatus = "PROVIDER_UNAVAILABLE".equals(outcome.result())
                ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
        return ResponseEntity.status(httpStatus).body(VerifyResponse.from(outcome));
    }
}
