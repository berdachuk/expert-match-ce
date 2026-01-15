package com.berdachuk.expertmatch.graph.service;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for building graph relationships from database data.
 * Populates Apache AGE graph with experts, projects, technologies, etc.
 */
@Slf4j
@Service
public class GraphBuilderServiceImpl implements GraphBuilderService {
    private static final String GRAPH_NAME = "expertmatch_graph";
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final GraphService graphService;
    // Map to maintain project name -> project ID mapping during graph build
    private final Map<String, String> projectIdMap = new HashMap<>();

    public GraphBuilderServiceImpl(NamedParameterJdbcTemplate namedJdbcTemplate, GraphService graphService) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.graphService = graphService;
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
            createGraph();
            log.info("Graph structure created");
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
        createGraphIndexes();
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
     * Creates graph if it doesn't exist.
     */
    private void createGraph() {
        try {
            // Use fully qualified function name - no need to set search_path
            String sql = String.format("SELECT * FROM ag_catalog.create_graph('%s')", GRAPH_NAME);
            if (sql != null) {
                namedJdbcTemplate.getJdbcTemplate().execute(sql);
            }
        } catch (Exception e) {
            // Graph might already exist, that's fine
            // Re-throw if it's not a "already exists" error
            String message = e.getMessage();
            if (message != null && !message.contains("already exists") && !message.contains("graph already exists")) {
                throw new RuntimeException("Failed to create graph", e);
            }
        }
    }

    /**
     * Creates graph indexes for frequently queried properties.
     * Apache AGE automatically creates indexes on id columns, but we can create
     * additional indexes on property values for better query performance.
     * Note: Indexes are created after vertices exist, so this is called after graph build.
     */
    private void createGraphIndexes() {
        try {
            // Apache AGE stores vertices in tables with pattern: graphname_vertexlabel
            // Create indexes on property values using GIN indexes for JSONB properties
            // Note: Indexes are created after vertices exist, so tables should be available

            String graphName = GRAPH_NAME;

            // Check if graph tables exist before creating indexes
            @SuppressWarnings("null")
            String checkTableSql = String.format("""
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables 
                        WHERE table_schema = 'ag_catalog' 
                        AND table_name = 'ag_%s_Expert'
                    )
                    """, graphName);

            if (checkTableSql == null) {
                log.debug("Could not build check table SQL, skipping index creation");
                return;
            }

            Boolean tableExists = namedJdbcTemplate.getJdbcTemplate().queryForObject(checkTableSql, Boolean.class);
            if (tableExists == null || !tableExists) {
                log.debug("Graph tables do not exist yet, skipping index creation");
                return;
            }

            // Create GIN indexes on properties JSONB column for efficient property lookups
            // These indexes help with queries like MATCH (e:Expert {id: '...'})
            try {
                @SuppressWarnings("null")
                String expertIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_expert_props_id 
                        ON ag_catalog.ag_%s_Expert USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                if (expertIndexSql != null) {
                    namedJdbcTemplate.getJdbcTemplate().execute(expertIndexSql);
                }
            } catch (Exception e) {
                log.debug("Could not create Expert index: {}", e.getMessage());
            }

            try {
                @SuppressWarnings("null")
                String projectIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_project_props_id 
                        ON ag_catalog.ag_%s_Project USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                if (projectIndexSql != null) {
                    namedJdbcTemplate.getJdbcTemplate().execute(projectIndexSql);
                }
            } catch (Exception e) {
                log.debug("Could not create Project index: {}", e.getMessage());
            }

            try {
                @SuppressWarnings("null")
                String technologyIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_technology_props_name 
                        ON ag_catalog.ag_%s_Technology USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                if (technologyIndexSql != null) {
                    namedJdbcTemplate.getJdbcTemplate().execute(technologyIndexSql);
                }
            } catch (Exception e) {
                log.debug("Could not create Technology index: {}", e.getMessage());
            }

            try {
                @SuppressWarnings("null")
                String customerIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_customer_props_id 
                        ON ag_catalog.ag_%s_Customer USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                if (customerIndexSql != null) {
                    namedJdbcTemplate.getJdbcTemplate().execute(customerIndexSql);
                }
            } catch (Exception e) {
                log.debug("Could not create Customer index: {}", e.getMessage());
            }

            log.debug("Graph indexes created successfully");
        } catch (Exception e) {
            // Index creation might fail if tables don't exist yet or indexes already exist
            // Log but continue - indexes are optional for functionality
            log.debug("Could not create graph indexes: {}", e.getMessage());
        }
    }

