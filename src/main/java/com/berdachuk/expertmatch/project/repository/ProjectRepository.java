package com.berdachuk.expertmatch.project.repository;

import com.berdachuk.expertmatch.project.domain.Project;

import java.util.Optional;

/**
 * Repository interface for project operations.
 */
public interface ProjectRepository {

    /**
     * Creates or updates a project.
     * Uses ON CONFLICT to handle duplicate IDs gracefully.
     *
     * @param project Project entity to create/update
     * @return The project ID
     */
    String createOrUpdate(Project project);

    /**
     * Finds a project by ID.
     *
     * @param projectId The unique identifier of the project
     * @return Optional containing the project if found, empty otherwise
     */
    Optional<Project> findById(String projectId);

    /**
     * Finds project ID by name (for lookup during ingestion).
     * Uses partial matching to find projects with similar names.
     *
     * @param projectName The project name to search for
     * @return Optional containing the project ID if found, empty otherwise
     */
    Optional<String> findIdByName(String projectName);

    /**
     * Returns the total count of project records.
     *
     * @return total count
     */
    long count();

    /**
     * Deletes all project records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
