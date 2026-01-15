package com.berdachuk.expertmatch.llm.service;

import com.berdachuk.expertmatch.query.service.ExecutionTracer;

import java.util.List;
import java.util.Map;

/**
 * Service interface for answer generation operations.
 */
public interface AnswerGenerationService {
    /**
     * Generates an answer using LLM with expert recommendations.
     *
     * @param query          The user's query
     * @param expertContexts List of expert contexts with their details
     * @return Generated answer text
     */
    String generateAnswer(String query, List<ExpertContext> expertContexts);

    /**
     * Generates an answer using LLM with expert recommendations and intent.
     *
     * @param query          The user's query
     * @param expertContexts List of expert contexts with their details
     * @param intent         The query intent (e.g., "expert_search", "team_formation", "rfp_response")
     * @return Generated answer text
     */
    String generateAnswer(String query, List<ExpertContext> expertContexts, String intent);

    /**
     * Generates an answer using LLM with expert recommendations, intent, and conversation history.
     *
     * @param query               The user's query
     * @param expertContexts      List of expert contexts with their details
     * @param intent              The query intent (e.g., "expert_search", "team_formation", "rfp_response")
     * @param conversationHistory Recent conversation history for context
     * @return Generated answer text
     */
    String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                          List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory);

    /**
     * Generates an answer using LLM with expert recommendations, intent, conversation history, and SGR patterns.
     *
     * @param query               The user's query
     * @param expertContexts      List of expert contexts with their details
     * @param intent              The query intent (e.g., "expert_search", "team_formation", "rfp_response")
     * @param conversationHistory Recent conversation history for context
     * @param useCascadePattern   If true, uses Cascade pattern for answer generation
     * @param useCyclePattern     If true, uses Cycle pattern for answer generation
     * @return Generated answer text
     */
    String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                          List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory,
                          boolean useCascadePattern,
                          boolean useCyclePattern);

    /**
     * Generates an answer using LLM with expert recommendations, intent, conversation history, SGR patterns, and execution tracing.
     *
     * @param query               The user's query
     * @param expertContexts      List of expert contexts with their details
     * @param intent              The query intent (e.g., "expert_search", "team_formation", "rfp_response")
     * @param conversationHistory Recent conversation history for context
     * @param useCascadePattern   If true, uses Cascade pattern for answer generation
     * @param useCyclePattern     If true, uses Cycle pattern for answer generation
     * @param tracer              Optional execution tracer for tracking answer generation steps
     * @return Generated answer text
     */
    String generateAnswer(String query, List<ExpertContext> expertContexts, String intent,
                          List<com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository.ConversationMessage> conversationHistory,
                          boolean useCascadePattern,
                          boolean useCyclePattern,
                          ExecutionTracer tracer);

    /**
     * Expert context for answer generation.
     */
    record ExpertContext(
            String expertId,
            String name,
            String email,
            String seniority,
            List<String> skills,
            List<String> projects,
            Map<String, Object> metadata
    ) {
    }
}
