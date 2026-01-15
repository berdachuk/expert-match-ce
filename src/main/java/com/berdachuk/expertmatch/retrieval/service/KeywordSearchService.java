package com.berdachuk.expertmatch.retrieval.service;

import java.util.List;


/**
 * Service interface for keywordsearch operations.
 */
public interface KeywordSearchService {
    /**
     * Searches for expert identifiers using keyword matching.
     *
     * @param keywords   List of keywords to search for
     * @param maxResults Maximum number of results to return
     * @return List of expert identifiers matching the keywords, empty list if none found
     */
    List<String> searchByKeywords(List<String> keywords, int maxResults);

    /**
     * Searches for expert identifiers by technology names using keyword matching.
     *
     * @param technologies List of technology names to search for
     * @param maxResults   Maximum number of results to return
     * @return List of expert identifiers matching the technologies, empty list if none found
     */
    List<String> searchByTechnologies(List<String> technologies, int maxResults);
}
