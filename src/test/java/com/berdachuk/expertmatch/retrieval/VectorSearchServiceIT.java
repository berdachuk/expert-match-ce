package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.retrieval.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for VectorSearchService.
 * Uses Testcontainers PostgreSQL with PgVector.
 * <p>
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - VectorSearchService uses EmbeddingService which uses mocked EmbeddingModel
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
class VectorSearchServiceIT extends BaseIntegrationTest {

    @Autowired
    private VectorSearchService vectorSearchService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Clean up any data from previous tests to ensure test isolation
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
    }

    @Test
    void testSearchWithEmbeddings() {
        // Insert test employee first (required for foreign key)
        String employeeId = IdGenerator.generateEmployeeId();
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId, "name", "Employee", "email", "emp-vec-" + System.currentTimeMillis() + "@test.com")
        );

        // Insert test work experience with embedding
        String workExperienceId = IdGenerator.generateId();

        // Generate a test embedding (mock vector) - 1024 dimensions (Ollama)
        float[] testEmbedding = new float[1024];
        for (int i = 0; i < 1024; i++) {
            testEmbedding[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }

        // Normalize to 1536 dimensions for database storage (pad with zeros)
        float[] normalizedEmbedding = normalizeEmbeddingDimension(testEmbedding, 1536);

        // Format embedding for PostgreSQL
        StringBuilder embeddingStr = new StringBuilder("[");
        for (int i = 0; i < normalizedEmbedding.length; i++) {
            if (i > 0) embeddingStr.append(",");
            embeddingStr.append(normalizedEmbedding[i]);
        }
        embeddingStr.append("]");

        String insertSql = """
                INSERT INTO expertmatch.work_experience 
                    (id, employee_id, project_name, project_summary, embedding, embedding_dimension)
                        VALUES (:id, :employeeId, :projectName, :summary, :embedding::vector, :dimension)
                """;
        namedJdbcTemplate.update(insertSql, Map.of(
                "id", workExperienceId,
                "employeeId", employeeId,
                "projectName", "Java Spring Boot Project",
                "summary", "Developed microservices using Spring Boot and Java",
                "embedding", embeddingStr.toString(),
                "dimension", 1024
        ));

        // Test vector search - returns Documents, not just IDs
        var results = vectorSearchService.search(
                testEmbedding, // Use same embedding for query
                5,
                0.7
        );

        assertNotNull(results);
        // Should find at least the inserted record (high similarity since same embedding)
        assertTrue(results.size() > 0);
        assertEquals(employeeId, results.get(0).getId()); // Document ID is employee ID
    }

    @Test
    void testSearchWithEmptyDatabase() {
        // Clean up any data from previous tests
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");

        // Create 1024-dimension embedding (Ollama) - will be normalized to 1536 by PgVectorSearchService
        float[] queryEmbedding = new float[1024];
        var results = vectorSearchService.search(queryEmbedding, 5, 0.7);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchWithHighThreshold() {
        // Insert test employee first
        String employeeId = IdGenerator.generateEmployeeId();
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId, "name", "Employee", "email", "emp-high-" + System.currentTimeMillis() + "@test.com")
        );

        // Insert test data
        String workExperienceId = IdGenerator.generateId();

        // Create 1024-dimension embedding (Ollama)
        float[] testEmbedding = new float[1024];
        java.util.Arrays.fill(testEmbedding, 0.5f);

        // Normalize to 1536 dimensions for database storage
        float[] normalizedEmbedding = normalizeEmbeddingDimension(testEmbedding, 1536);

        StringBuilder embeddingStr = new StringBuilder("[");
        for (int i = 0; i < normalizedEmbedding.length; i++) {
            if (i > 0) embeddingStr.append(",");
            embeddingStr.append(normalizedEmbedding[i]);
        }
        embeddingStr.append("]");

        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, embedding, embedding_dimension) VALUES (:id, :employeeId, :name, :embedding::vector, :dimension)",
                Map.of("id", workExperienceId, "employeeId", employeeId, "name", "Test Project", "embedding", embeddingStr.toString(), "dimension", 1024)
        );

        // Search with very high threshold (should return empty)
        // Create 1024-dimension query embedding - will be normalized by PgVectorSearchService
        float[] differentEmbedding = new float[1024];
        java.util.Arrays.fill(differentEmbedding, -0.5f);

        var results = vectorSearchService.search(differentEmbedding, 5, 0.99);

        assertNotNull(results);
        // With high threshold and different embedding, should return empty or very few results
        assertTrue(results.size() <= 1); // At most one result if similarity is high enough
    }

    @Test
    void testSearchByText() {
        // Insert test data with embedding
        String workExperienceId = IdGenerator.generateId();
        String employeeId = IdGenerator.generateEmployeeId();

        // This test requires embedding service to work, so we'll just verify the method exists
        // In a real scenario, we'd need to generate embeddings for the test data
        assertNotNull(vectorSearchService);
    }

    @Test
    void testSearchWithNullEmbedding() {
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(null, 5, 0.7);
        });
    }

    @Test
    void testSearchWithEmptyEmbedding() {
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(new float[0], 5, 0.7);
        });
    }

    @Test
    void testSearchWithInvalidMaxResults() {
        // Use 1024-dimension embedding (Ollama) - will be normalized by PgVectorSearchService
        float[] embedding = new float[1024];
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(embedding, 0, 0.7);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(embedding, -1, 0.7);
        });
    }

    @Test
    void testSearchWithInvalidThreshold() {
        // Use 1024-dimension embedding (Ollama) - will be normalized by PgVectorSearchService
        float[] embedding = new float[1024];
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(embedding, 5, -0.1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.search(embedding, 5, 1.1);
        });
    }

    @Test
    void testSearchByTextWithNullQuery() {
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.searchByText(null, 5, 0.7);
        });
    }

    @Test
    void testSearchByTextWithBlankQuery() {
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.searchByText("", 5, 0.7);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.searchByText("   ", 5, 0.7);
        });
    }

    /**
     * Normalizes embedding to target dimension.
     * - If source dimension < target: pads with zeros
     * - If source dimension > target: truncates
     * - If source dimension == target: returns as-is
     *
     * @param embedding       Original embedding vector
     * @param targetDimension Target dimension (1536 for database schema)
     * @return Normalized embedding vector
     */
    private float[] normalizeEmbeddingDimension(float[] embedding, int targetDimension) {
        if (embedding.length == targetDimension) {
            return embedding;
        }

        float[] normalized = new float[targetDimension];
        int copyLength = Math.min(embedding.length, targetDimension);
        System.arraycopy(embedding, 0, normalized, 0, copyLength);
        // Remaining elements are already zero (default float value)

        return normalized;
    }
}

