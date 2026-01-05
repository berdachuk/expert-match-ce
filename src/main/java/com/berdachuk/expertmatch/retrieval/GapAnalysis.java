package com.berdachuk.expertmatch.retrieval;

import java.util.List;

/**
 * Represents gap analysis results from initial retrieval.
 * Identifies information gaps, ambiguities, and missing context that may require expanded retrieval.
 */
public record GapAnalysis(
        /**
         * List of identified information gaps - what information is missing from initial results.
         */
        List<String> identifiedGaps,

        /**
         * List of ambiguities in the query or requirements that need clarification.
         */
        List<String> ambiguities,

        /**
         * List of missing context or information that would improve expert matching.
         */
        List<String> missingInformation,

        /**
         * Whether the search should be expanded based on identified gaps.
         */
        boolean needsExpansion
) {
    /**
     * Creates an empty gap analysis indicating no expansion is needed.
     */
    public static GapAnalysis noExpansionNeeded() {
        return new GapAnalysis(List.of(), List.of(), List.of(), false);
    }

    /**
     * Checks if there are any significant gaps that warrant expansion.
     */
    public boolean hasSignificantGaps() {
        return needsExpansion &&
                (!identifiedGaps.isEmpty() || !ambiguities.isEmpty() || !missingInformation.isEmpty());
    }
}

