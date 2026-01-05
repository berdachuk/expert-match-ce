package com.berdachuk.expertmatch.ingestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for expanding domain-specific constants using LLM.
 * Uses prompt templates to generate additional values for technologies, tools, project types, etc.
 *
 * <p>Features:
 * <ul>
 *   <li>Caching to avoid repeated LLM calls</li>
 *   <li>Graceful fallback to existing constants on errors</li>
 *   <li>Handles both plain JSON and markdown-wrapped JSON responses</li>
 * </ul>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "expertmatch.ingestion.constant-expansion.enabled", havingValue = "true", matchIfMissing = false)
public class ConstantExpansionService {

    private final ChatClient chatClient;
    private final PromptTemplate technologiesExpansionTemplate;
    private final PromptTemplate toolsExpansionTemplate;
    private final PromptTemplate projectTypesExpansionTemplate;
    private final PromptTemplate teamNamesExpansionTemplate;
    private final PromptTemplate technologyCategoriesExpansionTemplate;
    private final PromptTemplate technologySynonymsExpansionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cache for expanded constants to avoid repeated LLM calls.
     * Key: "{constantType}_{inputHash}", Value: Expanded result
     */
    private final Map<String, Object> expandedConstantsCache = new HashMap<>();

    @Autowired
    public ConstantExpansionService(
            ChatClient chatClient,
            @Qualifier("technologiesExpansionPromptTemplate") PromptTemplate technologiesExpansionTemplate,
            @Qualifier("toolsExpansionPromptTemplate") PromptTemplate toolsExpansionTemplate,
            @Qualifier("projectTypesExpansionPromptTemplate") PromptTemplate projectTypesExpansionTemplate,
            @Qualifier("teamNamesExpansionPromptTemplate") PromptTemplate teamNamesExpansionTemplate,
            @Qualifier("technologyCategoriesExpansionPromptTemplate") PromptTemplate technologyCategoriesExpansionTemplate,
            @Qualifier("technologySynonymsExpansionPromptTemplate") PromptTemplate technologySynonymsExpansionTemplate,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.technologiesExpansionTemplate = technologiesExpansionTemplate;
        this.toolsExpansionTemplate = toolsExpansionTemplate;
        this.projectTypesExpansionTemplate = projectTypesExpansionTemplate;
        this.teamNamesExpansionTemplate = teamNamesExpansionTemplate;
        this.technologyCategoriesExpansionTemplate = technologyCategoriesExpansionTemplate;
        this.technologySynonymsExpansionTemplate = technologySynonymsExpansionTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Expands technologies list using LLM.
     *
     * @param existingTechnologies Existing technology list
     * @return Expanded list with new technologies merged (no duplicates)
     */
    public List<String> expandTechnologies(List<String> existingTechnologies) {
        String cacheKey = "technologies_" + existingTechnologies.hashCode();
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            List<String> cached = (List<String>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached technologies expansion ({} items)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingTechnologies", objectMapper.writeValueAsString(existingTechnologies));

            String promptText = technologiesExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            List<String> expanded = parseJsonArray(content);

            // Merge with existing, remove duplicates
            Set<String> allTechnologies = new LinkedHashSet<>(existingTechnologies);
            allTechnologies.addAll(expanded);
            List<String> result = new ArrayList<>(allTechnologies);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded technologies from {} to {} items", existingTechnologies.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand technologies with LLM, using existing list: {}", e.getMessage());
            return new ArrayList<>(existingTechnologies); // Fallback to existing
        }
    }

