package com.berdachuk.expertmatch.workexperience.repository;

import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * Repository interface for workexperience operations.
 */
public interface WorkExperienceRepository {

    /**
     * Finds all work experience records for an employee.
     *
     * @param employeeId The unique identifier of the employee
     * @return List of work experience records, empty list if none found
     */
    List<WorkExperience> findByEmployeeId(String employeeId);

    /**
     * Finds work experience records for multiple employees.
     * Uses batch loading to efficiently retrieve data for multiple employees.
     *
     * @param employeeIds List of employee unique identifiers
     * @return Map of employee ID to list of work experience records, empty map if none found
     */
    Map<String, List<WorkExperience>> findByEmployeeIds(List<String> employeeIds);

    /**
     * Finds employee identifiers who have experience with the specified technologies.
     *
     * @param technologies List of technology names to search for
     * @return List of employee identifiers who have experience with any of the specified technologies, empty list if none found
     */
    List<String> findEmployeeIdsByTechnologies(List<String> technologies);

    /**
     * Creates or updates a work experience record.
     * Uses ON CONFLICT to handle duplicate IDs gracefully.
     *
     * @param workExperience WorkExperience entity to create/update
     * @param metadata       Optional metadata JSON string (can be null)
     * @return The work experience ID
     */
    String createOrUpdate(WorkExperience workExperience, String metadata);

    /**
     * Checks if a work experience record exists for the given criteria.
     *
     * @param employeeId  Employee ID
     * @param projectName Project name
     * @param startDate   Start date
     * @return true if work experience exists, false otherwise
     */
    boolean exists(String employeeId, String projectName, LocalDate startDate);

    /**
     * Finds work experience records that don't have embeddings.
     *
     * @return List of work experience records without embeddings
     */
    List<WorkExperience> findWithoutEmbeddings();

    /**
     * Updates the embedding for a work experience record.
     *
     * @param workExpId Work experience ID
     * @param embedding Embedding vector
     * @param dimension Embedding dimension (1024 or 1536)
     */
    void updateEmbedding(String workExpId, List<Double> embedding, int dimension);

    /**
     * Deletes all work experience records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();

}
