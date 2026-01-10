package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.core.exception.RetrievalException;

import java.util.List;


/**
 * Service interface for graphsearch operations.
 */
public interface GraphSearchService {
    /**
     * Finds expert identifiers who have experience with a specific technology.
     *
     * @param technology The technology name to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByTechnology(String technology);

    /**
     * Finds expert identifiers who have collaborated with a specific expert.
     *
     * @param expertId The unique identifier of the expert
     * @return List of collaborating expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findCollaboratingExperts(String expertId);

    /**
     * Finds expert identifiers who have experience in a specific domain.
     *
     * @param domain The domain name to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByDomain(String domain);

    /**
     * Finds expert identifiers who have experience with any of the specified technologies.
     *
     * @param technologies List of technology names to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByTechnologies(List<String> technologies);

    /**
     * Finds expert identifiers who have experience with a specific project type.
     *
     * @param projectType The project type to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByProjectType(String projectType);

    /**
     * Finds expert identifiers who have worked for a specific customer.
     *
     * @param customerName The customer name to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByCustomer(String customerName);

    /**
     * Finds expert identifiers who have worked for a specific customer and have experience with a specific technology.
     *
     * @param customerName The customer name to search for
     * @param technology   The technology name to search for
     * @return List of expert identifiers, empty list if none found
     * @throws RetrievalException if the graph query fails
     */
    List<String> findExpertsByCustomerAndTechnology(String customerName, String technology);
}
