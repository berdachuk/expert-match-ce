package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.data.EmployeeRepository;
import com.berdachuk.expertmatch.exception.RetrievalException;
import com.berdachuk.expertmatch.query.EntityExtractor;
import com.berdachuk.expertmatch.query.ExecutionTracer;
import com.berdachuk.expertmatch.query.QueryParser;
import com.berdachuk.expertmatch.query.QueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main service for hybrid GraphRAG retrieval combining vector, graph, and keyword search.
 */
@Slf4j
@Service
public class HybridRetrievalService {
    private final VectorSearchService vectorSearch;
    private final GraphSearchService graphSearch;
    private final KeywordSearchService keywordSearch;
    private final ResultFusionService fusionService;
    private final SemanticReranker reranker;
    private final EntityExtractor entityExtractor;
    private final EmployeeRepository employeeRepository;
    private final Environment environment;

    public HybridRetrievalService(
            VectorSearchService vectorSearch,
            GraphSearchService graphSearch,
            KeywordSearchService keywordSearch,
            ResultFusionService fusionService,
            SemanticReranker reranker,
            EntityExtractor entityExtractor,
            EmployeeRepository employeeRepository,
            Environment environment) {
        this.vectorSearch = vectorSearch;
        this.graphSearch = graphSearch;
        this.keywordSearch = keywordSearch;
        this.fusionService = fusionService;
        this.reranker = reranker;
        this.entityExtractor = entityExtractor;
        this.employeeRepository = employeeRepository;
        this.environment = environment;
    }

    /**
     * Performs hybrid retrieval for expert matching.
     */
    public RetrievalResult retrieve(QueryRequest request, QueryParser.ParsedQuery parsedQuery) {
        return retrieve(request, parsedQuery, null);
    }

