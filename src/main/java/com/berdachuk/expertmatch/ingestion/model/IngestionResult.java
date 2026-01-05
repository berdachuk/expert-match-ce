package com.berdachuk.expertmatch.ingestion.model;

import java.util.List;

/**
 * Result of ingesting expert profiles from JSON files.
 */
public record IngestionResult(
        int totalProfiles,
        int successCount,
        int errorCount,
        List<ProcessingResult> results,
        String sourceName
) {
    /**
     * Creates an ingestion result.
     */
    public static IngestionResult of(int totalProfiles, int successCount, int errorCount,
                                     List<ProcessingResult> results, String sourceName) {
        return new IngestionResult(totalProfiles, successCount, errorCount, results, sourceName);
    }
}