    /**
     * Creates Expert vertices from employees table.
     */
    private void createExpertVertices() {
        String sql = """
                SELECT id, name, email, seniority
                    FROM expertmatch.employee
                """;

        List<String> expertIds = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String id = rs.getString("id");
            String name = rs.getString("name");
            String email = rs.getString("email");
            String seniority = rs.getString("seniority");

            createExpertVertex(id, name, email, seniority);
            return id;
        });

        log.info("  Created {} expert vertices", expertIds.size());
    }

    /**
     * Creates Project vertices from work_experience table.
     */
    private void createProjectVertices() {
        String sql = """
                    SELECT DISTINCT project_id, project_name, industry
                FROM expertmatch.work_experience
                WHERE project_name IS NOT NULL
                """;

        List<String> projectIds = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String projectName = rs.getString("project_name");
            String industry = rs.getString("industry");

            // Use existing project_id if available, otherwise generate new one
            if (projectId == null || projectId.isEmpty()) {
                projectId = IdGenerator.generateId();
            }
            // Store mapping for backward compatibility (though not used anymore)
            projectIdMap.put(projectName, projectId);
            createProjectVertex(projectId, projectName, industry);
            return projectId;
        });

        log.info("  Created {} project vertices", projectIds.size());
    }

    /**
     * Creates Technology vertices from work_experience technologies.
     */
    private void createTechnologyVertices() {
        String sql = """
                SELECT DISTINCT unnest(technologies) as technology
                FROM expertmatch.work_experience
                WHERE technologies IS NOT NULL
                """;

        List<String> technologies = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String technology = rs.getString("technology");
            if (technology != null && !technology.isEmpty()) {
                createTechnologyVertex(technology);
                return technology;
            }
            return null;
        }).stream().filter(t -> t != null).toList();

        log.info("  Created {} technology vertices", technologies.size());
    }

    /**
     * Creates Domain vertices from industries.
     */
    private void createDomainVertices() {
        String sql = """
                SELECT DISTINCT industry as domain
                FROM expertmatch.work_experience
                WHERE industry IS NOT NULL
                """;

        List<String> domains = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String domain = rs.getString("domain");
            if (domain != null && !domain.isEmpty()) {
                createDomainVertex(domain);
                return domain;
            }
            return null;
        }).stream().filter(d -> d != null).toList();

        log.info("  Created {} domain vertices", domains.size());
    }

    /**
     * Creates Customer vertices from work_experience table.
     */
    private void createCustomerVertices() {
        String sql = """
                SELECT DISTINCT 
                    COALESCE(customer_id, 'CUSTOMER_' || customer_name) as customer_id,
                    customer_name
                FROM expertmatch.work_experience
                WHERE customer_name IS NOT NULL
                """;

        // Use LinkedHashSet for deduplication (like Technology vertices)
        Set<String> customerSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String customerId = rs.getString("customer_id");
            String customerName = rs.getString("customer_name");
            if (customerName != null && !customerName.isEmpty()) {
                // Generate customer ID if it's null or empty
                // COALESCE in SQL already handles the fallback pattern, so customerId should not be null
                // But if it is null or empty, generate one
                if (customerId == null || customerId.isEmpty()) {
                    // Fallback: generate customer ID
                    customerId = IdGenerator.generateCustomerId();
                }
                customerSet.add(customerId + "|" + customerName);
                createCustomerVertex(customerId, customerName);
                return customerId;
            }
            return null;
        });

        log.info("  Created {} customer vertices", customerSet.size());
    }

    /**
     * Creates PARTICIPATED_IN relationships between experts and projects.
     */
    private void createExpertProjectRelationships() {
        long startTime = System.currentTimeMillis();
        String sql = """
                    SELECT we.employee_id, we.project_id, we.project_name, we.role, we.start_date, we.end_date
                FROM expertmatch.work_experience we
                WHERE we.project_name IS NOT NULL
                """;

        List<ParticipationRelationship> relationships = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String employeeId = rs.getString("employee_id");
            String projectId = rs.getString("project_id");
            String role = rs.getString("role");

            if (projectId != null) {
                return new ParticipationRelationship(employeeId, projectId, role);
            }
            return null;
        }).stream().filter(r -> r != null).toList();

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
        String sql = """
                SELECT DISTINCT 
                    we.employee_id,
                    COALESCE(we.customer_id, 'CUSTOMER_' || we.customer_name) as customer_id
                FROM expertmatch.work_experience we
                WHERE we.customer_name IS NOT NULL
                """;

        // Use LinkedHashSet for deduplication
        Set<ExpertCustomerRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String employeeId = rs.getString("employee_id");
            String customerId = rs.getString("customer_id");

            if (employeeId != null && customerId != null) {
                relationshipSet.add(new GraphBuilderService.ExpertCustomerRelationship(employeeId, customerId));
            }
            return null;
        });

        List<ExpertCustomerRelationship> relationships = new ArrayList<>(relationshipSet);

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
        String sql = """
                        SELECT DISTINCT we.project_id, we.project_name, unnest(we.technologies) as technology
                FROM expertmatch.work_experience we
                WHERE we.technologies IS NOT NULL AND we.project_name IS NOT NULL
                """;

        // First collect all relationships without creating them immediately
        // Use a Set to deduplicate relationships (same project-technology pair may appear multiple times)
        Set<ProjectTechnologyRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String technology = rs.getString("technology");

            if (technology != null && !technology.isEmpty() && projectId != null) {
                relationshipSet.add(new GraphBuilderService.ProjectTechnologyRelationship(projectId, technology));
            }
            return null;
        });

        List<ProjectTechnologyRelationship> relationships = new ArrayList<>(relationshipSet);

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
        String sql = """
                    SELECT DISTINCT we.project_id, we.project_name, we.industry as domain
                FROM expertmatch.work_experience we
                WHERE we.industry IS NOT NULL AND we.project_name IS NOT NULL
                """;

        List<String> relationships = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String domain = rs.getString("domain");

            if (domain != null && !domain.isEmpty() && projectId != null) {
                createInDomainRelationship(projectId, domain);
                return projectId + "-" + domain;
            }
            return null;
        }).stream().filter(r -> r != null).toList();

        long endTime = System.currentTimeMillis();
        log.info("  Created {} project-domain relationships in {}ms", relationships.size(), endTime - startTime);
    }

    /**
     * Creates FOR_CUSTOMER relationships between projects and customers.
     */
    private void createProjectCustomerRelationships() {
        long startTime = System.currentTimeMillis();
        String sql = """
                SELECT DISTINCT 
                    we.project_id,
                    COALESCE(we.customer_id, 'CUSTOMER_' || we.customer_name) as customer_id
                FROM expertmatch.work_experience we
                WHERE we.customer_name IS NOT NULL AND we.project_id IS NOT NULL
                """;

        // Use LinkedHashSet for deduplication
        Set<ProjectCustomerRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String customerId = rs.getString("customer_id");

            if (projectId != null && customerId != null) {
                relationshipSet.add(new GraphBuilderService.ProjectCustomerRelationship(projectId, customerId));
            }
            return null;
        });

        List<ProjectCustomerRelationship> relationships = new ArrayList<>(relationshipSet);

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
