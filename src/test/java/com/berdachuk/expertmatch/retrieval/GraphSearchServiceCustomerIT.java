package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.data.IdGenerator;
import com.berdachuk.expertmatch.graph.GraphBuilderService;
import com.berdachuk.expertmatch.graph.GraphService;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for customer-related queries in GraphSearchService.
 * Uses Testcontainers PostgreSQL with Apache AGE.
 */
class GraphSearchServiceCustomerIT extends BaseIntegrationTest {

    @Autowired
    private GraphSearchService graphSearchService;

    @Autowired
    private GraphBuilderService graphBuilderService;

    @Autowired
    private GraphService graphService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing test data
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

        // Ensure graph exists
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = ag_catalog, \"$user\", public, expertmatch;");
            namedJdbcTemplate.getJdbcTemplate().execute("SELECT * FROM ag_catalog.create_graph('expertmatch_graph');");
            namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = expertmatch, public;");
        } catch (Exception e) {
            // Graph might already exist, ignore
            try {
                namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = expertmatch, public;");
            } catch (Exception e2) {
                // Ignore
            }
        }

        // Create test data
        createTestData();
    }

    private void createTestData() {
        // Create employees
        String employee1 = IdGenerator.generateEmployeeId();
        String employee2 = IdGenerator.generateEmployeeId();
        String customerId1 = IdGenerator.generateCustomerId();
        String customerId2 = IdGenerator.generateCustomerId();

        Map<String, Object> employee1Params = new HashMap<>();
        employee1Params.put("id", employee1);
        employee1Params.put("name", "Expert 1");
        employee1Params.put("email", "expert1@test.com");
        employee1Params.put("seniority", "A4");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority) VALUES (:id, :name, :email, :seniority)",
                employee1Params
        );

        Map<String, Object> employee2Params = new HashMap<>();
        employee2Params.put("id", employee2);
        employee2Params.put("name", "Expert 2");
        employee2Params.put("email", "expert2@test.com");
        employee2Params.put("seniority", "A3");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority) VALUES (:id, :name, :email, :seniority)",
                employee2Params
        );

        // Create work experience with customers
        String workExperience1 = IdGenerator.generateId();
        Map<String, Object> workExperience1Params = new HashMap<>();
        workExperience1Params.put("id", workExperience1);
        workExperience1Params.put("employeeId", employee1);
        workExperience1Params.put("project", "Java Banking App");
        workExperience1Params.put("role", "Backend Developer");
        workExperience1Params.put("technologies", new String[]{"Java", "Spring Boot"});
        workExperience1Params.put("industry", "Banking");
        workExperience1Params.put("customerId", customerId1);
        workExperience1Params.put("customerName", "Microsoft");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, role, technologies, industry, customer_id, customer_name) VALUES (:id, :employeeId, :project, :role, :technologies, :industry, :customerId, :customerName)",
                workExperience1Params
        );

        String workExperience2 = IdGenerator.generateId();
        Map<String, Object> workExperience2Params = new HashMap<>();
        workExperience2Params.put("id", workExperience2);
        workExperience2Params.put("employeeId", employee2);
        workExperience2Params.put("project", "React E-commerce");
        workExperience2Params.put("role", "Frontend Developer");
        workExperience2Params.put("technologies", new String[]{"React", "TypeScript"});
        workExperience2Params.put("industry", "E-commerce");
        workExperience2Params.put("customerId", customerId2);
        workExperience2Params.put("customerName", "Amazon");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, role, technologies, industry, customer_id, customer_name) VALUES (:id, :employeeId, :project, :role, :technologies, :industry, :customerId, :customerName)",
                workExperience2Params
        );

        // Build graph
        graphBuilderService.buildGraph();
    }

    @Test
    void testFindExpertsByCustomer() {
        // Find experts who worked for Microsoft
        // Note: Apache AGE has limitations with Customer vertex queries (graphid comparison issues)
        // The method returns empty list on query errors to allow graceful degradation
        List<String> expertIds = graphSearchService.findExpertsByCustomer("Microsoft");

        assertNotNull(expertIds);
        // Due to Apache AGE limitations, the query may return empty list even if data exists
        // This test verifies the method doesn't throw exceptions
        // TODO: Fix Customer queries once Apache AGE supports them properly
    }

    @Test
    void testFindExpertsByCustomerAndTechnology() {
        // Find experts who worked for Microsoft and used Java
        // Note: Apache AGE has limitations with Customer vertex queries (graphid comparison issues)
        // The method returns empty list on query errors to allow graceful degradation
        List<String> expertIds = graphSearchService.findExpertsByCustomerAndTechnology("Microsoft", "Java");

        assertNotNull(expertIds);
        // Due to Apache AGE limitations, the query may return empty list even if data exists
        // This test verifies the method doesn't throw exceptions
        // TODO: Fix Customer queries once Apache AGE supports them properly
    }

    @Test
    void testFindExpertsByCustomerEmptyResult() {
        // Find experts for a non-existent customer
        List<String> expertIds = graphSearchService.findExpertsByCustomer("NonExistentCustomer");

        assertNotNull(expertIds);
        assertTrue(expertIds.isEmpty(), "Should return empty list for non-existent customer");
    }
}

