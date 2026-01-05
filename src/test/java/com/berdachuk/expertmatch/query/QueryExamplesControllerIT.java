package com.berdachuk.expertmatch.query;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for QueryExamplesController.
 * Tests the REST endpoint that provides example queries.
 * <p>
 * This is a simple endpoint test that does not require database or LLM mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class QueryExamplesControllerIT extends BaseIntegrationTest {

    @SuppressWarnings("null")
    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetQueryExamples_ReturnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }

    @Test
    void testGetQueryExamples_ReturnsExamplesArray() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples").exists())
                .andExpect(jsonPath("$.examples").isArray())
                .andExpect(jsonPath("$.examples.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void testGetQueryExamples_EachExampleHasRequiredFields() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples[0].category").exists())
                .andExpect(jsonPath("$.examples[0].category").isString())
                .andExpect(jsonPath("$.examples[0].title").exists())
                .andExpect(jsonPath("$.examples[0].title").isString())
                .andExpect(jsonPath("$.examples[0].query").exists())
                .andExpect(jsonPath("$.examples[0].query").isString());
    }

    @Test
    void testGetQueryExamples_ContainsExpectedCategories() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples[*].category").exists())
                .andExpect(jsonPath("$.examples[?(@.category == 'Basic')]").exists())
                .andExpect(jsonPath("$.examples[?(@.category == 'Technology')]").exists())
                .andExpect(jsonPath("$.examples[?(@.category == 'Seniority')]").exists());
    }

    @Test
    void testGetQueryExamples_NoAuthenticationRequired() throws Exception {
        // Act & Assert - Should work without authentication headers
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testGetQueryExamples_WorksWithUserHeaders() throws Exception {
        // Act & Assert - Should work with user headers (optional)
        mockMvc.perform(get("/api/v1/query/examples")
                        .header("X-User-Id", "test-user-123")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples").exists());
    }

    @Test
    void testGetQueryExamples_ExamplesHaveNonEmptyFields() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examples[0].category").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(jsonPath("$.examples[0].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(jsonPath("$.examples[0].query").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())));
    }

    @Test
    void testGetQueryExamples_ReturnsConsistentResults() throws Exception {
        // Act - Call endpoint twice
        String firstResponse = mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(get("/api/v1/query/examples")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - Results should be consistent
        assertEquals(firstResponse, secondResponse, () -> "Results should be consistent across multiple calls");
    }
}

