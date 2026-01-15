package com.berdachuk.expertmatch.chat.service;

import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;

import java.util.List;

/**
 * Service interface for counting tokens in text messages.
 * Uses estimation algorithm (approximately 4 characters per token for English text).
 */
public interface TokenCountingService {

    /**
     * Estimates the number of tokens in a text string.
     * Uses a simple approximation: ~4 characters per token for English text.
     *
     * @param text The text to count tokens for
     * @return Estimated number of tokens (always >= 0)
     */
    int estimateTokens(String text);

    /**
     * Estimates tokens for a formatted conversation message.
     * Includes formatting overhead (e.g., "User: " prefix).
     *
     * @param role    Message role (user, assistant, system)
     * @param content Message content
     * @return Estimated tokens including formatting
     */
    int estimateFormattedMessageTokens(String role, String content);

    /**
     * Estimates total tokens for a list of conversation messages.
     *
     * @param messages List of conversation messages
     * @return Total estimated tokens
     */
    int estimateHistoryTokens(List<ConversationHistoryRepository.ConversationMessage> messages);

    /**
     * Estimates tokens for a prompt section (e.g., expert info, instructions).
     *
     * @param sectionText The section text
     * @return Estimated tokens
     */
    int estimateSectionTokens(String sectionText);
}
