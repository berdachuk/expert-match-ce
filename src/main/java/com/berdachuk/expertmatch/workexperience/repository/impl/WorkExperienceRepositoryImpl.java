package com.berdachuk.expertmatch.workexperience.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for work experience data access.
 */
@Repository
public class WorkExperienceRepositoryImpl implements WorkExperienceRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/workexperience/findByEmployeeId.sql")
    private String findByEmployeeIdSql;

    @InjectSql("/sql/workexperience/findByEmployeeIds.sql")
    private String findByEmployeeIdsSql;

    @InjectSql("/sql/workexperience/findEmployeeIdsByTechnologies.sql")
    private String findEmployeeIdsByTechnologiesSql;

    @InjectSql("/sql/workexperience/createOrUpdate.sql")
    private String createOrUpdateSql;

    @InjectSql("/sql/workexperience/exists.sql")
    private String existsSql;

    @InjectSql("/sql/workexperience/findWithoutEmbeddings.sql")
    private String findWithoutEmbeddingsSql;

    @InjectSql("/sql/workexperience/updateEmbedding.sql")
    private String updateEmbeddingSql;

    @InjectSql("/sql/workexperience/count.sql")
    private String countSql;

    @InjectSql("/sql/workexperience/deleteAll.sql")
    private String deleteAllSql;

    public WorkExperienceRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Finds work experience for an employee.
     */
    @Override
    public List<WorkExperience> findByEmployeeId(String employeeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);

        return namedJdbcTemplate.query(findByEmployeeIdSql, params, (rs, rowNum) -> {
            Array technologiesArray = rs.getArray("technologies");

            List<String> technologies = technologiesArray != null
                    ? List.of((String[]) technologiesArray.getArray())
                    : List.of();

            Date startDate = rs.getDate("start_date");
            Date endDate = rs.getDate("end_date");

            Instant startInstant = startDate != null
                    ? startDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;
            Instant endInstant = endDate != null
                    ? endDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;

            return new WorkExperience(
                    rs.getString("id"),
                    rs.getString("employee_id"),
                    rs.getString("project_id"),
                    rs.getString("customer_id"),
                    rs.getString("project_name"),
                    rs.getString("customer_name"),
                    rs.getString("industry"),
                    rs.getString("role"),
                    startInstant,
                    endInstant,
                    rs.getString("project_summary"),
                    rs.getString("responsibilities"),
                    technologies
            );
        });
    }

    /**
     * Finds work experience for multiple employees.
     */
    @Override
    public Map<String, List<WorkExperience>> findByEmployeeIds(List<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("employeeIds", employeeIds.toArray(new String[0]));

        Map<String, List<WorkExperience>> result = new HashMap<>();

        namedJdbcTemplate.query(findByEmployeeIdsSql, params, (rs, rowNum) -> {
            String employeeId = rs.getString("employee_id");

            Array technologiesArray = rs.getArray("technologies");

            List<String> technologies = technologiesArray != null
                    ? List.of((String[]) technologiesArray.getArray())
                    : List.of();

            Date startDate = rs.getDate("start_date");
            Date endDate = rs.getDate("end_date");

            Instant startInstant = startDate != null
                    ? startDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;
            Instant endInstant = endDate != null
                    ? endDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;

            WorkExperience workExperience = new WorkExperience(
                    rs.getString("id"),
                    employeeId,
                    rs.getString("project_id"),
                    rs.getString("customer_id"),
                    rs.getString("project_name"),
                    rs.getString("customer_name"),
                    rs.getString("industry"),
                    rs.getString("role"),
                    startInstant,
                    endInstant,
                    rs.getString("project_summary"),
                    rs.getString("responsibilities"),
                    technologies
            );

            result.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(workExperience);
            return null;
        });

        return result;
    }

    /**
     * Finds work experience by technologies.
     */
    @Override
    public List<String> findEmployeeIdsByTechnologies(List<String> technologies) {
        if (technologies.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("technologies", technologies.toArray(new String[0]));

        return namedJdbcTemplate.query(findEmployeeIdsByTechnologiesSql, params, (rs, rowNum) -> rs.getString("employee_id"));
    }

    /**
     * Creates or updates a work experience record.
     * Uses ON CONFLICT to handle duplicate IDs gracefully.
     */
    @Override
    public String createOrUpdate(WorkExperience workExperience, String metadata) {
        // Convert Instant to LocalDate for database
        LocalDate startDate = workExperience.startDate() != null
                ? workExperience.startDate().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;
        LocalDate endDate = workExperience.endDate() != null
                ? workExperience.endDate().atZone(ZoneId.systemDefault()).toLocalDate()
                : null;

        Map<String, Object> params = new HashMap<>();
        params.put("id", workExperience.id());
        params.put("employeeId", workExperience.employeeId());
        params.put("projectId", workExperience.projectId());
        params.put("projectName", workExperience.projectName());
        params.put("projectSummary", workExperience.projectSummary());
        params.put("role", workExperience.role());
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("technologies", workExperience.technologies() != null
                ? workExperience.technologies().toArray(new String[0])
                : null);
        params.put("responsibilities", workExperience.responsibilities());
        params.put("customerName", workExperience.customerName());
        params.put("industry", workExperience.industry());
        params.put("metadata", metadata != null ? metadata : "{}");

        try {
            namedJdbcTemplate.update(createOrUpdateSql, params);
            return workExperience.id();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create/update work experience", e);
        }
    }

    /**
     * Checks if a work experience record exists for the given criteria.
     */
    @Override
    public boolean exists(String employeeId, String projectName, LocalDate startDate) {
        Map<String, Object> params = Map.of(
                "employeeId", employeeId,
                "projectName", projectName,
                "startDate", startDate
        );

        List<String> results = namedJdbcTemplate.query(existsSql, params, (rs, rowNum) -> rs.getString("id"));
        return !results.isEmpty();
    }

    /**
     * Finds work experience records that don't have embeddings.
     */
    @Override
    public List<WorkExperience> findWithoutEmbeddings() {
        return namedJdbcTemplate.query(findWithoutEmbeddingsSql, Map.of(), (rs, rowNum) -> {
            Array technologiesArray = rs.getArray("technologies");
            List<String> technologies = technologiesArray != null
                    ? List.of((String[]) technologiesArray.getArray())
                    : List.of();

            Date startDate = rs.getDate("start_date");
            Date endDate = rs.getDate("end_date");

            Instant startInstant = startDate != null
                    ? startDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;
            Instant endInstant = endDate != null
                    ? endDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
                    : null;

            return new WorkExperience(
                    rs.getString("id"),
                    rs.getString("employee_id"),
                    rs.getString("project_id"),
                    rs.getString("customer_id"),
                    rs.getString("project_name"),
                    rs.getString("customer_name"),
                    rs.getString("industry"),
                    rs.getString("role"),
                    startInstant,
                    endInstant,
                    rs.getString("project_summary"),
                    rs.getString("responsibilities"),
                    technologies
            );
        });
    }

    /**
     * Updates the embedding for a work experience record.
     */
    @Override
    public void updateEmbedding(String workExpId, List<Double> embedding, int dimension) {
        // Convert to float array
        float[] embeddingArray = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            embeddingArray[i] = embedding.get(i).floatValue();
        }

        // Normalize to 1536 dimensions (database schema supports max 1536)
        float[] normalizedEmbedding = normalizeEmbeddingDimension(embeddingArray, 1536);

        String vectorString = formatVector(normalizedEmbedding);

        Map<String, Object> params = new HashMap<>();
        params.put("id", workExpId);
        params.put("embedding", vectorString);
        params.put("dimension", dimension);

        namedJdbcTemplate.update(updateEmbeddingSql, params);
    }

    /**
     * Returns the total count of work experience records.
     */
    @Override
    public long count() {
        Long result = namedJdbcTemplate.queryForObject(countSql, Map.of(), Long.class);
        return result != null ? result : 0L;
    }

    /**
     * Deletes all work experience records.
     */
    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }

    /**
     * Normalizes embedding to target dimension.
     */
    private float[] normalizeEmbeddingDimension(float[] embedding, int targetDimension) {
        if (embedding.length == targetDimension) {
            return embedding;
        }

        float[] normalized = new float[targetDimension];
        int copyLength = Math.min(embedding.length, targetDimension);
        System.arraycopy(embedding, 0, normalized, 0, copyLength);
        // Remaining elements are already zero (default float value)

        return normalized;
    }

    /**
     * Formats float array as PostgreSQL vector string.
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

}

