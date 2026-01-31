package com.berdachuk.expertmatch.retrieval.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.retrieval.repository.KeywordSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository implementation for keyword/full-text search operations.
 */
@Slf4j
@Repository
public class KeywordSearchRepositoryImpl implements KeywordSearchRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RowMapper<String> employeeIdMapper = (rs, rowNum) -> rs.getString("employee_id");
    @InjectSql("/sql/retrieval/keywordSearch.sql")
    private String keywordSearchSql;
    @InjectSql("/sql/retrieval/searchByTechnologies.sql")
    private String searchByTechnologiesSql;

    public KeywordSearchRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public List<String> searchByKeywords(String searchTerms, int maxResults) {
        Map<String, Object> params = Map.of(
                "searchTerms", searchTerms,
                "maxResults", maxResults
        );
        return namedJdbcTemplate.query(keywordSearchSql, params, employeeIdMapper);
    }

    @Override
    public List<String> searchByTechnologies(String[] technologies, int maxResults) {
        Map<String, Object> params = Map.of(
                "technologies", technologies,
                "maxResults", maxResults
        );
        return namedJdbcTemplate.query(searchByTechnologiesSql, params, employeeIdMapper);
    }
}
