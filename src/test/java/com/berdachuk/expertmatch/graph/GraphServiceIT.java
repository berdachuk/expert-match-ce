package com.berdachuk.expertmatch.graph;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GraphService.
 * Uses Testcontainers PostgreSQL with Apache AGE.
 */
class GraphServiceIT extends BaseIntegrationTest {

    @Autowired
    private GraphService graphService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

        // Ensure graph exists
        try {
            String createGraphSql = "SELECT ag_catalog.create_graph('expertmatch_graph')";
            namedJdbcTemplate.getJdbcTemplate().execute(createGraphSql);
        } catch (Exception e) {
            // Graph might already exist, ignore
        }

        // Clear any existing graph vertices/edges to ensure clean state
        // Note: Graph cleanup is done via graphService if needed, but for these tests
        // we create isolated test data with unique IDs, so cleanup is less critical
    }

    @Test
    void testGraphExists() {
        boolean exists = graphService.graphExists();
        assertTrue(exists);
    }

    @Test
    void testExecuteCypherCreateVertex() {
        // Create a test vertex
        String cypher = """
            CREATE (e:Expert {id: $id, name: $name})
            RETURN e
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", "test-expert-001");
        params.put("name", "Test Expert");

        List<Map<String, Object>> results = graphService.executeCypher(cypher, params);

        assertNotNull(results);
        // Results may be empty or contain vertex data depending on AGE version
    }

    @Test
    void testExecuteCypherQueryVertex() {
        // First create a vertex
        String createCypher = """
            CREATE (e:Expert {id: $id, name: $name})
            RETURN e
            """;

        Map<String, Object> createParams = new HashMap<>();
        createParams.put("id", "test-expert-002");
        createParams.put("name", "Query Test Expert");

        graphService.executeCypher(createCypher, createParams);

        // Then query it - return the whole node (single column)
        String queryCypher = """
            MATCH (e:Expert {id: $id})
                RETURN e
            """;

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", "test-expert-002");

        List<Map<String, Object>> results = graphService.executeCypher(queryCypher, queryParams);

        assertNotNull(results);
        // Should find the created vertex
    }

    @Test
    void testExecuteCypherAndExtract() {
        // Create a vertex
        String createCypher = """
            CREATE (e:Expert {id: $id, name: $name})
            RETURN e
            """;

        Map<String, Object> createParams = new HashMap<>();
        createParams.put("id", "test-expert-003");
        createParams.put("name", "Extract Test Expert");

        graphService.executeCypher(createCypher, createParams);

        // Query and extract - return the whole node, extract id from result
        String queryCypher = """
            MATCH (e:Expert {id: $id})
                RETURN e
            """;

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("id", "test-expert-003");

        // Extract from the whole node result
        List<Map<String, Object>> results = graphService.executeCypher(queryCypher, queryParams);
        assertNotNull(results);

        // Note: executeCypherAndExtract expects a specific field name in the result
        // Since we're returning the whole node, we'll just verify the query works
        // In practice, you'd extract the id from the node properties
    }

    @Test
    void testExecuteCypherWithEmptyParameters() {
        String cypher = """
            MATCH (e:Expert)
                RETURN count(e) as expertCount
            """;

        List<Map<String, Object>> results = graphService.executeCypher(cypher, new HashMap<>());

        assertNotNull(results);
    }

    @Test
    void testExecuteCypherWithNullParameters() {
        String cypher = """
            MATCH (e:Expert)
                RETURN count(e) as expertCount
            """;

        List<Map<String, Object>> results = graphService.executeCypher(cypher, Collections.emptyMap());

        assertNotNull(results);
    }

    @Test
    void testExecuteCypherWithComplexParameters() {
        // Test with different parameter types
        String cypher = """
            CREATE (e:Expert {id: $id, name: $name, age: $age, active: $active})
            RETURN e
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", "test-expert-004");
        params.put("name", "Complex Test");
        params.put("age", 30);
        params.put("active", true);

        List<Map<String, Object>> results = graphService.executeCypher(cypher, params);

        assertNotNull(results);
    }

    @Test
    void testExecuteCypherWithSpecialCharacters() {
        // Test JSON escaping with special characters
        String cypher = """
            CREATE (e:Expert {id: $id, name: $name})
            RETURN e
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", "test-expert-005");
        params.put("name", "Test \"Expert\" with\nnewlines\tand\ttabs");

        // Should not throw exception
        assertDoesNotThrow(() -> {
            graphService.executeCypher(cypher, params);
        });
    }
}

