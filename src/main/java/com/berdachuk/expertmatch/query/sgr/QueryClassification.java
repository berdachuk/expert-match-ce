package com.berdachuk.expertmatch.query.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * Query classification result using Routing pattern.
 * Forces LLM to explicitly choose one reasoning path.
 */
public record QueryClassification(
        @JsonProperty("intent")
        QueryIntent intent,

        @JsonProperty("confidence")
        @Min(0)
        @Max(100)
        int confidence,

        @JsonProperty("reasoning")
        String reasoning,

        @JsonProperty("extractedRequirements")
        Map<String, Object> extractedRequirements
) {
    public QueryClassification {
        if (reasoning == null) {
            reasoning = "";
        }
        if (extractedRequirements == null) {
            extractedRequirements = Map.of();
        }
    }
}

