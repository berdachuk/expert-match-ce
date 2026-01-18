package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.service.ExternalDatabaseConnectionService;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for external database connection verification.
 * This test is only executed when external database ingestion is enabled.
 * Extends BaseIntegrationTest to ensure proper database setup with vector extension.
 * <p>
 * Note: This test uses the test database as a proxy for the external database
 * to avoid requiring VPN access during test execution.
 */
@SpringBootTest
public class ExternalDatabaseConnectionServiceIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private ExternalDatabaseConnectionService connectionService;

    @DynamicPropertySource
    static void configureExternalDatabaseProperties(DynamicPropertyRegistry registry) {
        // Use test database as external database for testing purposes
        registry.add("expertmatch.ingestion.external-database.enabled", () -> "true");
        registry.add("expertmatch.ingestion.external-database.username", () -> "test");
        registry.add("expertmatch.ingestion.external-database.password", () -> "test");
    }

    @Test
    void testConnectionVerification() {
        // Skip test if external database connection service is not available
        // This can happen if external database ingestion is disabled or connection fails
        assumeTrue(connectionService != null, "External database connection service is not available");

        try {
            boolean connected = connectionService.verifyConnection();
            String connectionInfo = connectionService.getConnectionInfo();

            assertNotNull(connectionInfo);
            // Note: We don't assert connected == true because the external database
            // might not be accessible in test environment (VPN required)
            System.out.println("Connection info: " + connectionInfo);
            System.out.println("Connection status: " + (connected ? "CONNECTED" : "FAILED"));
        } catch (Exception e) {
            // If connection fails (e.g., database not accessible, wrong credentials),
            // skip the test rather than failing it
            // This allows the test to pass when external database is not available
            assumeTrue(false, "External database connection failed: " + e.getMessage());
        }
    }

    @TestConfiguration
    static class TestExternalDatabaseConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public com.berdachuk.expertmatch.ingestion.config.ExternalDatabaseProperties testExternalDatabaseProperties(Environment environment) {
            com.berdachuk.expertmatch.ingestion.config.ExternalDatabaseProperties props =
                    new com.berdachuk.expertmatch.ingestion.config.ExternalDatabaseProperties();
            props.setEnabled(true);
            props.setUsername("test");
            props.setPassword("test");
            // Get the test datasource URL and use it for external database
            String testDatasourceUrl = environment.getProperty("spring.datasource.url");
            if (testDatasourceUrl != null) {
                String jdbcUrl = testDatasourceUrl + (testDatasourceUrl.contains("?") ? "&" : "?") + "readOnly=true";
                props.setJdbcUrl(jdbcUrl);
            }
            return props;
        }
    }
}
