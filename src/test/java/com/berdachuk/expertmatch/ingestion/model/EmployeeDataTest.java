package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testValidation_AllRequiredFieldsPresent_ShouldBeValid() {
        EmployeeData data = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        assertTrue(data.isValid());
    }

    @Test
    void testValidation_MissingId_ShouldBeInvalid() {
        EmployeeData data = new EmployeeData(
                null,
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_BlankId_ShouldBeInvalid() {
        EmployeeData data = new EmployeeData(
                "   ",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_MissingName_ShouldBeInvalid() {
        EmployeeData data = new EmployeeData(
                "4000741400013306668",
                null,
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testValidation_BlankName_ShouldBeInvalid() {
        EmployeeData data = new EmployeeData(
                "4000741400013306668",
                "   ",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        assertFalse(data.isValid());
    }

    @Test
    void testWithDefaults_AllFieldsPresent_ShouldReturnSame() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals(original, withDefaults);
    }

    @Test
    void testWithDefaults_MissingEmail_ShouldGenerateFromName() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                null,
                "B1",
                "B2",
                "available"
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals("john.doe@example.com", withDefaults.email());
    }

    @Test
    void testWithDefaults_MissingSeniority_ShouldUseDefault() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                null,
                "B2",
                "available"
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals("B1", withDefaults.seniority());
    }

    @Test
    void testWithDefaults_MissingLanguageEnglish_ShouldUseDefault() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                null,
                "available"
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals("B2", withDefaults.languageEnglish());
    }

    @Test
    void testWithDefaults_MissingAvailabilityStatus_ShouldUseDefault() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                null
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals("available", withDefaults.availabilityStatus());
    }

    @Test
    void testWithDefaults_MultipleMissingFields_ShouldApplyAllDefaults() {
        EmployeeData original = new EmployeeData(
                "4000741400013306668",
                "Jane Smith",
                null,
                null,
                null,
                null
        );
        EmployeeData withDefaults = original.withDefaults();
        assertEquals("jane.smith@example.com", withDefaults.email());
        assertEquals("B1", withDefaults.seniority());
        assertEquals("B2", withDefaults.languageEnglish());
        assertEquals("available", withDefaults.availabilityStatus());
    }

    @Test
    void testJsonDeserialization_FullData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "id": "4000741400013306668",
                  "name": "John Doe",
                  "email": "john.doe@example.com",
                  "seniority": "B1",
                  "languageEnglish": "B2",
                  "availabilityStatus": "available"
                }
                """;
        EmployeeData data = objectMapper.readValue(json, EmployeeData.class);
        assertEquals("4000741400013306668", data.id());
        assertEquals("John Doe", data.name());
        assertEquals("john.doe@example.com", data.email());
    }

    @Test
    void testJsonDeserialization_PartialData_ShouldDeserialize() throws Exception {
        String json = """
                {
                  "id": "4000741400013306668",
                  "name": "John Doe"
                }
                """;
        EmployeeData data = objectMapper.readValue(json, EmployeeData.class);
        assertEquals("4000741400013306668", data.id());
        assertEquals("John Doe", data.name());
        assertNull(data.email());
        assertNull(data.seniority());
    }
}

