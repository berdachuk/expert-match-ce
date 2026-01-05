package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.IngestionResult;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonProfileIngestionServiceTest {

    @TempDir
    Path tempDir;
    @Mock
    private JsonProfileParser parser;
    @Mock
    private ProfileProcessor processor;
    private JsonProfileIngestionService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new JsonProfileIngestionService(parser, processor, objectMapper);
    }

    @Test
    void testIngestFromContent_ArrayFormat_ShouldProcessMultipleProfiles() throws Exception {
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

        EmployeeProfile profile1 = createProfile("4000741400013306668", "John Doe");
        EmployeeProfile profile2 = createProfile("5000741400013306669", "Jane Smith");

        when(parser.parseProfiles(json)).thenReturn(List.of(profile1, profile2));
        when(processor.processProfile(any(EmployeeProfile.class), any()))
                .thenReturn(ProcessingResult.success("4000741400013306668", "John Doe", 0, 0, List.of()))
                .thenReturn(ProcessingResult.success("5000741400013306669", "Jane Smith", 0, 0, List.of()));

        IngestionResult result = service.ingestFromContent(json, "test-source");

        assertEquals(2, result.totalProfiles());
        assertEquals(2, result.successCount());
        assertEquals(0, result.errorCount());
        assertEquals("test-source", result.sourceName());
    }

    @Test
    void testIngestFromContent_SingleObjectFormat_ShouldProcessSingleProfile() throws Exception {
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  }
                }
                """;

        EmployeeProfile profile = createProfile("4000741400013306668", "John Doe");

        when(parser.parseProfiles(json)).thenReturn(List.of(profile));
        when(processor.processProfile(any(EmployeeProfile.class), any()))
                .thenReturn(ProcessingResult.success("4000741400013306668", "John Doe", 0, 0, List.of()));

        IngestionResult result = service.ingestFromContent(json, "test-source");

        assertEquals(1, result.totalProfiles());
        assertEquals(1, result.successCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testIngestFromContent_OneProfileFails_ShouldContinueWithOthers() throws Exception {
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

        EmployeeProfile profile1 = createProfile("4000741400013306668", "John Doe");
        EmployeeProfile profile2 = createProfile("5000741400013306669", "Jane Smith");

        when(parser.parseProfiles(json)).thenReturn(List.of(profile1, profile2));
        when(processor.processProfile(any(EmployeeProfile.class), any()))
                .thenAnswer(invocation -> {
                    EmployeeProfile profile = invocation.getArgument(0);
                    if (profile.employee().id().equals("4000741400013306668")) {
                        return ProcessingResult.failure("4000741400013306668", "John Doe", "Error");
                    } else {
                        return ProcessingResult.success("5000741400013306669", "Jane Smith", 0, 0, List.of());
                    }
                });

        IngestionResult result = service.ingestFromContent(json, "test-source");

        assertEquals(2, result.totalProfiles());
        assertEquals(1, result.successCount());
        assertEquals(1, result.errorCount());
    }

    @Test
    void testIngestFromContent_InvalidJson_ShouldReturnError() throws Exception {
        String json = "{ invalid json }";

        when(parser.parseProfiles(json)).thenThrow(
                new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        IngestionResult result = service.ingestFromContent(json, "test-source");

        assertEquals(0, result.totalProfiles());
        assertEquals(0, result.successCount());
        assertEquals(0, result.errorCount()); // Error is logged, not counted in errorCount
    }

    @Test
    void testIngestFromFile_ValidFile_ShouldProcess() throws Exception {
        File jsonFile = tempDir.resolve("test-profile.json").toFile();
        String json = """
                {
                  "employee": {
                    "id": "4000741400013306668",
                    "name": "John Doe"
                  }
                }
                """;
        Files.writeString(jsonFile.toPath(), json);

        EmployeeProfile profile = createProfile("4000741400013306668", "John Doe");

        when(parser.parseProfilesFromResource(anyString())).thenReturn(List.of(profile));
        when(processor.processProfile(any(EmployeeProfile.class), any()))
                .thenReturn(ProcessingResult.success("4000741400013306668", "John Doe", 0, 0, List.of()));

        IngestionResult result = service.ingestFromFile(jsonFile.getAbsolutePath());

        assertEquals(1, result.totalProfiles());
        assertEquals(1, result.successCount());
    }

    @Test
    void testIngestFromFile_FileNotFound_ShouldThrowException() throws Exception {
        // The parser will throw IOException when file is not found
        when(parser.parseProfilesFromResource(anyString()))
                .thenThrow(new IOException("Resource not found: /nonexistent/file.json"));

        assertThrows(IOException.class, () -> {
            service.ingestFromFile("/nonexistent/file.json");
        });
    }

    @Test
    @org.junit.jupiter.api.Disabled("Complex file system test - covered in integration tests")
    void testIngestFromDirectory_MultipleFiles_ShouldProcessAll() throws Exception {
        File file1 = tempDir.resolve("profile1.json").toFile();
        File file2 = tempDir.resolve("profile2.json").toFile();
        Files.writeString(file1.toPath(), "{}");
        Files.writeString(file2.toPath(), "{}");

        EmployeeProfile profile1 = createProfile("4000741400013306668", "John Doe");
        EmployeeProfile profile2 = createProfile("5000741400013306669", "Jane Smith");

        // Mock parseProfilesFromResource to handle file paths
        when(parser.parseProfilesFromResource(anyString()))
                .thenAnswer(invocation -> {
                    String path = invocation.getArgument(0);
                    if (path.contains("profile1.json")) {
                        return List.of(profile1);
                    } else if (path.contains("profile2.json")) {
                        return List.of(profile2);
                    }
                    return List.of();
                });
        when(processor.processProfile(any(EmployeeProfile.class), any()))
                .thenReturn(ProcessingResult.success("4000741400013306668", "John Doe", 0, 0, List.of()))
                .thenReturn(ProcessingResult.success("5000741400013306669", "Jane Smith", 0, 0, List.of()));

        IngestionResult result = service.ingestFromDirectory(tempDir.toString());

        // Should process both files
        assertTrue(result.totalProfiles() >= 2);
    }

    private EmployeeProfile createProfile(String id, String name) {
        return new EmployeeProfile(
                new com.berdachuk.expertmatch.ingestion.model.EmployeeData(id, name, null, null, null, null),
                null,
                null
        );
    }
}

