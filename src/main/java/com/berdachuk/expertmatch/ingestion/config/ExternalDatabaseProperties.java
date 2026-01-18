package com.berdachuk.expertmatch.ingestion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for external database connection.
 */
@Component
@ConfigurationProperties(prefix = "expertmatch.ingestion.external-database")
@Getter
@Setter
public class ExternalDatabaseProperties {

    private boolean enabled = false;
    private String host;
    private Integer port = 5432;
    private String database;
    private String username;
    private String password;
    private String schema = "work_experience";
    private String jdbcUrl; // Optional: direct JDBC URL (useful for testing)
    private int connectionTimeout = 30000;
    private int maximumPoolSize = 5;
    private int minimumIdle = 2;

    /**
     * Builds JDBC URL from configuration properties.
     * The external database is READ-ONLY - no write operations are allowed.
     * If jdbcUrl is provided directly, it will be used (with readOnly parameter added if not present).
     * Otherwise, constructs URL from host, port, and database.
     *
     * @return JDBC URL string with read-only connection parameters
     */
    public String getJdbcUrl() {
        // If jdbcUrl is provided directly, use it (useful for testing with same datasource)
        if (jdbcUrl != null && !jdbcUrl.isEmpty()) {
            // Ensure readOnly parameter is present
            if (jdbcUrl.contains("readOnly=")) {
                return jdbcUrl;
            } else {
                String separator = jdbcUrl.contains("?") ? "&" : "?";
                return jdbcUrl + separator + "readOnly=true";
            }
        }
        // Otherwise, construct from host, port, and database
        // Add readOnly=true to ensure connections are marked as read-only
        // This prevents accidental write operations on the source database
        // Note: host can be null in test scenarios where jdbc-url is set via BeanPostProcessor
        // Schema is set via connection-init-sql in ExternalDatabaseConfig
        if (host == null || host.isEmpty()) {
            throw new IllegalStateException("Either jdbc-url or host must be configured for external database");
        }
        int effectivePort = port != null ? port : 5432;
        return String.format("jdbc:postgresql://%s:%d/%s?readOnly=true", host, effectivePort, database);
    }
}
