package com.berdachuk.expertmatch.technology.repository;

import com.berdachuk.expertmatch.technology.domain.Technology;

import java.util.Optional;

/**
 * Repository interface for technology operations.
 */
public interface TechnologyRepository {

    /**
     * Creates or updates a technology.
     * Uses ON CONFLICT on name to handle duplicates gracefully.
     *
     * @param technology Technology entity to create/update
     * @return The technology ID
     */
    String createOrUpdate(Technology technology);

    /**
     * Finds a technology by name.
     *
     * @param name The technology name
     * @return Optional containing the technology if found, empty otherwise
     */
    Optional<Technology> findByName(String name);

    /**
     * Deletes all technology records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
