package com.berdachuk.expertmatch.workexperience.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
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
public class WorkExperienceRepositoryImpl implements WorkExperienceRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/workexperience/findByEmployeeId.sql")
    private String findByEmployeeIdSql;

    @InjectSql("/sql/workexperience/findByEmployeeIds.sql")
    private String findByEmployeeIdsSql;

    @InjectSql("/sql/workexperience/findEmployeeIdsByTechnologies.sql")
    private String findEmployeeIdsByTechnologiesSql;

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

}

