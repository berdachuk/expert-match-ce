package com.berdachuk.expertmatch.ingestion.repository;

import java.util.List;
import java.util.Map;

/**
 * Repository for reading work experience data from external source database.
 * <p>
 * IMPORTANT: This repository is ONLY for reading from the external source database.
 * It uses a separate DataSource (externalDataSource) and should never be confused
 * with the internal WorkExperienceRepository used for the application database.
 */
public interface ExternalWorkExperienceRepository {

    /**
     * Counts total records in work_experience_json table.
     *
     * @return total count
     */
    long countAll();

    /**
     * Finds all work experience records with pagination.
     *
     * @param offset offset for pagination
     * @param limit  maximum number of records to return
     * @return list of work experience records as maps
     */
    List<Map<String, Object>> findAll(int offset, int limit);

    /**
     * Finds work experience records starting from a specific message offset.
     *
     * @param fromOffset starting message offset
     * @param limit      maximum number of records to return
     * @return list of work experience records as maps
     */
    List<Map<String, Object>> findFromOffset(long fromOffset, int limit);
}
