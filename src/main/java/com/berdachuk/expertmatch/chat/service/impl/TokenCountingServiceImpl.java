package com.berdachuk.expertmatch.chat.service.impl;

import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;
import com.berdachuk.expertmatch.chat.service.TokenCountingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for counting tokens in text messages.
 * Uses estimation algorithm: approximately 4 characters per token for English text.
 */
@Slf4j
@Service
public class TokenCountingServiceImpl implements TokenCountingService {

    private static final double CHARS_PER_TOKEN = 4.0;

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Conservative estimate: ~4 characters per token for English text
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    @Override
    public int estimateFormattedMessageTokens(String role, String content) {
        if (content == null) {
            content = "";
        }
        if (role == null) {
            role = "";
        }
        // Format: "Role: content\n"
        String formatted = role + ": " + content + "\n";
        return estimateTokens(formatted);
    }

    @Override
    public int estimateHistoryTokens(List<ConversationHistoryRepository.ConversationMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int totalTokens = 0;
        for (ConversationHistoryRepository.ConversationMessage message : messages) {
            totalTokens += estimateFormattedMessageTokens(message.role(), message.content());
        }
        // Add overhead for section header "Conversation History:\n"
        totalTokens += estimateTokens("Conversation History:\n");
        return totalTokens;
    }

    @Override
    public int estimateSectionTokens(String sectionText) {
        return estimateTokens(sectionText);
    }
}
