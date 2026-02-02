package com.berdachuk.expertmatch.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs ingestion workflow (clear+ingest, embeddings, graph) in three separate
 * transactions so a failure in one phase does not leave the connection aborted
 * (SQL state 25P02) for the next. Called from IngestionAsyncService.
 */
@Slf4j
@Service
public class IngestionTransactionalRunner {

    private final IngestionPhaseRunner ingestionPhaseRunner;
    private final DataGenerationProgressService progressService;

    public IngestionTransactionalRunner(
            IngestionPhaseRunner ingestionPhaseRunner,
            DataGenerationProgressService progressService) {
        this.ingestionPhaseRunner = ingestionPhaseRunner;
        this.progressService = progressService;
    }

    /**
     * Runs clear+ingest, then embeddings, then graph build, each in its own transaction.
     * If a phase fails, the transaction for that phase rolls back and the error is reported;
     * later phases are not run in an aborted transaction.
     */
    public void runInTransaction(String jobId, int batch, boolean clearExisting) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null) {
            log.warn("Progress not found for job {}, skipping ingestion", jobId);
            return;
        }
        if (progress.isCancelled()) return;

        ingestionPhaseRunner.runPhase1ClearAndIngest(jobId, batch, clearExisting);
        if (progress.isCancelled()) return;

        ingestionPhaseRunner.runPhase2Embeddings(jobId);
        if (progress.isCancelled()) return;

        ingestionPhaseRunner.runPhase3Graph(jobId);
        if (progress.isCancelled()) return;

        progress.complete();
    }
}
