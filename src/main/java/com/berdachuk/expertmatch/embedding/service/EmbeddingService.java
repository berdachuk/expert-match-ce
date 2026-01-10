package com.berdachuk.expertmatch.embedding.service;

import java.util.List;

/**
 * Service interface for embedding operations.
 */
public interface EmbeddingService {

    /**
     * Generates an embedding vector for a text string.
     *
     * @param text The text to generate embedding for
     * @return List of embedding values as Double objects
     */
    List<Double> generateEmbedding(String text);

    /**
     * Generates embedding vectors for multiple text strings.
     *
     * @param texts List of texts to generate embeddings for
     * @return List of embedding vectors, where each vector is a list of Double values
     */
    List<List<Double>> generateEmbeddings(List<String> texts);

    /**
     * Generates an embedding vector for a text string as a float array.
     * This is more efficient for vector operations and database storage.
     *
     * @param text The text to generate embedding for
     * @return Embedding vector as float array
     */
    float[] generateEmbeddingAsFloatArray(String text);
}
