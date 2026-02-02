package com.berdachuk.expertmatch.core.domain;

/**
 * Domain types for entity extraction.
 */
public class EntityExtractorTypes {

    private EntityExtractorTypes() {
        // Prevent instantiation
    }

    /**
     * Extracted entities result.
     */
    public record ExtractedEntities(
            java.util.List<Entity> persons,
            java.util.List<Entity> organizations,
            java.util.List<Entity> technologies,
            java.util.List<Entity> projects,
            java.util.List<Entity> domains
    ) {
    }

    /**
     * Entity representation.
     */
    public record Entity(
            String type,
            String name,
            String id
    ) {
    }
}
