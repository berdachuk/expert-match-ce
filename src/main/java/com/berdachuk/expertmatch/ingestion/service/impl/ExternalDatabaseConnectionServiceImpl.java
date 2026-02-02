package com.berdachuk.expertmatch.ingestion.service.impl;

import com.berdachuk.expertmatch.ingestion.config.ExternalDatabaseProperties;
import com.berdachuk.expertmatch.ingestion.service.ExternalDatabaseConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service implementation for verifying external database connection.
 * <p>
 * IMPORTANT: Uses the read-only external datasource for connection verification only.
 * No write operations are performed on the external database.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
public class ExternalDatabaseConnectionServiceImpl implements ExternalDatabaseConnectionService {

    private final DataSource externalDataSource;
    private final ExternalDatabaseProperties properties;

    public ExternalDatabaseConnectionServiceImpl(
            @Qualifier("externalDataSource") DataSource externalDataSource,
            ExternalDatabaseProperties properties) {
        this.externalDataSource = externalDataSource;
        this.properties = properties;
    }

    @Override
    public boolean verifyConnection() {
        try (Connection connection = externalDataSource.getConnection()) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(externalDataSource);
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean connected = result != null && result == 1;
            if (connected) {
                int effectivePort = properties.getPort() != null ? properties.getPort() : 5432;
                log.info("Successfully verified connection to external database: {}@{}:{}/{}",
                        properties.getUsername(), properties.getHost(), effectivePort, properties.getDatabase());
            }
            return connected;
        } catch (SQLException e) {
            int effectivePort = properties.getPort() != null ? properties.getPort() : 5432;
            log.error("Failed to verify connection to external database: {}@{}:{}/{} - {}",
                    properties.getUsername(), properties.getHost(), effectivePort, properties.getDatabase(),
                    e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getConnectionInfo() {
        int effectivePort = properties.getPort() != null ? properties.getPort() : 5432;
        return String.format("%s@%s:%d/%s (schema: %s)",
                properties.getUsername(), properties.getHost(), effectivePort,
                properties.getDatabase(), properties.getSchema());
    }
}
