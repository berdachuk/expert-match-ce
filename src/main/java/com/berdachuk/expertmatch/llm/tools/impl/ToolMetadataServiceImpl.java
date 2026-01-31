package com.berdachuk.expertmatch.llm.tools.impl;

import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import com.berdachuk.expertmatch.llm.tools.ToolMetadataService;
import com.berdachuk.expertmatch.llm.tools.repository.ToolMetadataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for indexing tool metadata with embeddings for semantic search.
 * Uses existing PgVector infrastructure to store and search tool definitions.
 */
@Slf4j
@Service
public class ToolMetadataServiceImpl implements ToolMetadataService {
    private static final int DATABASE_EMBEDDING_DIMENSION = 1536;

    private final ToolMetadataRepository repository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public ToolMetadataServiceImpl(
            ToolMetadataRepository repository,
            EmbeddingService embeddingService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Indexes all tools from a component class.
     * Scans for @Tool annotated methods and stores their metadata with embeddings.
     */
    @Override
    public void indexTools(Object toolComponent) {
        Class<?> clazz = toolComponent.getClass();
        String toolClass = clazz.getName();

        log.info("Indexing tools from class: {}", toolClass);

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                indexTool(toolComponent, method, toolAnnotation, toolClass);
            }
        }
    }

    /**
     * Indexes a single tool method.
     */
    private void indexTool(Object toolComponent, Method method, Tool toolAnnotation, String toolClass) {
        String toolName = method.getName();
        String description = toolAnnotation.description();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Tool method " + toolName + " must have a non-empty description");
        }

        // Build searchable text for embedding
        String searchableText = buildSearchableText(toolName, description, method);

        // Generate embedding
        float[] embedding = embeddingService.generateEmbeddingAsFloatArray(searchableText);
        int embeddingDimension = embedding.length;

        // Normalize to 1536 dimensions
        float[] normalizedEmbedding = normalizeEmbedding(embedding, DATABASE_EMBEDDING_DIMENSION);
        String vectorString = formatVector(normalizedEmbedding);

        // Extract parameters
        Map<String, Object> parameters = extractParameters(method);
        String parametersJson;
        try {
            parametersJson = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize parameters for tool {}: {}", toolName, e.getMessage());
            parametersJson = "{}";
        }

        // Call repository for data access
        try {
            repository.insertToolMetadata(
                    generateToolId(toolClass, toolName),
                    toolName,
                    description,
                    toolClass,
                    method.getName(),
                    parametersJson,
                    vectorString,
                    embeddingDimension,
                    java.sql.Timestamp.from(Instant.now())
            );
            log.debug("Indexed tool: {} from class {}", toolName, toolClass);
        } catch (Exception e) {
            log.error("Failed to index tool {}: {}", toolName, e.getMessage(), e);
        }
    }

    /**
     * Builds searchable text from tool metadata.
     */
    private String buildSearchableText(String toolName, String description, Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(toolName).append(" ");
        sb.append(description).append(" ");

        // Add parameter descriptions
        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            ToolParam toolParam = param.getAnnotation(ToolParam.class);
            if (toolParam != null) {
                String paramDesc = toolParam.description();
                if (paramDesc != null && !paramDesc.isBlank()) {
                    sb.append(paramDesc).append(" ");
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Extracts parameter metadata from method.
     */
    private Map<String, Object> extractParameters(Method method) {
        Map<String, Object> params = new HashMap<>();
        List<Map<String, Object>> paramList = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            Map<String, Object> paramInfo = new HashMap<>();
            paramInfo.put("name", param.getName());
            paramInfo.put("type", param.getType().getName());

            ToolParam toolParam = param.getAnnotation(ToolParam.class);
            if (toolParam != null) {
                paramInfo.put("description", toolParam.description());
            }

            paramList.add(paramInfo);
        }

        params.put("parameters", paramList);
        return params;
    }

    /**
     * Generates a unique ID for a tool.
     */
    private String generateToolId(String toolClass, String toolName) {
        return toolClass + "#" + toolName;
    }

    /**
     * Normalizes embedding to target dimension.
     */
    private float[] normalizeEmbedding(float[] embedding, int targetDimension) {
        if (embedding.length == targetDimension) {
            return embedding;
        }

        float[] normalized = new float[targetDimension];
        int copyLength = Math.min(embedding.length, targetDimension);
        System.arraycopy(embedding, 0, normalized, 0, copyLength);
        // Remaining elements are already zero (default float value)

        return normalized;
    }

    /**
     * Formats float array as PostgreSQL vector string.
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Re-indexes all tools from a component (useful for updates).
     */
    @Override
    public void reindexTools(Object toolComponent) {
        Class<?> clazz = toolComponent.getClass();
        String toolClass = clazz.getName();

        // Call repository for data access
        repository.deleteByToolClass(toolClass);

        // Re-index
        indexTools(toolComponent);
    }
}
