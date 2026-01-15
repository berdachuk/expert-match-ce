package com.berdachuk.expertmatch.retrieval.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for pgvectorsearch operations.
 */
public interface PgVectorSearchService {
    /**
     * Searches for experts using vector similarity search.
     *
     * @param queryEmbedding      The query embedding vector
     * @param maxResults          Maximum number of results to return
     * @param similarityThreshold Minimum similarity threshold (0.0 to 1.0)
     * @return List of vector search results ordered by similarity, empty list if none found
     */
    List<VectorSearchResult> search(float[] queryEmbedding, int maxResults, double similarityThreshold);

    /**
     * Searches for experts using vector similarity search with query text.
     *
     * @param queryText           The query text (for logging/debugging)
     * @param queryEmbedding      The query embedding vector
     * @param maxResults          Maximum number of results to return
     * @param similarityThreshold Minimum similarity threshold (0.0 to 1.0)
     * @return List of vector search results ordered by similarity, empty list if none found
     */
    List<VectorSearchResult> searchByText(String queryText, float[] queryEmbedding, int maxResults, double similarityThreshold);

    /**
     * Vector search result containing expert information and similarity score.
     *
     * @param employeeId The unique identifier of the employee
     * @param similarity The similarity score (0.0 to 1.0)
     * @param metadata   Additional metadata about the search result
     */
    record VectorSearchResult(
            String employeeId,
            double similarity,
            Map<String, Object> metadata
    ) {
    }
}
