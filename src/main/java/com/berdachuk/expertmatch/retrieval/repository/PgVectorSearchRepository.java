package com.berdachuk.expertmatch.retrieval.repository;

import java.util.List;

/**
 * Repository for PgVector similarity search operations.
 */
public interface PgVectorSearchRepository {

    /**
     * Performs vector similarity search using cosine distance.
     *
     * @param queryVector         PostgreSQL vector string format
     * @param similarityThreshold Minimum similarity score (0.0-1.0)
     * @param maxResults          Maximum number of results
     * @return List of search results with metadata
     */
    List<PgVectorSearchResult> search(String queryVector, double similarityThreshold, int maxResults);
}
