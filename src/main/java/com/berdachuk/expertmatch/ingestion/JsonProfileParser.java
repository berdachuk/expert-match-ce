package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Service for parsing JSON content into EmployeeProfile objects.
 * Supports both array format and single object format for backward compatibility.
 */
@Slf4j
@Component
public class JsonProfileParser {

    private final ObjectMapper objectMapper;

    public JsonProfileParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses JSON content (string) into list of EmployeeProfile objects.
     * Supports both array format and single object format.
     *
     * @param jsonContent JSON content as string
     * @return List of EmployeeProfile objects
     * @throws JsonProcessingException  if JSON is invalid
     * @throws IllegalArgumentException if jsonContent is null or empty
     */
    public List<EmployeeProfile> parseProfiles(String jsonContent) throws JsonProcessingException {
        if (jsonContent == null || jsonContent.isBlank()) {
            throw new IllegalArgumentException("JSON content cannot be null or empty");
        }

        JsonNode rootNode = objectMapper.readTree(jsonContent);

        if (rootNode.isArray()) {
            // Array format: [ {employee: {...}, ...}, ... ]
            return objectMapper.readValue(
                    jsonContent,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, EmployeeProfile.class
                    )
            );
        } else if (rootNode.isObject()) {
            // Single object format: {employee: {...}, ...}
            EmployeeProfile profile = objectMapper.treeToValue(rootNode, EmployeeProfile.class);
            return List.of(profile);
        } else {
            throw new IllegalArgumentException("JSON must be an object or array");
        }
    }

    /**
     * Parses JSON content (InputStream) into list of EmployeeProfile objects.
     * Supports both array format and single object format.
     *
     * @param inputStream InputStream containing JSON content
     * @return List of EmployeeProfile objects
     * @throws JsonProcessingException if JSON is invalid
     * @throws IOException             if input stream cannot be read
     */
    public List<EmployeeProfile> parseProfiles(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        // Read all content from stream first
        String jsonContent = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return parseProfiles(jsonContent);
    }

    /**
     * Parses JSON content from a resource (classpath or file system).
     *
     * @param resourcePath Path to resource (e.g., "classpath:data/profiles.json" or "/path/to/file.json")
     * @return List of EmployeeProfile objects
     * @throws IOException if resource cannot be read or JSON is invalid
     */
    public List<EmployeeProfile> parseProfilesFromResource(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Resource path cannot be null or empty");
        }

        Resource resource;
        if (resourcePath.startsWith("classpath:")) {
            String classpathPath = resourcePath.substring("classpath:".length());
            resource = new ClassPathResource(classpathPath);
        } else {
            resource = new org.springframework.core.io.FileSystemResource(resourcePath);
        }

        if (!resource.exists()) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return parseProfiles(inputStream);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON from resource: {}", resourcePath, e);
            throw new IOException("Invalid JSON in resource: " + resourcePath, e);
        }
    }
}

