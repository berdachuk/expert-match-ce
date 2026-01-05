package com.berdachuk.expertmatch.exception;

/**
 * Exception thrown when retrieval operations fail.
 */
public class RetrievalException extends ExpertMatchException {

    public RetrievalException(String errorCode, String message) {
        super(errorCode, message);
    }

    public RetrievalException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public RetrievalException(String message) {
        super("RETRIEVAL_ERROR", message);
    }

    public RetrievalException(String message, Throwable cause) {
        super("RETRIEVAL_ERROR", message, cause);
    }
}

