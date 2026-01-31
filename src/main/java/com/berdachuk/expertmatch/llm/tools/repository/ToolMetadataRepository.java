package com.berdachuk.expertmatch.llm.tools.repository;

/**
 * Repository for tool metadata operations.
 */
public interface ToolMetadataRepository {

    /**
     * Inserts tool metadata with embedding.
     *
     * @param id                 Unique tool ID (class#method)
     * @param toolName           Method name
     * @param description        Tool description
     * @param toolClass          Fully qualified class name
     * @param methodName         Method name
     * @param parametersJson     Parameters metadata as JSON
     * @param embedding          PostgreSQL vector string
     * @param embeddingDimension Embedding dimension
     * @param now                Current timestamp
     */
    void insertToolMetadata(
            String id,
            String toolName,
            String description,
            String toolClass,
            String methodName,
            String parametersJson,
            String embedding,
            int embeddingDimension,
            java.sql.Timestamp now
    );

    /**
     * Deletes all tools from a specific class.
     *
     * @param toolClass Fully qualified class name
     * @return Number of deleted records
     */
    int deleteByToolClass(String toolClass);
}
