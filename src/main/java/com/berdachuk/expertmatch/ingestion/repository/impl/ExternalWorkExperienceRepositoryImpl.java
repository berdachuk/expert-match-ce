package com.berdachuk.expertmatch.ingestion.repository.impl;

import com.berdachuk.expertmatch.ingestion.config.ExternalDatabaseProperties;
import com.berdachuk.expertmatch.ingestion.repository.ExternalWorkExperienceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository implementation for reading work experience data from external source database.
 * <p>
 * IMPORTANT: This repository ONLY reads from the external source database (aist-tool-networking).
 * It uses the externalDataSource and externalJdbcTemplate, which are completely isolated
 * from the primary application DataSource.
 */
@Slf4j
@Repository("externalWorkExperienceRepository")
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
public class ExternalWorkExperienceRepositoryImpl implements ExternalWorkExperienceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource externalDataSource;
    private final ExternalDatabaseProperties properties;

    public ExternalWorkExperienceRepositoryImpl(
            @Qualifier("externalJdbcTemplate") NamedParameterJdbcTemplate externalJdbcTemplate,
            @Qualifier("externalDataSource") DataSource externalDataSource,
            ExternalDatabaseProperties properties) {
        this.jdbcTemplate = externalJdbcTemplate;
        this.externalDataSource = externalDataSource;
        this.properties = properties;

        // Validate that we're using the correct DataSource
        validateDataSource();
    }

    /**
     * Validates that the DataSource is pointing to the external database, not the primary one.
     */
    private void validateDataSource() {
        try {
            try (Connection conn = externalDataSource.getConnection()) {
                String dbUrl = conn.getMetaData().getURL();
                String dbName = conn.getCatalog();
                String expectedDb = properties.getDatabase();

                log.info("External repository DataSource validation - URL: {}, Database: {}, Expected: {}",
                        dbUrl, dbName, expectedDb);

                if (expectedDb != null && !dbName.equals(expectedDb)) {
                    log.error("CRITICAL: External repository DataSource is pointing to wrong database! " +
                            "Expected: {}, Actual: {}", expectedDb, dbName);
                    throw new IllegalStateException(
                            String.format("External repository DataSource points to wrong database. Expected: %s, Actual: %s",
                                    expectedDb, dbName));
                }

                // Verify it's not pointing to the primary database
                if (dbName != null && (dbName.equals("expertmatch") || dbUrl.contains("/expertmatch"))) {
                    log.error("CRITICAL: External repository DataSource is pointing to primary database (expertmatch)! " +
                            "This should never happen. URL: {}", dbUrl);
                    throw new IllegalStateException(
                            "External repository DataSource is pointing to primary database. This is a configuration error.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to validate external DataSource: {}", e.getMessage(), e);
            throw new IllegalStateException("External DataSource validation failed", e);
        }
    }

    /**
     * Gets the schema name for table references.
     * Treats null or empty as unset and defaults to work_experience.
     */
    private String getSchema() {
        String s = properties.getSchema();
        return (s != null && !s.isEmpty()) ? s : "work_experience";
    }

    @Override
    public long countAll() {
        String schema = getSchema();
        String sql = String.format("SELECT COUNT(*) FROM %s.work_experience_json", schema);
        log.debug("Executing count query: {}", sql);

        try (Connection conn = externalDataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path = " + schema + ", public");
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    long count = rs.getLong(1);
                    log.info("Total records in {}.work_experience_json: {}", schema, count);
                    return count;
                }
                return 0L;
            }
        } catch (SQLException e) {
            log.error("Failed to execute count query: {}. Error: {}", sql, e.getMessage(), e);
            throw new RuntimeException("Failed to execute count query", e);
        } catch (Exception e) {
            log.error("Failed to execute count query: {}. Error: {}", sql, e.getMessage(), e);
            throw new RuntimeException("Failed to execute count query", e);
        }
    }

    @Override
    public List<Map<String, Object>> findAll(int offset, int limit) {
        String schema = getSchema();
        String sql = String.format("SELECT * FROM %s.work_experience_json ORDER BY message_offset LIMIT %d OFFSET %d",
                schema, limit, offset);
        log.debug("Executing findAll query: {}", sql);

        try (Connection conn = externalDataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path = " + schema + ", public");
            }
            List<Map<String, Object>> results = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName != null ? columnName.toLowerCase() : "col_" + i, value);
                    }
                    results.add(row);
                }
            }
            return results;
        } catch (SQLException e) {
            log.error("Failed to execute findAll query: {}. Error: {}", sql, e.getMessage(), e);
            throw new RuntimeException("Failed to execute findAll query", e);
        } catch (Exception e) {
            log.error("Failed to execute findAll query: {}. Error: {}", sql, e.getMessage(), e);
            throw new RuntimeException("Failed to execute findAll query", e);
        }
    }

    @Override
    public List<Map<String, Object>> findFromOffset(long fromOffset, int limit) {
        String schema = getSchema();
        // Use direct connection to ensure search_path and query execute on same connection
        // This is more reliable than relying on connection pool behavior
        String sql = String.format("SELECT * FROM %s.work_experience_json WHERE message_offset >= %d ORDER BY message_offset LIMIT %d",
                schema, fromOffset, limit);
        log.info("Executing findFromOffset query: {} with params: fromOffset={}, limit={}", sql, fromOffset, limit);

        try (Connection conn = externalDataSource.getConnection()) {
            // Set search_path on this specific connection
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path = " + schema + ", public");
                log.debug("Set search_path to {}, public", schema);
            }

            // Execute query on the same connection
            List<Map<String, Object>> results = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                int columnCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName != null ? columnName.toLowerCase() : "col_" + i, value);
                    }
                    results.add(row);
                }
            }

            log.info("Found {} records from offset {} with limit {}", results.size(), fromOffset, limit);
            return results;
        } catch (SQLException e) {
            log.error("Failed to execute findFromOffset query: {}. SQLState: {}, ErrorCode: {}, Message: {}",
                    sql, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to execute findFromOffset query: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to execute findFromOffset query: {}. Error: {}", sql, e.getMessage(), e);
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof SQLException sqlEx) {
                    log.error("SQL error details - SQLState: {}, ErrorCode: {}, Message: {}",
                            sqlEx.getSQLState(), sqlEx.getErrorCode(), sqlEx.getMessage());
                }
                cause = cause.getCause();
            }
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new RuntimeException("Failed to execute findFromOffset query: " + msg, e);
        }
    }
}
