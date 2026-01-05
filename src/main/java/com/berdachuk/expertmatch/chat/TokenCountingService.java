package com.berdachuk.expertmatch.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for counting tokens in text messages.
 * Uses estimation algorithm (approximately 4 characters per token for English text).
 * <p>
 * Note: This is an approximation. For more accurate counting, consider using
 * model-specific tokenizers (e.g., tiktoken for OpenAI models).
 */
@Slf4j
@Service
public class TokenCountingService {

    /**
     * Estimates the number of tokens in a text string.
     * Uses a simple approximation: ~4 characters per token for English text.
     * <p>
     * This is a conservative estimate that works reasonably well for most cases.
     * Actual token counts may vary based on:
     * - Language (non-English may have different ratios)
     * - Model tokenizer (different models tokenize differently)
     * - Special characters and whitespace
     *
     * @param text The text to count tokens for
     * @return Estimated number of tokens (always >= 0)
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple estimation: ~4 characters per token
        // This is a conservative estimate that works reasonably well for English text
        // For more accuracy, consider using model-specific tokenizers
        int estimatedTokens = (int) Math.ceil(text.length() / 4.0);
        return Math.max(0, estimatedTokens);
    }

    /**
     * Estimates tokens for a formatted conversation message.
     * Includes formatting overhead (e.g., "User: " prefix).
     *
     * @param role    Message role (user, assistant, system)
     * @param content Message content
     * @return Estimated tokens including formatting
     */
    public int estimateFormattedMessageTokens(String role, String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // Format: "User: " or "Assistant: " + content
        String prefix = "user".equalsIgnoreCase(role) ? "User: " : "Assistant: ";
        String formatted = prefix + content + "\n";
        return estimateTokens(formatted);
    }

    /**
     * Estimates total tokens for a list of conversation messages.
     *
     * @param messages List of conversation messages
     * @return Total estimated tokens
     */
    public int estimateHistoryTokens(java.util.List<ConversationHistoryRepository.ConversationMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        int totalTokens = 0;
        for (ConversationHistoryRepository.ConversationMessage message : messages) {
            totalTokens += estimateFormattedMessageTokens(message.role(), message.content());
        }

        // Add overhead for section header: "## Conversation History\nPrevious messages in this conversation:\n\n"
        totalTokens += estimateTokens("## Conversation History\nPrevious messages in this conversation:\n\n");

        return totalTokens;
    }

    /**
     * Estimates tokens for a prompt section (e.g., expert info, instructions).
     *
     * @param sectionText The section text
     * @return Estimated tokens
     */
    public int estimateSectionTokens(String sectionText) {
        return estimateTokens(sectionText);
    }
}

