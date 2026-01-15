package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for vector similarity search using PgVector directly.
 * This is an alternative to Spring AI VectorStore for more control.
 */
@Service
public class PgVectorSearchServiceImpl implements PgVectorSearchService {

    /**
     * Database schema supports vector(1536) to accommodate both:
     * - Ollama BAAI/bge-m3: 1024 dimensions (padded to 1536 when stored)
     * - OpenAI/DIAL text-embedding-3-large: 1536 dimensions (used as-is)
     */
    private static final int DATABASE_EMBEDDING_DIMENSION = 1536;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/retrieval/vectorSearch.sql")
    private String vectorSearchSql;

    public PgVectorSearchServiceImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Performs vector similarity search using cosine distance.
     * Supports both 1024 (Ollama) and 1536 (OpenAI/DIAL) query embeddings.
     * Normalizes query embeddings to 1536 dimensions to match database schema.
     *
     * @param queryEmbedding      The query embedding vector (1024 or 1536 dimensions)
     * @param maxResults          Maximum number of results
     * @param similarityThreshold Minimum similarity score (0.0-1.0)
     * @return List of similar documents with employee IDs
     */
    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, int maxResults, double similarityThreshold) {
        // Validate input parameters before executing query
        if (queryEmbedding == null) {
            throw new IllegalArgumentException("Query embedding cannot be null");
        }
        if (queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Query embedding cannot be empty");
        }
        // Validate dimension (must be 1024 or 1536)
        if (queryEmbedding.length != 1024 && queryEmbedding.length != 1536) {
            throw new IllegalArgumentException(
                    "Query embedding dimension must be 1024 (Ollama) or 1536 (OpenAI/DIAL), got " + queryEmbedding.length);
        }
        // Validate maxResults
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }
        // Validate similarityThreshold
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException(
                    "Similarity threshold must be between 0.0 and 1.0, got: " + similarityThreshold);
        }

        // Normalize to database dimension (1536)
        float[] normalizedQuery = normalizeEmbeddingDimension(queryEmbedding, DATABASE_EMBEDDING_DIMENSION);

        // Convert normalized float[] to PostgreSQL vector format
        String vectorString = formatVector(normalizedQuery);

        // Validate formatted vector string
        if (vectorString == null || vectorString.isEmpty() || vectorString.equals("[]")) {
            throw new IllegalArgumentException("Formatted vector string is invalid: " + vectorString);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("queryVector", vectorString);
        params.put("threshold", similarityThreshold);
        params.put("maxResults", maxResults);

        return namedJdbcTemplate.query(vectorSearchSql, params, (rs, rowNum) -> {
            String employeeId = rs.getString("employee_id");
            double similarity = rs.getDouble("similarity");

            // Build document metadata
            Map<String, Object> metadata = new HashMap<>();
            // Only add non-null values to metadata
            String projectName = rs.getString("project_name");
            if (projectName != null) {
                metadata.put("projectName", projectName);
            }

            String projectSummary = rs.getString("project_summary");
            if (projectSummary != null) {
                metadata.put("projectSummary", projectSummary);
            }

            String role = rs.getString("role");
            if (role != null) {
                metadata.put("role", role);
            }

            // Extract technologies array
            Array techArray = rs.getArray("technologies");
            List<String> technologies = techArray != null
                    ? List.of((String[]) techArray.getArray())
                    : List.of();
            metadata.put("technologies", technologies);

            return new PgVectorSearchService.VectorSearchResult(employeeId, similarity, metadata);
        });
    }

    /**
     * Searches by query text (requires embedding generation first).
     * This method should be called after generating embedding from query text.
     */
    @Override
    public List<VectorSearchResult> searchByText(String queryText, float[] queryEmbedding, int maxResults, double similarityThreshold) {
        return search(queryEmbedding, maxResults, similarityThreshold);
    }

    /**
     * Normalizes embedding to target dimension.
     * - If source dimension < target: pads with zeros
     * - If source dimension > target: truncates
     * - If source dimension == target: returns as-is
     *
     * @param embedding       Original embedding vector
     * @param targetDimension Target dimension (1536 for database schema)
     * @return Normalized embedding vector
     */
    private float[] normalizeEmbeddingDimension(float[] embedding, int targetDimension) {
        if (embedding.length == targetDimension) {
            return embedding;
        }

        float[] normalized = new float[targetDimension];
        int copyLength = Math.min(embedding.length, targetDimension);
        System.arraycopy(embedding, 0, normalized, 0, copyLength);
        // Remaining elements are already zero (default float value)

        return normalized;
    }

    /**
     * Formats float array as PostgreSQL vector string.
     * Format: "[0.1,0.2,0.3,...]" (space-separated for pgvector)
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            // Format with reasonable precision
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

}

