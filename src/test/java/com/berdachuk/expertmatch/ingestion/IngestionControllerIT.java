package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.service.TestDataGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for IngestionController.
 * Uses Testcontainers PostgreSQL and MockMvc for endpoint testing.
 * <p>
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - TestDataGenerator.generateEmbeddings() uses mocked EmbeddingModel
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class IngestionControllerIT extends BaseIntegrationTest {

    @SuppressWarnings("null")
    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestDataGenerator testDataGenerator;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

        // Ensure graph exists (required for build-graph and complete endpoints)
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
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTestData() throws Exception {
        mockMvc.perform(post("/api/v1/test-data")
                        .param("size", "small")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.size").value("small"));

        // Verify data was created
        Integer count = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expertmatch.employee",
                new HashMap<>(),
                Integer.class
        );
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateTestDataDefaultSize() throws Exception {
        mockMvc.perform(post("/api/v1/test-data")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetTestDataSizes() throws Exception {
        mockMvc.perform(get("/api/v1/test-data/sizes").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].size").value("tiny"))
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].estimatedTimeMinutes").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateEmbeddings() throws Exception {
        // First generate test data
        testDataGenerator.generateTestData("small");

        mockMvc.perform(post("/api/v1/test-data/embeddings")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testBuildGraph() throws Exception {
        // First generate test data
        testDataGenerator.generateTestData("small");

        mockMvc.perform(post("/api/v1/test-data/graph")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateCompleteDataset() throws Exception {
        mockMvc.perform(post("/api/v1/test-data/complete")
                        .param("size", "small")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.size").value("small"));

        // Verify data was created
        Integer empCount = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expertmatch.employee",
                new HashMap<>(),
                Integer.class
        );
        assertNotNull(empCount);
        assertTrue(empCount > 0);
    }

    @Test
    @Disabled("TestSecurityConfig permits all requests, so security tests cannot work with current test setup. " +
            "These tests would require a different security configuration that enforces authorization.")
    @WithMockUser(roles = "USER")
        // Non-admin user
    void testGenerateTestDataUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/test-data")
                        .param("size", "small")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Disabled("TestSecurityConfig permits all requests, so security tests cannot work with current test setup. " +
            "These tests would require a different security configuration that enforces authentication.")
    void testGenerateTestDataUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/test-data")
                        .param("size", "small")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

