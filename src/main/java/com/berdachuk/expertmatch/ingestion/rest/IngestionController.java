package com.berdachuk.expertmatch.ingestion.rest;

import com.berdachuk.expertmatch.api.IngestionApi;
import com.berdachuk.expertmatch.api.model.*;
import com.berdachuk.expertmatch.graph.service.GraphBuilderService;
import com.berdachuk.expertmatch.ingestion.service.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for JSON profile ingestion operations.
 * Implements generated API interface from OpenAPI specification.
 * Authorization is handled by Spring Gateway, which validates user roles
 * and populates X-User-Roles header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class IngestionController implements IngestionApi {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final JsonProfileIngestionService jsonProfileIngestionService;
    private final Optional<ExternalDatabaseConnectionService> connectionService;
    private final Optional<DatabaseIngestionService> databaseIngestionService;
    private final TestDataGenerator testDataGenerator;
    private final GraphBuilderService graphBuilderService;
    private final DataGenerationProgressService progressService;
    private final IngestionAsyncService ingestionAsyncService;
    private final Environment environment;

    public IngestionController(
            JsonProfileIngestionService jsonProfileIngestionService,
            Optional<ExternalDatabaseConnectionService> connectionService,
            Optional<DatabaseIngestionService> databaseIngestionService,
            TestDataGenerator testDataGenerator,
            GraphBuilderService graphBuilderService,
            DataGenerationProgressService progressService,
            IngestionAsyncService ingestionAsyncService,
            Environment environment) {
        this.jsonProfileIngestionService = jsonProfileIngestionService;
        this.connectionService = connectionService;
        this.databaseIngestionService = databaseIngestionService;
        this.testDataGenerator = testDataGenerator;
        this.graphBuilderService = graphBuilderService;
        this.progressService = progressService;
        this.ingestionAsyncService = ingestionAsyncService;
        this.environment = environment;
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

    private static String maskPasswordInUrl(String url) {
        if (url == null || !url.contains("?")) {
            return url;
        }
        return url.replaceAll("([?&]password=)[^&]*", "$1****");
    }

    /**
     * Returns target (primary) and external database settings for verification.
     * Target is the same DB for test data generator and ingest (ProfileProcessor).
     * Passwords are not included.
     */
    @GetMapping("/ingestion/database/settings")
    public ResponseEntity<Map<String, Object>> getDatabaseSettings() {
        Map<String, Object> target = new HashMap<>();
        target.put("url", maskPasswordInUrl(environment.getProperty("spring.datasource.url", "not configured")));
        target.put("username", environment.getProperty("spring.datasource.username", "not configured"));
        target.put("role", "primary (writes: employees, work_experience, graph)");
        target.put("usedBy", List.of(
                "test-data-generator (clearTestData, generateEmbeddings, generateTestData)",
                "ingest (ProfileProcessor: employees, work_experience, projects)",
                "test-data-stats (GET /api/v1/test-data/stats)"));

        Map<String, Object> body = new HashMap<>();
        body.put("target", target);

        if (connectionService.isPresent()) {
            String info = connectionService.get().getConnectionInfo();
            Map<String, Object> external = new HashMap<>();
            external.put("connectionInfo", info);
            external.put("host", environment.getProperty("expertmatch.ingestion.external-database.host", "not set"));
            external.put("port", environment.getProperty("expertmatch.ingestion.external-database.port", "5432"));
            external.put("database", environment.getProperty("expertmatch.ingestion.external-database.database", "not set"));
            external.put("schema", environment.getProperty("expertmatch.ingestion.external-database.schema", "work_experience"));
            external.put("username", environment.getProperty("expertmatch.ingestion.external-database.username", "not set"));
            external.put("role", "read-only (ingestion source: work_experience_json)");
            body.put("external", external);
        } else {
            body.put("external", Map.of("enabled", false, "message", "External database ingestion is disabled"));
        }
        return ResponseEntity.ok(body);
    }

    /**
     * Ingests work experience data from external database.
     *
     * @param fromOffset starting message offset (optional, defaults to 0)
     * @param batchSize  batch size for processing (optional, defaults to 1000)
     * @return ingestion result
     */
    @PostMapping("/ingestion/database")
    public ResponseEntity<com.berdachuk.expertmatch.api.model.IngestionResult> ingestFromDatabase(
            @Parameter(name = "fromOffset", description = "Starting message offset. If not specified, starts from beginning.", in = ParameterIn.QUERY)
            @RequestParam(value = "fromOffset", required = false) Long fromOffset,
            @Parameter(name = "batchSize", description = "Batch size for processing records. Defaults to 1000.", in = ParameterIn.QUERY)
            @RequestParam(value = "batchSize", required = false, defaultValue = "1000") Integer batchSize) {
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

    /**
     * Starts async ingestion from external database, then generates embeddings and builds graph.
     *
     * @param batchSize batch size for ingestion (optional, default 1000)
     * @param clear     whether to clear existing data first (optional, default false)
     * @return job ID for progress polling
     */
    @Override
    @PostMapping("/ingestion/database/async")
    public ResponseEntity<IngestionAsyncJobResponse> startIngestionFromDatabaseAsync(
            @Parameter(name = "batchSize", description = "Batch size for processing records. Defaults to 1000.", in = ParameterIn.QUERY)
            @RequestParam(value = "batchSize", required = false, defaultValue = "1000") Integer batchSize,
            @Parameter(name = "clear", description = "Clear existing data before ingestion.", in = ParameterIn.QUERY)
            @RequestParam(value = "clear", required = false, defaultValue = "false") Boolean clear) {
        if (databaseIngestionService.isEmpty()) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "External database ingestion is not enabled");
        }
        int batch = batchSize != null && batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        boolean clearExisting = Boolean.TRUE.equals(clear);

        log.info("POST /api/v1/ingestion/database/async - batchSize: {}, clear: {}", batch, clearExisting);
        String jobId = UUID.randomUUID().toString();
        progressService.createProgress(jobId);
        DataGenerationProgress progress = progressService.getProgress(jobId);
        progress.addTraceEntry("INFO", "Start", "Ingestion from external database started (batchSize: " + batch + ", clear: " + clearExisting + ")", 0);

        ingestionAsyncService.runIngestionAsync(jobId, batch, clearExisting);

        IngestionAsyncJobResponse response = new IngestionAsyncJobResponse()
                .jobId(jobId)
                .status("started")
                .message("Ingestion from external database started")
                .batchSize(batch)
                .cleared(clearExisting);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns list of ingestion jobs with status (newest first).
     *
     * @return list of job summaries
     */
    @Override
    @GetMapping("/ingestion/jobs")
    public ResponseEntity<IngestionJobListResponse> listIngestionJobs() {
        List<DataGenerationProgress> list = progressService.listJobs();
        List<IngestionJobSummary> jobs = list.stream()
                .map(p -> {
                    IngestionJobSummary summary = new IngestionJobSummary();
                    summary.setJobId(p.getJobId());
                    if (p.getStatus() != null) {
                        try {
                            summary.setStatus(IngestionJobSummary.StatusEnum.fromValue(p.getStatus()));
                        } catch (IllegalArgumentException e) {
                            summary.setStatus(null);
                        }
                    }
                    int jobProgress = p.getProgress();
                    if ("running".equals(p.getStatus()) && jobProgress >= 100) {
                        jobProgress = 99;
                    }
                    summary.setProgress(jobProgress);
                    summary.setCurrentStep(p.getCurrentStep());
                    summary.setMessage(p.getMessage());
                    summary.setStartTime(p.getStartTime() != null ? p.getStartTime().atOffset(ZoneOffset.UTC) : null);
                    if (p.getEndTime() != null) {
                        summary.setEndTime(JsonNullable.of(p.getEndTime().atOffset(ZoneOffset.UTC)));
                    }
                    if (p.getErrorMessage() != null) {
                        summary.setErrorMessage(JsonNullable.of(p.getErrorMessage()));
                    }
                    return summary;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(new IngestionJobListResponse(jobs));
    }

    /**
     * Returns progress for an ingestion job.
     *
     * @param jobId job identifier returned from POST /ingestion/database/async
     * @return progress status, percentage, and trace entries
     */
    @Override
    @GetMapping("/ingestion/progress/{jobId}")
    public ResponseEntity<ProgressResponse> getIngestionProgress(@PathVariable("jobId") UUID jobId) {
        String jobIdStr = jobId != null ? jobId.toString() : null;
        DataGenerationProgress progress = progressService.getProgress(jobIdStr);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        ProgressResponse.StatusEnum statusEnum = null;
        if (progress.getStatus() != null) {
            try {
                statusEnum = ProgressResponse.StatusEnum.fromValue(progress.getStatus());
            } catch (IllegalArgumentException e) {
                log.debug("Unknown progress status '{}' for job {}, returning null", progress.getStatus(), jobIdStr);
            }
        }
        int reportedProgress = progress.getProgress();
        if (statusEnum == ProgressResponse.StatusEnum.RUNNING && reportedProgress >= 100) {
            reportedProgress = 99;
        }
        ProgressResponse response = new ProgressResponse()
                .jobId(progress.getJobId())
                .status(statusEnum)
                .progress(reportedProgress)
                .currentStep(progress.getCurrentStep())
                .message(progress.getMessage())
                .startTime(progress.getStartTime() != null ? progress.getStartTime().atOffset(ZoneOffset.UTC) : null)
                .endTime(progress.getEndTime() != null ? progress.getEndTime().atOffset(ZoneOffset.UTC) : null)
                .errorMessage(progress.getErrorMessage());
        if (progress.getTraceEntries() != null) {
            List<TraceEntry> trace = new ArrayList<>();
            for (DataGenerationProgress.TraceEntry e : progress.getTraceEntries()) {
                TraceEntry.LevelEnum levelEnum = null;
                if (e.getLevel() != null) {
                    try {
                        levelEnum = TraceEntry.LevelEnum.fromValue(e.getLevel());
                    } catch (IllegalArgumentException ex) {
                        log.debug("Unknown trace level '{}', skipping", e.getLevel());
                    }
                }
                trace.add(new TraceEntry()
                        .timestamp(e.getTimestamp() != null ? e.getTimestamp().atOffset(ZoneOffset.UTC) : null)
                        .level(levelEnum)
                        .step(e.getStep())
                        .message(e.getMessage())
                        .progress(e.getProgress()));
            }
            response.setTraceEntries(trace);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a running ingestion job.
     *
     * @param jobId job identifier
     * @return success or failure
     */
    @Override
    @PostMapping("/ingestion/cancel/{jobId}")
    public ResponseEntity<SuccessResponse> cancelIngestionJob(@PathVariable("jobId") UUID jobId) {
        String jobIdStr = jobId != null ? jobId.toString() : null;
        log.info("POST /api/v1/ingestion/cancel/{}", jobIdStr);
        boolean cancelled = progressService.cancelJob(jobIdStr);
        if (!cancelled) {
            DataGenerationProgress progress = progressService.getProgress(jobIdStr);
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(new SuccessResponse()
                    .success(false)
                    .message("Job cannot be cancelled. Current status: " + progress.getStatus()));
        }
        return ResponseEntity.ok(new SuccessResponse()
                .success(true)
                .message("Ingestion job cancelled successfully"));
    }

}
