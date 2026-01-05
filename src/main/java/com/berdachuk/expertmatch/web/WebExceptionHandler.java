package com.berdachuk.expertmatch.web;

import com.berdachuk.expertmatch.exception.ExpertMatchException;
import com.berdachuk.expertmatch.exception.ResourceNotFoundException;
import com.berdachuk.expertmatch.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.util.ArrayList;

/**
 * Global exception handler for Thymeleaf views.
 * Handles exceptions that occur in web controllers and renders error messages in the view.
 * Handles template rendering errors from any controller.
 * Higher priority than GlobalExceptionHandler to handle template errors first.
 * Skips API requests (paths starting with /api/) to let GlobalExceptionHandler handle them.
 */
@Slf4j
@Order(2) // Lower priority - let GlobalExceptionHandler handle REST API exceptions first
@ControllerAdvice
public class WebExceptionHandler {

    /**
     * Handles ExpertMatch exceptions in web views.
     * Skips API requests to let GlobalExceptionHandler handle them.
     */
    @ExceptionHandler(ExpertMatchException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleExpertMatchException(ExpertMatchException ex, Model model, WebRequest request) {
        // Skip API requests - let GlobalExceptionHandler handle them
        if (isApiRequest(request)) {
            log.debug("Skipping API request exception in web handler, delegating to GlobalExceptionHandler: {}", ex.getMessage());
            // Re-throw to let GlobalExceptionHandler (with higher priority) handle it
            throw ex;
        }

        log.error("ExpertMatch exception in web view", ex);

        String errorMessage = ex.getMessage();
        if (ex instanceof ResourceNotFoundException) {
            errorMessage = "Resource not found: " + errorMessage;
        } else if (ex instanceof ValidationException validationEx) {
            errorMessage = "Validation error: " + errorMessage;
            if (validationEx.getValidationErrors() != null && !validationEx.getValidationErrors().isEmpty()) {
                errorMessage += " (" + String.join(", ", validationEx.getValidationErrors()) + ")";
            }
        }

        model.addAttribute("error", errorMessage);
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles Spring AI exceptions (LLM API errors) in web views.
     * Skips API requests to let GlobalExceptionHandler handle them.
     */
    @ExceptionHandler(NonTransientAiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleNonTransientAiException(NonTransientAiException ex, Model model, WebRequest request) {
        // Skip API requests - let GlobalExceptionHandler handle them
        if (isApiRequest(request)) {
            log.debug("Skipping API request exception in web handler, delegating to GlobalExceptionHandler: {}", ex.getMessage());
            // Re-throw to let GlobalExceptionHandler (with higher priority) handle it
            throw ex;
        }

        log.error("LLM API error in web view", ex);

        String errorMessage = "LLM API error: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " (Cause: " + ex.getCause().getMessage() + ")";
        }

        model.addAttribute("error", errorMessage);
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles REST client exceptions in web views.
     */
    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleRestClientException(RestClientException ex, Model model) {
        log.error("REST client error in web view", ex);

        String errorMessage = "Error communicating with API: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " (Cause: " + ex.getCause().getMessage() + ")";
        }

        model.addAttribute("error", errorMessage);
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles illegal argument exceptions in web views.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex, Model model) {
        log.error("Illegal argument in web view", ex);

        model.addAttribute("error", "Invalid request: " + ex.getMessage());
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles illegal state exceptions in web views.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String handleIllegalStateException(IllegalStateException ex, Model model) {
        log.error("Illegal state in web view", ex);

        model.addAttribute("error", "Service unavailable: " + ex.getMessage());
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles Thymeleaf template processing errors.
     */
    @ExceptionHandler({TemplateInputException.class, TemplateProcessingException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleTemplateException(Exception ex, Model model) {
        log.error("Thymeleaf template error in web view", ex);

        String errorMessage = "Template rendering error: " + ex.getMessage();
        if (ex.getCause() != null) {
            errorMessage += " (Cause: " + ex.getCause().getMessage() + ")";
        }

        model.addAttribute("error", errorMessage);
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Handles all other exceptions in web views.
     * Also checks for wrapped NonTransientAiException in the cause chain.
     * Skips API requests to let GlobalExceptionHandler handle them.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model, WebRequest request) {
        // Skip API requests - let GlobalExceptionHandler handle them
        if (isApiRequest(request)) {
            log.debug("Skipping API request exception in web handler, delegating to GlobalExceptionHandler: {}", ex.getMessage());
            // Re-throw to let GlobalExceptionHandler (with higher priority) handle it
            throw new RuntimeException("Delegating to GlobalExceptionHandler", ex);
        }

        // Check if the exception or its cause is a NonTransientAiException
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof NonTransientAiException aiEx) {
                return handleNonTransientAiException(aiEx, model, request);
            }
            cause = cause.getCause();
        }

        log.error("Unexpected error in web view", ex);

        String errorMessage = "An unexpected error occurred";
        if (ex.getMessage() != null) {
            errorMessage += ": " + ex.getMessage();
        }

        model.addAttribute("error", errorMessage);
        model.addAttribute("chats", new ArrayList<>());
        model.addAttribute("currentPage", "index");

        return "index";
    }

    /**
     * Checks if the request is an API request (path starts with /api/).
     */
    private boolean isApiRequest(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            String requestPath = servletRequest.getRequest().getRequestURI();
            if (requestPath != null && requestPath.startsWith("/api/")) {
                return true;
            }
            // Also check Accept header - if it's JSON, it's likely an API request
            String acceptHeader = servletRequest.getRequest().getHeader("Accept");
            return acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE);
        }
        return false;
    }
}

