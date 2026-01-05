package com.berdachuk.expertmatch.data;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for work experience data access.
 */
@Repository
public class WorkExperienceRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public WorkExperienceRepository(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Finds work experience for an employee.
     */
    public List<WorkExperience> findByEmployeeId(String employeeId) {
        String sql = """
                SELECT id, employee_id, project_id, customer_id, project_name, customer_name, industry, 
                       role, start_date, end_date, 
                       project_summary, responsibilities, technologies
                FROM expertmatch.work_experience
                WHERE employee_id = :employeeId
                ORDER BY start_date DESC
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
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
    public Map<String, List<WorkExperience>> findByEmployeeIds(List<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT id, employee_id, project_id, customer_id, project_name, customer_name, industry, 
                       role, start_date, end_date, 
                       project_summary, responsibilities, technologies
                FROM expertmatch.work_experience
                WHERE employee_id = ANY(:employeeIds)
                ORDER BY employee_id, start_date DESC
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("employeeIds", employeeIds.toArray(new String[0]));

        Map<String, List<WorkExperience>> result = new HashMap<>();

        namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
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
    public List<String> findEmployeeIdsByTechnologies(List<String> technologies) {
        if (technologies.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT DISTINCT employee_id
                FROM expertmatch.work_experience
                WHERE technologies && ARRAY[:technologies]::text[]
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("technologies", technologies.toArray(new String[0]));

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("employee_id"));
    }

    /**
     * Work experience entity.
     */
    public record WorkExperience(
            String id,
            String employeeId,
            String projectId,        // External system project_id (nullable)
            String customerId,       // External system customer_id (nullable)
            String projectName,
            String customerName,
            String industry,
            String role,
            Instant startDate,
            Instant endDate,
            String projectSummary,
            String responsibilities,
            List<String> technologies
    ) {
    }
}

