package com.berdachuk.expertmatch.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory service for tracking test data generation progress.
 * Used by the admin Test data page for progress bar and execution trace.
 */
@Slf4j
@Service
public class TestDataGenerationProgressService {

    private final Map<String, TestDataGenerationProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * Creates a new progress tracker for a job.
     *
     * @param jobId unique job identifier
     * @return progress tracker
     */
    public TestDataGenerationProgress createProgress(String jobId) {
        TestDataGenerationProgress progress = new TestDataGenerationProgress(jobId);
        progressMap.put(jobId, progress);
        log.debug("Created progress tracker for job: {}", jobId);
        return progress;
    }

    /**
     * Gets progress for a job.
     *
     * @param jobId job identifier
     * @return progress tracker or null if not found
     */
    public TestDataGenerationProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    /**
     * Cancels a running job.
     *
     * @param jobId job identifier
     * @return true if job was cancelled, false if not found or already completed
     */
    public boolean cancelJob(String jobId) {
        TestDataGenerationProgress progress = progressMap.get(jobId);
        if (progress != null && "running".equals(progress.getStatus())) {
            progress.cancel();
            log.info("Cancelled job: {}", jobId);
            return true;
        }
        return false;
    }
}
