package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.ingestion.model.IngestionBatchResult;
import com.berdachuk.expertmatch.ingestion.model.IngestionResult;

/**
 * Service for ingesting work experience data from external database.
 */
public interface DatabaseIngestionService {

    /**
     * Ingests all work experience records from external database.
     *
     * @param batchSize batch size for processing records
     * @return ingestion result
     */
    IngestionResult ingestAll(int batchSize);

    /**
     * Ingests all work experience records with optional progress callback (per batch).
     *
     * @param batchSize batch size for processing records
     * @param callback  optional callback for granular progress; null to ignore
     * @return ingestion result
     */
    IngestionResult ingestAll(int batchSize, IngestProgressCallback callback);

    /**
     * Ingests one batch of work experience records and commits. Used for per-batch commits.
     *
     * @param fromOffset starting message offset
     * @param batchSize  batch size for processing records
     * @param callback   optional callback for progress; null to ignore
     * @return batch result with next offset and whether more data exists
     */
    IngestionBatchResult ingestOneBatch(long fromOffset, int batchSize, IngestProgressCallback callback);

    /**
     * Ingests work experience records starting from a specific message offset.
     *
     * @param fromOffset starting message offset
     * @param batchSize  batch size for processing records
     * @return ingestion result
     */
    IngestionResult ingestFromOffset(long fromOffset, int batchSize);
}