    /**
     * Performs hybrid retrieval for expert matching with optional execution tracing.
     */
    public RetrievalResult retrieve(QueryRequest request, QueryParser.ParsedQuery parsedQuery, ExecutionTracer tracer) {
        log.info("Starting hybrid retrieval for query: '{}'", parsedQuery.originalQuery());
        int maxResults = request.options().maxResults();
        double minConfidence = request.options().minConfidence();

        Map<String, List<String>> results = new HashMap<>();

        // 1. Vector search
        if (tracer != null) {
            tracer.startStep("Vector Search", "VectorSearchService", "searchByText");
        }
        log.info("Step 1/5: Performing vector search...");
        List<String> vectorResults = performVectorSearch(parsedQuery, maxResults);
        results.put("vector", vectorResults);
        log.info("Vector search completed: {} experts found", vectorResults.size());
        if (tracer != null) {
            tracer.endStep("Query: " + parsedQuery.originalQuery(), "Results: " + vectorResults.size() + " expert IDs");
        }

        // 2. Graph traversal
        if (tracer != null) {
            tracer.startStep("Graph Search", "GraphSearchService", "findExpertsByTechnology");
        }
        log.info("Step 2/5: Performing graph search (technologies: {}, skills: {})...",
                parsedQuery.technologies().size(), parsedQuery.skills().size());
        List<String> graphResults = performGraphSearch(parsedQuery, maxResults);
        results.put("graph", graphResults);
        log.info("Graph search completed: {} experts found", graphResults.size());
        if (tracer != null) {
            tracer.endStep("Technologies: " + parsedQuery.technologies().size() +
                            ", Skills: " + parsedQuery.skills().size(),
                    "Results: " + graphResults.size() + " expert IDs");
        }

        // 3. Keyword search
        if (tracer != null) {
            tracer.startStep("Keyword Search", "KeywordSearchService", "searchByKeywords");
        }
        log.info("Step 3/6: Performing keyword search...");
        List<String> keywordResults = performKeywordSearch(parsedQuery, maxResults);
        results.put("keyword", keywordResults);
        log.info("Keyword search completed: {} experts found", keywordResults.size());
        if (tracer != null) {
            tracer.endStep("Keywords: " + (parsedQuery.skills().size() + parsedQuery.technologies().size()),
                    "Results: " + keywordResults.size() + " expert IDs");
        }

        // 4. Person name search (if person entities are found)
        if (tracer != null) {
            tracer.startStep("Person Name Search", "HybridRetrievalService", "performPersonNameSearch");
        }
        log.info("Step 4/6: Performing person name search...");
        List<String> personNameResults = performPersonNameSearch(parsedQuery, maxResults);
        results.put("person", personNameResults);
        log.info("Person name search completed: {} experts found", personNameResults.size());
        if (tracer != null) {
            tracer.endStep("Query: " + parsedQuery.originalQuery(),
                    "Results: " + personNameResults.size() + " expert IDs");
        }

        // 5. Fuse results using RRF
        if (tracer != null) {
            tracer.startStep("Fuse Results", "ResultFusionService", "fuseResults");
        }
        log.info("Step 5/6: Fusing results (Vector: {}, Graph: {}, Keyword: {}, Person: {})...",
                vectorResults.size(), graphResults.size(), keywordResults.size(), personNameResults.size());
        List<String> fusedResults = fusionService.fuseResults(results, getWeights(parsedQuery));
        log.info("Result fusion completed: {} unique experts", fusedResults.size());
        if (tracer != null) {
            tracer.endStep("Vector: " + vectorResults.size() + ", Graph: " + graphResults.size() +
                            ", Keyword: " + keywordResults.size() + ", Person: " + personNameResults.size(),
                    "Fused: " + fusedResults.size() + " unique expert IDs");
        }

        // 6. Semantic reranking (if enabled and we have results)
        List<String> finalResults;
        if (request.options().rerank() && !fusedResults.isEmpty()) {
            if (tracer != null) {
                tracer.startStep("Semantic Reranking", "SemanticReranker", "rerank");
            }
            log.info("Step 6/6: Performing semantic reranking ({} candidates)...", fusedResults.size());
            List<String> rerankedResults = reranker.rerank(request.query(), fusedResults, maxResults);

            // If reranking failed and returned empty list, fallback to fused results
            if (rerankedResults.isEmpty() && !fusedResults.isEmpty()) {
                log.warn("Reranking returned empty list, falling back to fused results ({} experts)", fusedResults.size());
                finalResults = fusedResults.stream().limit(maxResults).toList();
            } else {
                finalResults = rerankedResults;
            }
            log.info("Reranking completed: {} experts (from {} candidates)", finalResults.size(), fusedResults.size());

            if (tracer != null) {
                tracer.endStep("Query: " + request.query() + ", Candidates: " + fusedResults.size(),
                        "Reranked: " + finalResults.size() + " expert IDs");
            }
        } else {
            log.info("Reranking skipped (disabled or no candidates), using fused results");
            finalResults = fusedResults.stream().limit(maxResults).toList();
        }

        // 6. Calculate relevance scores (only if we have results and reranking is enabled)
        // Note: Relevance scores are calculated using the reranking model, so we only call it when reranking is enabled
        Map<String, Double> relevanceScores;
        if (!finalResults.isEmpty() && request.options().rerank()) {
            if (tracer != null) {
                tracer.startStep("Calculate Relevance Scores", "SemanticReranker", "calculateRelevanceScores");
            }
            log.info("Calculating relevance scores for {} experts...", finalResults.size());
            relevanceScores = reranker.calculateRelevanceScores(request.query(), finalResults);
            log.info("Relevance scores calculated: {} scores", relevanceScores.size());
            if (tracer != null) {
                tracer.endStep("Expert IDs: " + finalResults.size(), "Scores calculated: " + relevanceScores.size());
            }
        } else {
            // When reranking is disabled, return placeholder scores (0.8 for all experts)
            if (!finalResults.isEmpty()) {
                log.info("Using placeholder relevance scores (reranking disabled)");
                relevanceScores = new HashMap<>();
                for (String expertId : finalResults) {
                    relevanceScores.put(expertId, 0.8);
                }
            } else {
                relevanceScores = Map.of();
            }
        }

        log.info("Hybrid retrieval completed: {} final experts found", finalResults.size());
        return new RetrievalResult(finalResults, relevanceScores);
    }

    /**
     * Performs vector similarity search.
     */
    private List<String> performVectorSearch(QueryParser.ParsedQuery parsedQuery, int maxResults) {
        try {
            return vectorSearch.searchByText(
                            parsedQuery.originalQuery(),
                            maxResults,
                            0.7
                    ).stream()
                    .map(doc -> {
                        // Extract employee ID from document metadata or ID
                        // Document ID should be employee_id from work_experience
                        String docId = doc.getId();
                        // If metadata contains employee_id, use that
                        if (doc.getMetadata().containsKey("employeeId")) {
                            return doc.getMetadata().get("employeeId").toString();
                        }
                        return docId;
                    })
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.error("Failed to perform vector search for query: {}", parsedQuery.originalQuery(), e);
            throw new RetrievalException(
                    "VECTOR_SEARCH_ERROR",
                    "Failed to perform vector search for query: " + parsedQuery.originalQuery(),
                    e
            );
        }
    }

