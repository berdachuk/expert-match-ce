package com.berdachuk.expertmatch.llm.tools;

/**
 * Service interface for toolmetadata operations.
 */
public interface ToolMetadataService {
    /**
     * Indexes tools from a Spring component for metadata storage.
     *
     * @param toolComponent The Spring component containing @Tool annotated methods
     */
    void indexTools(Object toolComponent);

    /**
     * Re-indexes tools from a Spring component, clearing existing metadata first.
     *
     * @param toolComponent The Spring component containing @Tool annotated methods
     */
    void reindexTools(Object toolComponent);
}
