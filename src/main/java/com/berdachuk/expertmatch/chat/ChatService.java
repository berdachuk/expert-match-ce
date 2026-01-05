package com.berdachuk.expertmatch.chat;

import com.berdachuk.expertmatch.data.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for chat management operations.
 */
@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ConversationHistoryRepository historyRepository;

    public ChatService(ChatRepository chatRepository, ConversationHistoryRepository historyRepository) {
        this.chatRepository = chatRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Gets or creates default chat for a user.
     */
    public ChatRepository.Chat getOrCreateDefaultChat(String userId) {
        return chatRepository.findDefaultChat(userId)
                .orElseGet(() -> {
                    ChatRepository.Chat chat = chatRepository.createChat(userId, "Default Chat", true);
                    // If createChat returns null (transaction aborted), throw exception
                    // This allows the error to be handled at a higher level
                    if (chat == null) {
                        throw new RuntimeException("Failed to create default chat due to database transaction error");
                    }
                    return chat;
                });
    }

    /**
     * Creates a new chat.
     */
    @Transactional
    public ChatRepository.Chat createChat(String userId, String name) {
        return chatRepository.createChat(userId, name, false);
    }

    /**
     * Lists all chats for a user.
     */
    public List<ChatRepository.Chat> listChats(String userId) {
        return chatRepository.findAllByUserId(userId);
    }

    /**
     * Finds chat by ID.
     */
    public Optional<ChatRepository.Chat> findChat(String chatId) {
        return chatRepository.findById(chatId);
    }

    /**
     * Updates chat name.
     */
    @Transactional
    public boolean updateChat(String chatId, String name) {
        return chatRepository.updateChatName(chatId, name);
    }

    /**
     * Deletes a chat (cannot delete default chat).
     */
    @Transactional
    public boolean deleteChat(String chatId) {
        // Verify it's not the default chat
        Optional<ChatRepository.Chat> chat = chatRepository.findById(chatId);
        if (chat.isPresent() && chat.get().isDefault()) {
            return false; // Cannot delete default chat
        }
        return chatRepository.deleteChat(chatId);
    }

    /**
     * Updates last activity timestamp.
     */
    @Transactional
    public void updateLastActivity(String chatId) {
        chatRepository.updateLastActivity(chatId);
    }

    /**
     * Clears all conversation history for a chat.
     */
    @Transactional
    public boolean clearHistory(String chatId) {
        // Verify chat exists
        Optional<ChatRepository.Chat> chat = chatRepository.findById(chatId);
        if (chat.isEmpty()) {
            return false; // Chat not found
        }
        return historyRepository.deleteAllMessages(chatId);
    }
}