    /**
     * Performs graph traversal search.
     */
    private List<String> performGraphSearch(QueryParser.ParsedQuery parsedQuery, int maxResults) {
        List<String> results = new ArrayList<>();

        // Search by technologies (if multiple, use AND search)
        if (!parsedQuery.technologies().isEmpty()) {
            if (parsedQuery.technologies().size() == 1) {
                results.addAll(graphSearch.findExpertsByTechnology(parsedQuery.technologies().get(0)));
            } else {
                // Multiple technologies: find experts with ALL technologies
                results.addAll(graphSearch.findExpertsByTechnologies(parsedQuery.technologies()));
            }
        }

        // Search by skills (treat as technologies)
        if (!parsedQuery.skills().isEmpty()) {
            for (String skill : parsedQuery.skills()) {
                results.addAll(graphSearch.findExpertsByTechnology(skill));
            }
        }

        // Search by domain (extract from query if available)
        EntityExtractor.ExtractedEntities entities = entityExtractor.extract(parsedQuery.originalQuery());
        if (!entities.domains().isEmpty()) {
            for (EntityExtractor.Entity domain : entities.domains()) {
                results.addAll(graphSearch.findExpertsByDomain(domain.name()));
            }
        }

        return results.stream().distinct().limit(maxResults).toList();
    }

    /**
     * Performs keyword search.
     */
    private List<String> performKeywordSearch(QueryParser.ParsedQuery parsedQuery, int maxResults) {
        // Use skills and technologies as keywords
        List<String> keywords = new ArrayList<>(parsedQuery.skills());
        keywords.addAll(parsedQuery.technologies());

        if (!keywords.isEmpty()) {
            return keywordSearch.searchByKeywords(keywords, maxResults);
        }

        return List.of();
    }

    /**
     * Performs person name search when person entities are found in the query.
     * First tries exact/partial name match, then falls back to similarity search if no results.
     */
    private List<String> performPersonNameSearch(QueryParser.ParsedQuery parsedQuery, int maxResults) {
        // Extract person entities from the query
        EntityExtractor.ExtractedEntities entities = entityExtractor.extract(parsedQuery.originalQuery());

        if (entities.persons().isEmpty()) {
            log.debug("No person entities found in query, skipping person name search");
            return List.of();
        }

        List<String> allResults = new ArrayList<>();
        for (EntityExtractor.Entity person : entities.persons()) {
            String personName = person.name();
            log.info("Searching for employee by name: '{}'", personName);

            // First try exact/partial name match
            List<String> employeeIds = employeeRepository.findEmployeeIdsByName(personName, maxResults);
            log.info("Exact/partial name search found {} employees matching '{}'", employeeIds.size(), personName);

            // If no results, try similarity search (fuzzy matching for typos/variations)
            if (employeeIds.isEmpty()) {
                log.info("No exact matches found, trying similarity search for '{}'", personName);
                // Use similarity threshold of 0.3 (30% similarity) to catch typos and variations
                // This is a reasonable threshold: too low (e.g., 0.1) gives false positives,
                // too high (e.g., 0.7) misses legitimate variations
                employeeIds = employeeRepository.findEmployeeIdsByNameSimilarity(personName, 0.3, maxResults);
                if (!employeeIds.isEmpty()) {
                    log.info("Similarity search found {} employees matching '{}' (fuzzy match)", employeeIds.size(), personName);
                } else {
                    log.debug("Similarity search also found no matches for '{}'", personName);
                }
            }

            allResults.addAll(employeeIds);
        }

        return allResults.stream().distinct().limit(maxResults).toList();
    }

    /**
     * Gets weights for fusion based on query characteristics.
     */
    private Map<String, Double> getWeights(QueryParser.ParsedQuery parsedQuery) {
        Map<String, Double> weights = new HashMap<>();

        // Default weights
        weights.put("vector", 1.0);
        weights.put("graph", 0.8);
        weights.put("keyword", 0.6);
        weights.put("person", 2.0); // High weight for person name search (exact match)

        // Adjust weights based on query characteristics
        if (!parsedQuery.technologies().isEmpty()) {
            weights.put("keyword", 0.8); // Higher weight for keyword when technologies specified
        }

        if (parsedQuery.intent().equals("team_formation")) {
            weights.put("graph", 1.0); // Higher weight for graph in team formation
        }

        // If person entities are found, prioritize person name search
        EntityExtractor.ExtractedEntities entities = entityExtractor.extract(parsedQuery.originalQuery());
        if (!entities.persons().isEmpty()) {
            weights.put("person", 3.0); // Very high weight when person names are explicitly mentioned
        }

        return weights;
    }

    /**
     * Retrieval result.
     */
    public record RetrievalResult(
            List<String> expertIds,
            Map<String, Double> relevanceScores
    ) {
    }
}

