package com.berdachuk.expertmatch.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Query response DTO with structured expert data.
 */
@Schema(description = "Response containing expert recommendations, sources, entities, and processing metadata")
public record QueryResponse(
        @Schema(description = "Natural language answer with expert recommendations", example = "Based on your requirements, I found several experts...")
        @JsonProperty("answer")
        String answer,

        @Schema(description = "List of matched experts with detailed information")
        @JsonProperty("experts")
        List<ExpertMatch> experts,

        @Schema(description = "Source citations used in the answer")
        @JsonProperty("sources")
        List<Source> sources,

        @Schema(description = "Extracted entities from the query")
        @JsonProperty("entities")
        List<Entity> entities,

        @Schema(description = "Confidence score for the response (0.0-1.0)", example = "0.85")
        @JsonProperty("confidence")
        Double confidence,

        @Schema(description = "Unique query ID", example = "507f1f77bcf86cd799439012")
        @JsonProperty("queryId")
        String queryId,

        @Schema(description = "Chat ID used for context", example = "507f1f77bcf86cd799439011")
        @JsonProperty("chatId")
        String chatId,

        @Schema(description = "Message ID for this response", example = "507f1f77bcf86cd799439013")
        @JsonProperty("messageId")
        String messageId,

        @Schema(description = "Processing time in milliseconds", example = "1234")
        @JsonProperty("processingTimeMs")
        Long processingTimeMs,

        @Schema(description = "Match summary statistics")
        @JsonProperty("summary")
        MatchSummary summary,

        @Schema(description = "Step-by-step execution trace (only included if includeExecutionTrace=true)")
        @JsonProperty("executionTrace")
        ExecutionTrace.ExecutionTraceData executionTrace
) {
    /**
     * Expert match information.
     */
    public record ExpertMatch(
            String id,
            String name,
            String email,
            String seniority,
            LanguageProficiency language,
            SkillMatch skillMatch,
            MatchedSkills matchedSkills,
            List<RelevantProject> relevantProjects,
            Experience experience,
            Double relevanceScore,
            String availability
    ) {
    }

    /**
     * Skill match details.
     */
    public record SkillMatch(
            Integer mustHaveMatched,
            Integer mustHaveTotal,
            Integer niceToHaveMatched,
            Integer niceToHaveTotal,
            Double matchScore
    ) {
    }

    /**
     * Matched skills breakdown.
     */
    public record MatchedSkills(
            List<String> mustHave,
            List<String> niceToHave
    ) {
    }

    /**
     * Relevant project information.
     */
    public record RelevantProject(
            String name,
            List<String> technologies,
            String role,
            String duration
    ) {
    }

    /**
     * Experience indicators.
     */
    public record Experience(
            Boolean etlPipelines,
            Boolean highPerformanceServices,
            Boolean systemArchitecture,
            Boolean monitoring,
            Boolean onCall
    ) {
    }

    /**
     * Language proficiency.
     */
    public record LanguageProficiency(
            String english
    ) {
    }

    /**
     * Match summary statistics.
     */
    public record MatchSummary(
            Integer totalExpertsFound,
            Integer perfectMatches,
            Integer goodMatches,
            Integer partialMatches
    ) {
    }

    /**
     * Source citation.
     */
    public record Source(
            String type,
            String id,
            String title,
            Double relevanceScore,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Extracted entity.
     */
    public record Entity(
            String type,
            String name,
            String id
    ) {
    }
}