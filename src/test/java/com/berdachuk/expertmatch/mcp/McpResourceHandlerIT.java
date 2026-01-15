package com.berdachuk.expertmatch.mcp;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.mcp.service.McpResourceHandler;
import com.berdachuk.expertmatch.project.domain.Project;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for McpResourceHandler.
 * Tests resource retrieval for experts, projects, technologies, and domains.
 */
class McpResourceHandlerIT extends BaseIntegrationTest {

    @Autowired
    private McpResourceHandler mcpResourceHandler;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkExperienceRepository workExperienceRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String employeeId1;
    private String employeeId2;
    private String projectId1;
    private String projectId2;
    private String workExpId1;
    private String workExpId2;

    @BeforeEach
    void setUp() {
        // Clear existing data
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.project");

        // Create test data
        createTestData();
    }

    private void createTestData() {
        // Create employees
        employeeId1 = "8760000000000420950";
        Employee employee1 = new Employee(
                employeeId1,
                "John Doe",
                "john.doe@example.com",
                "A3",
                "C1",
                "AVAILABLE"
        );
        employeeRepository.createOrUpdate(employee1);

        employeeId2 = "8760000000000420951";
        Employee employee2 = new Employee(
                employeeId2,
                "Jane Smith",
                "jane.smith@example.com",
                "A2",
                "B2",
                "BUSY"
        );
        employeeRepository.createOrUpdate(employee2);

        // Create projects (note: project table doesn't have customer_id, customer_name, industry columns)
        // These fields are stored in work_experience table instead
        projectId1 = IdGenerator.generateProjectId();
        Project project1 = new Project(
                projectId1,
                "E-Commerce Platform",
                "Large-scale e-commerce platform",
                "https://example.com/project1",
                "WEB",
                List.of("Java", "Spring Boot", "PostgreSQL"),
                null, // customerId - not in project table
                null, // customerName - not in project table  
                null  // industry - not in project table
        );
        projectRepository.createOrUpdate(project1);

        projectId2 = IdGenerator.generateProjectId();
        Project project2 = new Project(
                projectId2,
                "Mobile Banking App",
                "Mobile banking application",
                "https://example.com/project2",
                "MOBILE",
                List.of("React", "TypeScript", "Node.js"),
                null, // customerId - not in project table
                null, // customerName - not in project table
                null  // industry - not in project table
        );
        projectRepository.createOrUpdate(project2);

        // Create work experiences
        workExpId1 = IdGenerator.generateId();
        Instant start1 = LocalDate.of(2020, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end1 = LocalDate.of(2022, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant();
        WorkExperience workExp1 = new WorkExperience(
                workExpId1,
                employeeId1,
                projectId1,
                null,
                "E-Commerce Platform",
                "Retail Corp",
                "Retail",
                "Senior Developer",
                start1,
                end1,
                "Developed e-commerce platform",
                "Backend development",
                List.of("Java", "Spring Boot", "PostgreSQL")
        );
        workExperienceRepository.createOrUpdate(workExp1, "{}");

        workExpId2 = IdGenerator.generateId();
        Instant start2 = LocalDate.of(2021, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end2 = LocalDate.of(2023, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant();
        WorkExperience workExp2 = new WorkExperience(
                workExpId2,
                employeeId2,
                projectId2,
                null,
                "Mobile Banking App",
                "Bank Inc",
                "Finance",
                "Frontend Developer",
                start2,
                end2,
                "Developed mobile banking app",
                "Frontend development",
                List.of("React", "TypeScript", "Node.js")
        );
        workExperienceRepository.createOrUpdate(workExp2, "{}");
    }

    @Test
    void testReadExpertResource() throws Exception {
        String uri = "expertmatch://experts/" + employeeId1;
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals(employeeId1, root.get("id").asText());
        assertEquals("John Doe", root.get("name").asText());
        assertEquals("john.doe@example.com", root.get("email").asText());
        assertEquals("A3", root.get("seniority").asText());
        assertTrue(root.has("workExperiences"));
        assertTrue(root.get("workExperiences").isArray());
        assertEquals(1, root.get("workExperiences").size());

        JsonNode workExp = root.get("workExperiences").get(0);
        assertEquals(workExpId1, workExp.get("id").asText());
        assertEquals("E-Commerce Platform", workExp.get("projectName").asText());
        assertEquals("Retail", workExp.get("industry").asText());
        assertTrue(workExp.get("technologies").isArray());
    }

    @Test
    void testReadExpertResourceNotFound() throws Exception {
        String uri = "expertmatch://experts/nonexistent";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.has("error"));
        assertTrue(root.get("error").asText().contains("Expert not found"));
    }

    @Test
    void testReadProjectResource() throws Exception {
        String uri = "expertmatch://projects/" + projectId1;
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals(projectId1, root.get("id").asText());
        assertEquals("E-Commerce Platform", root.get("name").asText());
        assertEquals("Large-scale e-commerce platform", root.get("summary").asText());
        // Note: industry is not stored in project table, it's in work_experience table
        assertTrue(root.get("industry").isNull());
        assertTrue(root.get("technologies").isArray());
        assertEquals(3, root.get("technologies").size());
    }

    @Test
    void testReadProjectResourceNotFound() throws Exception {
        String uri = "expertmatch://projects/nonexistent";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.has("error"));
        assertTrue(root.get("error").asText().contains("Project not found"));
    }

    @Test
    void testReadTechnologyResource() throws Exception {
        String uri = "expertmatch://technologies/Java";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals("Java", root.get("name").asText());
        assertTrue(root.has("employeeCount"));
        assertTrue(root.has("employees"));
        assertTrue(root.get("employees").isArray());
        
        // Should find employee1 who has Java experience
        assertTrue(root.get("employeeCount").asInt() >= 1);
        boolean foundEmployee1 = false;
        for (JsonNode emp : root.get("employees")) {
            if (employeeId1.equals(emp.get("id").asText())) {
                foundEmployee1 = true;
                assertEquals("John Doe", emp.get("name").asText());
                break;
            }
        }
        assertTrue(foundEmployee1, "Should find employee with Java experience");
    }

    @Test
    void testReadTechnologyResourceNotFound() throws Exception {
        String uri = "expertmatch://technologies/NonexistentTechnology";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals("NonexistentTechnology", root.get("name").asText());
        assertEquals(0, root.get("employeeCount").asInt());
        assertTrue(root.get("employees").isArray());
        assertEquals(0, root.get("employees").size());
    }

    @Test
    void testReadDomainResource() throws Exception {
        String uri = "expertmatch://domains/Retail";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals("Retail", root.get("name").asText());
        assertTrue(root.has("employeeCount"));
        assertTrue(root.has("employees"));
        assertTrue(root.get("employees").isArray());
        
        // Should find employee1 who has Retail industry experience
        assertTrue(root.get("employeeCount").asInt() >= 1);
        boolean foundEmployee1 = false;
        for (JsonNode emp : root.get("employees")) {
            if (employeeId1.equals(emp.get("id").asText())) {
                foundEmployee1 = true;
                assertEquals("John Doe", emp.get("name").asText());
                break;
            }
        }
        assertTrue(foundEmployee1, "Should find employee with Retail domain experience");
    }

    @Test
    void testReadDomainResourceCaseInsensitive() throws Exception {
        String uri = "expertmatch://domains/retail";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals("retail", root.get("name").asText());
        assertTrue(root.get("employeeCount").asInt() >= 1);
    }

    @Test
    void testReadDomainResourceNotFound() throws Exception {
        String uri = "expertmatch://domains/NonexistentDomain";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertEquals("NonexistentDomain", root.get("name").asText());
        assertEquals(0, root.get("employeeCount").asInt());
        assertTrue(root.get("employees").isArray());
        assertEquals(0, root.get("employees").size());
    }

    @Test
    void testReadResourceUnknownUri() throws Exception {
        String uri = "expertmatch://unknown/resource";
        String json = mcpResourceHandler.readResource(uri);

        assertNotNull(json);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.has("error"));
        assertTrue(root.get("error").asText().contains("Unknown resource URI"));
    }

    @Test
    void testListResources() {
        var resources = mcpResourceHandler.listResources();

        assertNotNull(resources);
        assertEquals(4, resources.size());
        assertEquals("expertmatch://experts/{expert_id}", resources.get(0).uri());
        assertEquals("expertmatch://projects/{project_id}", resources.get(1).uri());
        assertEquals("expertmatch://technologies/{technology_name}", resources.get(2).uri());
        assertEquals("expertmatch://domains/{domain_name}", resources.get(3).uri());
    }
}
