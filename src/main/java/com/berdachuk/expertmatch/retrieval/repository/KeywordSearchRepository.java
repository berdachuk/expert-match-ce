package com.berdachuk.expertmatch.retrieval.repository;

import java.util.List;

/**
 * Repository for keyword/full-text search operations.
 */
public interface KeywordSearchRepository {

    /**
     * Searches work experience by keywords using PostgreSQL full-text search.
     *
     * @param searchTerms Combined search terms for plainto_tsquery
     * @param maxResults  Maximum number of results to return
     * @return List of employee IDs matching the search
     */
    List<String> searchByKeywords(String searchTerms, int maxResults);

    /**
     * Searches by exact technology match.
     *
     * @param technologies Array of technology names
     * @param maxResults   Maximum number of results to return
     * @return List of employee IDs matching the technologies
     */
    List<String> searchByTechnologies(String[] technologies, int maxResults);
}
