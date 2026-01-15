package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.domain.QueryResponse;


/**
 * Service interface for query operations.
 */
public interface QueryService {
    /**
     * Processes an expert discovery query and returns recommendations.
     * This method handles the complete query processing pipeline including:
     * parsing, entity extraction, retrieval, enrichment, and answer generation.
     *
     * @param request The query request containing the user's query and options
     * @param chatId  The unique identifier of the chat session
     * @param userId  The unique identifier of the user
     * @return Query response containing answer, expert recommendations, sources, and metadata
     */
    QueryResponse processQuery(QueryRequest request, String chatId, String userId);
}
