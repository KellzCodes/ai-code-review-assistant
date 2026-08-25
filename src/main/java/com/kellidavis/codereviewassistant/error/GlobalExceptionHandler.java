package com.kellidavis.codereviewassistant.error;

import com.kellidavis.codereviewassistant.github.GitHubApiException;
import com.kellidavis.codereviewassistant.github.GitHubWebhookErrorResponse;
import com.kellidavis.codereviewassistant.github.InvalidGitHubWebhookPayloadException;
import com.kellidavis.codereviewassistant.github.InvalidGitHubWebhookSignatureException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existingMessage, newMessage) -> existingMessage + "; " + newMessage));

        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidGitHubWebhookSignatureException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGitHubWebhookSignature(
            InvalidGitHubWebhookSignatureException ex
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintValidationExceptions(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (existingMessage, incomingMessage) ->
                                existingMessage + "; " + incomingMessage
                ));

        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InvalidGitHubWebhookPayloadException.class)
    public ResponseEntity<GitHubWebhookErrorResponse> handleInvalidGitHubWebhookPayload(
            InvalidGitHubWebhookPayloadException ex
    ) {
        GitHubWebhookErrorResponse response = new GitHubWebhookErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<GitHubWebhookErrorResponse> handleGitHubApiException(
            GitHubApiException ex
    ) {
        GitHubWebhookErrorResponse response = new GitHubWebhookErrorResponse(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }
}