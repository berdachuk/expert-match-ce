package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Recommendation for expert evaluation (Cascade pattern).
 */
public record Recommendation(
        @JsonProperty("recommendationType")
        RecommendationType recommendationType,

        @JsonProperty("confidenceScore")
        @Min(0)
        @Max(100)
        int confidenceScore,

        @JsonProperty("rationale")
        String rationale
) {
    public Recommendation {
        if (rationale == null) {
            rationale = "";
        }
    }
}

