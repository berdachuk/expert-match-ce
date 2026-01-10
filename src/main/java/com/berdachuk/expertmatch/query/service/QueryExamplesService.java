package com.berdachuk.expertmatch.query.service;

import java.util.List;

/**
 * Service interface for query examples operations.
 */
public interface QueryExamplesService {
    /**
     * Retrieves example queries organized by category.
     *
     * @return List of query examples, empty list if none found
     */
    List<QueryExample> getExamples();

    /**
     * Query example record containing category, title, and query text.
     *
     * @param category The category of the example (e.g., "expert_search", "team_formation")
     * @param title    The title/description of the example
     * @param query    The example query text
     */
    record QueryExample(
            String category,
            String title,
            String query
    ) {
    }
}
