package com.berdachuk.expertmatch.query.sgr;

/**
 * Service interface for queryclassification operations.
 */
public interface QueryClassificationService {
    /**
     * Classifies a query using the Routing pattern.
     * Determines the query intent and routes to appropriate processing path.
     *
     * @param query The user's query to classify
     * @return Query classification result with intent and routing information
     */
    QueryClassification classifyWithRouting(String query);
}
