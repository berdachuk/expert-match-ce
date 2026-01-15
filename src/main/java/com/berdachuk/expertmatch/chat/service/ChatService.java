package com.berdachuk.expertmatch.chat.service;

import com.berdachuk.expertmatch.chat.domain.Chat;

import java.util.List;
import java.util.Optional;


/**
 * Service interface for chat operations.
 */
public interface ChatService {

    /**
     * Gets or creates the default chat for a user.
     * If a default chat already exists, returns it. Otherwise, creates a new default chat.
     *
     * @param userId The unique identifier of the user
     * @return The default chat for the user (never null)
     * @throws RuntimeException if chat creation fails due to database transaction error
     */
    Chat getOrCreateDefaultChat(String userId);

    /**
     * Creates a new chat for a user.
     *
     * @param userId The unique identifier of the user
     * @param name   The name of the chat
     * @return The created chat
     */
    Chat createChat(String userId, String name);

    /**
     * Lists all chats for a user.
     *
     * @param userId The unique identifier of the user
     * @return List of chats for the user, empty list if none found
     */
    List<Chat> listChats(String userId);

    /**
     * Finds a chat by its unique identifier.
     *
     * @param chatId The unique identifier of the chat
     * @return Optional containing the chat if found, empty otherwise
     */
    Optional<Chat> findChat(String chatId);

    /**
     * Updates the name of a chat.
     *
     * @param chatId The unique identifier of the chat
     * @param name   The new name for the chat
     * @return true if the chat was updated, false if chat not found
     */
    boolean updateChat(String chatId, String name);

    /**
     * Deletes a chat.
     * Cannot delete the default chat.
     *
     * @param chatId The unique identifier of the chat
     * @return true if the chat was deleted, false if chat not found or is default chat
     */
    boolean deleteChat(String chatId);

    /**
     * Updates the last activity timestamp for a chat.
     *
     * @param chatId The unique identifier of the chat
     */
    void updateLastActivity(String chatId);

    /**
     * Clears all conversation history for a chat.
     *
     * @param chatId The unique identifier of the chat
     * @return true if history was cleared, false if chat not found
     */
    boolean clearHistory(String chatId);
}
