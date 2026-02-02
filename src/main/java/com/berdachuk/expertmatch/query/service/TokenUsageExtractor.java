package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.core.domain.ExecutionTrace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Utility to extract token usage information from ChatResponse.
 * <p>
 * Note: Token usage availability depends on the Spring AI provider implementation.
 * Some providers (like Ollama) may not provide usage information.
 */
@Slf4j
public class TokenUsageExtractor {

    /**
     * Extracts token usage from a ChatResponse.
     * Returns TokenUsage object if available, null otherwise.
     *
     * @param chatResponse The ChatResponse from LLM call
     * @return TokenUsage with input/output/total tokens, or null if not available
     */
    public static ExecutionTrace.TokenUsage extractTokenUsage(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        }

        try {
            // Try to access metadata and usage
            // Spring AI 1.1.1 provides usage through getMetadata().getUsage()
            var metadata = chatResponse.getMetadata();
            if (metadata == null) {
                return null;
            }

            Usage usage = metadata.getUsage();
            if (usage == null) {
                return null;
            }

            // Extract token values using the Usage interface methods
            // Spring AI 1.1.1 uses getPromptTokens()/getCompletionTokens()
            // Note: Some documentation mentions getInputTokens()/getOutputTokens() for newer versions
            Integer promptTokens = usage.getPromptTokens();
            Integer completionTokens = usage.getCompletionTokens();
            Integer totalTokens = usage.getTotalTokens();

            // If we have at least one token value, return usage
            if (promptTokens != null || completionTokens != null || totalTokens != null) {
                return new ExecutionTrace.TokenUsage(
                        promptTokens,      // Maps to inputTokens in our model
                        completionTokens,  // Maps to outputTokens in our model
                        totalTokens
                );
            }
        } catch (Exception e) {
            // Usage information may not be available for all providers
            log.debug("Token usage not available from ChatResponse: {}", e.getMessage());
        }

        return null;
    }
}
