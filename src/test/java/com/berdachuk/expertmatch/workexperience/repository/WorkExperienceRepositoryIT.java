package com.berdachuk.expertmatch.workexperience.repository;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for WorkExperienceRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class WorkExperienceRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
    }

    @Test
    void testFindByEmployeeId() {
        // Insert test employee
        String employeeId = IdGenerator.generateEmployeeId();
        Map<String, Object> employeeParams = new HashMap<>();
        employeeParams.put("id", employeeId);
        employeeParams.put("name", "Test Employee");
        employeeParams.put("email", "test@example.com");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                employeeParams
        );

        // Insert work experience
        String workExperienceId = IdGenerator.generateId();
        String insertSql = """
                INSERT INTO expertmatch.work_experience 
                (id, employee_id, project_name, role, start_date, end_date, technologies, responsibilities)
                VALUES (:id, :employeeId, :projectName, :role, :startDate, :endDate, :technologies, :responsibilities)
                """;
        Map<String, Object> workExperienceParams = new HashMap<>();
        workExperienceParams.put("id", workExperienceId);
        workExperienceParams.put("employeeId", employeeId);
        workExperienceParams.put("projectName", "Test Project");
        workExperienceParams.put("role", "Developer");
        workExperienceParams.put("startDate", LocalDate.of(2023, 1, 1));
        workExperienceParams.put("endDate", LocalDate.of(2023, 12, 31));
        workExperienceParams.put("technologies", new String[]{"Java", "Spring Boot"});
        workExperienceParams.put("responsibilities", "Developed features");
        namedJdbcTemplate.update(insertSql, workExperienceParams);

        // Test findByEmployeeId
        List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience> workExp =
                workExperienceRepository.findByEmployeeId(employeeId);

        assertNotNull(workExp);
        assertEquals(1, workExp.size());
        assertEquals("Test Project", workExp.get(0).projectName());
        assertEquals("Developer", workExp.get(0).role());
    }

    @Test
    void testFindByEmployeeIds() {
        // Insert test employees
        String employeeId1 = IdGenerator.generateEmployeeId();
        String employeeId2 = IdGenerator.generateEmployeeId();

        Map<String, Object> employee1Params = new HashMap<>();
        employee1Params.put("id", employeeId1);
        employee1Params.put("name", "Employee 1");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name) VALUES (:id, :name)",
                employee1Params
        );
        Map<String, Object> employee2Params = new HashMap<>();
        employee2Params.put("id", employeeId2);
        employee2Params.put("name", "Employee 2");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name) VALUES (:id, :name)",
                employee2Params
        );

        // Insert work experience
        String workExperience1 = IdGenerator.generateId();
        String workExperience2 = IdGenerator.generateId();

        Map<String, Object> workExperience1Params = new HashMap<>();
        workExperience1Params.put("id", workExperience1);
        workExperience1Params.put("employeeId", employeeId1);
        workExperience1Params.put("name", "Project 1");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name) VALUES (:id, :employeeId, :name)",
                workExperience1Params
        );
        Map<String, Object> workExperience2Params = new HashMap<>();
        workExperience2Params.put("id", workExperience2);
        workExperience2Params.put("employeeId", employeeId2);
        workExperience2Params.put("name", "Project 2");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name) VALUES (:id, :employeeId, :name)",
                workExperience2Params
        );

        // Test findByEmployeeIds
        var result = workExperienceRepository.findByEmployeeIds(List.of(employeeId1, employeeId2));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(employeeId1));
        assertTrue(result.containsKey(employeeId2));
    }
}

