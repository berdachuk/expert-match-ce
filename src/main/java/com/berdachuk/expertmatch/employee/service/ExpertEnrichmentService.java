package com.berdachuk.expertmatch.employee.service;

import com.berdachuk.expertmatch.query.domain.QueryParser;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService;

import java.util.List;

/**
 * Service interface for expert enrichment operations.
 */
public interface ExpertEnrichmentService {
    /**
     * Enriches expert recommendations with detailed information.
     * Loads full employee data, skills, projects, and work experience for each expert ID.
     *
     * @param retrievalResult The retrieval result containing expert IDs and relevance scores
     * @param parsedQuery     The parsed query containing intent, skills, and technologies
     * @return List of enriched expert matches with full details
     */
    List<QueryResponse.ExpertMatch> enrichExperts(
            HybridRetrievalService.RetrievalResult retrievalResult,
            QueryParser.ParsedQuery parsedQuery);
}
