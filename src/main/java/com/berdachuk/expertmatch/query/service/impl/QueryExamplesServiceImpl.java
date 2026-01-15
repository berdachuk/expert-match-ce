package com.berdachuk.expertmatch.query.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing example queries for the ExpertMatch system.
 * Helps new users understand how to formulate queries.
 * Examples are loaded from query-examples.json resource file.
 */
@Slf4j
@Service
public class QueryExamplesServiceImpl implements QueryExamplesService {

    private static final String EXAMPLES_RESOURCE = "query-examples.json";
    private final ObjectMapper objectMapper;
    private List<QueryExample> cachedExamples;

    public QueryExamplesServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Get list of example queries organized by categories.
     * Examples are loaded from query-examples.json resource file and cached.
     *
     * @return List of query examples with categories and descriptions
     */
    @Override
    public List<QueryExample> getExamples() {
        if (cachedExamples != null) {
            return cachedExamples;
        }

        try {
            cachedExamples = loadExamplesFromResource();
            log.debug("Loaded {} query examples from resource file", cachedExamples.size());
            return cachedExamples;
        } catch (Exception e) {
            log.error("Failed to load query examples from resource file: {}", EXAMPLES_RESOURCE, e);
            // Return empty list on error to prevent service failure
            cachedExamples = new ArrayList<>();
            return cachedExamples;
        }
    }

    /**
     * Loads examples from the JSON resource file.
     *
     * @return List of query examples
     * @throws IOException if the resource file cannot be read or parsed
     */
    private List<QueryExample> loadExamplesFromResource() throws IOException {
        ClassPathResource resource = new ClassPathResource(EXAMPLES_RESOURCE);

        if (!resource.exists()) {
            throw new IOException("Resource file not found: " + EXAMPLES_RESOURCE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            QueryExamplesData data = objectMapper.readValue(inputStream, QueryExamplesData.class);
            return data.examples();
        }
    }


    /**
     * Data model for JSON resource file structure.
     */
    private record QueryExamplesData(
            List<QueryExample> examples
    ) {
    }
}

