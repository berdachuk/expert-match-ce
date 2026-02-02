package com.berdachuk.expertmatch.ingestion.rest;

import com.berdachuk.expertmatch.api.TestDataApi;
import com.berdachuk.expertmatch.api.model.*;
import com.berdachuk.expertmatch.graph.service.GraphBuilderService;
import com.berdachuk.expertmatch.ingestion.service.DataGenerationProgress;
import com.berdachuk.expertmatch.ingestion.service.DataGenerationProgressService;
import com.berdachuk.expertmatch.ingestion.service.TestDataGenerator;
import com.berdachuk.expertmatch.ingestion.service.TestDataStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for test data generation operations.
 * Implements generated API interface from OpenAPI specification (API-first).
 * Authorization is handled by Spring Gateway, which validates user roles
 * and populates X-User-Roles header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class TestDataController implements TestDataApi {

    private static final String[] VALID_SIZES = {"tiny", "small", "medium", "large", "huge"};

    private final TestDataGenerator testDataGenerator;
    private final GraphBuilderService graphBuilderService;
    private final DataGenerationProgressService progressService;
    private final TestDataStatisticsService testDataStatisticsService;

    public TestDataController(
            TestDataGenerator testDataGenerator,
            GraphBuilderService graphBuilderService,
            DataGenerationProgressService progressService,
            TestDataStatisticsService testDataStatisticsService) {
        this.testDataGenerator = testDataGenerator;
        this.graphBuilderService = graphBuilderService;
        this.progressService = progressService;
        this.testDataStatisticsService = testDataStatisticsService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<TestDataStatsResponse> getTestDataStats() {
        log.info("GET /api/v1/test-data/stats");
        TestDataStatsResponse stats = testDataStatisticsService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Generate test data with optional clear flag.
     */
    @Override
    public ResponseEntity<TestDataSizeResponse> generateTestData(String size, Boolean clear) {
        String sizeParam = size != null ? size : "small";
        boolean clearExisting = clear != null && clear;

        // Validate size parameter
        String[] validSizes = {"tiny", "small", "medium", "large", "huge"};
        boolean isValidSize = false;
        for (String validSize : validSizes) {
            if (validSize.equals(sizeParam)) {
                isValidSize = true;
                break;
            }
        }
        if (!isValidSize) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "Invalid size parameter. Must be one of: 'tiny', 'small', 'medium', 'large', 'huge'. Got: " + sizeParam
            );
        }

        testDataGenerator.generateTestData(sizeParam, clearExisting);
        TestDataSizeResponse response = new TestDataSizeResponse()
                .success(true)
                .message("Test data generated successfully")
                .size(TestDataSizeResponse.SizeEnum.fromValue(sizeParam));
        return ResponseEntity.ok(response);
    }

    /**
     * Generate embeddings for work experience records.
     */
    @Override
    public ResponseEntity<SuccessResponse> generateEmbeddings() {
        testDataGenerator.generateEmbeddings();
        SuccessResponse response = new SuccessResponse()
                .success(true)
                .message("Embeddings generated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Build graph relationships from database data.
     */
    @Override
    public ResponseEntity<SuccessResponse> buildGraph() {
        graphBuilderService.buildGraph();
        SuccessResponse response = new SuccessResponse()
                .success(true)
                .message("Graph relationships built successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate complete dataset: data + embeddings + graph.
     */
    @Override
    public ResponseEntity<TestDataSizeResponse> generateCompleteDataset(String size, Boolean clear) {
        String sizeParam = size != null ? size : "small";
        boolean clearExisting = clear != null && clear;

        // Validate size parameter
        String[] validSizes = {"tiny", "small", "medium", "large", "huge"};
        boolean isValidSize = false;
        for (String validSize : validSizes) {
            if (validSize.equals(sizeParam)) {
                isValidSize = true;
                break;
            }
        }
        if (!isValidSize) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "Invalid size parameter. Must be one of: 'tiny', 'small', 'medium', 'large', 'huge'. Got: " + sizeParam
            );
        }

        testDataGenerator.generateTestData(sizeParam, clearExisting);
        testDataGenerator.generateEmbeddings();
        graphBuilderService.buildGraph();
        TestDataSizeResponse response = new TestDataSizeResponse()
                .success(true)
                .message("Complete dataset generated successfully")
                .size(TestDataSizeResponse.SizeEnum.fromValue(sizeParam));
        return ResponseEntity.ok(response);
    }

    /**
     * Generate banking domain subset with default parameters (10 employees, 15 projects, 2-3 work experiences per employee).
     * This creates a small focused dataset for testing banking domain queries.
     */
    public ResponseEntity<SuccessResponse> generateBankingDomainSubset() {
        testDataGenerator.generateBankingDomainSubset(10, 2, 15);
        SuccessResponse response = new SuccessResponse()
                .success(true)
                .message("Banking domain subset generated successfully: 10 employees, 15 projects, ~20-30 work experiences");
        return ResponseEntity.ok(response);
    }

    /**
     * Generate healthcare domain subset with default parameters (10 employees, 15 projects, 2-3 work experiences per employee).
     * This creates a small focused dataset for testing healthcare domain queries.
     */
    public ResponseEntity<SuccessResponse> generateHealthcareDomainSubset() {
        testDataGenerator.generateHealthcareDomainSubset(10, 2, 15);
        SuccessResponse response = new SuccessResponse()
                .success(true)
                .message("Healthcare domain subset generated successfully: 10 employees, 15 projects, ~20-30 work experiences");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<TestDataSizeOption>> getTestDataSizes() {
        log.info("GET /api/v1/test-data/sizes");
        List<TestDataSizeOption> sizes = new ArrayList<>();
        sizes.add(new TestDataSizeOption().size(TestDataSizeOption.SizeEnum.TINY).description("5 employees, 5 projects, ~15 work experiences").estimatedTimeMinutes(1));
        sizes.add(new TestDataSizeOption().size(TestDataSizeOption.SizeEnum.SMALL).description("50 employees, 100 projects, ~250 work experiences").estimatedTimeMinutes(2));
        sizes.add(new TestDataSizeOption().size(TestDataSizeOption.SizeEnum.MEDIUM).description("500 employees, 1,000 projects, ~4,000 work experiences").estimatedTimeMinutes(10));
        sizes.add(new TestDataSizeOption().size(TestDataSizeOption.SizeEnum.LARGE).description("2,000 employees, 4,000 projects, ~20,000 work experiences").estimatedTimeMinutes(40));
        sizes.add(new TestDataSizeOption().size(TestDataSizeOption.SizeEnum.HUGE).description("50,000 employees, 100,000 projects, ~750,000 work experiences").estimatedTimeMinutes(120));
        return ResponseEntity.ok(sizes);
    }

    @Override
    public ResponseEntity<AsyncJobResponse> generateCompleteDatasetAsync(String size, Boolean clear) {
        log.info("POST /api/v1/test-data/complete/async - size: {}, clear: {}", size, clear);
        String sizeParam = size != null ? size : "small";
        boolean clearExisting = clear != null && clear;
        if (!isValidSize(sizeParam)) {
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "Invalid size. Must be one of: tiny, small, medium, large, huge. Got: " + sizeParam);
        }
        String jobId = UUID.randomUUID().toString();
        DataGenerationProgress progress = progressService.createProgress(jobId);
        progress.addTraceEntry("INFO", "Start", "Test data generation started (size: " + sizeParam + ", clear: " + clearExisting + ")", 0);

        CompletableFuture.runAsync(() -> {
            try {
                if (progress.isCancelled()) return;
                progress.updateProgress(5, "Clear", clearExisting ? "Clearing existing test data..." : "Skipping clear (append mode)");
                if (clearExisting) {
                    testDataGenerator.clearTestData();
                }
                if (progress.isCancelled()) return;
                progress.updateProgress(20, "Data", "Generating test data (employees, projects, work experiences)...");
                testDataGenerator.generateTestData(sizeParam, false);
                if (progress.isCancelled()) return;
                progress.updateProgress(60, "Embeddings", "Generating vector embeddings for work experiences...");
                testDataGenerator.generateEmbeddings();
                if (progress.isCancelled()) return;
                progress.updateProgress(85, "Graph", "Building graph relationships in Apache AGE...");
                graphBuilderService.buildGraph();
                if (progress.isCancelled()) return;
                progress.complete();
            } catch (Exception e) {
                log.error("Test data generation failed", e);
                progress.error(e.getMessage());
            }
        });

        AsyncJobResponse response = new AsyncJobResponse()
                .status("started")
                .message("Test data generation started")
                .jobId(jobId)
                .size(AsyncJobResponse.SizeEnum.fromValue(sizeParam))
                .cleared(clearExisting);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProgressResponse> getTestDataProgress(String jobId) {
        DataGenerationProgress progress = progressService.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        ProgressResponse response = new ProgressResponse()
                .jobId(progress.getJobId())
                .status(progress.getStatus() != null ? ProgressResponse.StatusEnum.fromValue(progress.getStatus()) : null)
                .progress(progress.getProgress())
                .currentStep(progress.getCurrentStep())
                .message(progress.getMessage())
                .startTime(progress.getStartTime() != null ? progress.getStartTime().atOffset(ZoneOffset.UTC) : null)
                .endTime(progress.getEndTime() != null ? progress.getEndTime().atOffset(ZoneOffset.UTC) : null)
                .errorMessage(progress.getErrorMessage());
        if (progress.getTraceEntries() != null) {
            List<TraceEntry> trace = new ArrayList<>();
            for (DataGenerationProgress.TraceEntry e : progress.getTraceEntries()) {
                TraceEntry te = new TraceEntry()
                        .timestamp(e.getTimestamp() != null ? e.getTimestamp().atOffset(ZoneOffset.UTC) : null)
                        .level(e.getLevel() != null ? TraceEntry.LevelEnum.fromValue(e.getLevel()) : null)
                        .step(e.getStep())
                        .message(e.getMessage())
                        .progress(e.getProgress());
                trace.add(te);
            }
            response.setTraceEntries(trace);
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SuccessResponse> cancelTestDataJob(String jobId) {
        log.info("POST /api/v1/test-data/cancel/{}", jobId);
        boolean cancelled = progressService.cancelJob(jobId);
        if (!cancelled) {
            DataGenerationProgress progress = progressService.getProgress(jobId);
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(new SuccessResponse()
                    .success(false)
                    .message("Job cannot be cancelled. Current status: " + progress.getStatus()));
        }
        return ResponseEntity.ok(new SuccessResponse()
                .success(true)
                .message("Generation job cancelled successfully"));
    }

    @Override
    public ResponseEntity<SuccessResponse> clearTestData() {
        log.info("POST /api/v1/test-data/clear");
        try {
            graphBuilderService.clearGraph();
            testDataGenerator.clearTestData();
            return ResponseEntity.ok(new SuccessResponse()
                    .success(true)
                    .message("Test data and graph cleared successfully"));
        } catch (Exception e) {
            log.error("Failed to clear test data", e);
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(
                    "Failed to clear test data: " + e.getMessage());
        }
    }

    private static boolean isValidSize(String size) {
        for (String valid : VALID_SIZES) {
            if (valid.equals(size)) return true;
        }
        return false;
    }
}

