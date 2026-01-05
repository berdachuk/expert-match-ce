package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeProfileTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testValidation_AllRequiredFieldsPresent_ShouldBeValid() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(
                employee,
                "Summary",
                List.of()
        );
        assertTrue(profile.isValid());
    }

    @Test
    void testValidation_MissingEmployee_ShouldBeInvalid() {
        EmployeeProfile profile = new EmployeeProfile(
                null,
                "Summary",
                List.of()
        );
        assertFalse(profile.isValid());
    }

    @Test
    void testValidation_InvalidEmployee_ShouldBeInvalid() {
        EmployeeData employee = new EmployeeData(
                null,
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(
                employee,
                "Summary",
                List.of()
        );
        assertFalse(profile.isValid());
    }

    @Test
    void testValidation_MissingSummary_ShouldBeValid() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(
                employee,
                null,
                List.of()
        );
        assertTrue(profile.isValid());
    }

    @Test
    void testValidation_MissingProjects_ShouldBeValid() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(
                employee,
                "Summary",
                null
        );
        assertTrue(profile.isValid());
    }

    @Test
    void testJsonDeserialization_FullData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe",
                    "email": "john.doe@example.com",
                    "seniority": "B1",
                    "languageEnglish": "B2",
                    "availabilityStatus": "available"
                  },
                  "summary": "Summary text",
                  "projects": []
                }
                """;
        EmployeeProfile profile = objectMapper.readValue(json, EmployeeProfile.class);
        assertNotNull(profile.employee());
        assertEquals("John Doe", profile.employee().name());
        assertEquals("Summary text", profile.summary());
        assertNotNull(profile.projects());
    }

    @Test
    void testJsonDeserialization_PartialData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  }
                }
                """;
        EmployeeProfile profile = objectMapper.readValue(json, EmployeeProfile.class);
        assertNotNull(profile.employee());
        assertEquals("John Doe", profile.employee().name());
        assertNull(profile.summary());
        assertNull(profile.projects());
    }
}

