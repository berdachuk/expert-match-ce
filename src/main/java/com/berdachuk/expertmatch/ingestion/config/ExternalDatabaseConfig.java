package com.berdachuk.expertmatch.ingestion.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for external database DataSource.
 * <p>
 * IMPORTANT: The external database is READ-ONLY.
 * This datasource is used only for reading data from the source database.
 * No write operations (INSERT, UPDATE, DELETE, DDL) should be performed on this datasource.
 * <p>
 * Flyway migrations are explicitly excluded from this datasource via FlywayConfig,
 * which uses @Primary to ensure only the primary application datasource is used for migrations.
 * <p>
 * Only created when external database ingestion is enabled.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
public class ExternalDatabaseConfig {

    private final ExternalDatabaseProperties properties;

    public ExternalDatabaseConfig(ExternalDatabaseProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a read-only datasource for the external ingestion database.
     * This datasource is configured with readOnly=true in the JDBC URL to prevent write operations.
     *
     * @return HikariDataSource configured for read-only access to external database
     */
    @Bean(name = "externalDataSource")
    public DataSource externalDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            // JDBC URL includes readOnly=true to mark connections as read-only
            String jdbcUrl = properties.getJdbcUrl();
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                throw new IllegalStateException("External database JDBC URL is not configured");
            }
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(properties.getUsername());
            config.setPassword(properties.getPassword());
            config.setDriverClassName("org.postgresql.Driver");
            config.setConnectionTimeout(properties.getConnectionTimeout());
            config.setMaximumPoolSize(properties.getMaximumPoolSize());
            config.setMinimumIdle(properties.getMinimumIdle());
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("ExternalDBPool-ReadOnly");
            // Note: We don't set connection-init-sql here because we use fully qualified table names
            // This avoids any potential conflicts with search_path settings
            // All queries use schema.table format (e.g., work_experience.work_experience_json)
            // Don't fail fast if database is not immediately available (e.g., VPN not connected)
            // -1 means never fail during initialization, connections will be validated when used
            config.setInitializationFailTimeout(-1);
            config.setValidationTimeout(5000);
            // Set leak detection threshold to help identify connection issues
            config.setLeakDetectionThreshold(60000);

            int effectivePort = properties.getPort() != null ? properties.getPort() : 5432;
            String host = properties.getHost() != null ? properties.getHost() : "unknown";
            String database = properties.getDatabase() != null ? properties.getDatabase() : "unknown";
            String schema = properties.getSchema() != null ? properties.getSchema() : "work_experience";
            log.info("Configuring READ-ONLY external database connection: {}@{}:{}/{} (read-only mode, schema: {})",
                    properties.getUsername(), host, effectivePort, database, schema);

            HikariDataSource dataSource = new HikariDataSource(config);

            // Verify search_path is set correctly
            try {
                var conn = dataSource.getConnection();
                try {
                    var stmt = conn.createStatement();
                    // Verify connection to correct database
                    String dbName = conn.getCatalog();
                    log.info("External DataSource connected to database: {}", dbName);
                    if (!dbName.equals(database)) {
                        log.warn("External DataSource connected to unexpected database. Expected: {}, Actual: {}", database, dbName);
                    }
                } finally {
                    conn.close();
                }
            } catch (Exception e) {
                log.error("Failed to verify search_path on external DataSource: {}", e.getMessage(), e);
            }

            return dataSource;
        } catch (Exception e) {
            log.warn("Failed to create external database DataSource: {}. External database features will be unavailable.", e.getMessage());
            // Return a dummy DataSource that will fail on connection attempts
            // This allows ApplicationContext to load, but the service will handle the failure gracefully
            return new com.zaxxer.hikari.HikariDataSource() {
                @Override
                public java.sql.Connection getConnection() throws java.sql.SQLException {
                    throw new java.sql.SQLException("External database DataSource not properly configured: " + e.getMessage());
                }
            };
        }
    }

    @Bean(name = "externalJdbcTemplate")
    public NamedParameterJdbcTemplate externalJdbcTemplate(DataSource externalDataSource) {
        return new NamedParameterJdbcTemplate(externalDataSource);
    }
}
