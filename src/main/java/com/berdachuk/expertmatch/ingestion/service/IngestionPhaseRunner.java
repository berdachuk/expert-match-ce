package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.graph.service.GraphBuilderService;
import com.berdachuk.expertmatch.ingestion.model.IngestionBatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs each ingestion phase in its own transaction (REQUIRES_NEW) so a failure
 * in one phase does not leave the connection aborted (25P02) for the next phase.
 * Ingest phase commits after every batch.
 */
@Slf4j
@Service
public class IngestionPhaseRunner {

    private static final int INGEST_PROGRESS_MIN = 25;
    private static final int INGEST_PROGRESS_MAX = 60;

    private final IngestionBatchTransaction ingestionBatchTransaction;
    private final TestDataGenerator testDataGenerator;
    private final GraphBuilderService graphBuilderService;
    private final DataGenerationProgressService progressService;

    public IngestionPhaseRunner(
            IngestionBatchTransaction ingestionBatchTransaction,
            TestDataGenerator testDataGenerator,
            GraphBuilderService graphBuilderService,
            DataGenerationProgressService progressService) {
        this.ingestionBatchTransaction = ingestionBatchTransaction;
        this.testDataGenerator = testDataGenerator;
        this.graphBuilderService = graphBuilderService;
        this.progressService = progressService;
    }

    private static boolean callbackNeedsUpdate(int processedInBatch) {
        return processedInBatch > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhase1ClearAndIngest(String jobId, int batch, boolean clearExisting) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null || progress.isCancelled()) return;

        progress.updateProgress(5, "Clear", clearExisting ? "Clearing existing data..." : "Skipping clear (append mode)");
        if (clearExisting) {
            graphBuilderService.clearGraph();
            testDataGenerator.clearTestData();
        }
        if (progress.isCancelled()) return;

        progress.updateProgress(INGEST_PROGRESS_MIN, "Ingest", "Ingesting from external database...");
        int[] cumulativeProcessed = {0};
        int[] cumulativeSuccess = {0};
        int[] cumulativeErrors = {0};
        int[] batchIndex = {0};
        IngestProgressCallback ingestCallback = (processedInBatch, ignoredBatchIdx, message) -> {
            if (progress.isCancelled()) return;
            cumulativeProcessed[0] += processedInBatch;
            int pct = Math.min(INGEST_PROGRESS_MAX, INGEST_PROGRESS_MIN + batchIndex[0]);
            progress.updateProgress(pct, "Ingest", message);
        };

        long offset = 0L;
        while (true) {
            if (progress.isCancelled()) return;
            IngestionBatchResult result = ingestionBatchTransaction.runOneBatch(offset, batch, ingestCallback);
            cumulativeSuccess[0] += result.successCount();
            cumulativeErrors[0] += result.errorCount();
            batchIndex[0]++;
            if (callbackNeedsUpdate(result.processedInBatch())) {
                int pct = Math.min(INGEST_PROGRESS_MAX, INGEST_PROGRESS_MIN + batchIndex[0]);
                progress.updateProgress(pct, "Ingest", "Processed " + cumulativeProcessed[0] + " employees");
            }
            offset = result.nextOffset();
            if (!result.hasMore()) break;
        }

        progress.addTraceEntry("INFO", "Ingest", String.format("Ingested %d employees (success: %d, errors: %d)",
                cumulativeProcessed[0], cumulativeSuccess[0], cumulativeErrors[0]), progress.getProgress());

        if (cumulativeProcessed[0] == 0) {
            throw new IllegalStateException("No records ingested from external database. Check connection and that work_experience_json has data.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhase2Embeddings(String jobId) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null || progress.isCancelled()) return;

        progress.updateProgress(60, "Embeddings", "Generating vector embeddings for work experiences...");
        testDataGenerator.generateEmbeddings();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runPhase3Graph(String jobId) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null || progress.isCancelled()) return;

        progress.updateProgress(85, "Graph", "Building graph relationships in Apache AGE...");
        graphBuilderService.buildGraph();
    }
}
