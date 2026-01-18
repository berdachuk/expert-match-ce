package com.berdachuk.expertmatch.ingestion.rest;

import com.berdachuk.expertmatch.api.IngestionApi;
import com.berdachuk.expertmatch.ingestion.service.DatabaseIngestionService;
import com.berdachuk.expertmatch.ingestion.service.ExternalDatabaseConnectionService;
import com.berdachuk.expertmatch.ingestion.service.JsonProfileIngestionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for JSON profile ingestion operations.
 * Implements generated API interface from OpenAPI specification.
 * Authorization is handled by Spring Gateway, which validates user roles
 * and populates X-User-Roles header.
 */
@RestController
@RequestMapping("/api/v1")
public class IngestionController implements IngestionApi {

    private final JsonProfileIngestionService jsonProfileIngestionService;
    private final Optional<ExternalDatabaseConnectionService> connectionService;
    private final Optional<DatabaseIngestionService> databaseIngestionService;

    public IngestionController(
            JsonProfileIngestionService jsonProfileIngestionService,
            Optional<ExternalDatabaseConnectionService> connectionService,
            Optional<DatabaseIngestionService> databaseIngestionService) {
        this.jsonProfileIngestionService = jsonProfileIngestionService;
        this.connectionService = connectionService;
        this.databaseIngestionService = databaseIngestionService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * Ingests expert profiles from JSON files.
     * Supports both directory and single file ingestion.
     */
    @Override
    public ResponseEntity<com.berdachuk.expertmatch.api.model.IngestionResult> ingestJsonProfiles(
            @Parameter(name = "directory", description = "Directory path containing JSON profile files. Supports classpath (e.g., \"classpath:data\") or file system paths. If not specified, defaults to \"classpath:data\".", in = ParameterIn.QUERY)
            @RequestParam(value = "directory", required = false) String directory,
            @Parameter(name = "file", description = "Single JSON file path to ingest. Supports classpath (e.g., \"classpath:data/profile.json\") or file system paths. If specified, directory parameter is ignored.", in = ParameterIn.QUERY)
            @RequestParam(value = "file", required = false) String file) {
        try {
            com.berdachuk.expertmatch.ingestion.model.IngestionResult result;

            if (file != null && !file.isBlank()) {
                // Ingest from single file
                result = jsonProfileIngestionService.ingestFromFile(file);
            } else if (directory != null && !directory.isBlank()) {
                // Ingest from directory
                result = jsonProfileIngestionService.ingestFromDirectory(directory);
            } else {
                // Default: use classpath:data
                result = jsonProfileIngestionService.ingestFromDirectory("classpath:data");
            }

            // Convert to API model
            com.berdachuk.expertmatch.api.model.IngestionResult response =
                    new com.berdachuk.expertmatch.api.model.IngestionResult()
                            .totalProfiles(result.totalProfiles())
                            .successCount(result.successCount())
                            .errorCount(result.errorCount())
                            .sourceName(result.sourceName());

            // Convert ProcessingResult list
            if (result.results() != null) {
                result.results().forEach(pr -> {
                    com.berdachuk.expertmatch.api.model.ProcessingResult apiPr =
                            new com.berdachuk.expertmatch.api.model.ProcessingResult()
                                    .employeeId(pr.employeeId())
                                    .employeeName(pr.employeeName())
                                    .success(pr.success())
                                    .errorMessage(pr.errorMessage())
                                    .projectsProcessed(pr.projectsProcessed())
                                    .projectsSkipped(pr.projectsSkipped())
                                    .projectErrors(pr.projectErrors());
                    response.addResultsItem(apiPr);
                });
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "Failed to ingest JSON profiles: " + e.getMessage());
        }
    }

    /**
     * Verifies connection to external database.
     *
     * @return connection verification result
     */
    @GetMapping("/ingestion/database/verify")
    public ResponseEntity<Map<String, Object>> verifyDatabaseConnection() {
        if (connectionService.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("connected", false);
            response.put("error", "External database ingestion is not enabled");
            return ResponseEntity.ok(response);
        }

        ExternalDatabaseConnectionService service = connectionService.get();
        boolean connected = service.verifyConnection();
        String connectionInfo = service.getConnectionInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("connected", connected);
        response.put("connectionInfo", connectionInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * Ingests work experience data from external database.
     *
     * @param fromOffset starting message offset (optional, defaults to 0)
     * @param batchSize  batch size for processing (optional, defaults to 100)
     * @return ingestion result
     */
    @PostMapping("/ingestion/database")
    public ResponseEntity<com.berdachuk.expertmatch.api.model.IngestionResult> ingestFromDatabase(
            @Parameter(name = "fromOffset", description = "Starting message offset. If not specified, starts from beginning.", in = ParameterIn.QUERY)
            @RequestParam(value = "fromOffset", required = false) Long fromOffset,
            @Parameter(name = "batchSize", description = "Batch size for processing records. Defaults to 100.", in = ParameterIn.QUERY)
            @RequestParam(value = "batchSize", required = false, defaultValue = "100") Integer batchSize) {
        if (databaseIngestionService.isEmpty()) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "External database ingestion is not enabled");
        }

        DatabaseIngestionService service = databaseIngestionService.get();
        com.berdachuk.expertmatch.ingestion.model.IngestionResult result;

        if (fromOffset != null && fromOffset > 0) {
            result = service.ingestFromOffset(fromOffset, batchSize);
        } else {
            result = service.ingestAll(batchSize);
        }

        // Convert to API model
        com.berdachuk.expertmatch.api.model.IngestionResult response =
                new com.berdachuk.expertmatch.api.model.IngestionResult()
                        .totalProfiles(result.totalProfiles())
                        .successCount(result.successCount())
                        .errorCount(result.errorCount())
                        .sourceName(result.sourceName());

        // Convert ProcessingResult list
        if (result.results() != null) {
            result.results().forEach(pr -> {
                com.berdachuk.expertmatch.api.model.ProcessingResult apiPr =
                        new com.berdachuk.expertmatch.api.model.ProcessingResult()
                                .employeeId(pr.employeeId())
                                .employeeName(pr.employeeName())
                                .success(pr.success())
                                .errorMessage(pr.errorMessage())
                                .projectsProcessed(pr.projectsProcessed())
                                .projectsSkipped(pr.projectsSkipped())
                                .projectErrors(pr.projectErrors());
                response.addResultsItem(apiPr);
            });
        }

        return ResponseEntity.ok(response);
    }

}
