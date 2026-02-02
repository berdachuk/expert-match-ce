package com.berdachuk.expertmatch.employee.service;

import com.berdachuk.expertmatch.core.domain.ParsedQuery;
import com.berdachuk.expertmatch.core.domain.QueryResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for expert enrichment operations.
 */
public interface ExpertEnrichmentService {
    /**
     * Enriches expert recommendations with detailed information.
     * Loads full employee data, skills, projects, and work experience for each expert ID.
     *
     * @param expertIdsWithScores Map of expert IDs to their relevance scores
     * @param parsedQuery         The parsed query containing intent, skills, and technologies
     * @return List of enriched expert matches with full details
     */
    List<QueryResponse.ExpertMatch> enrichExperts(
            Map<String, Double> expertIdsWithScores,
            ParsedQuery parsedQuery);
}
