package com.berdachuk.expertmatch.ingestion.service;

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
     * Ingests work experience records starting from a specific message offset.
     *
     * @param fromOffset starting message offset
     * @param batchSize  batch size for processing records
     * @return ingestion result
     */
    IngestionResult ingestFromOffset(long fromOffset, int batchSize);
}
