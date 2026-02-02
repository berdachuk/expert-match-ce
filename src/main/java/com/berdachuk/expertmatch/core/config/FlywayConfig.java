package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration to ensure Flyway uses only the primary DataSource,
 * not the external ingestion DataSource.
 * <p>
 * When multiple DataSources exist, Flyway must be explicitly configured
 * to use the primary DataSource (application database), not the external
 * read-only ingestion database.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

    /**
     * Creates a Flyway bean that uses ONLY the primary DataSource.
     * <p>
     * IMPORTANT: This explicitly excludes the external read-only ingestion datasource.
     * Flyway migrations will NEVER run on the external datasource, even if it exists.
     * <p>
     * The primary DataSource (from spring.datasource.*) is injected without qualifier
     * since Spring Boot marks it as @Primary automatically. The external datasource
     * is NOT marked as @Primary, so it will never be selected by this method.
     *
     * @param primaryDataSource The primary application datasource (auto-configured as @Primary)
     * @param properties        Flyway configuration properties
     * @return Configured Flyway instance that uses only the primary datasource
     */
    @Bean
    @Primary
    public Flyway flyway(@org.springframework.beans.factory.annotation.Autowired(required = false)
                         @org.springframework.beans.factory.annotation.Qualifier("dataSource")
                         DataSource primaryDataSource,
                         org.springframework.boot.autoconfigure.jdbc.DataSourceProperties dataSourceProperties,
                         FlywayProperties properties) {
        // Try to use existing primary DataSource first (created by Spring Boot auto-configuration)
        // If not available, create one explicitly from properties to ensure we use the PRIMARY database
        // This prevents Flyway from accidentally using the external read-only DataSource
        DataSource actualDataSource = primaryDataSource;

        if (actualDataSource == null) {
            log.info("Primary DataSource not found, creating PRIMARY DataSource for Flyway from properties - URL: {}, User: {}",
                    dataSourceProperties.getUrl(), dataSourceProperties.getUsername());

            com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
            config.setJdbcUrl(dataSourceProperties.getUrl());
            config.setUsername(dataSourceProperties.getUsername());
            config.setPassword(dataSourceProperties.getPassword());
            config.setDriverClassName(dataSourceProperties.getDriverClassName());
            config.setPoolName("FlywayPrimaryDBPool");
            // Use minimal pool size for Flyway to avoid connection exhaustion
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(1);
            actualDataSource = new com.zaxxer.hikari.HikariDataSource(config);
        } else {
            log.info("Using existing PRIMARY DataSource for Flyway");
        }
        // Verify we're using the primary DataSource by checking its URL
        try {
            java.sql.Connection conn = actualDataSource.getConnection();
            try {
                String url = conn.getMetaData().getURL();
                String username = conn.getMetaData().getUserName();
                log.info("Flyway will use PRIMARY DataSource - URL: {}, User: {}, Schema: {} (external read-only datasource is EXCLUDED)",
                        url, username, properties.getSchemas());

                // Verify this is NOT the external database
                if (url != null && (url.contains("aist-tool-networking") || url.contains("readOnly=true"))) {
                    throw new IllegalStateException("Flyway is configured to use EXTERNAL database instead of PRIMARY! " +
                            "Expected: jdbc:postgresql://localhost:5433/expertmatch, Got: " + url);
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.error("Could not verify DataSource URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to verify primary DataSource for Flyway", e);
        }

        // Use primary DataSource (application database) - Spring Boot auto-configures it as @Primary
        // This ensures Flyway does NOT use the external read-only ingestion database
        // The external datasource is NOT @Primary, so it will never be injected here
        // Configure Flyway with search_path set to include public schema for vector type access
        String schemas = String.join(", ", properties.getSchemas());
        String initSql = "SET search_path = public, " + schemas + ";";
        log.info("Configuring Flyway with initSql: {}", initSql);

        Flyway flyway = Flyway.configure()
                .dataSource(actualDataSource)
                .locations(properties.getLocations().toArray(new String[0]))
                .schemas(properties.getSchemas().toArray(new String[0]))
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .baselineVersion(properties.getBaselineVersion() != null ?
                        properties.getBaselineVersion() : "0")
                .initSql(initSql)
                // Disable validation on migrate to allow checksum updates after file changes
                .validateOnMigrate(false)
                .load();

        // Run migrations immediately to ensure schema is created
        if (properties.isEnabled()) {
            log.info("Running Flyway migrations on PRIMARY datasource only...");
            flyway.migrate();
            log.info("Flyway migrations completed successfully on primary datasource");
        } else {
            log.warn("Flyway is disabled in configuration");
        }

        return flyway;
    }

    /**
     * Creates a FlywayMigrationInitializer to ensure migrations run during application startup.
     * This bean depends on the Flyway bean and will trigger migrations if they haven't run yet.
     */
    @Bean
    @DependsOn("flyway")
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        // Flyway bean already ran migrations in flyway() method above,
        // but this ensures Spring Boot's migration initializer is also created
        // to maintain compatibility with Spring Boot's auto-configuration
        return new FlywayMigrationInitializer(flyway, null);
    }
}
