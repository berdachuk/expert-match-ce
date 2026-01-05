package com.berdachuk.expertmatch.chat;

import com.berdachuk.expertmatch.query.ExecutionTracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages conversation history with token counting and summarization.
 * Ensures history fits within context window limits by summarizing older messages when needed.
 */
@Slf4j
@Service
public class ConversationHistoryManager {

    private final ConversationHistoryRepository historyRepository;
    private final TokenCountingService tokenCountingService;
    private final ChatClient chatClient;
    private final PromptTemplate summarizePromptTemplate;

    // Configuration properties with defaults
    private final int maxHistoryTokens;
    private final int maxHistoryMessages;
    private final int maxSummaryTokens;

    public ConversationHistoryManager(
            ConversationHistoryRepository historyRepository,
            TokenCountingService tokenCountingService,
            @Lazy ChatClient chatClient,
            @org.springframework.beans.factory.annotation.Qualifier("summarizeHistoryPromptTemplate") PromptTemplate summarizePromptTemplate,
            @Value("${expertmatch.chat.history.max-tokens:2000}") int maxHistoryTokens,
            @Value("${expertmatch.chat.history.max-messages:10}") int maxHistoryMessages,
            @Value("${expertmatch.chat.history.max-summary-tokens:500}") int maxSummaryTokens) {
        this.historyRepository = historyRepository;
        this.tokenCountingService = tokenCountingService;
        this.chatClient = chatClient;
        this.summarizePromptTemplate = summarizePromptTemplate;
        this.maxHistoryTokens = maxHistoryTokens;
        this.maxHistoryMessages = maxHistoryMessages;
        this.maxSummaryTokens = maxSummaryTokens;
    }

