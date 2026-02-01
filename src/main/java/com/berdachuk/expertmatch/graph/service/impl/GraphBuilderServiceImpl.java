package com.berdachuk.expertmatch.graph.service.impl;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.graph.domain.*;
import com.berdachuk.expertmatch.graph.repository.GraphBuilderRepository;
import com.berdachuk.expertmatch.graph.service.GraphBuilderService;
import com.berdachuk.expertmatch.graph.service.GraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for building graph relationships from database data.
 * Populates Apache AGE graph with experts, projects, technologies, etc.
 */
@Slf4j
@Service
public class GraphBuilderServiceImpl implements GraphBuilderService {
    private final GraphService graphService;
    private final GraphBuilderRepository repository;
    // Map to maintain project name -> project ID mapping during graph build
    private final Map<String, String> projectIdMap = new HashMap<>();

    public GraphBuilderServiceImpl(
            GraphService graphService,
            GraphBuilderRepository repository) {
        this.graphService = graphService;
        this.repository = repository;
    }

    /**
     * Clears all vertices and edges from the Apache AGE graph.
     * Uses REQUIRES_NEW so the clear commits in its own transaction even when called from clearTestData.
     * Deletes in a single Cypher call; if that fails (e.g. timeout), retries by deleting edges first then nodes.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearGraph() {
        if (!graphService.graphExists()) {
            log.debug("Graph does not exist, nothing to clear");
            return;
        }
        log.info("Clearing graph data...");
        try {
            graphService.executeCypher("MATCH (n) DETACH DELETE n", new HashMap<>());
            log.info("Graph cleared successfully");
        } catch (Exception e) {
            log.warn("Single-query clear failed ({}), trying edges then nodes: {}", e.getMessage(), e.getClass().getSimpleName());
            try {
                graphService.executeCypher("MATCH ()-[r]->() DELETE r", new HashMap<>());
                graphService.executeCypher("MATCH (n) DELETE n", new HashMap<>());
                log.info("Graph cleared successfully (edges then nodes)");
            } catch (Exception e2) {
                log.error("Could not clear graph", e2);
                throw new RuntimeException("Failed to clear graph", e2);
            }
        }
    }

    /**
     * Builds graph from existing database data.
     * Creates vertices and edges for experts, projects, technologies, etc.
     */
    @Override
    public void buildGraph() {
        long startTime = System.currentTimeMillis();
        log.info("Starting graph build process...");

        if (!graphService.graphExists()) {
            // Graph should be created by migration script
            // If not, create it here
            log.info("Creating graph structure...");
            graphService.createGraph();
            log.info("Graph structure created");
        } else {
            clearGraph();
        }

        // Clear project ID map for fresh build
        projectIdMap.clear();

        // Build graph vertices and edges
        log.info("Building graph vertices and edges...");
        long verticesStartTime = System.currentTimeMillis();

        log.info("  Creating expert vertices...");
        long expertStartTime = System.currentTimeMillis();
        createExpertVertices();
        long expertEndTime = System.currentTimeMillis();
        log.info("  Expert vertices created in {}ms", expertEndTime - expertStartTime);

        log.info("  Creating project vertices...");
        long projectStartTime = System.currentTimeMillis();
        createProjectVertices();
        long projectEndTime = System.currentTimeMillis();
        log.info("  Project vertices created in {}ms", projectEndTime - projectStartTime);

        log.info("  Creating technology vertices...");
        long technologyStartTime = System.currentTimeMillis();
        createTechnologyVertices();
        long technologyEndTime = System.currentTimeMillis();
        log.info("  Technology vertices created in {}ms", technologyEndTime - technologyStartTime);

        log.info("  Creating domain vertices...");
        long domainStartTime = System.currentTimeMillis();
        createDomainVertices();
        long domainEndTime = System.currentTimeMillis();
        log.info("  Domain vertices created in {}ms", domainEndTime - domainStartTime);

        log.info("  Creating customer vertices...");
        long customerStartTime = System.currentTimeMillis();
        createCustomerVertices();
        long customerEndTime = System.currentTimeMillis();
        log.info("  Customer vertices created in {}ms", customerEndTime - customerStartTime);

        long verticesEndTime = System.currentTimeMillis();
        log.info("Graph vertices creation completed in {}ms", verticesEndTime - verticesStartTime);

        // Create graph indexes for better query performance
        // Note: Indexes are created after vertices exist, so tables are available
        log.info("Creating graph indexes...");
        graphService.createGraphIndexes();
        log.info("Graph indexes creation completed");

        // Create relationships
        log.info("Building graph relationships...");
        long relationshipsStartTime = System.currentTimeMillis();

        log.info("  Creating expert-project relationships...");
        createExpertProjectRelationships();
        log.info("  Expert-project relationships created");

        log.info("  Creating expert-customer relationships...");
        createExpertCustomerRelationships();
        log.info("  Expert-customer relationships created");

        log.info("  Creating project-technology relationships...");
        createProjectTechnologyRelationships();
        log.info("  Project-technology relationships created");

        log.info("  Creating project-domain relationships...");
        createProjectDomainRelationships();
        log.info("  Project-domain relationships created");

        long relationshipsEndTime = System.currentTimeMillis();
        log.info("Graph relationships creation completed in {}ms", relationshipsEndTime - relationshipsStartTime);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        log.info("Graph build process completed successfully in {}ms", totalTime);
        log.info("Graph build summary:");
        log.info("  - Total execution time: {}ms", totalTime);
        log.info("  - Vertices creation time: {}ms", verticesEndTime - verticesStartTime);
        log.info("  - Relationships creation time: {}ms", relationshipsEndTime - relationshipsStartTime);
    }

