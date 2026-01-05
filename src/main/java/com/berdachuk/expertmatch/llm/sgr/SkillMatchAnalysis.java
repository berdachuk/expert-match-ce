package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Skill match analysis for expert evaluation (Cascade pattern).
 */
public record SkillMatchAnalysis(
        @JsonProperty("mustHaveMatchScore")
        @Min(0)
        @Max(10)
        int mustHaveMatchScore,

        @JsonProperty("niceToHaveMatchScore")
        @Min(0)
        @Max(10)
        int niceToHaveMatchScore,

        @JsonProperty("missingSkills")
        List<String> missingSkills,

        @JsonProperty("strengthSkills")
        List<String> strengthSkills
) {
    public SkillMatchAnalysis {
        if (missingSkills == null) {
            missingSkills = List.of();
        }
        if (strengthSkills == null) {
            strengthSkills = List.of();
        }
    }
}

