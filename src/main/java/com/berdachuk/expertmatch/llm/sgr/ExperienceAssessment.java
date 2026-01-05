package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Experience assessment for expert evaluation (Cascade pattern).
 */
public record ExperienceAssessment(
        @JsonProperty("relevantProjectsCount")
        int relevantProjectsCount,

        @JsonProperty("domainExperienceYears")
        double domainExperienceYears,

        @JsonProperty("customerIndustryMatch")
        boolean customerIndustryMatch,

        @JsonProperty("seniorityMatch")
        boolean seniorityMatch
) {
}

