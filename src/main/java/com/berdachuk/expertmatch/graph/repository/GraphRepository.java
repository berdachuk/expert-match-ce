package com.berdachuk.expertmatch.graph.repository;

/**
 * Repository for Apache AGE graph administrative operations.
 * Handles graph creation, index management, and existence checks.
 */
public interface GraphRepository {

    /**
     * Checks if the Apache AGE graph exists in the database.
     *
     * @param graphName The name of the graph to check
     * @return true if the graph exists, false otherwise
     */
    boolean graphExists(String graphName);

    /**
     * Creates the Apache AGE graph.
     * If the graph already exists, this method does nothing.
     *
     * @param graphName The name of the graph to create
     */
    void createGraph(String graphName);

    /**
     * Checks if a vertex table exists in the ag_catalog schema.
     * Vertex tables are named with pattern: ag_graphname_VertexLabel
     *
     * @param graphName   The graph name
     * @param vertexLabel The vertex label (e.g., Expert, Project)
     * @return true if the table exists, false otherwise
     */
    boolean vertexTableExists(String graphName, String vertexLabel);

    /**
     * Creates a GIN index on a vertex table's properties column.
     * The index is created on the properties JSONB column using jsonb_path_ops.
     *
     * @param graphName   The graph name
     * @param vertexLabel The vertex label (e.g., Expert, Project)
     * @param indexName   The name of the index to create
     */
    void createPropertyIndex(String graphName, String vertexLabel, String indexName);
}