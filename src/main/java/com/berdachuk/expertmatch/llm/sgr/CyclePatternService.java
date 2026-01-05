package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.AnswerGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for multiple expert evaluation using Cycle pattern.
 * Forces LLM to repeat reasoning steps for multiple experts.
 */
@Slf4j
@Service
public class CyclePatternService {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final SGRPatternConfig config;
    private final PromptTemplate cycleEvaluationPromptTemplate;

    public CyclePatternService(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            SGRPatternConfig config,
            PromptTemplate cycleEvaluationPromptTemplate) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.config = config;
        this.cycleEvaluationPromptTemplate = cycleEvaluationPromptTemplate;
    }

    /**
     * Evaluates multiple experts using Cycle pattern.
     * Forces LLM to repeat evaluation steps for each expert.
     *
     * @param query          User query with requirements
     * @param expertContexts List of expert contexts to evaluate
     * @return List of expert evaluations
     */
    public List<ExpertEvaluation> evaluateMultipleExperts(
            String query,
            List<AnswerGenerationService.ExpertContext> expertContexts) {

        if (!config.isEnabled() || !config.getCycle().isEnabled()) {
            log.warn("Cycle pattern is disabled, cannot evaluate experts");
            throw new IllegalStateException("Cycle pattern is disabled");
        }

        if (expertContexts == null || expertContexts.isEmpty()) {
            log.warn("No expert contexts provided for cycle pattern evaluation");
            return List.of();
        }

        try {
            String prompt = buildCyclePrompt(query, expertContexts);
            String responseText = callLLM(prompt);
            return parseExpertEvaluationsList(responseText);
        } catch (Exception e) {
            log.error("Failed to evaluate experts with Cycle pattern", e);
            throw new RuntimeException("Failed to evaluate experts with Cycle pattern", e);
        }
    }

    /**
     * Calls LLM with prompt and returns response text.
     */
    private String callLLM(String prompt) {
        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null ||
                response.getResult().getOutput() == null ||
                response.getResult().getOutput().getText() == null) {
            throw new RuntimeException("Empty response from LLM");
        }

        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Blank response text from LLM");
        }
        return text;
    }

    /**
     * Parses list of expert evaluations from LLM response.
     */
    private List<ExpertEvaluation> parseExpertEvaluationsList(String responseText) {
        try {
            // Try to extract JSON array from response
            String jsonText = responseText.trim();

            // Remove markdown code blocks if present
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

            // Parse JSON array
            return objectMapper.readValue(jsonText, new TypeReference<List<ExpertEvaluation>>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse expert evaluations list: {}", responseText, e);
            throw new RuntimeException("Failed to parse expert evaluations list", e);
        }
    }

    /**
     * Builds prompt for Cycle pattern evaluation using PromptTemplate.
     */
    private String buildCyclePrompt(String query, List<AnswerGenerationService.ExpertContext> expertContexts) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("query", query);
        variables.put("expertCount", expertContexts.size());

        // Build experts section
        StringBuilder expertsSection = new StringBuilder();
        for (int i = 0; i < expertContexts.size(); i++) {
            AnswerGenerationService.ExpertContext expert = expertContexts.get(i);
            expertsSection.append("### Expert ").append(i + 1).append("\n");
            expertsSection.append("Name: ").append(expert.name()).append("\n");
            if (expert.email() != null) {
                expertsSection.append("Email: ").append(expert.email()).append("\n");
            }
            if (expert.seniority() != null) {
                expertsSection.append("Seniority: ").append(expert.seniority()).append("\n");
            }
            if (expert.skills() != null && !expert.skills().isEmpty()) {
                expertsSection.append("Skills: ").append(String.join(", ", expert.skills())).append("\n");
            }
            if (expert.projects() != null && !expert.projects().isEmpty()) {
                expertsSection.append("Projects: ").append(String.join(", ", expert.projects())).append("\n");
            }
            expertsSection.append("\n");
        }

        variables.put("expertsSection", expertsSection.toString());

        return cycleEvaluationPromptTemplate.render(variables);
    }
}

