package com.berdachuk.expertmatch.core.exception;

import java.util.List;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends ExpertMatchException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.validationErrors = List.of(message);
    }

    public ValidationException(List<String> validationErrors) {
        super("VALIDATION_ERROR", "Validation failed: " + String.join(", ", validationErrors));
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}