    /**
     * Creates Expert vertices from employees table.
     */
    private void createExpertVertices() {
        List<ExpertData> experts = repository.findAllExperts();
        experts.forEach(expert ->
                createExpertVertex(expert.id(), expert.name(), expert.email(), expert.seniority()));
        log.info("  Created {} expert vertices", experts.size());
    }

    /**
     * Creates Project vertices from work_experience table.
     */
    private void createProjectVertices() {
        List<ProjectData> projects = repository.findAllProjects();
        projects.forEach(project -> {
            String projectId = project.projectId();
            // Use existing project_id if available, otherwise generate new one
            if (projectId == null || projectId.isEmpty()) {
                projectId = IdGenerator.generateId();
            }
            // Store mapping for backward compatibility (though not used anymore)
            projectIdMap.put(project.projectName(), projectId);
            createProjectVertex(projectId, project.projectName(), project.projectType());
        });
        log.info("  Created {} project vertices", projects.size());
    }

    /**
     * Creates Technology vertices from work_experience technologies.
     */
    private void createTechnologyVertices() {
        List<String> technologies = repository.findAllTechnologies();
        technologies.forEach(this::createTechnologyVertex);
        log.info("  Created {} technology vertices", technologies.size());
    }

    /**
     * Creates Domain vertices from industries.
     */
    private void createDomainVertices() {
        List<String> domains = repository.findAllDomains();
        domains.forEach(this::createDomainVertex);
        log.info("  Created {} domain vertices", domains.size());
    }

    /**
     * Creates Customer vertices from work_experience table.
     */
    private void createCustomerVertices() {
        List<CustomerData> customers = repository.findAllCustomers();
        customers.forEach(customer ->
                createCustomerVertex(customer.customerId(), customer.customerName()));
        log.info("  Created {} customer vertices", customers.size());
    }

