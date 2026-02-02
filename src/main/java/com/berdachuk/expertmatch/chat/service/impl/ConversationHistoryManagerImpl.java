package com.berdachuk.expertmatch.chat.service.impl;

import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;
import com.berdachuk.expertmatch.chat.service.ConversationHistoryManager;
import com.berdachuk.expertmatch.chat.service.TokenCountingService;
import com.berdachuk.expertmatch.core.service.ExecutionTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for managing conversation history with token counting and summarization.
 * Ensures history fits within context window limits by summarizing older messages when needed.
 */
@Slf4j
@Service
public class ConversationHistoryManagerImpl implements ConversationHistoryManager {

    private final ConversationHistoryRepository historyRepository;
    private final TokenCountingService tokenCountingService;
    private final PromptTemplate summarizeHistoryPromptTemplate;
    private final ChatClient chatClient;

    @Value("${expertmatch.chat.history.max-tokens:2000}")
    private int maxTokens;

    @Value("${expertmatch.chat.history.max-messages:10}")
    private int maxMessages;

    @Value("${expertmatch.chat.history.max-summary-tokens:500}")
    private int maxSummaryTokens;

    public ConversationHistoryManagerImpl(
            ConversationHistoryRepository historyRepository,
            TokenCountingService tokenCountingService,
            @Qualifier("summarizeHistoryPromptTemplate") PromptTemplate summarizeHistoryPromptTemplate,
            ChatClient chatClient) {
        this.historyRepository = historyRepository;
        this.tokenCountingService = tokenCountingService;
        this.summarizeHistoryPromptTemplate = summarizeHistoryPromptTemplate;
        this.chatClient = chatClient;
    }

