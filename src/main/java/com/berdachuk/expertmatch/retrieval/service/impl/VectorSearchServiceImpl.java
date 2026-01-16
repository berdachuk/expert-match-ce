package com.berdachuk.expertmatch.retrieval.service.impl;

import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import com.berdachuk.expertmatch.retrieval.service.PgVectorSearchService;
import com.berdachuk.expertmatch.retrieval.service.VectorSearchService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for vector similarity search using PgVector.
 * Wraps PgVectorSearchService and provides Document-based interface.
 */
@Service
public class VectorSearchServiceImpl implements VectorSearchService {

    private final PgVectorSearchService pgVectorSearch;
    private final EmbeddingService embeddingService;

    public VectorSearchServiceImpl(
            PgVectorSearchService pgVectorSearch,
            EmbeddingService embeddingService) {
        this.pgVectorSearch = pgVectorSearch;
        this.embeddingService = embeddingService;
    }

    /**
     * Performs vector similarity search.
     *
     * @param queryEmbedding      The query embedding vector
     * @param maxResults          Maximum number of results
     * @param similarityThreshold Minimum similarity score
     * @return List of similar documents
     */
    @Override
    public List<Document> search(float[] queryEmbedding, int maxResults, double similarityThreshold) {
        // Validate input parameters
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Query embedding cannot be null or empty");
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0, got: " + similarityThreshold);
        }

        List<PgVectorSearchService.VectorSearchResult> results =
                pgVectorSearch.search(queryEmbedding, maxResults, similarityThreshold);

        return results.stream()
                .map(result -> {
                    Map<String, Object> metadata = new HashMap<>(result.metadata());
                    metadata.put("similarity", result.similarity());
                    metadata.put("employeeId", result.employeeId());

                    Document doc = new Document(
                            result.employeeId(), // Use employee ID as document ID
                            buildDocumentContent(result),
                            metadata
                    );
                    return doc;
                })
                .collect(Collectors.toList());
    }

    /**
     * Searches by query text (generates embedding first).
     */
    @Override
    public List<Document> searchByText(String queryText, int maxResults, double similarityThreshold) {
        // Validate input parameters
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("Query text cannot be null or blank");
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0, got: " + similarityThreshold);
        }

        // Generate embedding from query text
        List<Double> embeddingList = embeddingService.generateEmbedding(queryText);

        if (embeddingList.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert List<Double> to float[]
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }

        return search(embedding, maxResults, similarityThreshold);
    }

    /**
     * Builds document content from search result.
     */
    private String buildDocumentContent(PgVectorSearchService.VectorSearchResult result) {
        StringBuilder content = new StringBuilder();

        if (result.metadata().containsKey("projectName")) {
            content.append("Project: ").append(result.metadata().get("projectName")).append("\n");
        }

        if (result.metadata().containsKey("projectSummary")) {
            content.append("Summary: ").append(result.metadata().get("projectSummary")).append("\n");
        }

        if (result.metadata().containsKey("role")) {
            content.append("Role: ").append(result.metadata().get("role")).append("\n");
        }

        if (result.metadata().containsKey("technologies")) {
            @SuppressWarnings("unchecked")
            List<String> technologies = (List<String>) result.metadata().get("technologies");
            if (technologies != null && !technologies.isEmpty()) {
                content.append("Technologies: ").append(String.join(", ", technologies));
            }
        }

        return content.toString();
    }
}

