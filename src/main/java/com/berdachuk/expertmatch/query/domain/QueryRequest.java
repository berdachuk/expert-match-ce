package com.berdachuk.expertmatch.query.domain;

import com.berdachuk.expertmatch.query.validation.ValidSGRPatternCombination;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Query request DTO.
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
            options = new QueryOptions(10, 0.7, true, true, true, false, false, false, false, false);
        }
    }

    /**
     * Query options.
     */
    @Schema(description = "Query processing options")
    public record QueryOptions(
            @Schema(description = "Maximum number of results to return (1-100)", example = "10", minimum = "1", maximum = "100")
            @JsonProperty("maxResults")
            Integer maxResults,

            @Schema(description = "Minimum confidence score (0.0-1.0)", example = "0.7", minimum = "0.0", maximum = "1.0")
            @JsonProperty("minConfidence")
            Double minConfidence,

            @Schema(description = "Include source citations in response", example = "true")
            @JsonProperty("includeSources")
            Boolean includeSources,

            @Schema(description = "Include extracted entities in response", example = "true")
            @JsonProperty("includeEntities")
            Boolean includeEntities,

            @Schema(description = "Enable semantic reranking", example = "true")
            @JsonProperty("rerank")
            Boolean rerank,

            @Schema(description = "Enable deep research SGR pattern", example = "false")
            @JsonProperty("deepResearch")
            Boolean deepResearch,

            @Schema(description = "Enable Cascade pattern for structured expert evaluation", example = "false")
            @JsonProperty("useCascadePattern")
            Boolean useCascadePattern,

            @Schema(description = "Enable Routing pattern for LLM-based query classification", example = "false")
            @JsonProperty("useRoutingPattern")
            Boolean useRoutingPattern,

            @Schema(description = "Enable Cycle pattern for multiple expert evaluations", example = "false")
            @JsonProperty("useCyclePattern")
            Boolean useCyclePattern,

            @Schema(description = "Include execution trace in response", example = "false")
            @JsonProperty("includeExecutionTrace")
            Boolean includeExecutionTrace
    ) {
        public QueryOptions {
            if (maxResults == null) maxResults = 10;
            if (minConfidence == null) minConfidence = 0.7;
            if (includeSources == null) includeSources = false;
            if (includeEntities == null) includeEntities = true;
            if (rerank == null) rerank = true;
            if (deepResearch == null) deepResearch = false;
            if (useCascadePattern == null) useCascadePattern = false;
            if (useRoutingPattern == null) useRoutingPattern = false;
            if (useCyclePattern == null) useCyclePattern = false;
            if (includeExecutionTrace == null) includeExecutionTrace = false;
        }
    }
}

