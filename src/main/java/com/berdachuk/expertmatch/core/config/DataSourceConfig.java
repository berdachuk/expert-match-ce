package com.berdachuk.expertmatch.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration for the primary DataSource.
 * Ensures search_path is set correctly on all database connections.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private Integer maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private Integer minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private Long connectionTimeout;

    /**
     * Creates the primary DataSource with search_path configured.
     * This ensures all database connections can access tables in the expertmatch schema.
     */
    @Bean
    @Primary
    public DataSource primaryDataSource(DataSourceProperties properties) {
        log.info("Creating primary DataSource with search_path configuration - URL: {}, User: {}",
                properties.getUrl(), properties.getUsername());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());

        // Set search_path to include public and expertmatch schemas
        // This ensures vector type is accessible and tables in expertmatch schema can be found
        // CRITICAL: This must be set to allow queries to find tables in expertmatch schema
        config.setConnectionInitSql("SET search_path = public, expertmatch;");

        // Apply HikariCP settings from application.yml
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);

        HikariDataSource dataSource = new HikariDataSource(config);

        // Verify search_path is set correctly
        try {
            var conn = dataSource.getConnection();
            try {
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery("SHOW search_path");
                if (rs.next()) {
                    String searchPath = rs.getString(1);
                    log.info("Primary DataSource search_path verified: {}", searchPath);
                    if (!searchPath.contains("expertmatch")) {
                        log.warn("search_path does not include expertmatch schema: {}", searchPath);
                    }
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            log.error("Failed to verify search_path on primary DataSource: {}", e.getMessage(), e);
        }

        return dataSource;
    }
}
