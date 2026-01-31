package com.berdachuk.expertmatch.retrieval.repository;

import java.util.Map;

/**
 * Result from PgVector similarity search.
 */
public record PgVectorSearchResult(
        String employeeId,
        double similarity,
        Map<String, Object> metadata
) {
}