    /**
     * Expands tools list using LLM.
     *
     * @param existingTools Existing tools list
     * @return Expanded list with new tools merged (no duplicates)
     */
    public List<String> expandTools(List<String> existingTools) {
        String cacheKey = "tools_" + existingTools.hashCode();
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            List<String> cached = (List<String>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached tools expansion ({} items)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingTools", objectMapper.writeValueAsString(existingTools));

            String promptText = toolsExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            List<String> expanded = parseJsonArray(content);

            // Merge with existing, remove duplicates
            Set<String> allTools = new LinkedHashSet<>(existingTools);
            allTools.addAll(expanded);
            List<String> result = new ArrayList<>(allTools);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded tools from {} to {} items", existingTools.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand tools with LLM, using existing list: {}", e.getMessage());
            return new ArrayList<>(existingTools); // Fallback to existing
        }
    }

    /**
     * Expands project types list using LLM.
     *
     * @param existingProjectTypes Existing project types list
     * @return Expanded list with new project types merged (no duplicates)
     */
    public List<String> expandProjectTypes(List<String> existingProjectTypes) {
        String cacheKey = "projectTypes_" + existingProjectTypes.hashCode();
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            List<String> cached = (List<String>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached project types expansion ({} items)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingProjectTypes", objectMapper.writeValueAsString(existingProjectTypes));

            String promptText = projectTypesExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            List<String> expanded = parseJsonArray(content);

            // Merge with existing, remove duplicates
            Set<String> allProjectTypes = new LinkedHashSet<>(existingProjectTypes);
            allProjectTypes.addAll(expanded);
            List<String> result = new ArrayList<>(allProjectTypes);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded project types from {} to {} items", existingProjectTypes.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand project types with LLM, using existing list: {}", e.getMessage());
            return new ArrayList<>(existingProjectTypes); // Fallback to existing
        }
    }

    /**
     * Expands team names list using LLM.
     *
     * @param existingTeamNames Existing team names list
     * @return Expanded list with new team names merged (no duplicates)
     */
    public List<String> expandTeamNames(List<String> existingTeamNames) {
        String cacheKey = "teamNames_" + existingTeamNames.hashCode();
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            List<String> cached = (List<String>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached team names expansion ({} items)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingTeamNames", objectMapper.writeValueAsString(existingTeamNames));

            String promptText = teamNamesExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            List<String> expanded = parseJsonArray(content);

            // Merge with existing, remove duplicates
            Set<String> allTeamNames = new LinkedHashSet<>(existingTeamNames);
            allTeamNames.addAll(expanded);
            List<String> result = new ArrayList<>(allTeamNames);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded team names from {} to {} items", existingTeamNames.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand team names with LLM, using existing list: {}", e.getMessage());
            return new ArrayList<>(existingTeamNames); // Fallback to existing
        }
    }

    /**
     * Expands technology categories map using LLM.
     *
     * @param technologies       Existing technologies list
     * @param existingCategories Existing category mappings
     * @return Expanded map with new category mappings merged
     */
    public Map<String, String> expandTechnologyCategories(
            List<String> technologies,
            Map<String, String> existingCategories) {
        String cacheKey = "technologyCategories_" + (technologies.hashCode() + existingCategories.hashCode());
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            Map<String, String> cached = (Map<String, String>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached technology categories expansion ({} mappings)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingTechnologies", objectMapper.writeValueAsString(technologies));
            variables.put("existingCategories", objectMapper.writeValueAsString(existingCategories));

            String promptText = technologyCategoriesExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            Map<String, String> expanded = parseJsonObject(content, new TypeReference<Map<String, String>>() {
            });

            // Merge with existing
            Map<String, String> result = new HashMap<>(existingCategories);
            result.putAll(expanded);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded technology categories from {} to {} mappings",
                    existingCategories.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand technology categories with LLM, using existing: {}", e.getMessage());
            return new HashMap<>(existingCategories); // Fallback
        }
    }

    /**
     * Expands technology synonyms map using LLM.
     *
     * @param technologies     Existing technologies list
     * @param existingSynonyms Existing synonym mappings
     * @return Expanded map with new synonym mappings merged
     */
    public Map<String, String[]> expandTechnologySynonyms(
            List<String> technologies,
            Map<String, String[]> existingSynonyms) {
        String cacheKey = "technologySynonyms_" + (technologies.hashCode() + existingSynonyms.hashCode());
        if (expandedConstantsCache.containsKey(cacheKey)) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> cached = (Map<String, String[]>) expandedConstantsCache.get(cacheKey);
            log.debug("Returning cached technology synonyms expansion ({} mappings)", cached.size());
            return cached;
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("existingTechnologies", objectMapper.writeValueAsString(technologies));
            variables.put("existingSynonyms", objectMapper.writeValueAsString(existingSynonyms));

            String promptText = technologySynonymsExpansionTemplate.render(variables);
            ChatResponse response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            Map<String, List<String>> expandedList = parseJsonObject(content,
                    new TypeReference<Map<String, List<String>>>() {
                    });

            // Convert List<String> to String[]
            Map<String, String[]> expanded = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : expandedList.entrySet()) {
                expanded.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }

            // Merge with existing
            Map<String, String[]> result = new HashMap<>(existingSynonyms);
            result.putAll(expanded);

            expandedConstantsCache.put(cacheKey, result);
            log.info("Expanded technology synonyms from {} to {} mappings",
                    existingSynonyms.size(), result.size());

            return result;
        } catch (Exception e) {
            log.warn("Failed to expand technology synonyms with LLM, using existing: {}", e.getMessage());
            return new HashMap<>(existingSynonyms); // Fallback
        }
    }

    /**
     * Parses JSON array from LLM response.
     * Handles both plain JSON and markdown code blocks.
     *
     * @param jsonContent LLM response content
     * @return Parsed list of strings
     * @throws Exception if parsing fails
     */
    private List<String> parseJsonArray(String jsonContent) throws Exception {
        // Extract JSON array from response (may have markdown code blocks)
        String cleaned = jsonContent.trim();
        if (cleaned.startsWith("```")) {
            // Find the JSON array within markdown code block
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']') + 1;
            if (start > 0 && end > start) {
                cleaned = cleaned.substring(start, end);
            }
        }
        return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {
        });
    }

    /**
     * Parses JSON object from LLM response.
     * Handles both plain JSON and markdown code blocks.
     *
     * @param jsonContent LLM response content
     * @param typeRef     Type reference for the expected object type
     * @return Parsed object
     * @throws Exception if parsing fails
     */
    private <T> T parseJsonObject(String jsonContent, TypeReference<T> typeRef) throws Exception {
        String cleaned = jsonContent.trim();
        if (cleaned.startsWith("```")) {
            // Find the JSON object within markdown code block
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}') + 1;
            if (start > 0 && end > start) {
                cleaned = cleaned.substring(start, end);
            }
        }
        return objectMapper.readValue(cleaned, typeRef);
    }
}