    /**
     * Gets conversation history optimized for context window.
     * Automatically summarizes older messages if history exceeds token limits.
     *
     * @param chatId              Chat ID
     * @param excludeCurrentQuery If true, excludes the most recent message (current query)
     * @param tracer              Optional execution tracer for tracking
     * @return Optimized conversation history within token limits
     */
    public List<ConversationHistoryRepository.ConversationMessage> getOptimizedHistory(
            String chatId,
            boolean excludeCurrentQuery,
            ExecutionTracer tracer) {

        if (tracer != null) {
            tracer.startStep("Load Conversation History", "ConversationHistoryManager", "getOptimizedHistory");
        }

        // Get more messages than needed to allow for summarization
        int fetchLimit = Math.max(maxHistoryMessages * 2, 50); // Fetch up to 50 messages to have enough for summarization
        List<ConversationHistoryRepository.ConversationMessage> allHistory =
                historyRepository.getHistory(chatId, 0, fetchLimit, "sequence_number,desc");

        // Exclude current query if requested
        List<ConversationHistoryRepository.ConversationMessage> history = excludeCurrentQuery && !allHistory.isEmpty()
                ? allHistory.subList(1, allHistory.size())
                : new ArrayList<>(allHistory);

        // Reverse to chronological order (oldest first)
        List<ConversationHistoryRepository.ConversationMessage> chronologicalHistory = new ArrayList<>(history);
        java.util.Collections.reverse(chronologicalHistory);

        if (chronologicalHistory.isEmpty()) {
            log.info("[HISTORY] ChatId: {} - No conversation history found", chatId);
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId, "No history");
            }
            return List.of();
        }

        // Count tokens in current history
        int historyTokens = tokenCountingService.estimateHistoryTokens(chronologicalHistory);

        log.info("[HISTORY] ChatId: {} - Retrieved {} messages, {} tokens (max: {} tokens, {} messages)",
                chatId, chronologicalHistory.size(), historyTokens, maxHistoryTokens, maxHistoryMessages);

        // If history is within limits, return as-is
        if (historyTokens <= maxHistoryTokens && chronologicalHistory.size() <= maxHistoryMessages) {
            log.info("[HISTORY] ChatId: {} - History within limits, using {} messages ({} tokens) for query",
                    chatId, chronologicalHistory.size(), historyTokens);
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId + ", Tokens: " + historyTokens,
                        "Messages: " + chronologicalHistory.size() + " (within limits)");
            }
            return chronologicalHistory;
        }

        // History exceeds limits - need to optimize
        log.info("[HISTORY] ChatId: {} - History exceeds limits ({} tokens, {} messages). Optimizing...",
                chatId, historyTokens, chronologicalHistory.size());

        // Strategy: Keep recent messages, summarize older ones
        List<ConversationHistoryRepository.ConversationMessage> optimizedHistory = optimizeHistory(
                chronologicalHistory, tracer);

        int optimizedTokens = tokenCountingService.estimateHistoryTokens(optimizedHistory);
        log.info("[HISTORY] ChatId: {} - History optimized: {} tokens, {} messages (was: {} tokens, {} messages)",
                chatId, optimizedTokens, optimizedHistory.size(), historyTokens, chronologicalHistory.size());
        log.info("[HISTORY] ChatId: {} - Using optimized history with {} messages ({} tokens) for query",
                chatId, optimizedHistory.size(), optimizedTokens);

        if (tracer != null) {
            tracer.endStep("ChatId: " + chatId + ", Tokens: " + optimizedTokens + " (optimized from " + historyTokens + ")",
                    "Messages: " + optimizedHistory.size() + " (optimized from " + chronologicalHistory.size() + ")");
        }

        return optimizedHistory;
    }

    /**
     * Optimizes history by keeping recent messages and summarizing older ones.
     */
    private List<ConversationHistoryRepository.ConversationMessage> optimizeHistory(
            List<ConversationHistoryRepository.ConversationMessage> history,
            ExecutionTracer tracer) {

        if (history.size() <= maxHistoryMessages) {
            // Within message limit, but may exceed token limit
            // Try to fit within token limit by summarizing oldest messages
            return optimizeByTokens(history, tracer);
        }

        // Split into recent (keep) and old (summarize)
        int keepRecent = maxHistoryMessages / 2; // Keep half of max messages
        int summarizeCount = history.size() - keepRecent;

        List<ConversationHistoryRepository.ConversationMessage> recentMessages =
                history.subList(summarizeCount, history.size());
        List<ConversationHistoryRepository.ConversationMessage> oldMessages =
                history.subList(0, summarizeCount);

        // Summarize old messages
        String summary = summarizeMessages(oldMessages, tracer);
        if (summary == null || summary.isBlank()) {
            // Summarization failed, just keep recent messages
            log.warn("Summarization failed, keeping only recent {} messages", recentMessages.size());
            return recentMessages;
        }

        // Get chatId from messages (all messages should have the same chatId)
        String chatId = !oldMessages.isEmpty() ? oldMessages.get(0).chatId() : null;

        // Create a summary message
        ConversationHistoryRepository.ConversationMessage summaryMessage = createSummaryMessage(summary, chatId);

        // Combine: summary + recent messages
        List<ConversationHistoryRepository.ConversationMessage> optimized = new ArrayList<>();
        optimized.add(summaryMessage);
        optimized.addAll(recentMessages);

        // Check if still exceeds limits after summarization
        int optimizedTokens = tokenCountingService.estimateHistoryTokens(optimized);
        if (optimizedTokens > maxHistoryTokens) {
            // Still too large, recursively optimize
            log.debug("History still exceeds token limit after summarization, further optimizing...");
            return optimizeByTokens(optimized, tracer);
        }

        return optimized;
    }

    /**
     * Optimizes history by token count, summarizing oldest messages until within limit.
     */
    private List<ConversationHistoryRepository.ConversationMessage> optimizeByTokens(
            List<ConversationHistoryRepository.ConversationMessage> history,
            ExecutionTracer tracer) {

        int currentTokens = tokenCountingService.estimateHistoryTokens(history);
        if (currentTokens <= maxHistoryTokens) {
            return history;
        }

        // Need to summarize - find how many oldest messages to summarize
        List<ConversationHistoryRepository.ConversationMessage> optimized = new ArrayList<>(history);
        int summarizeFromIndex = 0;

        while (currentTokens > maxHistoryTokens && summarizeFromIndex < optimized.size() - 1) {
            // Try summarizing from the beginning
            int toSummarize = Math.min(3, optimized.size() - summarizeFromIndex - 1); // Summarize 3 at a time
            if (toSummarize <= 0) {
                break; // Can't summarize more
            }

            List<ConversationHistoryRepository.ConversationMessage> toSummarizeList =
                    optimized.subList(summarizeFromIndex, summarizeFromIndex + toSummarize);
            String summary = summarizeMessages(toSummarizeList, tracer);

            if (summary == null || summary.isBlank()) {
                // Summarization failed, remove oldest messages instead
                log.warn("Summarization failed, removing oldest {} messages", toSummarize);
                optimized.removeAll(toSummarizeList);
                currentTokens = tokenCountingService.estimateHistoryTokens(optimized);
                continue;
            }

            // Get chatId from messages
            String chatId = !toSummarizeList.isEmpty() && toSummarizeList.get(0).chatId() != null
                    ? toSummarizeList.get(0).chatId()
                    : "";

            // Replace summarized messages with summary
            ConversationHistoryRepository.ConversationMessage summaryMessage = createSummaryMessage(summary, chatId);
            optimized.removeAll(toSummarizeList);
            optimized.add(summarizeFromIndex, summaryMessage);

            currentTokens = tokenCountingService.estimateHistoryTokens(optimized);
            summarizeFromIndex++; // Move past the summary
        }

        return optimized;
    }

    /**
     * Summarizes a list of messages using LLM.
     */
    private String summarizeMessages(
            List<ConversationHistoryRepository.ConversationMessage> messages,
            ExecutionTracer tracer) {

        if (messages == null || messages.isEmpty()) {
            return null;
        }

        if (tracer != null) {
            tracer.startStep("Summarize History", "ConversationHistoryManager", "summarizeMessages");
        }

        try {
            // Build history text for summarization
            StringBuilder historyText = new StringBuilder();
            for (ConversationHistoryRepository.ConversationMessage message : messages) {
                String role = "user".equalsIgnoreCase(message.role()) ? "User" : "Assistant";
                historyText.append(role).append(": ").append(message.content()).append("\n");
            }

            // Build prompt
            Map<String, Object> variables = new HashMap<>();
            variables.put("history", historyText.toString());

            String prompt = summarizePromptTemplate.render(variables);

            // Call LLM for summarization
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.warn("Empty response from LLM during summarization");
                if (tracer != null) {
                    tracer.failStep("Summarize History", "ConversationHistoryManager", "summarizeMessages",
                            "Empty LLM response");
                }
                return null;
            }

            String summaryText = response.getResult().getOutput().getText();
            if (summaryText == null) {
                log.warn("Summary text is null from LLM response");
                if (tracer != null) {
                    tracer.failStep("Summarize History", "ConversationHistoryManager", "summarizeMessages",
                            "Summary text is null");
                }
                return null;
            }
            String summary = summaryText.trim();

            // Truncate summary if it exceeds max summary tokens
            int summaryTokens = tokenCountingService.estimateTokens(summary);
            if (summaryTokens > maxSummaryTokens) {
                // Rough truncation: estimate characters for max tokens
                int maxChars = maxSummaryTokens * 4;
                if (summary.length() > maxChars) {
                    summary = summary.substring(0, maxChars).trim() + "...";
                    log.debug("Truncated summary from {} to {} tokens", summaryTokens, maxSummaryTokens);
                }
            }

            log.debug("Summarized {} messages into {} tokens", messages.size(),
                    tokenCountingService.estimateTokens(summary));

            if (tracer != null) {
                tracer.endStep("Messages: " + messages.size(),
                        "Summary: " + tokenCountingService.estimateTokens(summary) + " tokens");
            }

            return summary;

        } catch (Exception e) {
            log.error("Failed to summarize conversation history", e);
            if (tracer != null) {
                tracer.failStep("Summarize History", "ConversationHistoryManager", "summarizeMessages",
                        "Error: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Creates a summary message from summarized text.
     */
    private ConversationHistoryRepository.ConversationMessage createSummaryMessage(String summary, String chatId) {
        // Create a synthetic message representing the summary
        // Use a special ID and sequence number to indicate it's a summary
        return new ConversationHistoryRepository.ConversationMessage(
                "summary-" + System.currentTimeMillis(), // Synthetic ID
                chatId, // Chat ID
                "system", // System message type
                "assistant", // Role
                "[Previous conversation summary] " + summary, // Content with prefix
                -1, // Special sequence number for summaries
                tokenCountingService.estimateTokens(summary), // Token count
                java.time.Instant.now() // Current time
        );
    }
}

