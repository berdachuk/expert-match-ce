package com.berdachuk.expertmatch.chat.repository;

import com.berdachuk.expertmatch.chat.domain.Chat;

import java.util.List;
import java.util.Optional;


/**
 * Repository interface for chat operations.
 */
public interface ChatRepository {

    /**
     * Creates a new chat in the database.
     *
     * @param userId    The unique identifier of the user
     * @param name      The name of the chat
     * @param isDefault Whether this chat is the default chat for the user
     * @return The created chat, or null if creation failed due to transaction error
     */
    Chat createChat(String userId, String name, boolean isDefault);

    /**
     * Finds the default chat for a user.
     *
     * @param userId The unique identifier of the user
     * @return Optional containing the default chat if found, empty otherwise
     */
    Optional<Chat> findDefaultChat(String userId);

    /**
     * Finds a chat by its unique identifier.
     *
     * @param chatId The unique identifier of the chat
     * @return Optional containing the chat if found, empty otherwise
     */
    Optional<Chat> findById(String chatId);

    /**
     * Finds all chats for a user.
     *
     * @param userId The unique identifier of the user
     * @return List of chats for the user, empty list if none found
     */
    List<Chat> findAllByUserId(String userId);

    /**
     * Updates the name of a chat.
     *
     * @param chatId The unique identifier of the chat
     * @param name   The new name for the chat
     * @return true if the chat was updated, false if chat not found
     */
    boolean updateChatName(String chatId, String name);

    /**
     * Deletes a chat from the database.
     *
     * @param chatId The unique identifier of the chat
     * @return true if the chat was deleted, false if chat not found
     */
    boolean deleteChat(String chatId);

    /**
     * Updates the last activity timestamp for a chat.
     *
     * @param chatId The unique identifier of the chat
     */
    void updateLastActivity(String chatId);

}
