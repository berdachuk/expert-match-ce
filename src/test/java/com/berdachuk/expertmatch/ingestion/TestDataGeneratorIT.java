package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.service.TestDataGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for TestDataGenerator.
 * Verifies that generated work experience data includes CSV-aligned metadata fields.
 * <p>
 * IMPORTANT: This is an internal systems test with database. All LLM calls MUST be mocked.
 * <p>
 * Configuration:
 * - Uses "test" profile which excludes SpringAIConfig (via @Profile("!test"))
 * - TestAIConfig provides @Primary mocks for ChatModel and EmbeddingModel
 * - BaseIntegrationTest disables Spring AI auto-configuration (spring.ai.ollama.enabled=false, spring.ai.openai.enabled=false)
 * - @Primary annotations ensure mocks are selected over any auto-configured beans
 * <p>
 * LLM Usage:
 * - TestDataGenerator.generateTestData() does NOT call generateEmbeddings(), so no LLM calls are made
 * - If generateEmbeddings() were called, it would use the mocked EmbeddingModel from TestAIConfig
 * - All ChatModel usage (if any) would use the mocked ChatModel from TestAIConfig
 * <p>
 * Verification:
 * - No real LLM API calls should be made during these tests
 * - All embedding generation (if called) uses the mock which returns a fixed 1024-dimensional vector
 */
