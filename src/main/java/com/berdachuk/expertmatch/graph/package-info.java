/**
 * Graph Management Module
 * <p>
 * Graph relationship management using Apache AGE:
 * - Graph relationship creation
 * - Cypher query builders
 * - Graph traversal algorithms
 * <p>
 * Exposes:
 * - GraphService (service layer)
 * - GraphSearchService (service layer)
 * - GraphBuilderService (service layer)
 * <p>
 * Internal packages:
 * - repository: Data access layer for graph building
 * - domain: Domain entities and value objects
 */
@org.springframework.modulith.ApplicationModule(
        id = "graph",
        displayName = "Graph Management",
        allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.graph;

