package com.berdachuk.expertmatch.core.exception;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Hidden from OpenAPI documentation to avoid SpringDoc compatibility issues.
 */
@Slf4j
@Hidden
@org.springframework.core.annotation.Order(1) // Higher priority for REST API exceptions
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ExpertMatch exceptions.
     */
    @ExceptionHandler(ExpertMatchException.class)
    public ResponseEntity<ErrorResponse> handleExpertMatchException(ExpertMatchException ex) {
        HttpStatus status = determineHttpStatus(ex);

        ErrorResponse error = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                Instant.now()
        );

        if (ex instanceof ValidationException validationEx) {
            error = new ErrorResponse(
                    ex.getErrorCode(),
                    ex.getMessage(),
                    Instant.now(),
                    validationEx.getValidationErrors()
            );
        }

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Handles resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles validation exceptions from Spring validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                Instant.now(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles illegal state exceptions (e.g., service not configured).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(
                "SERVICE_UNAVAILABLE",
                ex.getMessage(),
                Instant.now()
        );
        log.error("Service unavailable: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles HTTP message conversion exceptions (JSON serialization/deserialization errors).
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageConversionException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageConversionException(
            org.springframework.http.converter.HttpMessageConversionException ex) {
        ErrorResponse error = new ErrorResponse(
                "JSON_CONVERSION_ERROR",
                "Error processing JSON data: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()),
                Instant.now()
        );
        log.error("JSON conversion error", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles Spring AI exceptions (LLM API errors).
     */
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<ErrorResponse> handleNonTransientAiException(NonTransientAiException ex) {
        String errorMessage = "LLM API error: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " (Cause: " + ex.getCause().getMessage() + ")";
        }

        ErrorResponse error = new ErrorResponse(
                "LLM_API_ERROR",
                errorMessage,
                Instant.now()
        );

        log.error("LLM API error occurred", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles resource access exceptions (network/connection errors during LLM API calls).
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex) {
        String errorMessage = "LLM API connection error: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " (Cause: " + ex.getCause().getMessage() + ")";
        }

        ErrorResponse error = new ErrorResponse(
                "LLM_API_ERROR",
                errorMessage,
                Instant.now()
        );

        log.error("LLM API connection error occurred", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handles all other exceptions.
     * Also checks for wrapped NonTransientAiException in the cause chain.
     * Only handles REST API exceptions (not web controller exceptions).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // Only handle exceptions from API requests (paths starting with /api/)
        boolean isApiRequest = false;
        if (request instanceof ServletWebRequest servletRequest) {
            String requestPath = servletRequest.getRequest().getRequestURI();
            isApiRequest = requestPath != null && requestPath.startsWith("/api/");

            // Also check Accept header - if it's HTML, it's a web request
            String acceptHeader = servletRequest.getRequest().getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_HTML_VALUE) && !isApiRequest) {
                // This is a web request, don't handle it here
                log.debug("Skipping web request error in REST exception handler: {}", ex.getMessage());
                throw new RuntimeException("Delegating to WebExceptionHandler", ex);
            }
        }

        // Don't handle Thymeleaf template errors - let WebExceptionHandler handle them
        String exClassName = ex.getClass().getName().toLowerCase();
        String exMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (exClassName.contains("thymeleaf") ||
                exClassName.contains("template") ||
                exMessage.contains("template") ||
                exMessage.contains("springel expression")) {
            // This is a template error, let WebExceptionHandler handle it
            log.debug("Skipping template error in REST exception handler: {}", ex.getMessage());
            throw new RuntimeException("Delegating to WebExceptionHandler", ex);
        }

        // Only handle if it's an API request
        if (!isApiRequest) {
            log.debug("Skipping non-API request error in REST exception handler: {}", ex.getMessage());
            throw new RuntimeException("Delegating to WebExceptionHandler", ex);
        }

        // Check if the exception or its cause is a NonTransientAiException or ResourceAccessException
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof NonTransientAiException aiEx) {
                return handleNonTransientAiException(aiEx);
            }
            if (cause instanceof ResourceAccessException raEx) {
                return handleResourceAccessException(raEx);
            }
            cause = cause.getCause();
        }

        ErrorResponse error = new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                Instant.now()
        );

        log.error("Unexpected error occurred in API request", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Determines HTTP status based on exception type.
     */
    private HttpStatus determineHttpStatus(ExpertMatchException ex) {
        if (ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (ex instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        } else if (ex instanceof RetrievalException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Error response DTO.
     */
    public record ErrorResponse(
            String errorCode,
            String message,
            Instant timestamp,
            List<String> details
    ) {
        public ErrorResponse(String errorCode, String message, Instant timestamp) {
            this(errorCode, message, timestamp, null);
        }
    }
}

