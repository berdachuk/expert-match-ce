package com.berdachuk.expertmatch.project.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.project.domain.Project;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
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
 * Repository for project data access.
 */
@Slf4j
@Repository
public class ProjectRepositoryImpl implements ProjectRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/project/createOrUpdate.sql")
    private String createOrUpdateSql;

    @InjectSql("/sql/project/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/project/findIdByName.sql")
    private String findIdByNameSql;

    @InjectSql("/sql/project/count.sql")
    private String countSql;

    @InjectSql("/sql/project/deleteAll.sql")
    private String deleteAllSql;

    public ProjectRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Creates or updates a project.
     * Uses ON CONFLICT to handle duplicate IDs gracefully.
     */
    @Override
    public String createOrUpdate(Project project) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", project.id());
        params.put("name", project.name());
        params.put("summary", project.summary());
        params.put("link", project.link());
        params.put("projectType", project.projectType());
        params.put("technologies", project.technologies() != null ? project.technologies().toArray(new String[0]) : null);

        try {
            namedJdbcTemplate.update(createOrUpdateSql, params);
            return project.id();
        } catch (Exception e) {
            log.warn("Failed to create/update project {}: {}", project.id(), e.getMessage());
            throw new RuntimeException("Failed to create/update project", e);
        }
    }

    /**
     * Finds a project by ID.
     */
    @Override
    public Optional<Project> findById(String projectId) {
        Map<String, Object> params = Map.of("id", projectId);
        List<Project> results = namedJdbcTemplate.query(findByIdSql, params, (rs, rowNum) -> {
            Array technologiesArray = rs.getArray("technologies");
            List<String> technologies = technologiesArray != null
                    ? List.of((String[]) technologiesArray.getArray())
                    : List.of();

            return new Project(
                    rs.getString("id"),
                    rs.getString("name"),
                    rs.getString("summary"),
                    rs.getString("link"),
                    rs.getString("project_type"),
                    technologies,
                    null, // customerId - not in project table
                    null, // customerName - not in project table
                    null  // industry - not in project table
            );
        });

        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    /**
     * Finds project ID by name (for lookup during ingestion).
     * Uses partial matching to find projects with similar names.
     */
    @Override
    public Optional<String> findIdByName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return Optional.empty();
        }

        // Use partial matching - check if project name contains a substring of the search name
        // or if search name contains a substring of project name
        String namePattern = "%" + projectName.toLowerCase() + "%";
        Map<String, Object> params = Map.of("namePattern", namePattern);
        List<String> results = namedJdbcTemplate.query(findIdByNameSql, params, (rs, rowNum) -> rs.getString("id"));

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Returns the total count of project records.
     */
    @Override
    public long count() {
        Long result = namedJdbcTemplate.queryForObject(countSql, Map.of(), Long.class);
        return result != null ? result : 0L;
    }

    /**
     * Deletes all project records.
     */
    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
