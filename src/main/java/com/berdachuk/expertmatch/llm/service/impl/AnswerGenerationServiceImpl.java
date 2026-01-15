package com.berdachuk.expertmatch.llm.service;

import com.berdachuk.expertmatch.llm.sgr.CyclePatternService;
import com.berdachuk.expertmatch.llm.sgr.ExpertEvaluation;
import com.berdachuk.expertmatch.llm.sgr.ExpertEvaluationService;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;
import com.berdachuk.expertmatch.query.service.ModelInfoExtractor;
import com.berdachuk.expertmatch.query.service.TokenUsageExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating expert recommendations using LLM orchestration.
 */
@Slf4j
@Service
public class AnswerGenerationServiceImpl implements AnswerGenerationService {
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ExpertEvaluationService expertEvaluationService;
    private final CyclePatternService cyclePatternService;
    private final PromptTemplate ragPromptTemplate;
    private final Environment environment;

    public AnswerGenerationServiceImpl(@Lazy ChatClient chatClient, PromptTemplate ragPromptTemplate, ChatModel chatModel, Environment environment) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.ragPromptTemplate = ragPromptTemplate;
        this.environment = environment;
        this.expertEvaluationService = null;
        this.cyclePatternService = null;
    }

    @Autowired(required = false)
    public AnswerGenerationServiceImpl(
            @Lazy ChatClient chatClient,
            PromptTemplate ragPromptTemplate,
            ChatModel chatModel,
            Environment environment,
            ExpertEvaluationService expertEvaluationService,
            CyclePatternService cyclePatternService) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.ragPromptTemplate = ragPromptTemplate;
        this.environment = environment;
        this.expertEvaluationService = expertEvaluationService;
        this.cyclePatternService = cyclePatternService;
    }

    /**
     * Generates answer text with expert recommendations using RAG pattern.
     */
    @Override
    public String generateAnswer(String query, List<ExpertContext> expertContexts) {
        return generateAnswer(query, expertContexts, "expert_search");
    }

    /**
     * Generates answer text with expert recommendations using RAG pattern.
     *
     * @param query          User query
     * @param expertContexts Expert contexts
     * @param intent         Query intent (expert_search, team_formation, rfp_response)
     */
    @Override
    public String generateAnswer(String query, List<ExpertContext> expertContexts, String intent) {
        return generateAnswer(query, expertContexts, intent, List.of());
    }

    /**
     * Generates answer text with expert recommendations using RAG pattern and conversation context.
     *
     * @param query               User query
     * @param expertContexts      Expert contexts
     * @param intent              Query intent (expert_search, team_formation, rfp_response)
     * @param conversationHistory Recent conversation history for context
     */
    @Override
    public String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                                 List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory) {
        return generateAnswer(query, expertContexts, intent, conversationHistory, false, false);
    }

    /**
     * Generates answer text with expert recommendations using RAG pattern or SGR patterns.
     *
     * @param query               User query
     * @param expertContexts      Expert contexts
     * @param intent              Query intent (expert_search, team_formation, rfp_response)
     * @param conversationHistory Recent conversation history for context
     * @param useCascadePattern   If true, use Cascade pattern for structured evaluation
     * @param useCyclePattern     If true, use Cycle pattern for multiple expert evaluations
     */
    @Override
    public String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                                 List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory,
                                 boolean useCascadePattern,
                                 boolean useCyclePattern) {
        return generateAnswer(query, expertContexts, intent, conversationHistory, useCascadePattern, useCyclePattern, null);
    }

    /**
     * Generates answer text with expert recommendations using RAG pattern or SGR patterns with optional execution tracing.
     *
     * @param query               User query
     * @param expertContexts      Expert contexts
     * @param intent              Query intent (expert_search, team_formation, rfp_response)
     * @param conversationHistory Recent conversation history for context
     * @param useCascadePattern   If true, use Cascade pattern for structured evaluation
     * @param useCyclePattern     If true, use Cycle pattern for multiple expert evaluations
     * @param tracer              Optional execution tracer for tracking
     */
    @Override
    public String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                                 List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory,
                                 boolean useCascadePattern,
                                 boolean useCyclePattern,
                                 ExecutionTracer tracer) {

        // Use Cycle pattern if enabled and multiple experts
        if (useCyclePattern && cyclePatternService != null && expertContexts != null && expertContexts.size() > 1) {
            try {
                if (tracer != null) {
                    tracer.startStep("Cycle Pattern Evaluation", "CyclePatternService", "evaluateMultipleExperts");
                }
                List<ExpertEvaluation> evaluations = cyclePatternService.evaluateMultipleExperts(query, expertContexts);
                String answer = formatCyclePatternAnswer(evaluations, expertContexts);
                if (tracer != null) {
                    // Note: CyclePatternService should track its own LLM calls internally
                    // For now, we just track the overall step
                    tracer.endStep("Query: " + query + ", Experts: " + expertContexts.size(),
                            "Evaluations: " + evaluations.size());
                }
                return answer;
            } catch (Exception e) {
                log.warn("Cycle pattern failed, falling back to RAG pattern", e);
                if (tracer != null) {
                    tracer.failStep("Cycle Pattern Evaluation", "CyclePatternService", "evaluateMultipleExperts", "Error: " + e.getMessage());
                }
                // Fall through to RAG pattern
            }
        }

        // Use Cascade pattern if enabled and single expert
        if (useCascadePattern && expertEvaluationService != null && expertContexts != null && expertContexts.size() == 1) {
            try {
                if (tracer != null) {
                    tracer.startStep("Cascade Pattern Evaluation", "ExpertEvaluationService", "evaluateWithCascade");
                }
                ExpertEvaluation evaluation = expertEvaluationService.evaluateWithCascade(query, expertContexts.get(0));
                String answer = formatCascadePatternAnswer(evaluation, expertContexts.get(0));
                if (tracer != null) {
                    // Note: ExpertEvaluationService should track its own LLM calls internally
                    // For now, we just track the overall step
                    tracer.endStep("Query: " + query + ", Expert: " + expertContexts.get(0).name(),
                            "Evaluation completed");
                }
                return answer;
            } catch (Exception e) {
                log.warn("Cascade pattern failed, falling back to RAG pattern", e);
                if (tracer != null) {
                    tracer.failStep("Cascade Pattern Evaluation", "ExpertEvaluationService", "evaluateWithCascade", "Error: " + e.getMessage());
                }
                // Fall through to RAG pattern
            }
        }

        // Default: Use RAG pattern
        if (tracer != null) {
            tracer.startStep("Generate Answer (RAG)", "AnswerGenerationService", "generateAnswer");
        }
        String prompt = buildRAGPrompt(query, expertContexts, intent, conversationHistory);

        ChatResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null ||
                response.getResult().getOutput() == null ||
                response.getResult().getOutput().getText() == null) {
            log.warn("Empty response from LLM, returning empty answer");
            if (tracer != null) {
                tracer.failStep("Generate Answer (RAG)", "AnswerGenerationService", "generateAnswer", "Empty response from LLM");
            }
            return "";
        }

        String answer = response.getResult().getOutput().getText();
        if (answer == null || answer.isBlank()) {
            log.warn("Blank answer text from LLM, returning empty answer");
            return "";
        }
        if (tracer != null) {
            String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
            com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
            tracer.endStepWithLLM("Query: " + query + ", Experts: " + (expertContexts != null ? expertContexts.size() : 0),
                    "Answer: " + (answer != null ? answer.length() : 0) + " characters",
                    modelInfo, tokenUsage);
        }

        return answer;
    }

    /**
     * Formats answer from Cascade pattern evaluation.
     */
    private String formatCascadePatternAnswer(ExpertEvaluation evaluation, ExpertContext expert) {
        StringBuilder answer = new StringBuilder();

        answer.append("## ").append(expert.name()).append("\n\n");

        answer.append("### Summary\n");
        answer.append(evaluation.expertSummary()).append("\n\n");

        answer.append("### Skill Match Analysis\n");
        if (evaluation.skillMatchAnalysis() != null) {
            answer.append("- Must-have skills match: ").append(evaluation.skillMatchAnalysis().mustHaveMatchScore()).append("/10\n");
            answer.append("- Nice-to-have skills match: ").append(evaluation.skillMatchAnalysis().niceToHaveMatchScore()).append("/10\n");
            if (!evaluation.skillMatchAnalysis().missingSkills().isEmpty()) {
                answer.append("- Missing skills: ").append(String.join(", ", evaluation.skillMatchAnalysis().missingSkills())).append("\n");
            }
            if (!evaluation.skillMatchAnalysis().strengthSkills().isEmpty()) {
                answer.append("- Strength skills: ").append(String.join(", ", evaluation.skillMatchAnalysis().strengthSkills())).append("\n");
            }
        }
        answer.append("\n");

        answer.append("### Experience Assessment\n");
        if (evaluation.experienceAssessment() != null) {
            answer.append("- Relevant projects: ").append(evaluation.experienceAssessment().relevantProjectsCount()).append("\n");
            answer.append("- Domain experience: ").append(evaluation.experienceAssessment().domainExperienceYears()).append(" years\n");
            answer.append("- Customer/Industry match: ").append(evaluation.experienceAssessment().customerIndustryMatch() ? "Yes" : "No").append("\n");
            answer.append("- Seniority match: ").append(evaluation.experienceAssessment().seniorityMatch() ? "Yes" : "No").append("\n");
        }
        answer.append("\n");

        answer.append("### Recommendation\n");
        if (evaluation.recommendation() != null) {
            answer.append("- Type: ").append(evaluation.recommendation().recommendationType()).append("\n");
            answer.append("- Confidence: ").append(evaluation.recommendation().confidenceScore()).append("%\n");
            answer.append("- Rationale: ").append(evaluation.recommendation().rationale()).append("\n");
        }

        return answer.toString();
    }

    /**
     * Formats answer from Cycle pattern evaluation.
     */
    private String formatCyclePatternAnswer(List<ExpertEvaluation> evaluations, List<ExpertContext> experts) {
        StringBuilder answer = new StringBuilder();

        answer.append("## Expert Evaluations\n\n");

        for (int i = 0; i < Math.min(evaluations.size(), experts.size()); i++) {
            ExpertEvaluation evaluation = evaluations.get(i);
            ExpertContext expert = experts.get(i);

            answer.append("### ").append(i + 1).append(". ").append(expert.name()).append("\n\n");

            answer.append("**Summary**: ").append(evaluation.expertSummary()).append("\n\n");

            if (evaluation.skillMatchAnalysis() != null) {
                answer.append("**Skill Match**: Must-have: ").append(evaluation.skillMatchAnalysis().mustHaveMatchScore())
                        .append("/10, Nice-to-have: ").append(evaluation.skillMatchAnalysis().niceToHaveMatchScore()).append("/10\n");
            }

            if (evaluation.recommendation() != null) {
                answer.append("**Recommendation**: ").append(evaluation.recommendation().recommendationType())
                        .append(" (Confidence: ").append(evaluation.recommendation().confidenceScore()).append("%)\n");
                answer.append("**Rationale**: ").append(evaluation.recommendation().rationale()).append("\n");
            }

            answer.append("\n");
        }

        return answer.toString();
    }

    /**
     * Builds comprehensive RAG prompt with expert contexts.
     */
    private String buildRAGPrompt(String query, List<ExpertContext> expertContexts, String intent) {
        return buildRAGPrompt(query, expertContexts, intent, List.of());
    }

    /**
     * Builds comprehensive RAG prompt with expert contexts and conversation history using PromptTemplate.
     */
    private String buildRAGPrompt(String query, List<ExpertContext> expertContexts, String intent,
                                  List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory) {
        Map<String, Object> variables = new HashMap<>();

        // Build conversation history section
        StringBuilder conversationHistorySection = new StringBuilder();
        boolean hasConversationHistory = !conversationHistory.isEmpty();
        if (hasConversationHistory) {
            int historyMessages = conversationHistory.size();
            log.info("[PROMPT] Including conversation history in prompt: {} messages", historyMessages);
            conversationHistorySection.append("## Conversation History\n");
            conversationHistorySection.append("Previous messages in this conversation:\n\n");
            for (com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage message : conversationHistory) {
                if ("user".equals(message.role())) {
                    conversationHistorySection.append("User: ").append(message.content()).append("\n");
                } else if ("assistant".equals(message.role())) {
                    conversationHistorySection.append("Assistant: ").append(message.content()).append("\n");
                }
            }
            conversationHistorySection.append("\n");
        } else {
            log.debug("[PROMPT] No conversation history - prompt will be built without context");
        }
        variables.put("conversationHistorySection", conversationHistorySection.toString());

        // User query
        variables.put("query", query);

        // Build expert information section
        StringBuilder expertInfoSection = new StringBuilder();
        int expertCount = expertContexts != null ? expertContexts.size() : 0;
        if (expertCount == 0) {
            expertInfoSection.append("## Expert Information\n");
            expertInfoSection.append("No experts found matching the requirements.\n");
            expertInfoSection.append("Please suggest alternative search criteria or explain why no matches were found.\n");
        } else {
            expertInfoSection.append("## Expert Information\n");
            expertInfoSection.append("The following ").append(expertCount).append(" expert(s) have been identified as potential matches:\n\n");

            for (int i = 0; i < expertContexts.size(); i++) {
                ExpertContext expert = expertContexts.get(i);
                expertInfoSection.append("### Expert ").append(i + 1).append(": ").append(expert.name()).append("\n");

                if (expert.email() != null) {
                    expertInfoSection.append("- **Email**: ").append(expert.email()).append("\n");
                }

                if (expert.seniority() != null) {
                    expertInfoSection.append("- **Seniority**: ").append(expert.seniority()).append("\n");
                }

                if (expert.skills() != null && !expert.skills().isEmpty()) {
                    expertInfoSection.append("- **Skills**: ").append(String.join(", ", expert.skills())).append("\n");
                }

                if (expert.projects() != null && !expert.projects().isEmpty()) {
                    expertInfoSection.append("- **Relevant Projects**: ").append(String.join(", ", expert.projects())).append("\n");
                }

                if (expert.metadata() != null && !expert.metadata().isEmpty()) {
                    if (expert.metadata().containsKey("matchScore")) {
                        expertInfoSection.append("- **Match Score**: ").append(expert.metadata().get("matchScore")).append("\n");
                    }
                    if (expert.metadata().containsKey("relevanceScore")) {
                        expertInfoSection.append("- **Relevance Score**: ").append(expert.metadata().get("relevanceScore")).append("\n");
                    }
                }

                expertInfoSection.append("\n");
            }
        }
        variables.put("expertInfoSection", expertInfoSection.toString());

        // Build instructions section based on intent
        // Note: Instructions are built dynamically based on intent, which is acceptable
        // as they represent business logic that varies by use case
        StringBuilder instructionsSection = new StringBuilder();
        instructionsSection.append("## Instructions\n");

        switch (intent != null ? intent : "expert_search") {
            case "team_formation" -> {
                instructionsSection.append("Provide a direct team formation recommendation that:\n");
                instructionsSection.append("1. Directly analyzes the team composition needs\n");
                instructionsSection.append("2. Recommends optimal team structure with role assignments\n");
                instructionsSection.append("3. Highlights each expert's role and contribution\n");
                instructionsSection.append("4. Identifies any skill gaps or missing roles\n");
                instructionsSection.append("5. Suggests team formation strategy\n");
                instructionsSection.append("Start immediately with useful information. Do NOT include thank you messages or verbose introductions.\n");
            }
            case "rfp_response" -> {
                instructionsSection.append("Provide a direct RFP response recommendation that:\n");
                instructionsSection.append("1. Directly analyzes RFP requirements\n");
                instructionsSection.append("2. Matches experts to specific RFP sections/requirements\n");
                instructionsSection.append("3. Highlights relevant experience for each expert\n");
                instructionsSection.append("4. Identifies proposal strengths\n");
                instructionsSection.append("5. Suggests proposal structure and expert assignments\n");
                instructionsSection.append("Start immediately with useful information. Do NOT include thank you messages or verbose introductions.\n");
            }
            default -> {
                instructionsSection.append("Provide a direct, useful answer that:\n");
                if (hasConversationHistory) {
                    instructionsSection.append("1. Considers the conversation history when answering\n");
                    instructionsSection.append("2. Provides contextual answers that build upon previous exchanges\n");
                    instructionsSection.append("3. If the query is a follow-up, reference previous context appropriately\n");
                    instructionsSection.append("4. Directly presents the expert recommendations without verbose introductions\n");
                    instructionsSection.append("5. Highlights key strengths of each recommended expert\n");
                    instructionsSection.append("6. Explains why these experts are good matches\n");
                    instructionsSection.append("7. Mentions any gaps or limitations if applicable\n");
                    instructionsSection.append("8. Provides actionable next steps\n");
                } else {
                    instructionsSection.append("1. Directly presents the expert recommendations without verbose introductions\n");
                    instructionsSection.append("2. Highlights key strengths of each recommended expert\n");
                    instructionsSection.append("3. Explains why these experts are good matches\n");
                    instructionsSection.append("4. Mentions any gaps or limitations if applicable\n");
                    instructionsSection.append("5. Provides actionable next steps\n");
                }
            }
        }

        variables.put("instructionsSection", instructionsSection.toString());

        return ragPromptTemplate.render(variables);
    }

}
