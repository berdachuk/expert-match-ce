package com.berdachuk.expertmatch.query.tools;

import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import com.berdachuk.expertmatch.retrieval.service.GraphSearchService;
import com.berdachuk.expertmatch.retrieval.service.PgVectorSearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI tools for retrieval operations.
 * Provides direct access to vector, graph, and keyword search capabilities.
 */
@Component
public class RetrievalTools {

    private final PgVectorSearchService pgVectorSearch;
    private final GraphSearchService graphSearch;
    private final EmbeddingService embeddingService;

    public RetrievalTools(
            PgVectorSearchService pgVectorSearch,
            GraphSearchService graphSearch,
            EmbeddingService embeddingService
    ) {
        this.pgVectorSearch = pgVectorSearch;
        this.graphSearch = graphSearch;
        this.embeddingService = embeddingService;
    }

    @Tool(description = "Perform vector similarity search for experts. Use this for semantic search based on natural language queries.")
    public List<PgVectorSearchService.VectorSearchResult> vectorSearch(
            @ToolParam(description = "Search query text") String query,
            @ToolParam(description = "Maximum number of results (default: 10)") Integer maxResults,
            @ToolParam(description = "Minimum similarity threshold (0.0-1.0, default: 0.7)") Double similarityThreshold
    ) {
        if (maxResults == null) maxResults = 10;
        if (similarityThreshold == null) similarityThreshold = 0.7;

        // Generate embedding for query
        float[] queryEmbedding = embeddingService.generateEmbeddingAsFloatArray(query);

        // Perform vector search
        return pgVectorSearch.search(queryEmbedding, maxResults, similarityThreshold);
    }

    @Tool(description = "Find experts by technology using graph traversal. Use this to find experts who worked with specific technologies.")
    public List<String> findExpertsByTechnology(
            @ToolParam(description = "Technology name (e.g., Java, Spring Boot, AWS)") String technology
    ) {
        return graphSearch.findExpertsByTechnology(technology);
    }

    @Tool(description = "Find experts who collaborated with a specific expert on projects.")
    public List<String> findCollaboratingExperts(
            @ToolParam(description = "Expert ID (VARCHAR(74))") String expertId
    ) {
        return graphSearch.findCollaboratingExperts(expertId);
    }

    @Tool(description = "Find experts by domain or industry using graph traversal.")
    public List<String> findExpertsByDomain(
            @ToolParam(description = "Domain or industry name (e.g., Finance, Healthcare, E-commerce)") String domain
    ) {
        return graphSearch.findExpertsByDomain(domain);
    }

    @Tool(description = "Find experts who worked with multiple technologies (AND condition).")
    public List<String> findExpertsByTechnologies(
            @ToolParam(description = "List of technology names") List<String> technologies
    ) {
        return graphSearch.findExpertsByTechnologies(technologies);
    }

}