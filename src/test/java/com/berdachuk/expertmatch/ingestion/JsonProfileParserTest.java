package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonProfileParserTest {

    private JsonProfileParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new JsonProfileParser(objectMapper);
    }

    @Test
    void testParseProfiles_ArrayFormat_ShouldParseMultipleProfiles() throws JsonProcessingException {
        String json = """
                [
                  {
                    "employee": {
                      "id": "4000741400013306668",
                      "name": "John Doe"
                    },
                    "summary": "Summary 1",
                    "projects": []
                  },
                  {
                    "employee": {
                      "id": "5000741400013306669",
                      "name": "Jane Smith"
                    },
                    "summary": "Summary 2",
                    "projects": []
                  }
                ]
                """;
        List<EmployeeProfile> profiles = parser.parseProfiles(json);
        assertEquals(2, profiles.size());
        assertEquals("John Doe", profiles.get(0).employee().name());
        assertEquals("Jane Smith", profiles.get(1).employee().name());
    }

    @Test
    void testParseProfiles_SingleObjectFormat_ShouldParseSingleProfile() throws JsonProcessingException {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  },
                  "summary": "Summary",
                  "projects": []
                }
                """;
        List<EmployeeProfile> profiles = parser.parseProfiles(json);
        assertEquals(1, profiles.size());
        assertEquals("John Doe", profiles.get(0).employee().name());
    }

    @Test
    void testParseProfiles_EmptyArray_ShouldReturnEmptyList() throws JsonProcessingException {
        String json = "[]";
        List<EmployeeProfile> profiles = parser.parseProfiles(json);
        assertTrue(profiles.isEmpty());
    }

    @Test
    void testParseProfiles_InvalidJson_ShouldThrowException() {
        String json = "{ invalid json }";
        assertThrows(JsonProcessingException.class, () -> parser.parseProfiles(json));
    }

    @Test
    void testParseProfiles_NullString_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseProfiles((String) null));
    }

    @Test
    void testParseProfiles_EmptyString_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseProfiles(""));
    }

    @Test
    void testParseProfiles_InputStream_ShouldParse() throws IOException {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  }
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<EmployeeProfile> profiles = parser.parseProfiles(inputStream);
        assertEquals(1, profiles.size());
        assertEquals("John Doe", profiles.get(0).employee().name());
    }

    @Test
    void testParseProfiles_InputStream_ArrayFormat_ShouldParse() throws IOException {
        String json = """
                [
                  {
                    "employee": {
                      "id": "4000741400013306668",
                      "name": "John Doe"
                    }
                  },
                  {
                    "employee": {
                      "id": "5000741400013306669",
                      "name": "Jane Smith"
                    }
                  }
                ]
                """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<EmployeeProfile> profiles = parser.parseProfiles(inputStream);
        assertEquals(2, profiles.size());
    }

    @Test
    void testParseProfiles_InputStream_InvalidJson_ShouldThrowException() {
        String json = "{ invalid }";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonProcessingException.class, () -> parser.parseProfiles(inputStream));
    }

    @Test
    void testParseProfiles_WithProjects_ShouldParseProjects() throws JsonProcessingException {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  },
                  "projects": [
                    {
                      "projectName": "Project 1",
                      "startDate": "2023-01-01"
                    },
                    {
                      "projectName": "Project 2",
                      "startDate": "2023-06-01"
                    }
                  ]
                }
                """;
        List<EmployeeProfile> profiles = parser.parseProfiles(json);
        assertEquals(1, profiles.size());
        assertNotNull(profiles.get(0).projects());
        assertEquals(2, profiles.get(0).projects().size());
        assertEquals("Project 1", profiles.get(0).projects().get(0).projectName());
    }

    @Test
    void testParseProfiles_PartialData_ShouldParse() throws JsonProcessingException {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  }
                }
                """;
        List<EmployeeProfile> profiles = parser.parseProfiles(json);
        assertEquals(1, profiles.size());
        assertNull(profiles.get(0).summary());
        assertNull(profiles.get(0).projects());
    }
}