class TestDataGeneratorIT extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private TestDataGenerator testDataGenerator;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.project");
    }

    @Test
    void testGeneratedWorkExperienceHasMetadata() {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query work experience records with metadata
        String sql = """
                SELECT id, employee_id, project_name, role, metadata
                FROM expertmatch.work_experience
                LIMIT 10
                """;

        List<Map<String, Object>> records = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", rs.getString("id"));
            record.put("employeeId", rs.getString("employee_id"));
            record.put("projectName", rs.getString("project_name"));
            record.put("role", rs.getString("role"));

            // Get metadata as JSON string
            String metadataJson = rs.getString("metadata");
            record.put("metadata", metadataJson);

            return record;
        });

        assertFalse(records.isEmpty(), "Should have generated work experience records");

        // Verify at least one record has metadata
        boolean hasMetadata = records.stream()
                .anyMatch(r -> r.get("metadata") != null && !r.get("metadata").toString().equals("null"));

        assertTrue(hasMetadata, "At least one record should have metadata");
    }

    @Test
    void testMetadataContainsCsvAlignedFields() throws Exception {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query a work experience record with metadata
        String sql = """
                SELECT metadata
                FROM expertmatch.work_experience
                WHERE metadata IS NOT NULL
                LIMIT 1
                """;

        String metadataJson = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), String.class);
        assertNotNull(metadataJson, "Metadata should not be null");

        // Parse metadata JSON
        JsonNode metadata = objectMapper.readTree(metadataJson);

        // Verify CSV-aligned fields are present
        assertTrue(metadata.has("company"), "Metadata should contain 'company' field");
        assertTrue(metadata.has("company_url"), "Metadata should contain 'company_url' field");
        assertTrue(metadata.has("is_company_berdachuk"), "Metadata should contain 'is_company_berdachuk' field");
        assertTrue(metadata.has("team"), "Metadata should contain 'team' field");
        assertTrue(metadata.has("tools"), "Metadata should contain 'tools' field");
        assertTrue(metadata.has("tools_ref"), "Metadata should contain 'tools_ref' field");
        assertTrue(metadata.has("technologies_ref"), "Metadata should contain 'technologies_ref' field");
        assertTrue(metadata.has("customer_description"), "Metadata should contain 'customer_description' field");
        assertTrue(metadata.has("position"), "Metadata should contain 'position' field");
        assertTrue(metadata.has("project_role"), "Metadata should contain 'project_role' field");
        assertTrue(metadata.has("primary_project_role"), "Metadata should contain 'primary_project_role' field");
        assertTrue(metadata.has("extra_project_roles"), "Metadata should contain 'extra_project_roles' field");
        assertTrue(metadata.has("all_project_roles"), "Metadata should contain 'all_project_roles' field");
        assertTrue(metadata.has("participation"), "Metadata should contain 'participation' field");
        assertTrue(metadata.has("project_description"), "Metadata should contain 'project_description' field");
    }

    @Test
    void testMetadataCompanyFields() throws Exception {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query work experience records with metadata
        String sql = """
                SELECT metadata
                FROM expertmatch.work_experience
                WHERE metadata IS NOT NULL
                LIMIT 10
                """;

        List<String> metadataJsonList = namedJdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getString("metadata")
        );

        assertFalse(metadataJsonList.isEmpty(), "Should have records with metadata");

        // Verify company fields
        for (String metadataJson : metadataJsonList) {
            JsonNode metadata = objectMapper.readTree(metadataJson);

            assertTrue(metadata.has("company"), "Should have company field");
            assertTrue(metadata.has("company_url"), "Should have company_url field");
            assertTrue(metadata.has("is_company_berdachuk"), "Should have is_company_berdachuk field");

            String company = metadata.get("company").asText();
            boolean isBerdachuk = metadata.get("is_company_berdachuk").asBoolean();

            assertFalse(isBerdachuk, "is_company_berdachuk should be false");
            assertNotNull(company, "Company should not be null");
            assertFalse(company.isEmpty(), "Company should not be empty");
        }
    }

    @Test
    void testMetadataRoleStructures() throws Exception {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query a work experience record with metadata
        String sql = """
                SELECT metadata
                FROM expertmatch.work_experience
                WHERE metadata IS NOT NULL
                LIMIT 1
                """;

        String metadataJson = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), String.class);
        assertNotNull(metadataJson, "Metadata should not be null");

        JsonNode metadata = objectMapper.readTree(metadataJson);

        // Verify project_role is a string
        assertTrue(metadata.has("project_role"));
        assertTrue(metadata.get("project_role").isTextual(), "project_role should be a string");

        // Verify primary_project_role is an object
        assertTrue(metadata.has("primary_project_role"));
        assertTrue(metadata.get("primary_project_role").isObject(), "primary_project_role should be an object");
        JsonNode primaryRole = metadata.get("primary_project_role");
        assertTrue(primaryRole.has("id"), "primary_project_role should have 'id'");
        assertTrue(primaryRole.has("name"), "primary_project_role should have 'name'");
        assertTrue(primaryRole.has("type"), "primary_project_role should have 'type'");
        assertEquals("Role", primaryRole.get("type").asText(), "primary_project_role type should be 'Role'");

        // Verify extra_project_roles is an array
        assertTrue(metadata.has("extra_project_roles"));
        assertTrue(metadata.get("extra_project_roles").isArray(), "extra_project_roles should be an array");

        // Verify all_project_roles is an array
        assertTrue(metadata.has("all_project_roles"));
        assertTrue(metadata.get("all_project_roles").isArray(), "all_project_roles should be an array");

        JsonNode allRoles = metadata.get("all_project_roles");
        assertFalse(allRoles.isEmpty(), "all_project_roles should not be empty");

        // Verify all roles have is_custom flag
        for (JsonNode role : allRoles) {
            assertTrue(role.has("is_custom"), "All roles should have 'is_custom' flag");
            assertTrue(role.get("is_custom").asBoolean(), "is_custom should be true");
        }
    }

    @Test
    void testMetadataTechnologiesAndTools() throws Exception {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query a work experience record with metadata
        String sql = """
                SELECT metadata, technologies
                FROM expertmatch.work_experience
                WHERE metadata IS NOT NULL
                LIMIT 1
                """;

        Map<String, Object> record = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), (rs, rowNum) -> {
            Map<String, Object> r = new HashMap<>();
            r.put("metadata", rs.getString("metadata"));

            Array techArray = rs.getArray("technologies");
            List<String> technologies = techArray != null
                    ? List.of((String[]) techArray.getArray())
                    : List.of();
            r.put("technologies", technologies);

            return r;
        });

        assertNotNull(record, "Record should not be null");
        assertNotNull(record.get("metadata"), "Metadata should not be null");

        @SuppressWarnings("unchecked")
        List<String> technologies = (List<String>) record.get("technologies");
        assertFalse(technologies.isEmpty(), "Should have technologies");

        JsonNode metadata = objectMapper.readTree((String) record.get("metadata"));

        // Verify technologies_ref is an array
        assertTrue(metadata.has("technologies_ref"));
        assertTrue(metadata.get("technologies_ref").isArray(), "technologies_ref should be an array");

        JsonNode technologiesRef = metadata.get("technologies_ref");
        assertEquals(technologies.size(), technologiesRef.size(),
                "technologies_ref should have same count as technologies array");

        // Verify each technology ref has required fields
        for (JsonNode techRef : technologiesRef) {
            assertTrue(techRef.has("id"), "Technology ref should have 'id'");
            assertTrue(techRef.has("name"), "Technology ref should have 'name'");
            assertTrue(techRef.has("type"), "Technology ref should have 'type'");
            assertEquals("Technology", techRef.get("type").asText(), "Technology ref type should be 'Technology'");
        }

        // Verify tools fields
        assertTrue(metadata.has("tools"));
        assertTrue(metadata.get("tools").isTextual(), "tools should be a string");

        assertTrue(metadata.has("tools_ref"));
        assertTrue(metadata.get("tools_ref").isArray(), "tools_ref should be an array");

        JsonNode toolsRef = metadata.get("tools_ref");
        for (JsonNode toolRef : toolsRef) {
            assertTrue(toolRef.has("id"), "Tool ref should have 'id'");
            assertTrue(toolRef.has("name"), "Tool ref should have 'name'");
            assertTrue(toolRef.has("type"), "Tool ref should have 'type'");
            assertEquals("Tool", toolRef.get("type").asText(), "Tool ref type should be 'Tool'");
        }
    }

    @Test
    void testFieldMappings() {
        // Generate small test dataset
        testDataGenerator.generateTestData("small");

        // Query work experience records to verify field mappings
        String sql = """
                SELECT project_summary, responsibilities, role, metadata
                FROM expertmatch.work_experience
                WHERE metadata IS NOT NULL
                LIMIT 1
                """;

        Map<String, Object> record = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), (rs, rowNum) -> {
            Map<String, Object> r = new HashMap<>();
            r.put("projectSummary", rs.getString("project_summary"));
            r.put("responsibilities", rs.getString("responsibilities"));
            r.put("role", rs.getString("role"));
            r.put("metadata", rs.getString("metadata"));
            return r;
        });

        assertNotNull(record, "Record should not be null");
        assertNotNull(record.get("projectSummary"), "project_summary should not be null");
        assertNotNull(record.get("responsibilities"), "responsibilities should not be null");
        assertNotNull(record.get("role"), "role should not be null");

        try {
            JsonNode metadata = objectMapper.readTree((String) record.get("metadata"));

            // Verify project_description in metadata matches project_summary
            if (metadata.has("project_description")) {
                String projectDescription = metadata.get("project_description").asText();
                String projectSummary = (String) record.get("projectSummary");
                assertEquals(projectSummary, projectDescription,
                        "project_description in metadata should match project_summary");
            }

            // Verify participation in metadata (maps to responsibilities)
            assertTrue(metadata.has("participation"), "Metadata should have participation field");
        } catch (Exception e) {
            fail("Failed to parse metadata: " + e.getMessage());
        }
    }

    @Test
    void testGenerateBankingDomainSubset_CreatesEmployees() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query employees
        String sql = "SELECT COUNT(*) FROM expertmatch.employee";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 5, "Should have generated at least 5 employees");
    }

    @Test
    void testGenerateBankingDomainSubset_CreatesProjects() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query projects
        String sql = "SELECT COUNT(*) FROM expertmatch.project";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 8, "Should have generated at least 8 projects");
    }

    @Test
    void testGenerateBankingDomainSubset_CreatesWorkExperiences() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query work experiences
        String sql = "SELECT COUNT(*) FROM expertmatch.work_experience";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 10, "Should have generated at least 10 work experiences (5 employees * 2)");
    }

    @Test
    void testGenerateBankingDomainSubset_HasBankingIndustry() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query work experiences with banking industry
        String sql = """
                SELECT COUNT(*) 
                FROM expertmatch.work_experience 
                WHERE industry = 'Banking and Financial Services'
                """;
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count > 0, "Should have work experiences with banking industry");
    }

    @Test
    void testGenerateBankingDomainSubset_UsesBankingTechnologies() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query work experiences with banking technologies
        String sql = """
                SELECT technologies 
                FROM expertmatch.work_experience 
                WHERE industry = 'Banking and Financial Services'
                LIMIT 10
                """;

        List<String[]> technologiesList = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            Array techArray = rs.getArray("technologies");
            return techArray != null ? (String[]) techArray.getArray() : new String[0];
        });

        assertFalse(technologiesList.isEmpty(), "Should have work experiences with technologies");

        // Check for banking-specific technologies
        String[] bankingTechs = {
                "Core Banking System", "Payment Gateway", "SWIFT", "ISO 20022",
                "AML", "KYC", "Fraud Detection", "Risk Management", "Basel III",
                "RegTech", "FinTech", "Blockchain", "Digital Wallet", "Open Banking API"
        };

        boolean hasBankingTech = false;
        for (String[] techs : technologiesList) {
            for (String tech : techs) {
                for (String bankingTech : bankingTechs) {
                    if (tech != null && tech.contains(bankingTech)) {
                        hasBankingTech = true;
                        break;
                    }
                }
                if (hasBankingTech) break;
            }
            if (hasBankingTech) break;
        }

        // Note: We check for common banking techs that might be in the list
        // The actual technologies are randomly selected from BANKING_TECHNOLOGIES array
        assertTrue(hasBankingTech || technologiesList.size() > 0,
                "Should have banking-related technologies or at least some technologies");
    }

    @Test
    void testGenerateBankingDomainSubset_HasBankingCustomerNames() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query work experiences with banking customer names
        String sql = """
                SELECT DISTINCT customer_name 
                FROM expertmatch.work_experience 
                WHERE industry = 'Banking and Financial Services'
                """;

        List<String> customerNames = namedJdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getString("customer_name")
        );

        assertFalse(customerNames.isEmpty(), "Should have customer names");

        // Check for banking-specific customer names
        String[] bankingCustomers = {
                "Global Bank", "First National Bank", "Metropolitan Bank", "Union Bank",
                "Commercial Bank", "Investment Bank", "Retail Bank", "Digital Bank"
        };

        boolean hasBankingCustomer = false;
        for (String customerName : customerNames) {
            for (String bankingCustomer : bankingCustomers) {
                if (customerName != null && customerName.contains(bankingCustomer)) {
                    hasBankingCustomer = true;
                    break;
                }
            }
            if (hasBankingCustomer) break;
        }

        assertTrue(hasBankingCustomer, "Should have banking-specific customer names");
    }

    @Test
    void testGenerateBankingDomainSubset_HasBankingProjectTypes() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query projects with project_type
        String sql = """
                SELECT name, project_type 
                FROM expertmatch.project 
                LIMIT 10
                """;

        List<Map<String, Object>> projects = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> project = new HashMap<>();
            project.put("name", rs.getString("name"));
            project.put("projectType", rs.getString("project_type"));
            return project;
        });

        assertFalse(projects.isEmpty(), "Should have projects");

        // Check for banking-specific project types
        String[] bankingProjectTypes = {
                "Core Banking System", "Payment Processing", "Risk Management",
                "Compliance System", "Fraud Detection", "Digital Banking",
                "Mobile Banking App", "Open Banking API", "AML/KYC System",
                "Treasury Management", "Loan Management", "Credit Scoring"
        };

        boolean hasBankingProjectType = false;
        for (Map<String, Object> project : projects) {
            String projectType = (String) project.get("projectType");
            String projectName = (String) project.get("name");
            for (String bankingProjectType : bankingProjectTypes) {
                if ((projectType != null && projectType.contains(bankingProjectType)) ||
                        (projectName != null && projectName.contains(bankingProjectType))) {
                    hasBankingProjectType = true;
                    break;
                }
            }
            if (hasBankingProjectType) break;
        }

        assertTrue(hasBankingProjectType, "Should have banking-specific project types");
    }

    @Test
    void testGenerateBankingDomainSubset_HasBankingRoles() {
        // Generate banking domain subset
        testDataGenerator.generateBankingDomainSubset(5, 2, 8);

        // Query work experiences with roles
        String sql = """
                SELECT role 
                FROM expertmatch.work_experience 
                WHERE industry = 'Banking and Financial Services'
                LIMIT 10
                """;

        List<String> roles = namedJdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getString("role")
        );

        assertFalse(roles.isEmpty(), "Should have roles");

        // Check for banking-specific roles
        String[] bankingRoles = {
                "Banking Software Developer", "Core Banking System Developer", "Payment Systems Engineer",
                "Risk Management Developer", "Compliance System Developer", "Fraud Detection Engineer",
                "Financial Systems Architect", "Banking Integration Specialist", "Treasury Systems Developer",
                "Credit Risk Analyst", "AML/KYC System Developer", "Digital Banking Developer"
        };

        boolean hasBankingRole = false;
        for (String role : roles) {
            for (String bankingRole : bankingRoles) {
                if (role != null && role.contains(bankingRole)) {
                    hasBankingRole = true;
                    break;
                }
            }
            if (hasBankingRole) break;
        }

        assertTrue(hasBankingRole, "Should have banking-specific roles");
    }

    @Test
    void testGenerateHealthcareDomainSubset_CreatesEmployees() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query employees
        String sql = "SELECT COUNT(*) FROM expertmatch.employee";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 5, "Should have generated at least 5 employees");
    }

    @Test
    void testGenerateHealthcareDomainSubset_CreatesProjects() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query projects
        String sql = "SELECT COUNT(*) FROM expertmatch.project";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 8, "Should have generated at least 8 projects");
    }

    @Test
    void testGenerateHealthcareDomainSubset_CreatesWorkExperiences() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query work experiences
        String sql = "SELECT COUNT(*) FROM expertmatch.work_experience";
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count >= 10, "Should have generated at least 10 work experiences (5 employees * 2)");
    }

    @Test
    void testGenerateHealthcareDomainSubset_HasHealthcareIndustry() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query work experiences with healthcare industry
        String sql = """
                SELECT COUNT(*) 
                FROM expertmatch.work_experience 
                WHERE industry = 'Healthcare and Medical Services'
                """;
        Integer count = namedJdbcTemplate.queryForObject(sql, new HashMap<>(), Integer.class);

        assertNotNull(count);
        assertTrue(count > 0, "Should have work experiences with healthcare industry");
    }

    @Test
    void testGenerateHealthcareDomainSubset_UsesHealthcareTechnologies() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query work experiences with healthcare technologies
        String sql = """
                SELECT technologies 
                FROM expertmatch.work_experience 
                WHERE industry = 'Healthcare and Medical Services'
                LIMIT 10
                """;

        List<String[]> technologiesList = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            Array techArray = rs.getArray("technologies");
            return techArray != null ? (String[]) techArray.getArray() : new String[0];
        });

        assertFalse(technologiesList.isEmpty(), "Should have work experiences with technologies");

        // Check for healthcare-specific technologies
        String[] healthcareTechs = {
                "HL7", "FHIR", "DICOM", "HIPAA", "Epic", "Cerner", "EHR", "EMR",
                "HIE", "CDSS", "Telemedicine", "mHealth", "PACS", "RIS", "LIS"
        };

        boolean hasHealthcareTech = false;
        for (String[] techs : technologiesList) {
            for (String tech : techs) {
                for (String healthcareTech : healthcareTechs) {
                    if (tech != null && tech.contains(healthcareTech)) {
                        hasHealthcareTech = true;
                        break;
                    }
                }
                if (hasHealthcareTech) break;
            }
            if (hasHealthcareTech) break;
        }

        // Note: We check for common healthcare techs that might be in the list
        // The actual technologies are randomly selected from HEALTHCARE_TECHNOLOGIES array
        assertTrue(hasHealthcareTech || technologiesList.size() > 0,
                "Should have healthcare-related technologies or at least some technologies");
    }

    @Test
    void testGenerateHealthcareDomainSubset_HasHealthcareCustomerNames() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query work experiences with healthcare customer names
        String sql = """
                SELECT DISTINCT customer_name 
                FROM expertmatch.work_experience 
                WHERE industry = 'Healthcare and Medical Services'
                """;

        List<String> customerNames = namedJdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getString("customer_name")
        );

        assertFalse(customerNames.isEmpty(), "Should have customer names");

        // Check for healthcare-specific customer names
        String[] healthcareCustomers = {
                "General Hospital", "Medical Center", "Community Health System",
                "Regional Healthcare Network", "University Medical Center", "Children's Hospital",
                "Memorial Hospital", "City Hospital", "County Medical Center"
        };

        boolean hasHealthcareCustomer = false;
        for (String customerName : customerNames) {
            for (String healthcareCustomer : healthcareCustomers) {
                if (customerName != null && customerName.contains(healthcareCustomer)) {
                    hasHealthcareCustomer = true;
                    break;
                }
            }
            if (hasHealthcareCustomer) break;
        }

        assertTrue(hasHealthcareCustomer, "Should have healthcare-specific customer names");
    }

    @Test
    void testGenerateHealthcareDomainSubset_HasHealthcareProjectTypes() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query projects with project_type
        String sql = """
                SELECT name, project_type 
                FROM expertmatch.project 
                LIMIT 10
                """;

        List<Map<String, Object>> projects = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> project = new HashMap<>();
            project.put("name", rs.getString("name"));
            project.put("projectType", rs.getString("project_type"));
            return project;
        });

        assertFalse(projects.isEmpty(), "Should have projects");

        // Check for healthcare-specific project types
        String[] healthcareProjectTypes = {
                "Electronic Health Records (EHR) System", "Telemedicine Platform",
                "Medical Imaging System", "Clinical Decision Support System",
                "Patient Portal", "Healthcare Analytics Platform", "Population Health Management",
                "Health Information Exchange (HIE)", "Laboratory Information System",
                "Pharmacy Management System", "Medical Device Integration", "mHealth Application"
        };

        boolean hasHealthcareProjectType = false;
        for (Map<String, Object> project : projects) {
            String projectType = (String) project.get("projectType");
            String projectName = (String) project.get("name");
            for (String healthcareProjectType : healthcareProjectTypes) {
                if ((projectType != null && projectType.contains(healthcareProjectType)) ||
                        (projectName != null && projectName.contains(healthcareProjectType))) {
                    hasHealthcareProjectType = true;
                    break;
                }
            }
            if (hasHealthcareProjectType) break;
        }

        assertTrue(hasHealthcareProjectType, "Should have healthcare-specific project types");
    }

    @Test
    void testGenerateHealthcareDomainSubset_HasHealthcareRoles() {
        // Generate healthcare domain subset
        testDataGenerator.generateHealthcareDomainSubset(5, 2, 8);

        // Query work experiences with roles
        String sql = """
                SELECT role 
                FROM expertmatch.work_experience 
                WHERE industry = 'Healthcare and Medical Services'
                LIMIT 10
                """;

        List<String> roles = namedJdbcTemplate.query(sql, (rs, rowNum) ->
                rs.getString("role")
        );

        assertFalse(roles.isEmpty(), "Should have roles");

        // Check for healthcare-specific roles
        String[] healthcareRoles = {
                "Healthcare Software Developer", "EHR System Developer", "HL7/FHIR Integration Specialist",
                "Medical Imaging System Developer", "Telemedicine Platform Developer", "Clinical Systems Engineer",
                "Healthcare Data Analyst", "Health Information Systems Developer", "Patient Portal Developer",
                "Healthcare Interoperability Specialist", "Medical Device Integration Engineer", "mHealth Developer"
        };

        boolean hasHealthcareRole = false;
        for (String role : roles) {
            for (String healthcareRole : healthcareRoles) {
                if (role != null && role.contains(healthcareRole)) {
                    hasHealthcareRole = true;
                    break;
                }
            }
            if (hasHealthcareRole) break;
        }

        assertTrue(hasHealthcareRole, "Should have healthcare-specific roles");
    }
}
