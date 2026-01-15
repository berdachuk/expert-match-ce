package com.berdachuk.expertmatch.technology.repository;

import com.berdachuk.expertmatch.technology.domain.Technology;

import java.util.List;
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
     * Finds a technology by normalized name.
     *
     * @param normalizedName The normalized technology name
     * @return Optional containing the technology if found, empty otherwise
     */
    Optional<Technology> findByNormalizedName(String normalizedName);

    /**
     * Finds technologies that have the given synonym.
     *
     * @param synonym The synonym to search for
     * @return List of technologies with matching synonyms
     */
    List<Technology> findBySynonym(String synonym);

    /**
     * Finds all technologies.
     *
     * @return List of all technologies
     */
    List<Technology> findAll();

    /**
     * Deletes all technology records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
