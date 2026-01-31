package com.berdachuk.expertmatch.retrieval.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.retrieval.repository.PgVectorSearchRepository;
import com.berdachuk.expertmatch.retrieval.repository.PgVectorSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository implementation for PgVector similarity search operations.
 */
@Slf4j
@Repository
public class PgVectorSearchRepositoryImpl implements PgVectorSearchRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RowMapper<PgVectorSearchResult> resultMapper = (rs, rowNum) -> {
        String employeeId = rs.getString("employee_id");
        double similarity = rs.getDouble("similarity");

        Map<String, Object> metadata = new HashMap<>();
        String projectName = rs.getString("project_name");
        if (projectName != null) {
            metadata.put("projectName", projectName);
        }

        String projectSummary = rs.getString("project_summary");
        if (projectSummary != null) {
            metadata.put("projectSummary", projectSummary);
        }

        String role = rs.getString("role");
        if (role != null) {
            metadata.put("role", role);
        }

        Array techArray = rs.getArray("technologies");
        List<String> technologies = techArray != null
                ? List.of((String[]) techArray.getArray())
                : List.of();
        metadata.put("technologies", technologies);

        return new PgVectorSearchResult(employeeId, similarity, metadata);
    };
    @InjectSql("/sql/retrieval/vectorSearch.sql")
    private String vectorSearchSql;

    public PgVectorSearchRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public List<PgVectorSearchResult> search(String queryVector, double similarityThreshold, int maxResults) {
        Map<String, Object> params = Map.of(
                "queryVector", queryVector,
                "threshold", similarityThreshold,
                "maxResults", maxResults
        );
        return namedJdbcTemplate.query(vectorSearchSql, params, resultMapper);
    }
}
