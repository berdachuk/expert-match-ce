package com.berdachuk.expertmatch.query.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Execution trace data models for tracking query processing steps.
 */
public class ExecutionTrace {

    /**
     * Token usage information for LLM calls.
     */
    @Schema(description = "Token usage information for LLM calls")
    public record TokenUsage(
            @Schema(description = "Input/prompt tokens", example = "45")
            @JsonProperty("inputTokens")
            Integer inputTokens,

            @Schema(description = "Output/completion tokens", example = "12")
            @JsonProperty("outputTokens")
            Integer outputTokens,

            @Schema(description = "Total tokens (input + output)", example = "57")
            @JsonProperty("totalTokens")
            Integer totalTokens
    ) {
        /**
         * Creates a TokenUsage by summing two TokenUsage objects.
         */
        public static TokenUsage sum(TokenUsage a, TokenUsage b) {
            if (a == null && b == null) {
                return null;
            }
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            return new TokenUsage(
                    (a.inputTokens != null ? a.inputTokens : 0) + (b.inputTokens != null ? b.inputTokens : 0),
                    (a.outputTokens != null ? a.outputTokens : 0) + (b.outputTokens != null ? b.outputTokens : 0),
                    (a.totalTokens != null ? a.totalTokens : 0) + (b.totalTokens != null ? b.totalTokens : 0)
            );
        }
    }

    /**
     * Individual execution step with tracking information.
     */
    @Schema(description = "Individual execution step with tracking information")
    public record ExecutionStep(
            @Schema(description = "Step name", example = "Parse Query")
            @JsonProperty("name")
            String name,

            @Schema(description = "Service that executed the step", example = "QueryParser")
            @JsonProperty("service")
            String service,

            @Schema(description = "Method called", example = "parse")
            @JsonProperty("method")
            String method,

            @Schema(description = "Duration in milliseconds", example = "234")
            @JsonProperty("durationMs")
            Long durationMs,

            @Schema(description = "Step status", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED", "SKIPPED"})
            @JsonProperty("status")
            String status,

            @Schema(description = "Input summary", example = "Query: Find Java experts")
            @JsonProperty("inputSummary")
            String inputSummary,

            @Schema(description = "Output summary", example = "Intent: expert_search, Skills: [Java]")
            @JsonProperty("outputSummary")
            String outputSummary,

            @Schema(description = "LLM model used (null if no LLM)", example = "OllamaChatModel (qwen3:4b-instruct-2507-q4_K_M)")
            @JsonProperty("llmModel")
            String llmModel,

            @Schema(description = "Token usage (null if no LLM or usage not available)")
            @JsonProperty("tokenUsage")
            TokenUsage tokenUsage
    ) {
    }

    /**
     * Container for all execution steps with aggregated information.
     */
    @Schema(description = "Execution trace containing all processing steps")
    public record ExecutionTraceData(
            @Schema(description = "List of execution steps")
            @JsonProperty("steps")
            List<ExecutionStep> steps,

            @Schema(description = "Total processing duration in milliseconds", example = "2402")
            @JsonProperty("totalDurationMs")
            Long totalDurationMs,

            @Schema(description = "Aggregated token usage across all LLM steps")
            @JsonProperty("totalTokenUsage")
            TokenUsage totalTokenUsage
    ) {
    }
}

