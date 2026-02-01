package com.berdachuk.expertmatch.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs async ingestion (external DB + embeddings + graph) in a Spring-managed thread.
 * Delegates to IngestionTransactionalRunner so the full workflow runs in one transaction
 * and commits when the method completes (fixes data/embeddings/graph not persisting).
 */
@Slf4j
@Service
public class IngestionAsyncService {

    private final IngestionTransactionalRunner ingestionTransactionalRunner;
    private final DataGenerationProgressService progressService;

    public IngestionAsyncService(
            IngestionTransactionalRunner ingestionTransactionalRunner,
            DataGenerationProgressService progressService) {
        this.ingestionTransactionalRunner = ingestionTransactionalRunner;
        this.progressService = progressService;
    }

    /**
     * Schedules ingestion in async thread; the runner executes in a single transaction
     * so ingest, embeddings, and graph all commit together.
     */
    @Async
    public void runIngestionAsync(String jobId, int batch, boolean clearExisting) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null) {
            log.warn("Progress not found for job {}, skipping async ingestion", jobId);
            return;
        }
        try {
            ingestionTransactionalRunner.runInTransaction(jobId, batch, clearExisting);
        } catch (Exception e) {
            log.error("Ingestion from external database failed", e);
            progress.error(e.getMessage());
        }
    }
}
