package com.berdachuk.expertmatch.ingestion.service.impl;

import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.IngestionResult;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.berdachuk.expertmatch.ingestion.service.JsonProfileIngestionService;
import com.berdachuk.expertmatch.ingestion.service.JsonProfileParser;
import com.berdachuk.expertmatch.ingestion.service.ProfileProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for ingesting expert profiles from JSON files.
 * Supports single files, directories, and content strings.
 * Handles both array format and single object format.
 */
@Slf4j
@Service
public class JsonProfileIngestionServiceImpl implements JsonProfileIngestionService {

    private final JsonProfileParser parser;
    private final ProfileProcessor processor;
    private final ObjectMapper objectMapper;

    public JsonProfileIngestionServiceImpl(
            JsonProfileParser parser,
            ProfileProcessor processor,
            ObjectMapper objectMapper) {
        this.parser = parser;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingests expert profiles from JSON content (string).
     *
     * @param jsonContent JSON content as string
     * @param sourceName  Name of the source for logging
     * @return IngestionResult with success/error counts and details
     */
    @Override
    public IngestionResult ingestFromContent(String jsonContent, String sourceName) {
        try {
            List<EmployeeProfile> profiles = parser.parseProfiles(jsonContent);
            return processProfiles(profiles, sourceName);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON content from source '{}': {}", sourceName, e.getMessage(), e);
            return IngestionResult.of(0, 0, 0, List.of(), sourceName);
        }
    }

    /**
     * Ingests expert profiles from a JSON file.
     * Supports both classpath resources and file system paths.
     *
     * @param resourcePath Path to JSON resource (classpath:data/profiles.json or /path/to/file.json)
     * @return IngestionResult with success/error counts and details
     * @throws IOException if resource cannot be read
     */
    @Override
    public IngestionResult ingestFromFile(String resourcePath) throws IOException {
        try {
            List<EmployeeProfile> profiles = parser.parseProfilesFromResource(resourcePath);
            return processProfiles(profiles, resourcePath);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON from file '{}': {}", resourcePath, e.getMessage(), e);
            throw new IOException("Invalid JSON in file: " + resourcePath, e);
        }
    }

    /**
     * Ingests expert profiles from multiple JSON files in a directory.
     * Supports both classpath directories and file system directories.
     *
     * @param directoryPath Path to directory (classpath:data or /path/to/directory)
     * @return IngestionResult aggregated from all files
     */
    @Override
    public IngestionResult ingestFromDirectory(String directoryPath) {
        List<IngestionResult> fileResults = new ArrayList<>();
        List<Resource> resources;

        try {
            if (directoryPath.startsWith("classpath:")) {
                // Classpath directory
                String classpathPath = directoryPath.substring("classpath:".length());
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                String pattern = classpathPath.endsWith("/")
                        ? "classpath:" + classpathPath + "**/*.json"
                        : "classpath:" + classpathPath + "/**/*.json";
                resources = List.of(resolver.getResources(pattern));
            } else {
                // File system directory
                Path dir = Paths.get(directoryPath);
                if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                    log.warn("Directory does not exist or is not a directory: {}", directoryPath);
                    return IngestionResult.of(0, 0, 0, List.of(), directoryPath);
                }
                resources = Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .map(p -> new org.springframework.core.io.FileSystemResource(p.toFile()))
                        .map(Resource.class::cast)
                        .toList();
            }

            for (Resource resource : resources) {
                try {
                    String resourcePath;
                    if (resource instanceof org.springframework.core.io.FileSystemResource) {
                        resourcePath = ((org.springframework.core.io.FileSystemResource) resource).getFile().getAbsolutePath();
                    } else {
                        resourcePath = resource.getURI().toString();
                        if (resourcePath.startsWith("file:")) {
                            resourcePath = resourcePath.substring(5); // Remove "file:" prefix
                        }
                    }
                    IngestionResult result = ingestFromFile(resourcePath);
                    fileResults.add(result);
                } catch (Exception e) {
                    log.warn("Failed to process file '{}': {}", resource.getFilename(), e.getMessage());
                    // Continue with other files
                }
            }

            // Aggregate results
            return aggregateResults(fileResults, directoryPath);

        } catch (IOException e) {
            log.error("Failed to read directory '{}': {}", directoryPath, e.getMessage(), e);
            return IngestionResult.of(0, 0, 0, List.of(), directoryPath);
        }
    }

    /**
     * Processes a list of profiles and returns ingestion result.
     */
    private IngestionResult processProfiles(List<EmployeeProfile> profiles, String sourceName) {
        List<ProcessingResult> results = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        Map<String, String> existingProjects = new HashMap<>(); // Shared across profiles

        for (EmployeeProfile profile : profiles) {
            try {
                ProcessingResult result = processor.processProfile(profile, existingProjects);
                results.add(result);
                if (result.success()) {
                    successCount++;
                } else {
                    errorCount++;
                }
            } catch (Exception e) {
                log.error("Unexpected error processing profile: {}", e.getMessage(), e);
                String employeeId = profile.employee() != null ? profile.employee().id() : "unknown";
                String employeeName = profile.employee() != null ? profile.employee().name() : "unknown";
                results.add(ProcessingResult.failure(employeeId, employeeName,
                        "Unexpected error: " + e.getMessage()));
                errorCount++;
            }
        }

        return IngestionResult.of(profiles.size(), successCount, errorCount, results, sourceName);
    }

    /**
     * Aggregates results from multiple files.
     */
    private IngestionResult aggregateResults(List<IngestionResult> fileResults, String sourceName) {
        int totalProfiles = fileResults.stream().mapToInt(IngestionResult::totalProfiles).sum();
        int successCount = fileResults.stream().mapToInt(IngestionResult::successCount).sum();
        int errorCount = fileResults.stream().mapToInt(IngestionResult::errorCount).sum();
        List<ProcessingResult> allResults = fileResults.stream()
                .flatMap(r -> r.results().stream())
                .toList();

        return IngestionResult.of(totalProfiles, successCount, errorCount, allResults, sourceName);
    }
}

