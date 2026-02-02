package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.api.model.TestDataStatsResponse;

/**
 * Service for aggregating current test data statistics (counts).
 */
public interface TestDataStatisticsService {

    /**
     * Returns current counts for employees, work experiences, projects, and technologies.
     *
     * @return current data statistics
     */
    TestDataStatsResponse getStatistics();
}
