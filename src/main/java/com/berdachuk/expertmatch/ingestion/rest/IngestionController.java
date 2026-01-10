package com.berdachuk.expertmatch.ingestion.rest;

import com.berdachuk.expertmatch.api.IngestionApi;
import com.berdachuk.expertmatch.ingestion.service.JsonProfileIngestionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

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

    public IngestionController(JsonProfileIngestionService jsonProfileIngestionService) {
        this.jsonProfileIngestionService = jsonProfileIngestionService;
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

}
