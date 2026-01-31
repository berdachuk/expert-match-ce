package com.berdachuk.expertmatch.graph.service;

import com.berdachuk.expertmatch.core.exception.RetrievalException;

import java.util.List;
import java.util.Map;

/**
 * Service interface for graph operations.
 */
public interface GraphService {
    /**
     * Executes a Cypher query against the Apache AGE graph database.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @return List of result rows, where each row is a map of column names to values
     * @throws RetrievalException if the query execution fails
     */
    List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters);

    /**
     * Executes a Cypher query and extracts a specific field from the results.
     *
     * @param cypherQuery The Cypher query to execute
     * @param parameters  Map of parameter names to values for the query
     * @param resultField The field name to extract from each result row
     * @return List of extracted field values, empty list if no results
     * @throws RetrievalException if the query execution fails or field not found
     */
    List<String> executeCypherAndExtract(String cypherQuery, Map<String, Object> parameters, String resultField);

    /**
     * Checks if the Apache AGE graph exists in the database.
     *
     * @return true if the graph exists, false otherwise
     */
    boolean graphExists();

    /**
     * Creates the Apache AGE graph if it doesn't exist.
     * This is an administrative operation that calls ag_catalog.create_graph().
     * If the graph already exists, this method does nothing.
     */
    void createGraph();

    /**
     * Creates graph indexes for better query performance.
     * Creates GIN indexes on the properties JSONB column of vertex tables.
     * This method safely handles cases where tables don't exist yet.
     */
    void createGraphIndexes();
}
