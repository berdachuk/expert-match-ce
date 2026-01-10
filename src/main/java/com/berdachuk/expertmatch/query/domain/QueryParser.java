package com.berdachuk.expertmatch.query.domain;

import com.berdachuk.expertmatch.llm.sgr.SGRPatternConfig;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;
import com.berdachuk.expertmatch.query.service.ModelInfoExtractor;
import com.berdachuk.expertmatch.query.service.TokenUsageExtractor;
import com.berdachuk.expertmatch.query.sgr.QueryClassification;
import com.berdachuk.expertmatch.query.sgr.QueryClassificationService;
import com.berdachuk.expertmatch.query.sgr.QueryIntent;
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

import java.util.*;

/**
 * Parses natural language queries and extracts structured requirements.
 * Supports both rule-based and LLM-based (Routing pattern) classification.
 */
@Slf4j
@Service
public class QueryParser {

    private final QueryClassificationService queryClassificationService;
    private final SGRPatternConfig sgrConfig;
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final PromptTemplate skillExtractionPromptTemplate;
    private final PromptTemplate seniorityExtractionPromptTemplate;
    private final PromptTemplate languageExtractionPromptTemplate;
    private final PromptTemplate technologyExtractionPromptTemplate;

    public QueryParser(
            QueryClassificationService queryClassificationService,
            SGRPatternConfig sgrConfig,
            @Lazy ChatClient chatClient,
            ChatModel chatModel,
            ObjectMapper objectMapper,
            Environment environment,
            @Qualifier("skillExtractionPromptTemplate") PromptTemplate skillExtractionPromptTemplate,
            @Qualifier("seniorityExtractionPromptTemplate") PromptTemplate seniorityExtractionPromptTemplate,
            @Qualifier("languageExtractionPromptTemplate") PromptTemplate languageExtractionPromptTemplate,
            @Qualifier("technologyExtractionPromptTemplate") PromptTemplate technologyExtractionPromptTemplate) {
        this.queryClassificationService = queryClassificationService;
        this.sgrConfig = sgrConfig;
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.skillExtractionPromptTemplate = skillExtractionPromptTemplate;
        this.seniorityExtractionPromptTemplate = seniorityExtractionPromptTemplate;
        this.languageExtractionPromptTemplate = languageExtractionPromptTemplate;
        this.technologyExtractionPromptTemplate = technologyExtractionPromptTemplate;
    }

    /**
     * Parses a query and extracts requirements.
     * Uses rule-based classification by default.
     */
    public ParsedQuery parse(String query) {
        return parse(query, false);
    }

    /**
     * Parses a query and extracts requirements.
     *
     * @param query             The query to parse
     * @param useRoutingPattern If true, uses LLM-based Routing pattern for classification
     * @return Parsed query with extracted requirements
     */
    public ParsedQuery parse(String query, boolean useRoutingPattern) {
        return parse(query, useRoutingPattern, null);
    }

    /**
     * Parses a query and extracts requirements with optional execution tracing.
     *
     * @param query             The query to parse
     * @param useRoutingPattern If true, uses LLM-based Routing pattern for classification
     * @param tracer            Optional execution tracer for tracking
     * @return Parsed query with extracted requirements
     */
    public ParsedQuery parse(String query, boolean useRoutingPattern, ExecutionTracer tracer) {
        List<String> skills = extractSkills(query, tracer);
        List<String> seniorityLevels = extractSeniority(query, tracer);
        String language = extractLanguage(query, tracer);
        String intent = classifyIntent(query, useRoutingPattern);
        List<String> technologies = extractTechnologies(query, tracer);

        // If routing pattern is used, extract additional requirements from classification
        if (useRoutingPattern && queryClassificationService != null && sgrConfig != null
                && sgrConfig.isEnabled() && sgrConfig.getRouting().isEnabled()) {
            try {
                QueryClassification classification = queryClassificationService.classifyWithRouting(query);
                // Use LLM-classified intent
                intent = mapQueryIntentToString(classification.intent());
                // Merge extracted requirements from classification
                if (classification.extractedRequirements() != null) {
                    @SuppressWarnings("unchecked")
                    List<String> classifiedSkills = (List<String>) classification.extractedRequirements().get("skills");
                    if (classifiedSkills != null && !classifiedSkills.isEmpty()) {
                        skills = new ArrayList<>(skills);
                        skills.addAll(classifiedSkills);
                    }
                }
            } catch (Exception e) {
                log.warn("Query classification with routing pattern failed, using rule-based classification", e);
                // Intent already set by rule-based classification
            }
        }

        return new ParsedQuery(
                query,
                skills,
                seniorityLevels,
                language,
                intent,
                technologies
        );
    }

    /**
     * Maps QueryIntent enum to string format used by ParsedQuery.
     */
    private String mapQueryIntentToString(QueryIntent intent) {
        return switch (intent) {
            case EXPERT_SEARCH -> "expert_search";
            case TEAM_FORMATION -> "team_formation";
            case RFP_RESPONSE -> "rfp_response";
            case DOMAIN_INQUIRY -> "domain_inquiry";
        };
    }

