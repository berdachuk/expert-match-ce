package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for generating JSON Schema descriptions from Java classes.
 * Provides schema descriptions for LLM prompts.
 */
@Slf4j
@Component
public class SGRSchemaGenerator {
    private final ObjectMapper objectMapper;
    private final Map<Class<?>, String> schemaDescriptionCache = new ConcurrentHashMap<>();

    public SGRSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a JSON Schema description for the given class.
     * Results are cached for performance.
     * This generates a text description suitable for LLM prompts.
     *
     * @param clazz The class to generate schema description for
     * @return JSON Schema description as a string
     */
    public String generateSchemaDescription(Class<?> clazz) {
        return schemaDescriptionCache.computeIfAbsent(clazz, this::generateSchemaDescriptionInternal);
    }

    /**
     * Generates JSON Schema description for the given class.
     */
    private String generateSchemaDescriptionInternal(Class<?> clazz) {
        try {
            // Generate a simple JSON Schema description based on class structure
            // This is a simplified version that describes the expected JSON structure

            String schema = "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +

                    // For MVP, we'll use a simple description approach
                    // In a full implementation, we could use Jackson's introspection
                    // or a JSON Schema library to generate proper schemas

                    "    // Schema for " + clazz.getSimpleName() + "\n" +
                    "    // Use Jackson annotations to determine structure\n" +
                    "  }\n" +
                    "}\n";

            return schema;
        } catch (Exception e) {
            log.error("Failed to generate JSON Schema for class: {}", clazz.getName(), e);
            throw new RuntimeException("Failed to generate JSON Schema for class: " + clazz.getName(), e);
        }
    }

    /**
     * Clears the schema cache.
     */
    public void clearCache() {
        schemaDescriptionCache.clear();
    }
}

