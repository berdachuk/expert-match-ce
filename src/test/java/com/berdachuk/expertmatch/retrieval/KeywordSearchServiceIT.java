package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.retrieval.service.KeywordSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for KeywordSearchService.
 * Uses Testcontainers PostgreSQL with full-text search.
 */
class KeywordSearchServiceIT extends BaseIntegrationTest {

    @Autowired
    private KeywordSearchService keywordSearchService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Clean up any data from previous tests to ensure test isolation
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
    }

    @Test
    void testSearchByKeywords() {
        // Insert test employees first (required for foreign key)
        String employeeId1 = IdGenerator.generateEmployeeId();
        String employeeId2 = IdGenerator.generateEmployeeId();

        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId1, "name", "Employee 1", "email", "emp1-" + System.currentTimeMillis() + "@test.com")
        );
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId2, "name", "Employee 2", "email", "emp2-" + System.currentTimeMillis() + "@test.com")
        );

        // Insert test work experience
        String workExperienceId1 = IdGenerator.generateId();
        String workExperienceId2 = IdGenerator.generateId();

        String insertSql = """
                INSERT INTO expertmatch.work_experience 
                (id, employee_id, project_name, project_summary, role, technologies)
                    VALUES (:id, :employeeId, :projectName, :summary, :role, :technologies)
                """;

        namedJdbcTemplate.update(insertSql, Map.of(
                "id", workExperienceId1,
                "employeeId", employeeId1,
                "projectName", "Spring Boot Microservices",
                "summary", "Developed REST APIs using Spring Boot and Java",
                "role", "Backend Developer",
                "technologies", new String[]{"Java", "Spring Boot", "PostgreSQL"}
        ));

        namedJdbcTemplate.update(insertSql, Map.of(
                "id", workExperienceId2,
                "employeeId", employeeId2,
                "projectName", "React Frontend Application",
                "summary", "Built user interface with React and TypeScript",
                "role", "Frontend Developer",
                "technologies", new String[]{"React", "TypeScript", "JavaScript"}
        ));

        // Test keyword search for "Spring Boot"
        List<String> results = keywordSearchService.searchByKeywords(List.of("Spring", "Boot"), 5);

        assertNotNull(results);
        assertTrue(results.contains(employeeId1)); // Should find the Spring Boot expert
    }

    @Test
    void testSearchByMultipleKeywords() {
        // Insert test employee first
        String employeeId = IdGenerator.generateEmployeeId();
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId, "name", "Employee", "email", "emp-multi-" + System.currentTimeMillis() + "@test.com")
        );

        // Insert test data
        String workExperienceId = IdGenerator.generateId();

        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, project_summary, technologies) VALUES (:id, :employeeId, :name, :summary, :technologies)",
                Map.of(
                        "id", workExperienceId,
                        "employeeId", employeeId,
                        "name", "Banking Application",
                        "summary", "Developed secure banking system with Java and Spring",
                        "technologies", new String[]{"Java", "Spring Boot", "PostgreSQL", "Docker"}
                )
        );

        // Search for multiple keywords
        List<String> results = keywordSearchService.searchByKeywords(List.of("Java", "Spring", "PostgreSQL"), 5);

        assertNotNull(results);
        assertTrue(results.contains(employeeId));
    }

    @Test
    void testSearchWithNoMatches() {
        List<String> results = keywordSearchService.searchByKeywords(List.of("NonExistentTechnology"), 5);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchByTechnologies() {
        // Insert test employee first
        String employeeId = IdGenerator.generateEmployeeId();
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                Map.of("id", employeeId, "name", "Employee", "email", "emp-tech-" + System.currentTimeMillis() + "@test.com")
        );

        // Insert test data
        String workExperienceId = IdGenerator.generateId();

        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, technologies) VALUES (:id, :employeeId, :name, :technologies)",
                Map.of(
                        "id", workExperienceId,
                        "employeeId", employeeId,
                        "name", "Test Project",
                        "technologies", new String[]{"Java", "Spring Boot", "PostgreSQL"}
                )
        );

        // Search by technologies
        List<String> results = keywordSearchService.searchByTechnologies(List.of("Java", "Spring Boot"), 5);

        assertNotNull(results);
        assertTrue(results.contains(employeeId));
    }

    @Test
    void testSearchByKeywordsWithNullKeywords() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByKeywords(null, 5);
        });
    }

    @Test
    void testSearchByKeywordsWithEmptyKeywords() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByKeywords(List.of(), 5);
        });
    }

    @Test
    void testSearchByKeywordsWithInvalidMaxResults() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByKeywords(List.of("Java"), 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByKeywords(List.of("Java"), -1);
        });
    }

    @Test
    void testSearchByTechnologiesWithNullTechnologies() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByTechnologies(null, 5);
        });
    }

    @Test
    void testSearchByTechnologiesWithEmptyTechnologies() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByTechnologies(List.of(), 5);
        });
    }

    @Test
    void testSearchByTechnologiesWithInvalidMaxResults() {
        assertThrows(IllegalArgumentException.class, () -> {
            keywordSearchService.searchByTechnologies(List.of("Java"), 0);
        });
    }
}

