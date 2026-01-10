package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.core.exception.RetrievalException;
import com.berdachuk.expertmatch.graph.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for graph traversal using Apache AGE.
 */
@Slf4j
@Service
public class GraphSearchServiceImpl implements GraphSearchService {
    private final GraphService graphService;

    public GraphSearchServiceImpl(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Finds experts via graph traversal.
     * <p>
     * Example: Find experts who worked on projects using specific technologies.
     */
    @Override
    public List<String> findExpertsByTechnology(String technology) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        // Apache AGE Cypher query
        String cypher = """
                MATCH (e:Expert)-[:PARTICIPATED_IN]->(p:Project)-[:USES]->(t:Technology)
                WHERE t.name = $technology
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("technology", technology);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE graph queries can fail due to various reasons (missing data, query issues, etc.)
            // Return empty list instead of throwing to allow graceful degradation
            // Hybrid retrieval can still use vector and keyword search
            log.warn("Graph search failed for technology: {} - returning empty results to allow graceful degradation. Error: {}",
                    technology, e.getMessage());
            log.debug("Graph search error details", e);
            return List.of();
        }
    }

    /**
     * Finds experts who worked with other experts on same projects.
     */
    @Override
    public List<String> findCollaboratingExperts(String expertId) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        String cypher = """
                MATCH (e1:Expert)-[:PARTICIPATED_IN]->(p:Project)<-[:PARTICIPATED_IN]-(e2:Expert)
                WHERE e1.id = $expertId AND e1.id <> e2.id
                RETURN DISTINCT e2.id as expertId
                LIMIT 50
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("expertId", expertId);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            log.error("Failed to find collaborating experts for expertId: {}", expertId, e);
            throw new RetrievalException(
                    "GRAPH_SEARCH_ERROR",
                    "Failed to find collaborating experts for expertId: " + expertId,
                    e
            );
        }
    }

    /**
     * Finds experts by project domain/industry.
     */
    @Override
    public List<String> findExpertsByDomain(String domain) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        String cypher = """
                MATCH (e:Expert)-[:PARTICIPATED_IN]->(p:Project)-[:IN_DOMAIN]->(d:Domain)
                WHERE d.name = $domain
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("domain", domain);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE graph queries can fail due to various reasons (missing data, query issues, etc.)
            // Return empty list instead of throwing to allow graceful degradation
            // Hybrid retrieval can still use vector and keyword search
            log.warn("Graph search failed for domain: {} - returning empty results to allow graceful degradation. Error: {}",
                    domain, e.getMessage());
            log.debug("Graph search error details", e);
            return List.of();
        }
    }

    /**
     * Finds experts by multiple technologies (AND condition).
     */
    @Override
    public List<String> findExpertsByTechnologies(List<String> technologies) {
        if (!graphService.graphExists() || technologies.isEmpty()) {
            return List.of();
        }

        // Find experts who worked on projects using ALL specified technologies
        String cypher = """
                MATCH (e:Expert)-[:PARTICIPATED_IN]->(p:Project)-[:USES]->(t:Technology)
                WHERE t.name IN $technologies
                WITH e, COUNT(DISTINCT t.name) as techCount
                WHERE techCount = $techCount
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("technologies", technologies.toArray(new String[0]));
        params.put("techCount", technologies.size());

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE graph queries can fail due to various reasons (missing data, query issues, etc.)
            // Return empty list instead of throwing to allow graceful degradation
            // Hybrid retrieval can still use vector and keyword search
            log.warn("Graph search failed for technologies: {} - returning empty results to allow graceful degradation. Error: {}",
                    technologies, e.getMessage());
            log.debug("Graph search error details", e);
            return List.of();
        }
    }

    /**
     * Finds experts by project type.
     */
    @Override
    public List<String> findExpertsByProjectType(String projectType) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        String cypher = """
                MATCH (e:Expert)-[:PARTICIPATED_IN]->(p:Project)
                WHERE p.projectType = $projectType
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("projectType", projectType);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE graph queries can fail due to various reasons (missing data, query issues, etc.)
            // Return empty list instead of throwing to allow graceful degradation
            // Hybrid retrieval can still use vector and keyword search
            log.warn("Graph search failed for project type: {} - returning empty results to allow graceful degradation. Error: {}",
                    projectType, e.getMessage());
            log.debug("Graph search error details", e);
            return List.of();
        }
    }

    /**
     * Finds experts who worked for a specific customer.
     */
    @Override
    public List<String> findExpertsByCustomer(String customerName) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        // Use WITH clause to filter Customer by name to avoid agtype operator issues
        // This pattern works around Apache AGE limitations with property matching in MATCH
        String cypher = """
                MATCH (e:Expert)-[:WORKED_FOR]->(c:Customer)
                WITH e, c
                WHERE c.name = $customerName
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("customerName", customerName);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE has issues with Customer vertex queries (graphid comparison errors)
            // Return empty list instead of throwing to allow graceful degradation
            log.debug("Customer query failed (Apache AGE limitation): {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Finds experts who worked for a specific customer and used a specific technology.
     */
    @Override
    public List<String> findExpertsByCustomerAndTechnology(String customerName, String technology) {
        if (!graphService.graphExists()) {
            return List.of();
        }

        // Use WITH clause to filter Customer by name, then WHERE for Technology
        String cypher = """
                MATCH (e:Expert)-[:WORKED_FOR]->(c:Customer)
                WITH e, c
                WHERE c.name = $customerName
                MATCH (e)-[:PARTICIPATED_IN]->(p:Project)-[:USES]->(t:Technology)
                WHERE t.name = $technology
                RETURN DISTINCT e.id as expertId
                LIMIT 100
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("customerName", customerName);
        params.put("technology", technology);

        try {
            return graphService.executeCypherAndExtract(cypher, params, "expertId");
        } catch (Exception e) {
            // Apache AGE has issues with Customer vertex queries (graphid comparison errors)
            // Return empty list instead of throwing to allow graceful degradation
            log.debug("Customer+Technology query failed (Apache AGE limitation): {}", e.getMessage());
            return List.of();
        }
    }
}

