package com.berdachuk.expertmatch.retrieval.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Service for vector similarity search using PgVector.
 * Wraps PgVectorSearchService and provides Document-based interface.
 */
public interface VectorSearchService {

    /**
     * Performs vector similarity search.
     *
     * @param queryEmbedding      The query embedding vector
     * @param maxResults          Maximum number of results
     * @param similarityThreshold Minimum similarity score
     * @return List of similar documents
     */
    List<Document> search(float[] queryEmbedding, int maxResults, double similarityThreshold);

    /**
     * Searches by query text (generates embedding first).
     *
     * @param queryText           Query text
     * @param maxResults          Maximum number of results
     * @param similarityThreshold Minimum similarity score
     * @return List of similar documents
     */
    List<Document> searchByText(String queryText, int maxResults, double similarityThreshold);
}
