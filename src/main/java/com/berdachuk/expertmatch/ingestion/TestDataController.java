package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.api.TestDataApi;
import com.berdachuk.expertmatch.api.model.SuccessResponse;
import com.berdachuk.expertmatch.api.model.TestDataSizeResponse;
import com.berdachuk.expertmatch.graph.GraphBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

/**
 * REST controller for test data generation operations.
 * Implements generated API interface from OpenAPI specification.
 * Authorization is handled by Spring Gateway, which validates user roles
 * and populates X-User-Roles header.
 */
@RestController
@RequestMapping("/api/v1")
public class TestDataController implements TestDataApi {

    private final TestDataGenerator testDataGenerator;
    private final GraphBuilderService graphBuilderService;

    public TestDataController(
            TestDataGenerator testDataGenerator,
            GraphBuilderService graphBuilderService) {
        this.testDataGenerator = testDataGenerator;
        this.graphBuilderService = graphBuilderService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
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
            throw new com.berdachuk.expertmatch.exception.ValidationException(
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
            throw new com.berdachuk.expertmatch.exception.ValidationException(
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

}

