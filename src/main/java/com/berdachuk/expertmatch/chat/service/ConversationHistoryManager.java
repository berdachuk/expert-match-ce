package com.berdachuk.expertmatch.chat.service;

import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;
import com.berdachuk.expertmatch.core.service.ExecutionTracer;

import java.util.List;

/**
 * Service interface for managing conversation history with token counting and summarization.
 * Ensures history fits within context window limits by summarizing older messages when needed.
 */
public interface ConversationHistoryManager {

    /**
     * Gets conversation history optimized for context window.
     * Automatically summarizes older messages if history exceeds token limits.
     *
     * @param chatId              Chat ID
     * @param excludeCurrentQuery If true, excludes the most recent message (current query)
     * @param tracer              Optional execution tracer for tracking
     * @return Optimized conversation history within token limits
     */
    List<ConversationHistoryRepository.ConversationMessage> getOptimizedHistory(
            String chatId,
            boolean excludeCurrentQuery,
            ExecutionTracer tracer);
}
