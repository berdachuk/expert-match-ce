package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.ingestion.model.EmployeeData;
import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.berdachuk.expertmatch.ingestion.model.ProjectData;
import com.berdachuk.expertmatch.ingestion.service.ProfileProcessor;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileProcessorIT extends BaseIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    private ProfileProcessor processor;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

        // Create processor instance
        processor = new ProfileProcessor(employeeRepository, projectRepository, workExperienceRepository, objectMapper);
    }

    @Test
    void testProcessProfile_ValidProfile_ShouldCreateEmployee() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        assertEquals("4000741400013306668", result.employeeId());
        assertEquals("John Doe", result.employeeName());

        // Verify employee was created in database
        String sql = "SELECT COUNT(*) FROM expertmatch.employee WHERE id = :id";
        Integer count = namedJdbcTemplate.queryForObject(sql,
                Map.of("id", "4000741400013306668"), Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testProcessProfile_WithProjects_ShouldCreateWorkExperience() {
        EmployeeData employee = new EmployeeData(
                "5000741400013306669",
                "Jane Smith",
                "jane.smith@example.com",
                "B1",
                "B2",
                "available"
        );
        ProjectData project = new ProjectData(
                "PRJ-001",
                "Test Project",
                "Test Customer",
                "Test Company",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java", "Spring"),
                "Test responsibilities",
                "Technology",
                "Test summary"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of(project));

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        assertEquals(1, result.projectsProcessed());
        assertEquals(0, result.projectsSkipped());

        // Verify work experience was created
        String sql = """
                SELECT COUNT(*) FROM expertmatch.work_experience 
                WHERE employee_id = :employeeId AND project_name = :projectName
                """;
        Integer count = namedJdbcTemplate.queryForObject(sql,
                Map.of("employeeId", "5000741400013306669", "projectName", "Test Project"),
                Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testProcessProfile_Idempotency_ShouldNotCreateDuplicates() {
        EmployeeData employee = new EmployeeData(
                "6000741400013306670",
                "Bob Johnson",
                "bob.johnson@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        // Process first time
        ProcessingResult result1 = processor.processProfile(profile, new HashMap<>());
        assertTrue(result1.success());

        // Process second time (should update, not create duplicate)
        ProcessingResult result2 = processor.processProfile(profile, new HashMap<>());
        assertTrue(result2.success());

        // Verify only one employee record exists
        String sql = "SELECT COUNT(*) FROM expertmatch.employee WHERE id = :id";
        Integer count = namedJdbcTemplate.queryForObject(sql,
                Map.of("id", "6000741400013306670"), Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testProcessProfile_PartialData_ShouldApplyDefaults() {
        EmployeeData employee = new EmployeeData(
                "7000741400013306671",
                "Alice Brown",
                null, // Missing email
                null, // Missing seniority
                null, // Missing languageEnglish
                null  // Missing availabilityStatus
        );
        EmployeeProfile profile = new EmployeeProfile(employee, null, List.of());

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());

        // Verify employee was created with defaults
        String sql = """
                SELECT email, seniority, language_english, availability_status 
                FROM expertmatch.employee WHERE id = :id
                """;
        Map<String, Object> employeeData = namedJdbcTemplate.queryForMap(sql,
                Map.of("id", "7000741400013306671"));

        assertNotNull(employeeData.get("email"));
        assertEquals("B1", employeeData.get("seniority"));
        assertEquals("B2", employeeData.get("language_english"));
        assertEquals("available", employeeData.get("availability_status"));
    }
}

