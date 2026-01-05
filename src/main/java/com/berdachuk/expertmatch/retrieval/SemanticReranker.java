package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.data.EmployeeRepository;
import com.berdachuk.expertmatch.data.WorkExperienceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for semantic reranking of retrieval results using LLM.
 */
@Slf4j
@Service
public class SemanticReranker {

    private final ChatModel rerankingChatModel;
    private final PromptTemplate rerankingPromptTemplate;
    private final EmployeeRepository employeeRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final ObjectMapper objectMapper;

    public SemanticReranker(
            @Qualifier("rerankingChatModel") @Autowired(required = false) ChatModel rerankingChatModel,
            @Qualifier("rerankingPromptTemplate") PromptTemplate rerankingPromptTemplate,
            EmployeeRepository employeeRepository,
            WorkExperienceRepository workExperienceRepository,
            ObjectMapper objectMapper) {
        this.rerankingChatModel = rerankingChatModel;
        this.rerankingPromptTemplate = rerankingPromptTemplate;
        this.employeeRepository = employeeRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Reranks expert results using semantic similarity via LLM.
     *
     * @param queryText  Original query text
     * @param expertIds  List of expert IDs to rerank
     * @param maxResults Maximum number of results after reranking
     * @return Reranked list of expert IDs
     */
    public List<String> rerank(String queryText, List<String> expertIds, int maxResults) {
        // Validate input parameters
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("Query text cannot be null or blank");
        }
        if (expertIds == null || expertIds.isEmpty()) {
            // Return empty list instead of throwing exception - no results to rerank
            return List.of();
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }

        // If reranking model is not configured, return as-is (fallback behavior)
        if (rerankingChatModel == null || rerankingPromptTemplate == null) {
            log.debug("Reranking model not configured, returning results as-is");
            return expertIds.stream()
                    .limit(maxResults)
                    .toList();
        }

        try {
            // Get expert data
            List<EmployeeRepository.Employee> employees = employeeRepository.findByIds(expertIds);
            Map<String, EmployeeRepository.Employee> employeeMap = employees.stream()
                    .collect(Collectors.toMap(EmployeeRepository.Employee::id, e -> e));

            Map<String, List<WorkExperienceRepository.WorkExperience>> workExperienceMap =
                    workExperienceRepository.findByEmployeeIds(expertIds);

            // Build expert descriptions for reranking
            StringBuilder expertsText = new StringBuilder();
            for (String expertId : expertIds) {
                EmployeeRepository.Employee employee = employeeMap.get(expertId);
                if (employee == null) {
                    continue;
                }

                List<WorkExperienceRepository.WorkExperience> workExperiences =
                        workExperienceMap.getOrDefault(expertId, List.of());

                expertsText.append("Expert ID: ").append(expertId).append("\n");
                expertsText.append("Name: ").append(employee.name()).append("\n");
                expertsText.append("Seniority: ").append(employee.seniority() != null ? employee.seniority() : "N/A").append("\n");

                if (!workExperiences.isEmpty()) {
                    expertsText.append("Projects:\n");
                    for (WorkExperienceRepository.WorkExperience we : workExperiences.stream().limit(3).toList()) {
                        expertsText.append("  - ").append(we.projectName() != null ? we.projectName() : "N/A");
                        if (we.role() != null) {
                            expertsText.append(" (").append(we.role()).append(")");
                        }
                        if (we.technologies() != null && !we.technologies().isEmpty()) {
                            expertsText.append(" - Technologies: ").append(String.join(", ", we.technologies()));
                        }
                        expertsText.append("\n");
                    }
                }
                expertsText.append("\n");
            }

            // Build prompt
            Map<String, Object> variables = new HashMap<>();
            variables.put("query", queryText);
            variables.put("experts", expertsText.toString());

            String promptText = rerankingPromptTemplate.render(variables);
            Prompt prompt = new Prompt(promptText);

            // Call reranking model
            ChatResponse response = rerankingChatModel.call(prompt);

            // Check for null response or empty output
            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.warn("Reranking model returned null or empty response, falling back to original order");
                return expertIds.stream()
                        .limit(maxResults)
                        .toList();
            }

            String responseText = response.getResult().getOutput().getText();

            // Check for empty response text
            if (responseText == null || responseText.isBlank()) {
                log.warn("Reranking model returned blank response text, falling back to original order");
                return expertIds.stream()
                        .limit(maxResults)
                        .toList();
            }

            // Parse JSON response
            List<RerankingResult> results = parseRerankingResponse(responseText);

            // If parsing failed (empty list) or results don't match input, fallback to original order
            if (results.isEmpty()) {
                log.warn("Reranking response parsing returned empty list, falling back to original order");
                return expertIds.stream()
                        .limit(maxResults)
                        .toList();
            }

            // Validate that parsed results contain valid expert IDs from input
            Set<String> inputExpertIds = new HashSet<>(expertIds);
            Set<String> parsedExpertIds = results.stream()
                    .map(RerankingResult::expertId)
                    .filter(Objects::nonNull) // Filter out null expert IDs
                    .collect(Collectors.toSet());

            // Check if any parsed expert ID is not in the input list (invalid ID)
            boolean hasInvalidIds = parsedExpertIds.stream()
                    .anyMatch(id -> !inputExpertIds.contains(id));

            // If parsed results contain invalid IDs or no valid results, fallback to original order
            if (hasInvalidIds || parsedExpertIds.isEmpty()) {
                log.warn("Reranking response contains invalid expert IDs or no valid results (input: {}, parsed: {}), falling back to original order",
                        inputExpertIds.size(), parsedExpertIds.size());
                return expertIds.stream()
                        .limit(maxResults)
                        .toList();
            }

            // Filter out invalid IDs and sort by score (descending)
            List<String> rerankedIds = results.stream()
                    .filter(result -> inputExpertIds.contains(result.expertId())) // Only include valid IDs
                    .sorted(Comparator.comparing(RerankingResult::score).reversed())
                    .map(RerankingResult::expertId)
                    .collect(Collectors.toList());

            // If parsed results don't include all input IDs, add missing IDs at the end
            // LLM may return a subset, which is acceptable
            Set<String> missingIds = new HashSet<>(inputExpertIds);
            missingIds.removeAll(parsedExpertIds);

            if (!missingIds.isEmpty()) {
                log.debug("Reranking response missing {} expert IDs, appending them at the end", missingIds.size());
                // Add missing IDs in their original order
                for (String expertId : expertIds) {
                    if (missingIds.contains(expertId) && !rerankedIds.contains(expertId)) {
                        rerankedIds.add(expertId);
                    }
                }
            }

            return rerankedIds.stream()
                    .limit(maxResults)
                    .toList();

        } catch (Exception e) {
            log.error("Error during LLM-based reranking, falling back to original order: {}", e.getMessage(), e);
            // Fallback: return original order
            return expertIds.stream()
                    .limit(maxResults)
                    .toList();
        }
    }

    /**
     * Calculates relevance scores for experts using LLM.
     */
    public Map<String, Double> calculateRelevanceScores(String queryText, List<String> expertIds) {
        // Validate input parameters
        if (queryText == null || queryText.isBlank()) {
            throw new IllegalArgumentException("Query text cannot be null or blank");
        }
        if (expertIds == null || expertIds.isEmpty()) {
            // Return empty map instead of throwing exception - no results to score
            return Map.of();
        }

        // If reranking model is not configured, return placeholder scores
        if (rerankingChatModel == null || rerankingPromptTemplate == null) {
            log.debug("Reranking model not configured, returning placeholder scores");
            Map<String, Double> scores = new HashMap<>();
            for (String expertId : expertIds) {
                scores.put(expertId, 0.8); // Placeholder
            }
            return scores;
        }

        try {
            // Use rerank() to get scores, then extract scores
            List<EmployeeRepository.Employee> employees = employeeRepository.findByIds(expertIds);
            Map<String, EmployeeRepository.Employee> employeeMap = employees.stream()
                    .collect(Collectors.toMap(EmployeeRepository.Employee::id, e -> e));

            Map<String, List<WorkExperienceRepository.WorkExperience>> workExperienceMap =
                    workExperienceRepository.findByEmployeeIds(expertIds);

            // Build expert descriptions
            StringBuilder expertsText = new StringBuilder();
            for (String expertId : expertIds) {
                EmployeeRepository.Employee employee = employeeMap.get(expertId);
                if (employee == null) {
                    continue;
                }

                List<WorkExperienceRepository.WorkExperience> workExperiences =
                        workExperienceMap.getOrDefault(expertId, List.of());

                expertsText.append("Expert ID: ").append(expertId).append("\n");
                expertsText.append("Name: ").append(employee.name()).append("\n");
                expertsText.append("Seniority: ").append(employee.seniority() != null ? employee.seniority() : "N/A").append("\n");

                if (!workExperiences.isEmpty()) {
                    expertsText.append("Projects:\n");
                    for (WorkExperienceRepository.WorkExperience we : workExperiences.stream().limit(3).toList()) {
                        expertsText.append("  - ").append(we.projectName() != null ? we.projectName() : "N/A");
                        if (we.role() != null) {
                            expertsText.append(" (").append(we.role()).append(")");
                        }
                        if (we.technologies() != null && !we.technologies().isEmpty()) {
                            expertsText.append(" - Technologies: ").append(String.join(", ", we.technologies()));
                        }
                        expertsText.append("\n");
                    }
                }
                expertsText.append("\n");
            }

            // Build prompt
            Map<String, Object> variables = new HashMap<>();
            variables.put("query", queryText);
            variables.put("experts", expertsText.toString());

            String promptText = rerankingPromptTemplate.render(variables);
            Prompt prompt = new Prompt(promptText);

            // Call reranking model
            ChatResponse response = rerankingChatModel.call(prompt);

            // Check for null response or empty output
            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.warn("Reranking model returned null or empty response for score calculation, returning placeholder scores");
                Map<String, Double> scores = new HashMap<>();
                for (String expertId : expertIds) {
                    scores.put(expertId, 0.8);
                }
                return scores;
            }

            String responseText = response.getResult().getOutput().getText();

            // Check for empty response text
            if (responseText == null || responseText.isBlank()) {
                log.warn("Reranking model returned blank response text for score calculation, returning placeholder scores");
                Map<String, Double> scores = new HashMap<>();
                for (String expertId : expertIds) {
                    scores.put(expertId, 0.8);
                }
                return scores;
            }

            // Parse JSON response
            List<RerankingResult> results = parseRerankingResponse(responseText);

            // If parsing failed (empty list), return placeholder scores
            if (results.isEmpty()) {
                log.warn("Reranking response parsing returned empty list for score calculation, returning placeholder scores");
                Map<String, Double> scores = new HashMap<>();
                for (String expertId : expertIds) {
                    scores.put(expertId, 0.8);
                }
                return scores;
            }

            // Build score map
            Map<String, Double> scores = new HashMap<>();
            for (RerankingResult result : results) {
                scores.put(result.expertId(), result.score());
            }

            // Fill in missing scores with default
            for (String expertId : expertIds) {
                scores.putIfAbsent(expertId, 0.5);
            }

            return scores;

        } catch (Exception e) {
            log.error("Error calculating relevance scores, returning placeholder scores: {}", e.getMessage(), e);
            // Fallback: return placeholder scores
            Map<String, Double> scores = new HashMap<>();
            for (String expertId : expertIds) {
                scores.put(expertId, 0.8);
            }
            return scores;
        }
    }

    /**
     * Parses JSON response from reranking model.
     */
    private List<RerankingResult> parseRerankingResponse(String responseText) {
        try {
            // Clean response text (remove markdown code blocks if present)
            String cleaned = responseText.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            // Parse JSON
            TypeReference<List<RerankingResult>> typeRef = new TypeReference<List<RerankingResult>>() {
            };
            return objectMapper.readValue(cleaned, typeRef);
        } catch (Exception e) {
            log.error("Failed to parse reranking response: {}", responseText, e);
            return new ArrayList<>();
        }
    }

    /**
     * Reranking result from LLM.
     */
    private record RerankingResult(
            String expertId,
            Double score,
            String reason
    ) {
    }
}