    @Override
    public List<ConversationHistoryRepository.ConversationMessage> getOptimizedHistory(
            String chatId,
            boolean excludeCurrentQuery,
            ExecutionTracer tracer) {

        if (tracer != null) {
            tracer.startStep("Load Conversation History", "ConversationHistoryManager", "getOptimizedHistory");
        }

        try {
            // Fetch messages from database (up to 50)
            List<ConversationHistoryRepository.ConversationMessage> allMessages =
                    historyRepository.getHistory(chatId, 0, 50, "sequence_number,desc");

            if (allMessages.isEmpty()) {
                if (tracer != null) {
                    tracer.endStep("ChatId: " + chatId, "No messages found");
                }
                return List.of();
            }

            // Exclude current query if requested (most recent message)
            List<ConversationHistoryRepository.ConversationMessage> messages = new ArrayList<>(allMessages);
            if (excludeCurrentQuery && !messages.isEmpty()) {
                messages.remove(0); // Remove most recent message
            }

            if (messages.isEmpty()) {
                if (tracer != null) {
                    tracer.endStep("ChatId: " + chatId, "No messages after excluding current query");
                }
                return List.of();
            }

            // Count tokens for all messages
            int totalTokens = tokenCountingService.estimateHistoryTokens(messages);
            int messageCount = messages.size();

            // Check if within limits
            if (totalTokens <= maxTokens && messageCount <= maxMessages) {
                if (tracer != null) {
                    tracer.endStep("ChatId: " + chatId,
                            "Within limits: " + messageCount + " messages, " + totalTokens + " tokens");
                }
                return messages;
            }

            // Exceeds limits - optimize history
            log.info("Conversation history exceeds limits ({} messages, {} tokens). Optimizing...",
                    messageCount, totalTokens);

            if (tracer != null) {
                tracer.startStep("Optimize History", "ConversationHistoryManager", "optimizeHistory");
            }

            List<ConversationHistoryRepository.ConversationMessage> optimized =
                    optimizeHistory(messages, tracer);

            if (tracer != null) {
                int optimizedTokens = tokenCountingService.estimateHistoryTokens(optimized);
                tracer.endStep("ChatId: " + chatId,
                        "Optimized: " + optimized.size() + " messages, " + optimizedTokens + " tokens");
            }

            return optimized;

        } finally {
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId, "History loaded and optimized");
            }
        }
    }

    /**
     * Optimizes conversation history by summarizing older messages.
     * Keeps recent messages and summarizes older ones to fit within token limits.
     */
    private List<ConversationHistoryRepository.ConversationMessage> optimizeHistory(
            List<ConversationHistoryRepository.ConversationMessage> messages,
            ExecutionTracer tracer) {

        // Keep recent messages (half of max-messages)
        int keepRecentCount = Math.max(1, maxMessages / 2);
        List<ConversationHistoryRepository.ConversationMessage> recentMessages =
                messages.subList(Math.max(0, messages.size() - keepRecentCount), messages.size());
        List<ConversationHistoryRepository.ConversationMessage> olderMessages =
                messages.subList(0, Math.max(0, messages.size() - keepRecentCount));

        if (olderMessages.isEmpty()) {
            // No older messages to summarize, return recent messages
            return recentMessages;
        }

        // Summarize older messages
        String summary = summarizeMessages(olderMessages, tracer);

        // Create synthetic summary message
        ConversationHistoryRepository.ConversationMessage summaryMessage =
                new ConversationHistoryRepository.ConversationMessage(
                        null, // No ID for synthetic message
                        recentMessages.isEmpty() ? olderMessages.get(0).chatId() : recentMessages.get(0).chatId(),
                        "system",
                        "system",
                        "[Previous conversation summary] " + summary,
                        recentMessages.isEmpty() ? 0 : recentMessages.get(0).sequenceNumber() - 1,
                        null, // Tokens will be counted
                        null // No timestamp for synthetic message
                );

        // Combine summary + recent messages
        List<ConversationHistoryRepository.ConversationMessage> optimized = new ArrayList<>();
        optimized.add(summaryMessage);
        optimized.addAll(recentMessages);

        // Check if still within limits
        int optimizedTokens = tokenCountingService.estimateHistoryTokens(optimized);
        if (optimizedTokens <= maxTokens && optimized.size() <= maxMessages) {
            return optimized;
        }

        // Still exceeds limits - recursively optimize
        log.warn("Optimized history still exceeds limits ({} messages, {} tokens). Recursively optimizing...",
                optimized.size(), optimizedTokens);
        return optimizeHistory(optimized, tracer);
    }

    /**
     * Summarizes a list of conversation messages using LLM.
     */
    private String summarizeMessages(
            List<ConversationHistoryRepository.ConversationMessage> messages,
            ExecutionTracer tracer) {

        if (tracer != null) {
            tracer.startStep("Summarize Messages", "ConversationHistoryManager", "summarizeMessages");
        }

        try {
            // Format messages for summarization
            StringBuilder historyText = new StringBuilder();
            for (ConversationHistoryRepository.ConversationMessage message : messages) {
                historyText.append(message.role()).append(": ").append(message.content()).append("\n");
            }

            // Build prompt with history
            Map<String, Object> variables = new HashMap<>();
            variables.put("history", historyText.toString());

            String promptText = summarizeHistoryPromptTemplate.render(variables);

            // Call LLM for summarization
            String summary = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            if (summary == null || summary.trim().isEmpty()) {
                log.warn("LLM returned empty summary, using fallback");
                summary = "Previous conversation about expert matching.";
            }

            // Limit summary tokens
            int summaryTokens = tokenCountingService.estimateTokens(summary);
            if (summaryTokens > maxSummaryTokens) {
                log.warn("Summary exceeds max tokens ({} > {}), truncating...", summaryTokens, maxSummaryTokens);
                // Truncate to approximate max tokens (rough estimate)
                int maxChars = maxSummaryTokens * 4; // ~4 chars per token
                if (summary.length() > maxChars) {
                    summary = summary.substring(0, maxChars) + "...";
                }
            }

            if (tracer != null) {
                tracer.endStep("Messages: " + messages.size(),
                        "Summary: " + tokenCountingService.estimateTokens(summary) + " tokens");
            }

            return summary;

        } catch (Exception e) {
            log.error("Error summarizing conversation history", e);
            if (tracer != null) {
                tracer.endStep("Messages: " + messages.size(), "Error: " + e.getMessage());
            }
            // Fallback to simple summary
            return "Previous conversation about expert matching.";
        }
    }
}