    /**
     * Extracts skills from query text using LLM.
     */
    private List<String> extractSkills(String query) {
        return extractSkills(query, null);
    }

    /**
     * Extracts skills from query text using LLM with optional execution tracing.
     */
    private List<String> extractSkills(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Skills", "QueryParser", "extractSkills");
            }

            String prompt = Objects.requireNonNull(buildSkillExtractionPrompt(query), "Skill extraction prompt cannot be null");

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during skill extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Skills", "QueryParser", "extractSkills", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during skill extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText.isBlank()) {
                log.warn("Blank response text from LLM during skill extraction, returning empty list");
                return List.of();
            }
            List<String> skills = parseSkillsResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Skills: " + skills.size(), modelInfo, tokenUsage);
            }

            return skills;
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // Re-throw AI exceptions directly so they can be handled by GlobalExceptionHandler
            log.error("LLM API error during skill extraction: {}", e.getMessage(), e);
            if (tracer != null) {
                tracer.failStep("Extract Skills", "QueryParser", "extractSkills", "LLM API error: " + e.getMessage());
            }
            throw e;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Re-throw ResourceAccessException directly so it can be handled by GlobalExceptionHandler
            log.error("LLM API connection error during skill extraction: {}", e.getMessage(), e);
            if (tracer != null) {
                tracer.failStep("Extract Skills", "QueryParser", "extractSkills", "Connection error: " + e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            // Check if the exception is a ResourceAccessException in the cause chain
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof org.springframework.web.client.ResourceAccessException raEx) {
                    // Re-throw ResourceAccessException directly so it can be handled by GlobalExceptionHandler
                    log.error("LLM API connection error during skill extraction (from cause chain): {}", raEx.getMessage(), raEx);
                    if (tracer != null) {
                        tracer.failStep("Extract Skills", "QueryParser", "extractSkills", "Connection error: " + raEx.getMessage());
                    }
                    throw raEx;
                }
                cause = cause.getCause();
            }
            log.error("Error during skill extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Skills", "QueryParser", "extractSkills", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract skills from query: " + query, e);
        }
    }

    /**
     * Builds prompt for skill extraction using PromptTemplate.
     */
    private String buildSkillExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return skillExtractionPromptTemplate.render(variables);
    }

    /**
     * Parses skills from LLM response.
     */
    private List<String> parseSkillsResponse(String responseText) {
        try {
            String jsonText = extractJsonFromResponse(responseText);
            List<String> skills = objectMapper.readValue(jsonText, new TypeReference<List<String>>() {
            });
            return skills != null ? skills : List.of();
        } catch (Exception e) {
            log.error("Failed to parse skills JSON", e);
            throw new RuntimeException("Failed to parse skills response: " + responseText, e);
        }
    }

    /**
     * Extracts seniority levels from query using LLM.
     */
    private List<String> extractSeniority(String query) {
        return extractSeniority(query, null);
    }

    /**
     * Extracts seniority levels from query using LLM with optional execution tracing.
     */
    private List<String> extractSeniority(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Seniority", "QueryParser", "extractSeniority");
            }

            String prompt = Objects.requireNonNull(buildSeniorityExtractionPrompt(query), "Seniority extraction prompt cannot be null");

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during seniority extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Seniority", "QueryParser", "extractSeniority", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during seniority extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText.isBlank()) {
                log.warn("Blank response text from LLM during seniority extraction, returning empty list");
                return List.of();
            }
            List<String> seniority = parseSeniorityResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Seniority: " + seniority.size(), modelInfo, tokenUsage);
            }

            return seniority;
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // Re-throw AI exceptions directly so they can be handled by GlobalExceptionHandler
            log.error("LLM API error during seniority extraction: {}", e.getMessage(), e);
            if (tracer != null) {
                tracer.failStep("Extract Seniority", "QueryParser", "extractSeniority", "LLM API error: " + e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            log.error("Error during seniority extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Seniority", "QueryParser", "extractSeniority", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract seniority from query: " + query, e);
        }
    }

    /**
     * Builds prompt for seniority extraction using PromptTemplate.
     */
    private String buildSeniorityExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return seniorityExtractionPromptTemplate.render(variables);
    }

    /**
     * Parses seniority levels from LLM response.
     */
    private List<String> parseSeniorityResponse(String responseText) {
        try {
            String jsonText = extractJsonFromResponse(responseText);
            List<String> seniority = objectMapper.readValue(jsonText, new TypeReference<List<String>>() {
            });
            return seniority != null ? seniority : List.of();
        } catch (Exception e) {
            log.error("Failed to parse seniority JSON", e);
            throw new RuntimeException("Failed to parse seniority response: " + responseText, e);
        }
    }

    /**
     * Extracts language requirements using LLM.
     */
    private String extractLanguage(String query) {
        return extractLanguage(query, null);
    }

    /**
     * Extracts language requirements using LLM with optional execution tracing.
     */
    private String extractLanguage(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Language", "QueryParser", "extractLanguage");
            }

            String prompt = Objects.requireNonNull(buildLanguageExtractionPrompt(query), "Language extraction prompt cannot be null");

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during language extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Language", "QueryParser", "extractLanguage", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during language extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText.isBlank()) {
                log.warn("Blank response text from LLM during language extraction, returning default");
                return "en"; // Default to English
            }
            String language = parseLanguageResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Language: " + (language != null ? language : "none"), modelInfo, tokenUsage);
            }

            return language;
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // Re-throw AI exceptions directly so they can be handled by GlobalExceptionHandler
            log.error("LLM API error during language extraction: {}", e.getMessage(), e);
            if (tracer != null) {
                tracer.failStep("Extract Language", "QueryParser", "extractLanguage", "LLM API error: " + e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            log.error("Error during language extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Language", "QueryParser", "extractLanguage", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract language from query: " + query, e);
        }
    }

    /**
     * Builds prompt for language extraction using PromptTemplate.
     */
    private String buildLanguageExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return languageExtractionPromptTemplate.render(variables);
    }

    /**
     * Parses language from LLM response.
     * Returns proficiency level string (e.g., "C1") or null if no language requirements.
     */
    private String parseLanguageResponse(String responseText) {
        try {
            String jsonText = extractJsonFromResponse(responseText);

            // Handle null response
            if (jsonText.trim().equalsIgnoreCase("null")) {
                return null;
            }

            // Try to parse as array first (in case mock returns array by mistake)
            try {
                List<?> arrayData = objectMapper.readValue(jsonText, new TypeReference<List<?>>() {
                });
                if (arrayData != null) {
                    // If it's an array, return null (no language requirements)
                    return null;
                }
            } catch (Exception e) {
                // Not an array, continue to parse as object
            }

            Map<String, Object> languageData = objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {
            });
            if (languageData == null) {
                return null;
            }

            Object proficiency = languageData.get("proficiency");
            return proficiency != null ? proficiency.toString() : null;
        } catch (Exception e) {
            log.error("Failed to parse language JSON", e);
            throw new RuntimeException("Failed to parse language response: " + responseText, e);
        }
    }

    /**
     * Classifies query intent using rule-based approach.
     * Routing pattern classification is handled in parse() method.
     */
    private String classifyIntent(String query, boolean useRoutingPattern) {
        // If routing pattern is enabled, this will be overridden by LLM classification
        // This method provides rule-based classification
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("team") || lowerQuery.contains("team formation")) {
            return "team_formation";
        } else if (lowerQuery.contains("rfp") || lowerQuery.contains("proposal")) {
            return "rfp_response";
        } else {
            return "expert_search";
        }
    }

    /**
     * Extracts technologies from query using LLM.
     */
    private List<String> extractTechnologies(String query) {
        return extractTechnologies(query, null);
    }

    /**
     * Extracts technologies from query using LLM with optional execution tracing.
     */
    private List<String> extractTechnologies(String query, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Extract Technologies", "QueryParser", "extractTechnologies");
            }

            String prompt = Objects.requireNonNull(buildTechnologyExtractionPrompt(query), "Technology extraction prompt cannot be null");

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during technology extraction");
                if (tracer != null) {
                    tracer.failStep("Extract Technologies", "QueryParser", "extractTechnologies", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during technology extraction");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText.isBlank()) {
                log.warn("Blank response text from LLM during technology extraction, returning empty list");
                return List.of();
            }
            List<String> technologies = parseTechnologyResponse(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query, "Technologies: " + technologies.size(), modelInfo, tokenUsage);
            }

            return technologies;
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // Re-throw AI exceptions directly so they can be handled by GlobalExceptionHandler
            log.error("LLM API error during technology extraction: {}", e.getMessage(), e);
            if (tracer != null) {
                tracer.failStep("Extract Technologies", "QueryParser", "extractTechnologies", "LLM API error: " + e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            log.error("Error during technology extraction", e);
            if (tracer != null) {
                tracer.failStep("Extract Technologies", "QueryParser", "extractTechnologies", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to extract technologies from query: " + query, e);
        }
    }

    /**
     * Builds prompt for technology extraction using PromptTemplate.
     */
    private String buildTechnologyExtractionPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return technologyExtractionPromptTemplate.render(variables);
    }

    /**
     * Parses technologies from LLM response.
     */
    private List<String> parseTechnologyResponse(String responseText) {
        try {
            String jsonText = extractJsonFromResponse(responseText);
            List<String> technologies = objectMapper.readValue(jsonText, new TypeReference<List<String>>() {
            });
            return technologies != null ? technologies : List.of();
        } catch (Exception e) {
            log.error("Failed to parse technology JSON", e);
            throw new RuntimeException("Failed to parse technology response: " + responseText, e);
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
     * Parsed query result.
     */
    public record ParsedQuery(
            String originalQuery,
            List<String> skills,
            List<String> seniorityLevels,
            String language,
            String intent,
            List<String> technologies
    ) {
    }
}

