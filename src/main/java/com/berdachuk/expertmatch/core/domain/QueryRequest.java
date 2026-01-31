package com.berdachuk.expertmatch.core.domain;

import com.berdachuk.expertmatch.core.validation.ValidSGRPatternCombination;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Query request DTO with validation annotations.
 */
@ValidSGRPatternCombination
@Schema(description = "Request for processing a natural language query for expert discovery")
public record QueryRequest(
        @Schema(description = "Natural language query describing expert requirements", example = "Looking for experts in Java, Spring Boot, and AWS", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 5000)
        @NotBlank(message = "Query cannot be empty")
        @Size(max = 5000, message = "Query must not exceed 5000 characters")
        String query,

        @Schema(description = "Optional chat ID for conversation context (24-character hexadecimal string)", example = "507f1f77bcf86cd799439011", pattern = "^[0-9a-fA-F]{24}$")
        @Size(min = 24, max = 24, message = "Chat ID must be 24 characters")
        @JsonProperty("chatId")
        String chatId,

        @Schema(description = "Query processing options")
        @Valid
        QueryOptions options
) {
    public QueryRequest {
        if (options == null) {
            options = QueryOptions.DEFAULT;
        }
    }

    /**
     * Creates a QueryRequest with minimal required fields.
     */
    public static QueryRequest of(String query) {
        return new QueryRequest(query, null, QueryOptions.DEFAULT);
    }

    /**
     * Creates a QueryRequest with query and options.
     */
    public static QueryRequest of(String query, QueryOptions options) {
        return new QueryRequest(query, null, options);
    }

    /**
     * Creates a QueryRequest with all fields.
     */
    public static QueryRequest of(String query, String chatId, QueryOptions options) {
        return new QueryRequest(query, chatId, options);
    }
}