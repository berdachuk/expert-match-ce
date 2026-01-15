package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.tool.search.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PgVector-based ToolSearcher implementation.
 * Uses existing PgVector infrastructure for semantic tool search.
 * Implements the ToolSearcher interface from tool-search-tool library.
 */
@Slf4j
@Component
public class PgVectorToolSearcher implements ToolSearcher {

    private static final int DATABASE_EMBEDDING_DIMENSION = 1536;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmbeddingService embeddingService;

    @InjectSql("/sql/toolmetadata/clearIndex.sql")
    private String clearIndexSql;

    @InjectSql("/sql/toolmetadata/searchTools.sql")
    private String searchToolsSql;

    public PgVectorToolSearcher(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            EmbeddingService embeddingService
    ) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.embeddingService = embeddingService;
    }

    @Override
    public SearchType searchType() {
        return SearchType.SEMANTIC;
    }

    @Override
    public void indexTool(String sessionId, ToolReference toolReference) {
        // Tool indexing is handled by ToolMetadataService.indexTools()
        // This method is called by the library for per-tool indexing
        // We can use it for incremental updates if needed
        log.debug("Index tool called for sessionId: {}, toolName: {}", sessionId, toolReference.toolName());
        // For now, we rely on ToolMetadataService.indexTools() called at startup
    }

    @Override
    public ToolSearchResponse search(ToolSearchRequest request) {
        String query = request.query();
        Integer maxResults = request.maxResults() != null ? request.maxResults() : 5;

        // Validate input parameters
        if (query == null || query.trim().isEmpty()) {
            log.warn("Empty query provided to tool search");
            return ToolSearchResponse.builder()
                    .toolReferences(List.of())
                    .totalMatches(0)
                    .build();
        }

        if (maxResults < 1) {
            maxResults = 5; // Default to 5 if invalid
        }

        // Perform vector similarity search
        List<ToolReference> toolReferences = performVectorSearch(query, maxResults);

        return ToolSearchResponse.builder()
                .toolReferences(toolReferences)
                .totalMatches(toolReferences.size())
                .build();
    }

    @Override
    public void clearIndex(String sessionId) {
        // Clear all tool metadata from database
        namedJdbcTemplate.update(clearIndexSql, new HashMap<>());
        log.info("Cleared tool index for sessionId: {}", sessionId);
    }

    /**
     * Performs vector similarity search and returns ToolReference objects.
     * This is the core search implementation using PgVector.
     */
    private List<ToolReference> performVectorSearch(String query, int maxResults) {
        // Validate input parameters before executing query
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }

        // Generate embedding for search query
        float[] queryEmbedding = embeddingService.generateEmbeddingAsFloatArray(query);

        // Validate embedding was generated successfully
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Failed to generate embedding for query: " + query);
        }

        // Normalize to 1536 dimensions (matching database schema)
        float[] normalizedQuery = normalizeEmbedding(queryEmbedding, DATABASE_EMBEDDING_DIMENSION);
        String vectorString = formatVector(normalizedQuery);

        // Validate formatted vector string
        if (vectorString == null || vectorString.isEmpty() || vectorString.equals("[]")) {
            throw new IllegalArgumentException("Formatted vector string is invalid: " + vectorString);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("queryVector", vectorString);
        params.put("maxResults", maxResults);

        // Execute query and convert to ToolReference objects
        List<Map<String, Object>> results = namedJdbcTemplate.query(searchToolsSql, params, (rs, rowNum) -> {
            Map<String, Object> toolMetadata = new HashMap<>();
            toolMetadata.put("toolName", rs.getString("tool_name"));
            toolMetadata.put("description", rs.getString("description"));
            toolMetadata.put("toolClass", rs.getString("tool_class"));
            toolMetadata.put("methodName", rs.getString("method_name"));
            toolMetadata.put("parameters", rs.getString("parameters"));
            toolMetadata.put("similarity", rs.getDouble("similarity"));
            return toolMetadata;
        });

        // Convert to ToolReference objects
        return results.stream()
                .map(result -> {
                    String toolName = (String) result.get("toolName");
                    String description = (String) result.get("description");
                    Double similarity = (Double) result.get("similarity");

                    // Build summary from description (truncate if too long)
                    String summary = description != null && description.length() > 200
                            ? description.substring(0, 197) + "..."
                            : description;

                    return ToolReference.builder()
                            .toolName(toolName)
                            .relevanceScore(similarity != null ? similarity : 0.0)
                            .summary(summary != null ? summary : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Legacy method for backward compatibility with MCP service.
     * Converts ToolSearchResponse to List<Map<String, Object>>.
     *
     * @param query      The search query
     * @param maxResults Maximum number of results to return
     * @return List of tool metadata maps
     */
    public List<Map<String, Object>> search(String query, int maxResults) {
        ToolSearchRequest request = new ToolSearchRequest(null, query, maxResults, null);
        ToolSearchResponse response = search(request);

        return response.toolReferences().stream()
                .map(tr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("toolName", tr.toolName());
                    map.put("description", tr.summary());
                    map.put("similarity", tr.relevanceScore());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Normalizes embedding to target dimension.
     * - If source dimension < target: pads with zeros
     * - If source dimension > target: truncates
     * - If source dimension == target: returns as-is
     */
    private float[] normalizeEmbedding(float[] embedding, int targetDimension) {
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
     * Format: "[0.1,0.2,0.3,...]" (comma-separated for pgvector)
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