    /**
     * Creates PARTICIPATED_IN relationships between experts and projects.
     */
    private void createExpertProjectRelationships() {
        long startTime = System.currentTimeMillis();
        List<ParticipationRelationship> relationships = repository.findAllExpertProjectRelationships();

        // Use batched approach with chunking for better performance
        // Process in chunks of 1000 to balance performance and query size
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<ParticipationRelationship> batch = relationships.subList(i, end);
            createParticipationRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} expert-project relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} expert-project relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates WORKED_FOR relationships between experts and customers.
     */
    private void createExpertCustomerRelationships() {
        long startTime = System.currentTimeMillis();
        List<ExpertCustomerRelationship> relationships = repository.findAllExpertCustomerRelationships();

        // Process in chunks of 1000 to balance performance and query size
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<ExpertCustomerRelationship> batch = relationships.subList(i, end);
            createExpertCustomerRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} expert-customer relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} expert-customer relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates USES relationships between projects and technologies.
     */
    private void createProjectTechnologyRelationships() {
        long startTime = System.currentTimeMillis();
        List<ProjectTechnologyRelationship> relationships = repository.findAllProjectTechnologyRelationships();

        // Then create all relationships in batches to avoid huge query strings
        // Process in chunks of 1000 to balance performance and query size
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<ProjectTechnologyRelationship> batch = relationships.subList(i, end);
            createUsesRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} project-technology relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} unique project-technology relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates IN_DOMAIN relationships between projects and domains.
     */
    private void createProjectDomainRelationships() {
        long startTime = System.currentTimeMillis();
        List<String> relationships = repository.findAllProjectDomainRelationships();
        relationships.forEach(rel -> {
            String[] parts = rel.split("-", 2);
            if (parts.length == 2) {
                createInDomainRelationship(parts[0], parts[1]);
            }
        });

        long endTime = System.currentTimeMillis();
        log.info("  Created {} project-domain relationships in {}ms", relationships.size(), endTime - startTime);
    }

    /**
     * Creates FOR_CUSTOMER relationships between projects and customers.
     */
    private void createProjectCustomerRelationships() {
        long startTime = System.currentTimeMillis();
        List<ProjectCustomerRelationship> relationships = repository.findAllProjectCustomerRelationships();

        // Process in chunks of 1000 to balance performance and query size
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<ProjectCustomerRelationship> batch = relationships.subList(i, end);
            createProjectCustomerRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} project-customer relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} project-customer relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates a single Expert vertex.
     */
    @Override
    public void createExpertVertex(String expertId, String name, String email, String seniority) {
        String cypher = """
                CREATE (e:Expert {
                    id: $expertId,
                    name: $name,
                    email: $email,
                    seniority: $seniority
                })
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("expertId", expertId);
        params.put("name", name);
        params.put("email", email);
        params.put("seniority", seniority);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates a single Project vertex.
     */
    @Override
    public void createProjectVertex(String projectId, String projectName, String projectType) {
        String cypher = """
                CREATE (p:Project {
                    id: $projectId,
                    name: $projectName,
                    projectType: $projectType
                })
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("projectName", projectName);
        params.put("projectType", projectType);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates a single Technology vertex.
     */
    @Override
    public void createTechnologyVertex(String technologyName) {
        String cypher = """
                MERGE (t:Technology {name: $technologyName})
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("technologyName", technologyName);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates a single Domain vertex.
     */
    @Override
    public void createDomainVertex(String domainName) {
        String cypher = """
                MERGE (d:Domain {name: $domainName})
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("domainName", domainName);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates a single Customer vertex.
     * Uses MERGE on id only (not name) to ensure idempotency while allowing
     * property queries to work correctly. This is different from Technology/Domain
     * which use MERGE on name, but similar in that it prevents duplicates.
     */
    @Override
    public void createCustomerVertex(String customerId, String customerName) {
        String cypher = """
                MERGE (c:Customer {id: $customerId})
                SET c.name = $customerName
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("customerName", customerName);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates an IN_DOMAIN relationship.
     */
    @Override
    public void createInDomainRelationship(String projectId, String domainName) {
        String cypher = """
                MATCH (p:Project {id: $projectId})
                MERGE (d:Domain {name: $domainName})
                MERGE (p)-[:IN_DOMAIN]->(d)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("domainName", domainName);

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates a PARTICIPATED_IN relationship.
     */
    @Override
    public void createParticipationRelationship(String expertId, String projectId, String role) {
        // Apache AGE doesn't support ON CREATE SET / ON MATCH SET syntax
        // Use MERGE to ensure relationship exists, then SET the property
        // This approach works better with Apache AGE's relationship property handling
        String cypher = """
                MATCH (e:Expert {id: $expertId})
                MATCH (p:Project {id: $projectId})
                MERGE (e)-[r:PARTICIPATED_IN]->(p)
                    SET r.role = $role
                    RETURN r
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("expertId", expertId);
        params.put("projectId", projectId);
        params.put("role", role != null ? role : "Developer");

        graphService.executeCypher(cypher, params);
    }

    /**
     * Creates multiple PARTICIPATED_IN relationships in a single batched operation using UNWIND.
     * Uses UNWIND with SET to batch create relationships with properties efficiently.
     * This is significantly more efficient than creating relationships individually.
     * Based on Apache AGE source code (issue_1907 test), SET on MERGE with relationships is supported.
     */
    @Override
    public void createParticipationRelationshipsBatch(List<ParticipationRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Format relationships as Cypher list literal for UNWIND
        // Format: [{expertId: 'id1', projectId: 'id2', role: 'role1'}, ...]
        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (ParticipationRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            // Escape quotes in values
            String expertId = escapeCypherString(rel.expertId());
            String projectId = escapeCypherString(rel.projectId());
            String role = escapeCypherString(rel.role() != null ? rel.role() : "Developer");
            relationshipsList.append("{expertId: '").append(expertId)
                    .append("', projectId: '").append(projectId)
                    .append("', role: '").append(role).append("'}");
        }
        relationshipsList.append("]");

        // Use UNWIND to batch create all relationships in a single Cypher query
        // SET on MERGE is supported by Apache AGE (see issue_1907 test in AGE source)
        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (e:Expert {id: rel.expertId})
                MATCH (p:Project {id: rel.projectId})
                MERGE (e)-[r:PARTICIPATED_IN]->(p)
                SET r.role = rel.role
                """, relationshipsList);

        // Execute without parameters since we've embedded the list directly
        graphService.executeCypher(cypher, new HashMap<>());
    }

    /**
     * Creates multiple USES relationships in a single batched operation using UNWIND.
     * This is significantly more efficient than creating relationships individually
     * and prevents connection leaks.
     */
    @Override
    public void createUsesRelationshipsBatch(List<ProjectTechnologyRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Format relationships as Cypher list literal for UNWIND
        // Format: [{projectId: 'id1', technologyName: 'tech1'}, {projectId: 'id2', technologyName: 'tech2'}, ...]
        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (ProjectTechnologyRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            // Escape quotes in values
            String projectId = escapeCypherString(rel.projectId());
            String technologyName = escapeCypherString(rel.technologyName());
            relationshipsList.append("{projectId: '").append(projectId)
                    .append("', technologyName: '").append(technologyName).append("'}");
        }
        relationshipsList.append("]");

        // Use UNWIND to batch create all relationships in a single Cypher query
        // This is much more efficient than individual queries
        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (p:Project {id: rel.projectId})
                MERGE (t:Technology {name: rel.technologyName})
                MERGE (p)-[:USES]->(t)
                """, relationshipsList);

        // Execute without parameters since we've embedded the list directly
        graphService.executeCypher(cypher, new HashMap<>());
    }

    /**
     * Creates multiple FOR_CUSTOMER relationships in a single batched operation using UNWIND.
     * This is significantly more efficient than creating relationships individually.
     */
    @Override
    public void createProjectCustomerRelationshipsBatch(List<ProjectCustomerRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Format relationships as Cypher list literal for UNWIND
        // Format: [{projectId: 'id1', customerId: 'id2'}, {projectId: 'id3', customerId: 'id4'}, ...]
        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (ProjectCustomerRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            // Escape quotes in values
            String projectId = escapeCypherString(rel.projectId());
            String customerId = escapeCypherString(rel.customerId());
            relationshipsList.append("{projectId: '").append(projectId)
                    .append("', customerId: '").append(customerId).append("'}");
        }
        relationshipsList.append("]");

        // Use UNWIND to batch create all relationships in a single Cypher query
        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (p:Project {id: rel.projectId})
                MATCH (c:Customer {id: rel.customerId})
                MERGE (p)-[:FOR_CUSTOMER]->(c)
                """, relationshipsList);

        // Execute without parameters since we've embedded the list directly
        graphService.executeCypher(cypher, new HashMap<>());
    }

    /**
     * Creates multiple WORKED_FOR relationships in a single batched operation using UNWIND.
     * This is significantly more efficient than creating relationships individually.
     */
    @Override
    public void createExpertCustomerRelationshipsBatch(List<ExpertCustomerRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Format relationships as Cypher list literal for UNWIND
        // Format: [{expertId: 'id1', customerId: 'id2'}, {expertId: 'id3', customerId: 'id4'}, ...]
        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (ExpertCustomerRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            // Escape quotes in values
            String expertId = escapeCypherString(rel.expertId());
            String customerId = escapeCypherString(rel.customerId());
            relationshipsList.append("{expertId: '").append(expertId)
                    .append("', customerId: '").append(customerId).append("'}");
        }
        relationshipsList.append("]");

        // Use UNWIND to batch create all relationships in a single Cypher query
        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (e:Expert {id: rel.expertId})
                MATCH (c:Customer {id: rel.customerId})
                MERGE (e)-[:WORKED_FOR]->(c)
                """, relationshipsList);

        // Execute without parameters since we've embedded the list directly
        graphService.executeCypher(cypher, new HashMap<>());
    }

    /**
     * Escapes special characters in a string for use in Cypher queries.
     */
    private String escapeCypherString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Creates a USES relationship.
     */
    @Override
    public void createUsesRelationship(String projectId, String technologyName) {
        String cypher = """
                MATCH (p:Project {id: $projectId})
                MERGE (t:Technology {name: $technologyName})
                MERGE (p)-[:USES]->(t)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("technologyName", technologyName);

        graphService.executeCypher(cypher, params);
    }

}
