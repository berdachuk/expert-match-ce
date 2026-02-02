package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.ingestion.model.IngestionBatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Runs a single ingest batch in its own transaction (REQUIRES_NEW) so each batch commits.
 */
@Slf4j
@Service
public class IngestionBatchTransaction {

    private final Optional<DatabaseIngestionService> databaseIngestionService;

    public IngestionBatchTransaction(Optional<DatabaseIngestionService> databaseIngestionService) {
        this.databaseIngestionService = databaseIngestionService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionBatchResult runOneBatch(long fromOffset, int batchSize, IngestProgressCallback callback) {
        DatabaseIngestionService service = databaseIngestionService.orElseThrow(
                () -> new IllegalStateException("External database ingestion is not enabled"));
        return service.ingestOneBatch(fromOffset, batchSize, callback);
    }
}
