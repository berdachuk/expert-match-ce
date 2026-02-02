package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.core.domain.ParsedQuery;
import com.berdachuk.expertmatch.core.domain.QueryRequest;
import com.berdachuk.expertmatch.core.service.ExecutionTracer;

import java.util.List;
import java.util.Map;

/**
 * Service interface for hybrid retrieval operations.
 */
public interface HybridRetrievalService {
    /**
     * Retrieves expert recommendations using hybrid search (graph + vector + keyword).
     *
     * @param request     The query request containing the user's query
     * @param parsedQuery The parsed query containing intent, skills, and technologies
     * @return Retrieval result containing expert IDs and relevance scores
     */
    RetrievalResult retrieve(QueryRequest request, ParsedQuery parsedQuery);

    /**
     * Retrieves expert recommendations using hybrid search (graph + vector + keyword) with execution tracing.
     *
     * @param request     The query request containing the user's query
     * @param parsedQuery The parsed query containing intent, skills, and technologies
     * @param tracer      Optional execution tracer for tracking retrieval steps
     * @return Retrieval result containing expert IDs and relevance scores
     */
    RetrievalResult retrieve(QueryRequest request, ParsedQuery parsedQuery, ExecutionTracer tracer);

    /**
     * Retrieval result containing expert recommendations and relevance scores.
     *
     * @param expertIds       List of expert unique identifiers ordered by relevance
     * @param relevanceScores Map of expert ID to relevance score (0.0 to 1.0)
     */
    record RetrievalResult(
            List<String> expertIds,
            Map<String, Double> relevanceScores
    ) {
    }
}
