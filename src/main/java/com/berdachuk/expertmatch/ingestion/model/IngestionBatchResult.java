package com.berdachuk.expertmatch.ingestion.model;

/**
 * Result of ingesting a single batch from external database.
 */
public record IngestionBatchResult(
        int processedInBatch,
        int successCount,
        int errorCount,
        long nextOffset,
        boolean hasMore
) {
}
