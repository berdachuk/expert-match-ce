package com.berdachuk.expertmatch.ingestion.service;

/**
 * Callback for reporting progress during database ingestion (e.g. per batch).
 */
@FunctionalInterface
public interface IngestProgressCallback {

    /**
     * Called after each batch of employees is processed.
     *
     * @param totalProcessed total employees processed so far
     * @param batchIndex     zero-based batch index
     * @param message        progress message (e.g. "Processed 100 employees")
     */
    void onBatchProgress(int totalProcessed, int batchIndex, String message);
}
