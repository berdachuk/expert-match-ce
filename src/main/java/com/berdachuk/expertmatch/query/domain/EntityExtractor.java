package com.berdachuk.expertmatch.query.domain;

import com.berdachuk.expertmatch.core.domain.ExecutionTrace;
import com.berdachuk.expertmatch.core.service.ExecutionTracer;
import com.berdachuk.expertmatch.query.service.ModelInfoExtractor;
import com.berdachuk.expertmatch.query.service.TokenUsageExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts entities (people, organizations, technologies, projects, domains) from queries.
 */
@Slf4j
@Service
public class EntityExtractor {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final PromptTemplate domainExtractionPromptTemplate;
    private final PromptTemplate personExtractionPromptTemplate;
    private final PromptTemplate organizationExtractionPromptTemplate;
    private final PromptTemplate technologyEntityExtractionPromptTemplate;
    private final PromptTemplate projectExtractionPromptTemplate;

    public EntityExtractor(
            @Lazy ChatClient chatClient,
            ChatModel chatModel,
            ObjectMapper objectMapper,
            Environment environment,
            @Qualifier("domainExtractionPromptTemplate") PromptTemplate domainExtractionPromptTemplate,
            @Qualifier("personExtractionPromptTemplate") PromptTemplate personExtractionPromptTemplate,
            @Qualifier("organizationExtractionPromptTemplate") PromptTemplate organizationExtractionPromptTemplate,
            @Qualifier("technologyEntityExtractionPromptTemplate") PromptTemplate technologyEntityExtractionPromptTemplate,
            @Qualifier("projectExtractionPromptTemplate") PromptTemplate projectExtractionPromptTemplate) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.domainExtractionPromptTemplate = domainExtractionPromptTemplate;
        this.personExtractionPromptTemplate = personExtractionPromptTemplate;
        this.organizationExtractionPromptTemplate = organizationExtractionPromptTemplate;
        this.technologyEntityExtractionPromptTemplate = technologyEntityExtractionPromptTemplate;
        this.projectExtractionPromptTemplate = projectExtractionPromptTemplate;
    }

    /**
     * Extracts entities from query text.
     */
    public ExtractedEntities extract(String query) {
        return extract(query, null);
    }

    /**
     * Extracts entities from query text with optional execution tracing.
     */
    public ExtractedEntities extract(String query, ExecutionTracer tracer) {
        List<Entity> persons = extractPersons(query, tracer);
        List<Entity> organizations = extractOrganizations(query, tracer);
        List<Entity> technologies = extractTechnologies(query, tracer);
        List<Entity> projects = extractProjects(query, tracer);
        List<Entity> domains = extractDomains(query, tracer);

        return new ExtractedEntities(persons, organizations, technologies, projects, domains);
    }

    /**
     * Extracts person entities (expert names, etc.) using LLM.
     */
    private List<Entity> extractPersons(String query) {
        return extractPersons(query, null);
    }

    /**
     * Extracts person entities (expert names, etc.) using LLM with optional execution tracing.
     */
    private List<Entity> extractPersons(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Person Entities", "EntityExtractor", "extractPersons");
            }

            String prompt = buildPersonExtractionPrompt(query);
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during person extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Person Entities", "EntityExtractor", "extractPersons", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during person extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText == null || responseText.isBlank()) {
                log.warn("Blank response text from LLM during person extraction, returning empty list");
                return List.of();
            }
            List<Entity> entities = parseEntityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Persons: " + entities.size(), modelInfo, tokenUsage);
            }

            return entities;
        } catch (Exception e) {
            log.error("Error during person extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Person Entities", "EntityExtractor", "extractPersons", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract persons from query: " + query, e);
        }
    }

    /**
     * Extracts organization entities (companies, customers) using LLM.
     */
    private List<Entity> extractOrganizations(String query) {
        return extractOrganizations(query, null);
    }

    /**
     * Extracts organization entities (companies, customers) using LLM with optional execution tracing.
     */
    private List<Entity> extractOrganizations(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Organization Entities", "EntityExtractor", "extractOrganizations");
            }

            String prompt = buildOrganizationExtractionPrompt(query);
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                log.error("Empty response from LLM during organization extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Organization Entities", "EntityExtractor", "extractOrganizations", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during organization extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            List<Entity> entities = parseEntityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Organizations: " + entities.size(), modelInfo, tokenUsage);
            }

            return entities;
        } catch (Exception e) {
            log.error("Error during organization extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Organization Entities", "EntityExtractor", "extractOrganizations", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract organizations from query: " + query, e);
        }
    }

    /**
     * Extracts technology entities using LLM.
     */
    private List<Entity> extractTechnologies(String query) {
        return extractTechnologies(query, null);
    }

    /**
     * Extracts technology entities using LLM with optional execution tracing.
     */
    private List<Entity> extractTechnologies(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Technology Entities", "EntityExtractor", "extractTechnologies");
            }

            String prompt = buildTechnologyEntityExtractionPrompt(query);
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                log.error("Empty response from LLM during technology entity extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Technology Entities", "EntityExtractor", "extractTechnologies", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during technology entity extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            List<Entity> entities = parseEntityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Technologies: " + entities.size(), modelInfo, tokenUsage);
            }

            return entities;
        } catch (Exception e) {
            log.error("Error during technology entity extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Technology Entities", "EntityExtractor", "extractTechnologies", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract technologies from query: " + query, e);
        }
    }

    /**
     * Extracts project entities using LLM.
     */
    private List<Entity> extractProjects(String query) {
        return extractProjects(query, null);
    }

    /**
     * Extracts project entities using LLM with optional execution tracing.
     */
    private List<Entity> extractProjects(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Project Entities", "EntityExtractor", "extractProjects");
            }

            String prompt = buildProjectExtractionPrompt(query);
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                log.error("Empty response from LLM during project extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Project Entities", "EntityExtractor", "extractProjects", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during project extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            List<Entity> entities = parseEntityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Projects: " + entities.size(), modelInfo, tokenUsage);
            }

            return entities;
        } catch (Exception e) {
            log.error("Error during project extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Project Entities", "EntityExtractor", "extractProjects", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract projects from query: " + query, e);
        }
    }

    /**
     * Extracts domain/industry entities using LLM.
     */
    private List<Entity> extractDomains(String query) {
        return extractDomains(query, null);
    }

    /**
     * Extracts domain/industry entities using LLM with optional execution tracing.
     */
    private List<Entity> extractDomains(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Domain Entities", "EntityExtractor", "extractDomains");
            }

            String prompt = buildDomainExtractionPrompt(query);
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                log.error("Empty response from LLM during domain extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Domain Entities", "EntityExtractor", "extractDomains", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during domain extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            List<Entity> entities = parseEntityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Domains: " + entities.size(), modelInfo, tokenUsage);
            }

            return entities;
        } catch (Exception e) {
            log.error("Error during domain extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Domain Entities", "EntityExtractor", "extractDomains", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract domains from query: " + query, e);
        }
    }

    /**
     * Builds prompt for domain extraction using PromptTemplate.
     */
    private String buildDomainExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return domainExtractionPromptTemplate.render(variables);
    }

    /**
     * Builds prompt for person extraction using PromptTemplate.
     */
    private String buildPersonExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return personExtractionPromptTemplate.render(variables);
    }

    /**
     * Builds prompt for organization extraction using PromptTemplate.
     */
    private String buildOrganizationExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return organizationExtractionPromptTemplate.render(variables);
    }

    /**
     * Builds prompt for technology entity extraction using PromptTemplate.
     */
    private String buildTechnologyEntityExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return technologyEntityExtractionPromptTemplate.render(variables);
    }

    /**
     * Builds prompt for project extraction using PromptTemplate.
     */
    private String buildProjectExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return projectExtractionPromptTemplate.render(variables);
    }

    /**
     * Parses entity list from LLM response.
     */
    private List<Entity> parseEntityResponse(String responseText) {
        try {
            String jsonText = extractJsonFromResponse(responseText);

            // Try to parse as array first (expected format)
            try {
                List<Map<String, Object>> entityMaps = objectMapper.readValue(jsonText, new TypeReference<List<Map<String, Object>>>() {
                });

                if (entityMaps == null) {
                    return List.of();
                }

                List<Entity> entities = new ArrayList<>();
                for (Map<String, Object> entityMap : entityMaps) {
                    String type = entityMap.get("type") != null ? entityMap.get("type").toString() : null;
                    String name = entityMap.get("name") != null ? entityMap.get("name").toString() : null;
                    String id = entityMap.get("id") != null ? entityMap.get("id").toString() : null;

                    if (type != null && name != null && id != null) {
                        entities.add(new Entity(type, name, id));
                    }
                }
                return entities;
            } catch (Exception arrayException) {
                // If parsing as array fails, try parsing as object (might be language response format)
                // If it's an object, return empty list (no entities found)
                try {
                    objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {
                    });
                    // It's an object, not an array - return empty list
                    log.warn("Received object format instead of array for entity extraction, returning empty list");
                    return List.of();
                } catch (Exception objectException) {
                    // Not an object either, rethrow original array exception
                    throw arrayException;
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse entity JSON", e);
            throw new RuntimeException("Failed to parse entity response: " + responseText, e);
        }
    }

    /**
     * Extracts JSON from response text, handling markdown code blocks.
     */
    private String extractJsonFromResponse(String responseText) {
        String jsonText = responseText.trim();
        if (jsonText.contains("```json")) {
            int startIdx = jsonText.indexOf("```json") + 7;
            int endIdx = jsonText.indexOf("```", startIdx);
            if (endIdx > startIdx) {
                jsonText = jsonText.substring(startIdx, endIdx).trim();
            }
        } else if (jsonText.contains("```")) {
            int startIdx = jsonText.indexOf("```") + 3;
            int endIdx = jsonText.indexOf("```", startIdx);
            if (endIdx > startIdx) {
                jsonText = jsonText.substring(startIdx, endIdx).trim();
            }
        }
        return jsonText;
    }

    /**
     * Extracted entities result.
     */
    public record ExtractedEntities(
            List<Entity> persons,
            List<Entity> organizations,
            List<Entity> technologies,
            List<Entity> projects,
            List<Entity> domains
    ) {
    }

    /**
     * Entity representation.
     */
    public record Entity(
            String type,
            String name,
            String id
    ) {
    }
}

