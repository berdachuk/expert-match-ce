package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.AnswerGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for expert evaluation using Cascade pattern.
 * Forces LLM to follow predefined reasoning steps in sequence.
 */
@Slf4j
@Service
public class ExpertEvaluationService {
    private final StructuredOutputHelper structuredOutputHelper;
    private final SGRPatternConfig config;
    private final PromptTemplate cascadeEvaluationPromptTemplate;

    public ExpertEvaluationService(
            StructuredOutputHelper structuredOutputHelper,
            SGRPatternConfig config,
            PromptTemplate cascadeEvaluationPromptTemplate) {
        this.structuredOutputHelper = structuredOutputHelper;
        this.config = config;
        this.cascadeEvaluationPromptTemplate = cascadeEvaluationPromptTemplate;
    }

    /**
     * Evaluates an expert using Cascade pattern.
     * Forces LLM to follow: Expert Summary → Skill Match Analysis → Experience Assessment → Recommendation.
     *
     * @param query         User query with requirements
     * @param expertContext Expert context information
     * @return Structured expert evaluation
     */
    public ExpertEvaluation evaluateWithCascade(
            String query,
            AnswerGenerationService.ExpertContext expertContext) {

        if (!config.isEnabled() || !config.getCascade().isEnabled()) {
            log.warn("Cascade pattern is disabled, cannot evaluate expert");
            throw new IllegalStateException("Cascade pattern is disabled");
        }

        try {
            String prompt = buildCascadePrompt(query, expertContext);
            return structuredOutputHelper.callWithStructuredOutput(prompt, ExpertEvaluation.class);
        } catch (StructuredOutputHelper.StructuredOutputException e) {
            log.error("Failed to evaluate expert with Cascade pattern", e);
            throw new RuntimeException("Failed to evaluate expert with Cascade pattern", e);
        }
    }

    /**
     * Builds prompt for Cascade pattern evaluation using PromptTemplate.
     */
    private String buildCascadePrompt(String query, AnswerGenerationService.ExpertContext expertContext) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("query", query);

        // Build expert information section
        StringBuilder expertInfo = new StringBuilder();
        expertInfo.append("Name: ").append(expertContext.name()).append("\n");
        if (expertContext.email() != null) {
            expertInfo.append("Email: ").append(expertContext.email()).append("\n");
        }
        if (expertContext.seniority() != null) {
            expertInfo.append("Seniority: ").append(expertContext.seniority()).append("\n");
        }
        if (expertContext.skills() != null && !expertContext.skills().isEmpty()) {
            expertInfo.append("Skills: ").append(String.join(", ", expertContext.skills())).append("\n");
        }
        if (expertContext.projects() != null && !expertContext.projects().isEmpty()) {
            expertInfo.append("Projects: ").append(String.join(", ", expertContext.projects())).append("\n");
        }

        variables.put("expertInfo", expertInfo.toString());

        return cascadeEvaluationPromptTemplate.render(variables);
    }
}

