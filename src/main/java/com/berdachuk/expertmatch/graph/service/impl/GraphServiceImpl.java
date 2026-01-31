package com.berdachuk.expertmatch.graph.service.impl;

import com.berdachuk.expertmatch.core.exception.RetrievalException;
import com.berdachuk.expertmatch.graph.repository.GraphRepository;
import com.berdachuk.expertmatch.graph.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Apache AGE graph operations.
 * Handles graph creation, vertex/edge management, and Cypher query execution.
 */
@Slf4j
@Service
public class GraphServiceImpl implements GraphService {
    private static final String GRAPH_NAME = "expertmatch_graph";
    private final JdbcTemplate jdbcTemplate;
    private final GraphRepository graphRepository;

    public GraphServiceImpl(JdbcTemplate jdbcTemplate, GraphRepository graphRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.graphRepository = graphRepository;
    }

    /**
     * Loads AGE extension for the current session/connection.
     * Optimized to check if AGE is already loaded before attempting to load it.
     * If AGE is loaded via shared_preload_libraries, this is a no-op.
     */
    private void loadAgeExtension(Connection connection) {
        try {
            // Try to load AGE extension
            // LOAD 'age' must be executed outside of a transaction
            // Execute it directly on the provided connection
            // Note: If AGE is already loaded via shared_preload_libraries, this will fail
            // but that's okay - we'll catch the exception and continue
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("LOAD 'age';");
                log.trace("AGE extension loaded successfully");
            }
        } catch (Exception e) {
            // If LOAD fails, AGE might already be loaded via shared_preload_libraries
            // This is expected in production environments where AGE is preloaded
            // Log at debug level and continue - the cypher call will fail if AGE is truly not available
            log.debug("Could not LOAD 'age' (might already be loaded): {}", e.getMessage());
        }
    }

    /**
     * Executes a Cypher query on the graph.
     *
     * @param cypherQuery Cypher query string with $paramName placeholders
     * @param parameters  Query parameters to embed in the query
     * @return List of result maps
     */
    @Transactional
    public List<Map<String, Object>> executeCypher(String cypherQuery, Map<String, Object> parameters) {
        // Validate input parameters before executing query
        if (cypherQuery == null || cypherQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Cypher query cannot be null or empty");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null (use empty map if no parameters)");
        }

        // Apache AGE uses ag_catalog.cypher() as a set-returning function
        // Function signature: ag_catalog.cypher(name, cstring, agtype)
        // - name: graph name (PostgreSQL name type)
        // - cstring: Cypher query string (PostgreSQL cstring type)
        // - agtype: parameters as agtype (optional, defaults to NULL)
        //
        // Based on Apache AGE examples, parameters are embedded directly in the Cypher query string
        // using $paramName syntax, rather than using the third parameter.
        String dollarTag = "cypher_q";

        // Embed parameters directly in the Cypher query string
        String finalQuery = embedParameters(cypherQuery, parameters);

        // Validate final query is not empty after parameter embedding
        if (finalQuery == null || finalQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("Final Cypher query is empty after parameter embedding");
        }

        try {
            // Build SQL with 2 parameters (graph name and query string)
            // Apache AGE cypher function signature: ag_catalog.cypher(graph_name name, query_string cstring, params agtype)
            // The third parameter (params) has a default of NULL, so we omit it
            // Use dollar-quoted string for Cypher query to handle special characters
            // Explicitly use ag_catalog.cypher() and ag_catalog.agtype to ensure they are found
            String sql = String.format(
                    "SELECT * FROM ag_catalog.cypher('%s'::name, $%s$%s$%s$::cstring) AS t(result ag_catalog.agtype)",
                    GRAPH_NAME, dollarTag, finalQuery, dollarTag
            );

            // Execute LOAD 'age' and Cypher query on the same connection
            // This ensures AGE is loaded for the session that executes the query
            @SuppressWarnings("null")
            List<Map<String, Object>> results = jdbcTemplate.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
                // Load AGE extension on this connection before executing Cypher query
                // Based on AGE driver examples, LOAD can be executed in a transaction
                loadAgeExtension(connection);

                // Set search_path to include ag_catalog for AGE functions
                // This ensures AGE operators and functions are found
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET LOCAL search_path = ag_catalog, \"$user\", public, expertmatch;");
                }

                // Now execute the Cypher query on the same connection
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    List<Map<String, Object>> resultList = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> result = new HashMap<>();
                        // Parse AGE result format
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            Object value = rs.getObject(i);
                            result.put(columnName, value);
                        }
                        resultList.add(result);
                    }
                    return resultList;
                }
            });

            return results;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("Cypher query execution failed due to aborted transaction - returning empty results. Query: {}",
                        cypherQuery);
                log.debug("Transaction aborted error details", e);
                return List.of(); // Return empty list to allow graceful degradation
            }
            log.error("Failed to execute Cypher query: {}", cypherQuery, e);
            throw new RetrievalException(
                    "GRAPH_QUERY_ERROR",
                    "Failed to execute Cypher query: " + cypherQuery,
                    e
            );
        } catch (org.springframework.dao.DataAccessException e) {
            // Check if it's a transaction aborted error in the cause chain
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sqlEx) {
                    String sqlState = sqlEx.getSQLState();
                    if ("25P02".equals(sqlState)) {
                        log.warn("Cypher query execution failed due to aborted transaction (from DataAccessException) - returning empty results. Query: {}",
                                cypherQuery);
                        log.debug("Transaction aborted error details", e);
                        return List.of(); // Return empty list to allow graceful degradation
                    }
                }
                cause = cause.getCause();
            }
            log.error("Failed to execute Cypher query: {}", cypherQuery, e);
            throw new RetrievalException(
                    "GRAPH_QUERY_ERROR",
                    "Failed to execute Cypher query: " + cypherQuery,
                    e
            );
        } catch (Exception e) {
            // Check if it's a transaction aborted error in the cause chain
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sqlEx) {
                    String sqlState = sqlEx.getSQLState();
                    if ("25P02".equals(sqlState)) {
                        log.warn("Cypher query execution failed due to aborted transaction (from Exception) - returning empty results. Query: {}",
                                cypherQuery);
                        log.debug("Transaction aborted error details", e);
                        return List.of(); // Return empty list to allow graceful degradation
                    }
                }
                cause = cause.getCause();
            }
            log.error("Failed to execute Cypher query: {}", cypherQuery, e);
            throw new RetrievalException(
                    "GRAPH_QUERY_ERROR",
                    "Failed to execute Cypher query: " + cypherQuery,
                    e
            );
        }
    }

    /**
     * Embeds parameters directly into the Cypher query string.
     * Replaces $paramName placeholders with properly escaped values.
     *
     * @param cypherQuery Original Cypher query with $paramName placeholders
     * @param parameters  Map of parameter names to values
     * @return Cypher query with parameters embedded
     */
    private String embedParameters(String cypherQuery, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return cypherQuery;
        }

        String result = cypherQuery;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object value = entry.getValue();

            // Replace $paramName with the actual value
            String placeholder = "$" + paramName;
            String replacement = formatCypherValue(value);

            result = result.replace(placeholder, replacement);
        }

        return result;
    }

    /**
     * Formats a value for embedding in a Cypher query string.
     * Properly escapes and formats based on the value type.
     *
     * @param value The value to format
     * @return Formatted string representation for Cypher
     */
    private String formatCypherValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            // Escape single quotes and wrap in quotes
            String escaped = value.toString()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "'" + escaped + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            // For other types, convert to string and escape
            String escaped = value.toString()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            return "'" + escaped + "'";
        }
    }

    /**
     * Executes a Cypher query and extracts a specific field from results.
     *
     * @param cypherQuery Cypher query
     * @param parameters  Query parameters
     * @param resultField Field name to extract
     * @return List of field values
     */
    @Override
    public List<String> executeCypherAndExtract(String cypherQuery, Map<String, Object> parameters, String resultField) {
        List<Map<String, Object>> results = executeCypher(cypherQuery, parameters);

        return results.stream()
                .map(result -> {
                    Object value = result.get(resultField);
                    return value != null ? value.toString() : null;
                })
                .filter(value -> value != null)
                .distinct()
                .toList();
    }


    /**
     * Checks if graph exists.
     * Returns false if Apache AGE is not available.
     * Uses a separate transaction to avoid aborting the main transaction.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            readOnly = true
    )
    @Override
    public boolean graphExists() {
        return graphRepository.graphExists(GRAPH_NAME);
    }

    /**
     * Creates the Apache AGE graph if it doesn't exist.
     * This is an administrative operation that calls ag_catalog.create_graph().
     * If the graph already exists, this method does nothing.
     */
    @Transactional
    @Override
    public void createGraph() {
        graphRepository.createGraph(GRAPH_NAME);
    }

    /**
     * Creates graph indexes for better query performance.
     * Creates GIN indexes on the properties JSONB column of vertex tables.
     * This method safely handles cases where tables don't exist yet.
     */
    @Transactional
    @Override
    public void createGraphIndexes() {
        String graphName = GRAPH_NAME;

        // Check if tables exist
        if (!graphRepository.vertexTableExists(graphName, "Expert")) {
            log.debug("Graph tables do not exist yet, skipping index creation");
            return;
        }

        // Create indexes on properties JSONB column
        graphRepository.createPropertyIndex(graphName, "Expert", "idx_" + graphName + "_expert_props");
        graphRepository.createPropertyIndex(graphName, "Project", "idx_" + graphName + "_project_props");
        graphRepository.createPropertyIndex(graphName, "Technology", "idx_" + graphName + "_technology_props");
        graphRepository.createPropertyIndex(graphName, "Customer", "idx_" + graphName + "_customer_props");

        log.debug("Graph indexes created successfully");
    }
}

