package com.berdachuk.expertmatch.technology.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.technology.domain.Technology;
import com.berdachuk.expertmatch.technology.repository.TechnologyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for technology data access.
 */
@Slf4j
@Repository
public class TechnologyRepositoryImpl implements TechnologyRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/technology/createOrUpdate.sql")
    private String createOrUpdateSql;

    @InjectSql("/sql/technology/findByName.sql")
    private String findByNameSql;

    @InjectSql("/sql/technology/deleteAll.sql")
    private String deleteAllSql;

    public TechnologyRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Creates or updates a technology.
     * Uses ON CONFLICT on name to handle duplicates gracefully.
     */
    @Override
    public String createOrUpdate(Technology technology) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", technology.id());
        params.put("name", technology.name());
        params.put("normalizedName", technology.normalizedName());
        params.put("category", technology.category());
        params.put("synonyms", technology.synonyms() != null ? technology.synonyms().toArray(new String[0]) : null);

        try {
            namedJdbcTemplate.update(createOrUpdateSql, params);
            return technology.id();
        } catch (Exception e) {
            log.warn("Failed to create/update technology {}: {}", technology.name(), e.getMessage());
            throw new RuntimeException("Failed to create/update technology", e);
        }
    }

    /**
     * Finds a technology by name.
     */
    @Override
    public Optional<Technology> findByName(String name) {
        Map<String, Object> params = Map.of("name", name);
        List<Technology> results = namedJdbcTemplate.query(findByNameSql, params, (rs, rowNum) -> {
            Array synonymsArray = rs.getArray("synonyms");
            List<String> synonyms = synonymsArray != null
                    ? List.of((String[]) synonymsArray.getArray())
                    : List.of();

            return new Technology(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("normalized_name"),
                    rs.getString("category"),
                    synonyms
            );
        });

        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    /**
     * Deletes all technology records.
     */
    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
