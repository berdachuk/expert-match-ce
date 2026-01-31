package com.berdachuk.expertmatch.core.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Query options for expert search.
 */
@Schema(description = "Query options for expert search")
public record QueryOptions(
        @Schema(description = "Maximum number of results to return", example = "10")
        Integer maxResults,

        @Schema(description = "Minimum confidence threshold for matches (0.0 to 1.0)", example = "0.7")
        Double minConfidence,

        @Schema(description = "Whether to include detailed information about matched skills", example = "true")
        Boolean includeSkills,

        @Schema(description = "Whether to include detailed information about relevant projects", example = "true")
        Boolean includeProjects,

        @Schema(description = "Whether to include detailed information about work experience", example = "true")
        Boolean includeExperience,

        @Schema(description = "Whether to include language proficiency information", example = "true")
        Boolean includeLanguages,

        @Schema(description = "Whether to include information about data sources", example = "true")
        Boolean includeSources,

        @Schema(description = "Whether to include entities in the response", example = "false")
        Boolean includeEntities,

        @Schema(description = "Whether to include execution trace for debugging", example = "false")
        Boolean includeExecutionTrace,

        @Schema(description = "Whether to use reranking to improve result quality", example = "true")
        Boolean rerank,

        @Schema(description = "Whether to enable deep research mode", example = "false")
        Boolean deepResearch,

        @Schema(description = "Whether to use the cycle retrieval pattern", example = "true")
        Boolean useCyclePattern,

        @Schema(description = "Whether to use the cascade retrieval pattern", example = "true")
        Boolean useCascadePattern,

        @Schema(description = "Whether to use the routing pattern", example = "true")
        Boolean useRoutingPattern,

        @Schema(description = "Specific seniority levels to filter by", example = "[\"Senior\", \"Lead\"]")
        List<String> seniorityLevels,

        @Schema(description = "Specific languages to filter by", example = "[\"English\", \"Spanish\"]")
        List<String> languages,

        @Schema(description = "Whether to enable streaming responses", example = "false")
        Boolean stream,

        @Schema(description = "Context window size for streaming", example = "4096")
        Integer contextWindow,

        @Schema(description = "Maximum number of expert matches per result", example = "5")
        Integer maxExpertsPerResult,

        @Schema(description = "Whether to include match summary", example = "true")
        Boolean includeMatchSummary
) {
    /**
     * Default query options with sensible defaults.
     */
    public static final QueryOptions DEFAULT = new QueryOptions(
            10,           // maxResults
            0.7,          // minConfidence
            true,         // includeSkills
            true,         // includeProjects
            true,         // includeExperience
            true,         // includeLanguages
            true,         // includeSources
            false,        // includeEntities
            false,        // includeExecutionTrace
            true,         // rerank
            false,        // deepResearch
            true,         // useCyclePattern
            true,         // useCascadePattern
            true,         // useRoutingPattern
            null,         // seniorityLevels
            null,         // languages
            false,        // stream
            4096,         // contextWindow
            5,            // maxExpertsPerResult
            true          // includeMatchSummary
    );

    /**
     * Creates a builder for QueryOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for QueryOptions.
     */
    public static class Builder {
        private Integer maxResults = 10;
        private Double minConfidence = 0.7;
        private Boolean includeSkills = true;
        private Boolean includeProjects = true;
        private Boolean includeExperience = true;
        private Boolean includeLanguages = true;
        private Boolean includeSources = true;
        private Boolean includeEntities = false;
        private Boolean includeExecutionTrace = false;
        private Boolean rerank = true;
        private Boolean deepResearch = false;
        private Boolean useCyclePattern = true;
        private Boolean useCascadePattern = true;
        private Boolean useRoutingPattern = true;
        private List<String> seniorityLevels;
        private List<String> languages;
        private Boolean stream = false;
        private Integer contextWindow = 4096;
        private Integer maxExpertsPerResult = 5;
        private Boolean includeMatchSummary = true;

        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder minConfidence(Double minConfidence) {
            this.minConfidence = minConfidence;
            return this;
        }

        public Builder includeSkills(Boolean includeSkills) {
            this.includeSkills = includeSkills;
            return this;
        }

        public Builder includeProjects(Boolean includeProjects) {
            this.includeProjects = includeProjects;
            return this;
        }

        public Builder includeExperience(Boolean includeExperience) {
            this.includeExperience = includeExperience;
            return this;
        }

        public Builder includeLanguages(Boolean includeLanguages) {
            this.includeLanguages = includeLanguages;
            return this;
        }

        public Builder includeSources(Boolean includeSources) {
            this.includeSources = includeSources;
            return this;
        }

        public Builder includeEntities(Boolean includeEntities) {
            this.includeEntities = includeEntities;
            return this;
        }

        public Builder includeExecutionTrace(Boolean includeExecutionTrace) {
            this.includeExecutionTrace = includeExecutionTrace;
            return this;
        }

        public Builder rerank(Boolean rerank) {
            this.rerank = rerank;
            return this;
        }

        public Builder deepResearch(Boolean deepResearch) {
            this.deepResearch = deepResearch;
            return this;
        }

        public Builder useCyclePattern(Boolean useCyclePattern) {
            this.useCyclePattern = useCyclePattern;
            return this;
        }

        public Builder useCascadePattern(Boolean useCascadePattern) {
            this.useCascadePattern = useCascadePattern;
            return this;
        }

        public Builder useRoutingPattern(Boolean useRoutingPattern) {
            this.useRoutingPattern = useRoutingPattern;
            return this;
        }

        public Builder seniorityLevels(List<String> seniorityLevels) {
            this.seniorityLevels = seniorityLevels;
            return this;
        }

        public Builder languages(List<String> languages) {
            this.languages = languages;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder contextWindow(Integer contextWindow) {
            this.contextWindow = contextWindow;
            return this;
        }

        public Builder maxExpertsPerResult(Integer maxExpertsPerResult) {
            this.maxExpertsPerResult = maxExpertsPerResult;
            return this;
        }

        public Builder includeMatchSummary(Boolean includeMatchSummary) {
            this.includeMatchSummary = includeMatchSummary;
            return this;
        }

        public QueryOptions build() {
            return new QueryOptions(
                    maxResults,
                    minConfidence,
                    includeSkills,
                    includeProjects,
                    includeExperience,
                    includeLanguages,
                    includeSources,
                    includeEntities,
                    includeExecutionTrace,
                    rerank,
                    deepResearch,
                    useCyclePattern,
                    useCascadePattern,
                    useRoutingPattern,
                    seniorityLevels,
                    languages,
                    stream,
                    contextWindow,
                    maxExpertsPerResult,
                    includeMatchSummary
            );
        }
    }
}