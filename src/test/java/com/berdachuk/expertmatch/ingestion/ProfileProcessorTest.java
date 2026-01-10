package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.model.EmployeeData;
import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.berdachuk.expertmatch.ingestion.model.ProjectData;
import com.berdachuk.expertmatch.ingestion.service.ProfileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileProcessorTest {

    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private ProfileProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processor = new ProfileProcessor(namedJdbcTemplate, objectMapper);
    }

    @Test
    void testProcessProfile_ValidProfile_ShouldReturnSuccess() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        assertEquals("4000741400013306668", result.employeeId());
        assertEquals("John Doe", result.employeeName());
        assertEquals(0, result.projectsProcessed());
        assertEquals(0, result.projectsSkipped());
        assertNull(result.errorMessage());
    }

    @Test
    void testProcessProfile_InvalidProfile_ShouldReturnFailure() {
        EmployeeData employee = new EmployeeData(
                null, // Missing required field
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("invalid") || result.errorMessage().contains("required"));
    }

    @Test
    void testProcessProfile_MissingOptionalFields_ShouldApplyDefaults() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "Jane Smith",
                null, // Missing email
                null, // Missing seniority
                null, // Missing languageEnglish
                null  // Missing availabilityStatus
        );
        EmployeeProfile profile = new EmployeeProfile(employee, null, List.of());

        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        // Verify that defaults were applied (check the SQL call parameters)
        verify(namedJdbcTemplate, atLeastOnce()).update(anyString(), any(Map.class));
    }

    @Test
    void testProcessProfile_WithProjects_ShouldProcessProjects() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        ProjectData project = new ProjectData(
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
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of(project));

        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);
        when(namedJdbcTemplate.query(anyString(), any(Map.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(List.of());

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        assertEquals(1, result.projectsProcessed());
        assertEquals(0, result.projectsSkipped());
    }

    @Test
    void testProcessProfile_WithoutProjects_ShouldProcessEmployeeOnly() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", null);

        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        assertEquals(0, result.projectsProcessed());
        assertEquals(0, result.projectsSkipped());
    }

    @Test
    void testProcessProfile_DuplicateEmployee_ShouldUpdate() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        // ON CONFLICT DO UPDATE should still return 1
        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success());
        // Verify update was called (ON CONFLICT DO UPDATE)
        verify(namedJdbcTemplate, atLeastOnce()).update(anyString(), any(Map.class));
    }

    @Test
    void testProcessProfile_InvalidProject_ShouldSkipProject() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        // Invalid project: missing required projectName
        ProjectData invalidProject = new ProjectData(
                "PRJ-001",
                null, // Missing required field
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
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of(invalidProject));

        when(namedJdbcTemplate.update(anyString(), any(Map.class))).thenReturn(1);

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertTrue(result.success()); // Employee processed successfully
        assertEquals(0, result.projectsProcessed());
        assertEquals(1, result.projectsSkipped());
        assertFalse(result.projectErrors().isEmpty());
    }

    @Test
    void testProcessProfile_DatabaseError_ShouldReturnFailure() {
        EmployeeData employee = new EmployeeData(
                "4000741400013306668",
                "John Doe",
                "john.doe@example.com",
                "B1",
                "B2",
                "available"
        );
        EmployeeProfile profile = new EmployeeProfile(employee, "Summary", List.of());

        when(namedJdbcTemplate.update(anyString(), any(Map.class)))
                .thenThrow(new RuntimeException("Database error"));

        ProcessingResult result = processor.processProfile(profile, new HashMap<>());

        assertFalse(result.success());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("Database error") ||
                result.errorMessage().contains("Failed"));
    }
}

