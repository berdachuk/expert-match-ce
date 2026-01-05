package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Expert evaluation result using Cascade pattern.
 * Forces LLM to follow predefined reasoning steps in sequence.
 */
public record ExpertEvaluation(
        @JsonProperty("expertSummary")
        String expertSummary,

        @JsonProperty("skillMatchAnalysis")
        SkillMatchAnalysis skillMatchAnalysis,

        @JsonProperty("experienceAssessment")
        ExperienceAssessment experienceAssessment,

        @JsonProperty("recommendation")
        Recommendation recommendation
) {
    public ExpertEvaluation {
        if (expertSummary == null) {
            expertSummary = "";
        }
    }
}

