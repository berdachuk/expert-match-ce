package com.berdachuk.expertmatch.core.exception;

import lombok.Getter;

/**
 * Base exception for ExpertMatch application.
 */
@Getter
public class ExpertMatchException extends RuntimeException {

    private final String errorCode;

    public ExpertMatchException(String message) {
        super(message);
        this.errorCode = "EXPERT_MATCH_ERROR";
    }

    public ExpertMatchException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXPERT_MATCH_ERROR";
    }

    public ExpertMatchException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ExpertMatchException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

