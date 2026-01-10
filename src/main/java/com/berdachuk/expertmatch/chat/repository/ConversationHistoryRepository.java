package com.berdachuk.expertmatch.chat.repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for conversation history operations.
 */
public interface ConversationHistoryRepository {

    /**
     * Saves a conversation message to the database.
     *
     * @param chatId         The unique identifier of the chat
     * @param messageType    The type of message (e.g., "user", "assistant", "system")
     * @param role           The role of the message sender (e.g., "user", "assistant", "system")
     * @param content        The message content
     * @param sequenceNumber The sequence number of the message in the conversation
     * @param tokensUsed     The number of tokens used by this message (null for user messages)
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    void saveMessage(String chatId, String messageType, String role,
                     String content, int sequenceNumber, Integer tokensUsed);

    /**
     * Retrieves conversation history for a chat with pagination and sorting.
     *
     * @param chatId The unique identifier of the chat
     * @param page   The page number (0-based)
     * @param size   The number of messages per page
     * @param sort   The sort order (e.g., "sequence_number,desc")
     * @return List of conversation messages, empty list if none found
     */
    List<ConversationMessage> getHistory(String chatId, int page, int size, String sort);

    /**
     * Gets the total number of messages in a chat.
     *
     * @param chatId The unique identifier of the chat
     * @return Total number of messages in the chat
     */
    int getTotalMessageCount(String chatId);

    /**
     * Deletes all messages for a chat.
     *
     * @param chatId The unique identifier of the chat
     * @return true if messages were deleted, false if chat not found or no messages exist
     */
    boolean deleteAllMessages(String chatId);

    /**
     * Gets the next sequence number for a chat.
     * Returns 0 if no messages exist for the chat.
     *
     * @param chatId The unique identifier of the chat
     * @return The next sequence number (0-based), or 0 if no messages exist
     */
    int getNextSequenceNumber(String chatId);

    /**
     * Conversation message record.
     */
    record ConversationMessage(
            String id,
            String chatId,
            String messageType,
            String role,
            String content,
            int sequenceNumber,
            Integer tokensUsed,
            Instant createdAt
    ) {
    }
}
