package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testValidation_AllRequiredFieldsPresent_ShouldBeValid() {
        ProjectData data = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java", "Spring"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        assertTrue(data.isValid());
    }

    @Test
    void testValidation_MissingProjectName_ShouldBeInvalid() {
        ProjectData data = new ProjectData(
                "PRJ-001",
                null,
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_BlankProjectName_ShouldBeInvalid() {
        ProjectData data = new ProjectData(
                "PRJ-001",
                "   ",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_MissingStartDate_ShouldBeInvalid() {
        ProjectData data = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                null,
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_BlankStartDate_ShouldBeInvalid() {
        ProjectData data = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "   ",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testWithDefaults_AllFieldsPresent_ShouldReturnSame() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals(original.projectCode(), withDefaults.projectCode());
        assertEquals(original.projectName(), withDefaults.projectName());
    }

    @Test
    void testWithDefaults_MissingProjectCode_ShouldGenerate() {
        ProjectData original = new ProjectData(
                null,
                "My Awesome Project",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertNotNull(withDefaults.projectCode());
        // Generated code should be like "MY-AWESOME-PROJECT" or similar
        assertTrue(withDefaults.projectCode().length() > 0);
        assertTrue(withDefaults.projectCode().contains("MY") ||
                withDefaults.projectCode().contains("AWESOME") ||
                withDefaults.projectCode().equals("PRJ-UNKNOWN"));
    }

    @Test
    void testWithDefaults_MissingEndDate_ShouldUseCurrentDate() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                null,
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertNotNull(withDefaults.endDate());
        assertEquals(LocalDate.now().toString(), withDefaults.endDate());
    }

    @Test
    void testWithDefaults_MissingTechnologies_ShouldUseEmptyList() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                null,
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertNotNull(withDefaults.technologies());
        assertTrue(withDefaults.technologies().isEmpty());
    }

    @Test
    void testWithDefaults_MissingCustomerName_ShouldUseDefault() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                null,
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("Unknown Customer", withDefaults.customerName());
    }

    @Test
    void testWithDefaults_MissingCompanyName_ShouldUseCustomerName() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                null,
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("Customer Name", withDefaults.companyName());
    }

    @Test
    void testWithDefaults_MissingRole_ShouldUseDefault() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                null,
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("Developer", withDefaults.role());
    }

    @Test
    void testWithDefaults_MissingIndustry_ShouldUseDefault() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                null,
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("Technology", withDefaults.industry());
    }

    @Test
    void testWithDefaults_MissingResponsibilities_ShouldUseEmptyString() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                null,
                "Technology",
                "Summary"
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("", withDefaults.responsibilities());
    }

    @Test
    void testWithDefaults_MissingProjectSummary_ShouldUseEmptyString() {
        ProjectData original = new ProjectData(
                "PRJ-001",
                "Project Name",
                "Customer Name",
                "Company Name",
                "Developer",
                "2023-01-01",
                "2023-12-31",
                List.of("Java"),
                "Responsibilities",
                "Technology",
                null
        );
        ProjectData withDefaults = original.withDefaults();
        assertEquals("", withDefaults.projectSummary());
    }

    @Test
    void testJsonDeserialization_FullData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "projectCode": "PRJ-001",
                  "projectName": "Project Name",
                  "customerName": "Customer Name",
                  "companyName": "Company Name",
                  "role": "Developer",
                  "startDate": "2023-01-01",
                  "endDate": "2023-12-31",
                  "technologies": ["Java", "Spring"],
                  "responsibilities": "Responsibilities",
                  "industry": "Technology",
                  "projectSummary": "Summary"
                }
                """;
        ProjectData data = objectMapper.readValue(json, ProjectData.class);
        assertEquals("PRJ-001", data.projectCode());
        assertEquals("Project Name", data.projectName());
        assertEquals(2, data.technologies().size());
    }

    @Test
    void testJsonDeserialization_PartialData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "projectName": "Project Name",
                  "startDate": "2023-01-01"
                }
                """;
        ProjectData data = objectMapper.readValue(json, ProjectData.class);
        assertEquals("Project Name", data.projectName());
        assertEquals("2023-01-01", data.startDate());
        assertNull(data.projectCode());
        assertNull(data.technologies());
    }
}

