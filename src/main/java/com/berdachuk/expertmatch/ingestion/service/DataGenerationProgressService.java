package com.berdachuk.expertmatch.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory service for tracking data generation progress.
 * Used by the admin Test data page and Ingest page for progress bar and execution trace.
 */
@Slf4j
@Service
public class DataGenerationProgressService {

    private final Map<String, DataGenerationProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * Creates a new progress tracker for a job.
     *
     * @param jobId unique job identifier
     * @return progress tracker
     */
    public DataGenerationProgress createProgress(String jobId) {
        DataGenerationProgress progress = new DataGenerationProgress(jobId);
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
    public DataGenerationProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    /**
     * Cancels a running job.
     *
     * @param jobId job identifier
     * @return true if job was cancelled, false if not found or already completed
     */
    public boolean cancelJob(String jobId) {
        DataGenerationProgress progress = progressMap.get(jobId);
        if (progress != null && "running".equals(progress.getStatus())) {
            progress.cancel();
            log.info("Cancelled job: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * Returns all known jobs (newest first).
     *
     * @return list of progress trackers
     */
    public List<DataGenerationProgress> listJobs() {
        List<DataGenerationProgress> list = new ArrayList<>(progressMap.values());
        list.sort(Comparator.comparing(DataGenerationProgress::getStartTime, (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return b.compareTo(a);
        }));
        return list;
    }
}
