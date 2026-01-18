package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;

/**
 * Service for performing deep research using SGR pattern.
 * Implements multi-step iterative retrieval: initial retrieval → gap analysis → query refinement → expanded retrieval → synthesis.
 */
public interface DeepResearchService {
    /**
     * Performs deep research: initial retrieval → gap analysis → expansion → synthesis.
     *
     * @param request     Query request
     * @param parsedQuery Parsed query
     * @return Retrieval result with synthesized expert IDs
     */
    HybridRetrievalService.RetrievalResult performDeepResearch(
            QueryRequest request,
            com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery);

    /**
     * Performs deep research: initial retrieval → gap analysis → expansion → synthesis with optional execution tracing.
     *
     * @param request     Query request
     * @param parsedQuery Parsed query
     * @param tracer      Optional execution tracer
     * @return Retrieval result with synthesized expert IDs
     */
    HybridRetrievalService.RetrievalResult performDeepResearch(
            QueryRequest request,
            com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery,
            ExecutionTracer tracer);
}
