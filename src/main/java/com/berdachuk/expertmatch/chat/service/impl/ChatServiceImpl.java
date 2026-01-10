package com.berdachuk.expertmatch.chat.service.impl;

import com.berdachuk.expertmatch.chat.domain.Chat;
import com.berdachuk.expertmatch.chat.repository.ChatRepository;
import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;
import com.berdachuk.expertmatch.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for chat management operations.
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ConversationHistoryRepository historyRepository;

    @Override
    public Chat getOrCreateDefaultChat(String userId) {
        return chatRepository.findDefaultChat(userId)
                .orElseGet(() -> {
                    Chat chat = chatRepository.createChat(userId, "Default Chat", true);
                    // If createChat returns null (transaction aborted), throw exception
                    // This allows the error to be handled at a higher level
                    if (chat == null) {
                        throw new RuntimeException("Failed to create default chat due to database transaction error");
                    }
                    return chat;
                });
    }

    @Override
    @Transactional
    public Chat createChat(String userId, String name) {
        return chatRepository.createChat(userId, name, false);
    }

    @Override
    public List<Chat> listChats(String userId) {
        return chatRepository.findAllByUserId(userId);
    }

    @Override
    public Optional<Chat> findChat(String chatId) {
        return chatRepository.findById(chatId);
    }

    @Override
    @Transactional
    public boolean updateChat(String chatId, String name) {
        return chatRepository.updateChatName(chatId, name);
    }

    @Override
    @Transactional
    public boolean deleteChat(String chatId) {
        // Verify it's not the default chat
        Optional<Chat> chat = chatRepository.findById(chatId);
        if (chat.isPresent() && chat.get().isDefault()) {
            return false; // Cannot delete default chat
        }
        return chatRepository.deleteChat(chatId);
    }

    @Override
    @Transactional
    public void updateLastActivity(String chatId) {
        chatRepository.updateLastActivity(chatId);
    }

    @Override
    @Transactional
    public boolean clearHistory(String chatId) {
        // Verify chat exists
        Optional<Chat> chat = chatRepository.findById(chatId);
        if (chat.isEmpty()) {
            return false; // Chat not found
        }
        return historyRepository.deleteAllMessages(chatId);
    }
}
