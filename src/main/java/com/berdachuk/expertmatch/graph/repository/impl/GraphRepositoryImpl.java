package com.berdachuk.expertmatch.graph.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.graph.repository.GraphRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository implementation for Apache AGE graph administrative operations.
 * Handles graph creation, index management, and existence checks.
 */
@Slf4j
@Repository
public class GraphRepositoryImpl implements GraphRepository {

    private final JdbcTemplate jdbcTemplate;

    @InjectSql("/sql/graph/checkGraphExists.sql")
    private String checkGraphExistsSql;

    @InjectSql("/sql/graph/createGraph.sql")
    private String createGraphSql;

    @InjectSql("/sql/graph/checkVertexTableExists.sql")
    private String checkVertexTableExistsSql;

    @InjectSql("/sql/graph/createPropertyIndex.sql")
    private String createPropertyIndexSql;

    public GraphRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean graphExists(String graphName) {
        try {
            Integer count = jdbcTemplate.queryForObject(checkGraphExistsSql, Integer.class, graphName);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.debug("Graph check failed (AGE may not be available): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("Graph check failed with unexpected error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void createGraph(String graphName) {
        try {
            jdbcTemplate.execute(createGraphSql);
            log.info("Graph '{}' created successfully", graphName);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && !message.contains("already exists") && !message.contains("graph already exists")) {
                log.error("Failed to create graph", e);
                throw new RuntimeException("Failed to create graph", e);
            }
            log.debug("Graph already exists");
        }
    }

    @Override
    public boolean vertexTableExists(String graphName, String vertexLabel) {
        try {
            String tableName = "ag_" + graphName + "_" + vertexLabel;
            String sql = checkVertexTableExistsSql.replace("{tableName}", tableName);
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class);
            return exists != null && exists;
        } catch (Exception e) {
            log.debug("Failed to check vertex table existence: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void createPropertyIndex(String graphName, String vertexLabel, String indexName) {
        try {
            String tableName = "ag_" + graphName + "_" + vertexLabel;
            String sql = createPropertyIndexSql
                    .replace("{indexName}", indexName)
                    .replace("{tableName}", tableName);
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("Could not create index {}: {}", indexName, e.getMessage());
        }
    }
}
