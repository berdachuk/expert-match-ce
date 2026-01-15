package com.berdachuk.expertmatch.employee.repository;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for EmployeeRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class EmployeeRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

        // Ensure pg_trgm extension is enabled for similarity search tests
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (Exception e) {
            // Extension might already exist or not be available - that's okay
            // Tests will handle this gracefully
        }
    }

    /**
     * Checks if pg_trgm extension is available in the database.
     */
    private boolean isPgTrgmAvailable() {
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("SELECT similarity('test', 'test')");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testFindById() {
        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Test Employee");
        params.put("email", "test@example.com");
        // Test with A level seniority (A4 = Lead)
        params.put("seniority", "A4");
        params.put("language_english", "B2");
        namedJdbcTemplate.update(insertSql, params);

        // Test findById
        var result = employeeRepository.findById(testId);
        assertTrue(result.isPresent());
        assertEquals("Test Employee", result.get().name());
        assertEquals("test@example.com", result.get().email());
        assertEquals("A4", result.get().seniority());
    }

    @Test
    void testFindByIds() {
        // Insert test employees
        String id1 = IdGenerator.generateEmployeeId();
        String id2 = IdGenerator.generateEmployeeId();

        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Employee 1");
        params1.put("email", "emp1@example.com");
        params1.put("seniority", "A3");
        params1.put("language_english", "B1");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Employee 2");
        params2.put("email", "emp2@example.com");
        params2.put("seniority", "A4");
        params2.put("language_english", "B2");
        namedJdbcTemplate.update(insertSql, params2);

        // Test findByIds
        List<com.berdachuk.expertmatch.employee.domain.Employee> employees = employeeRepository.findByIds(List.of(id1, id2));

        assertNotNull(employees);
        assertEquals(2, employees.size());
        assertTrue(employees.stream().anyMatch(e -> e.id().equals(id1)));
        assertTrue(employees.stream().anyMatch(e -> e.id().equals(id2)));
    }

    @Test
    void testFindByIdNotFound() {
        var result = employeeRepository.findById(IdGenerator.generateEmployeeId());
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByIdsEmpty() {
        List<com.berdachuk.expertmatch.employee.domain.Employee> employees = employeeRepository.findByIds(List.of());
        assertNotNull(employees);
        assertTrue(employees.isEmpty());
    }

    @Test
    void testFindEmployeeIdsByName_ExactMatch() {
        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test exact match
        List<String> results = employeeRepository.findEmployeeIdsByName("Siarhei Berdachuk", 10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(testId));
    }

    @Test
    void testFindEmployeeIdsByName_PartialMatch() {
        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test partial match - first name only
        List<String> results = employeeRepository.findEmployeeIdsByName("Siarhei", 10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(testId));

        // Test partial match - last name only
        results = employeeRepository.findEmployeeIdsByName("Berdachuk", 10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(testId));
    }

    @Test
    void testFindEmployeeIdsByName_CaseInsensitive() {
        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test case-insensitive search
        List<String> results = employeeRepository.findEmployeeIdsByName("siarhei berdachuk", 10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(testId));

        results = employeeRepository.findEmployeeIdsByName("SIARHEI BERDACHUK", 10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.contains(testId));
    }

    @Test
    void testFindEmployeeIdsByName_NoMatch() {
        // Insert test employee with different name
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "John Doe");
        params.put("email", "john.doe@example.com");
        params.put("seniority", "A3");
        params.put("language_english", "B1");
        namedJdbcTemplate.update(insertSql, params);

        // Test no match
        List<String> results = employeeRepository.findEmployeeIdsByName("Siarhei Berdachuk", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindEmployeeIdsByName_EmptyInput() {
        List<String> results = employeeRepository.findEmployeeIdsByName(null, 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = employeeRepository.findEmployeeIdsByName("", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = employeeRepository.findEmployeeIdsByName("   ", 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_ExactMatch() {
        // Skip test if pg_trgm extension is not available
        if (!isPgTrgmAvailable()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "pg_trgm extension not available, skipping similarity test");
            return;
        }

        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test exact match with similarity search
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity("Siarhei Berdachuk", 0.3, 10);
        assertNotNull(results);
        assertTrue(results.size() >= 1);
        assertTrue(results.contains(testId));
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_TypoHandling() {
        // Skip test if pg_trgm extension is not available
        if (!isPgTrgmAvailable()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "pg_trgm extension not available, skipping similarity test");
            return;
        }

        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test with typo - "Sergei" instead of "Siarhei"
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity("Sergei Berdachuk", 0.3, 10);
        assertNotNull(results);
        // Should find the employee despite the typo
        assertTrue(results.size() >= 1);
        assertTrue(results.contains(testId), "Should find employee despite typo in first name");
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_SpellingVariation() {
        // Skip test if pg_trgm extension is not available
        if (!isPgTrgmAvailable()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "pg_trgm extension not available, skipping similarity test");
            return;
        }

        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test with spelling variation - "Berdachuk" vs "Berdachuck"
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity("Siarhei Berdachuck", 0.3, 10);
        assertNotNull(results);
        // Should find the employee despite the spelling variation
        assertTrue(results.size() >= 1);
        assertTrue(results.contains(testId), "Should find employee despite spelling variation in last name");
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_ThresholdFiltering() {
        // Skip test if pg_trgm extension is not available
        if (!isPgTrgmAvailable()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "pg_trgm extension not available, skipping similarity test");
            return;
        }

        // Insert test employee
        String testId = IdGenerator.generateEmployeeId();
        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Siarhei Berdachuk");
        params.put("email", "siarhei.berdachuk@example.com");
        params.put("seniority", "A5");
        params.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params);

        // Test with very high threshold - should not match completely different name
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity("John Smith", 0.7, 10);
        assertNotNull(results);
        // Should not find the employee with high threshold and completely different name
        assertFalse(results.contains(testId), "Should not match completely different name with high threshold");
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_EmptyInput() {
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity(null, 0.3, 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = employeeRepository.findEmployeeIdsByNameSimilarity("", 0.3, 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = employeeRepository.findEmployeeIdsByNameSimilarity("   ", 0.3, 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindEmployeeIdsByNameSimilarity_MultipleEmployees() {
        // Skip test if pg_trgm extension is not available
        if (!isPgTrgmAvailable()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "pg_trgm extension not available, skipping similarity test");
            return;
        }

        // Insert multiple test employees
        String id1 = IdGenerator.generateEmployeeId();
        String id2 = IdGenerator.generateEmployeeId();
        String id3 = IdGenerator.generateEmployeeId();

        String insertSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english)
                VALUES (:id, :name, :email, :seniority, :language_english)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Siarhei Berdachuk");
        params1.put("email", "siarhei.berdachuk@example.com");
        params1.put("seniority", "A5");
        params1.put("language_english", "C2");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Sergei Berdachuk");
        params2.put("email", "sergei.berdachuk@example.com");
        params2.put("seniority", "A4");
        params2.put("language_english", "B2");
        namedJdbcTemplate.update(insertSql, params2);

        Map<String, Object> params3 = new HashMap<>();
        params3.put("id", id3);
        params3.put("name", "John Smith");
        params3.put("email", "john.smith@example.com");
        params3.put("seniority", "A3");
        params3.put("language_english", "B1");
        namedJdbcTemplate.update(insertSql, params3);

        // Test similarity search - should find both Siarhei and Sergei
        List<String> results = employeeRepository.findEmployeeIdsByNameSimilarity("Siarhei Berdachuk", 0.3, 10);
        assertNotNull(results);
        assertTrue(results.size() >= 2);
        assertTrue(results.contains(id1), "Should find exact match");
        assertTrue(results.contains(id2), "Should find similar name (Sergei)");
    }
}

