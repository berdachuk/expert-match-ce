package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.api.model.IngestionResult;
import com.berdachuk.expertmatch.ingestion.rest.IngestionController;
import com.berdachuk.expertmatch.ingestion.service.TestDataGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestionControllerJsonProfilesIT extends BaseIntegrationTest {

    @Autowired
    private IngestionController ingestionController;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private TestDataGenerator testDataGenerator;

    @BeforeEach
    void setUp() {
        // Clear all test data before each test
        testDataGenerator.clearTestData();
    }

    @Test
    void testIngestJsonProfiles_DefaultDirectory_ShouldProcessProfiles() {
        // When: Ingest from default directory (classpath:data)
        var response = ingestionController.ingestJsonProfiles(null, null);

        // Then: Should process at least Siarhei Berdachuk profile
        assertNotNull(response.getBody());
        IngestionResult result = response.getBody();
        assertTrue(result.getTotalProfiles() >= 1,
                "Expected at least 1 profile, got " + result.getTotalProfiles());
        assertTrue(result.getSuccessCount() >= 1,
                "Expected at least 1 successful profile, got " + result.getSuccessCount());

        // Verify employee was created
        String sql = "SELECT COUNT(*) FROM expertmatch.employee WHERE id = :id";
        Integer count = namedJdbcTemplate.queryForObject(sql,
                Map.of("id", "4000741400013306668"), Integer.class);
        assertEquals(1, count, "Employee should be created in database");
    }

    @Test
    void testIngestJsonProfiles_SingleFile_ShouldProcessFile() {
        // When: Ingest from single file
        var response = ingestionController.ingestJsonProfiles(null, "classpath:data/siarhei-berdachuk-profile.json");

        // Then: Should process the profile
        assertNotNull(response.getBody());
        IngestionResult result = response.getBody();
        assertEquals(1, result.getTotalProfiles());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testIngestJsonProfiles_Directory_ShouldProcessMultipleFiles() {
        // When: Ingest from directory
        var response = ingestionController.ingestJsonProfiles("classpath:data", null);

        // Then: Should process files in directory
        assertNotNull(response.getBody());
        IngestionResult result = response.getBody();
        assertTrue(result.getTotalProfiles() >= 1);
        assertTrue(result.getSuccessCount() >= 1);
    }

    @Test
    void testIngestJsonProfiles_FileParameterTakesPrecedence_ShouldProcessFile() {
        // When: Both directory and file specified, file should take precedence
        var response = ingestionController.ingestJsonProfiles("classpath:data",
                "classpath:data/siarhei-berdachuk-profile.json");

        // Then: Should process only the specified file
        assertNotNull(response.getBody());
        IngestionResult result = response.getBody();
        assertEquals(1, result.getTotalProfiles(),
                "Should process only the specified file, not the entire directory");
    }

    @Test
    void testIngestJsonProfiles_InvalidFile_ShouldReturnError() {
        // When: Ingest from non-existent file
        assertThrows(Exception.class, () -> {
            ingestionController.ingestJsonProfiles(null, "classpath:data/nonexistent.json");
        });
    }
}

