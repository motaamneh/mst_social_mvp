package com.motaamneh.mstsocialmvp.verification.api;

import com.motaamneh.mstsocialmvp.verification.application.ChallengeNotFoundException;
import com.motaamneh.mstsocialmvp.verification.application.VerificationOperationException;
import com.motaamneh.mstsocialmvp.verification.domain.InvalidInstagramHandleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class VerificationProblemHandler {
    @ExceptionHandler(ChallengeNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound() {
        return problem(HttpStatus.NOT_FOUND, "CHALLENGE_NOT_FOUND", "Verification challenge not found");
    }

    @ExceptionHandler(InvalidInstagramHandleException.class)
    ResponseEntity<ProblemDetail> invalidHandle() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_INSTAGRAM_HANDLE", "Invalid Instagram username");
    }

    @ExceptionHandler(VerificationOperationException.class)
    ResponseEntity<ProblemDetail> invalidState(VerificationOperationException exception) {
        HttpStatus status = "CHALLENGE_EXPIRED".equals(exception.code()) ? HttpStatus.GONE : HttpStatus.CONFLICT;
        return problem(status, exception.code(), "Verification cannot be attempted in its current state");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> invalidRequest() {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid verification request");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, title);
        detail.setTitle(title);
        detail.setProperty("code", code);
        return ResponseEntity.status(status).body(detail);
    }
}
