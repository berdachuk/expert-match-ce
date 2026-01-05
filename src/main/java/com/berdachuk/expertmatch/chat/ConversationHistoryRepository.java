package com.berdachuk.expertmatch.chat;

import com.berdachuk.expertmatch.data.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for conversation history operations.
 */
@Slf4j
@Repository
public class ConversationHistoryRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ConversationHistoryRepository(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Saves a conversation message.
     */
    public void saveMessage(String chatId, String messageType, String role,
                            String content, int sequenceNumber, Integer tokensUsed) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }
        if (messageType == null || messageType.trim().isEmpty()) {
            throw new IllegalArgumentException("Message type cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (sequenceNumber < 0) {
            throw new IllegalArgumentException("Sequence number must be non-negative, got: " + sequenceNumber);
        }
        if (tokensUsed != null && tokensUsed < 0) {
            throw new IllegalArgumentException("Tokens used must be non-negative, got: " + tokensUsed);
        }

        String id = IdGenerator.generateId();
        Instant now = Instant.now();

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("chatId", chatId);
        params.put("messageType", messageType);
        params.put("role", role);
        params.put("content", content);
        params.put("sequenceNumber", sequenceNumber);
        params.put("tokensUsed", tokensUsed);
        // Convert Instant to Timestamp for PostgreSQL compatibility
        params.put("createdAt", java.sql.Timestamp.from(now));

        String sql = """
                INSERT INTO expertmatch.conversation_history
                (id, chat_id, message_type, role, content, sequence_number, tokens_used, created_at)
                VALUES (:id, :chatId, :messageType, :role, :content, :sequenceNumber, :tokensUsed, :createdAt)
                """;

        try {
            namedJdbcTemplate.update(sql, params);
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            // This can happen if a previous query in the transaction failed
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                // Transaction is aborted - log warning and return silently
                // The message won't be saved, but we don't want to fail the entire request
                log.warn("saveMessage failed due to aborted transaction - message will not be saved. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return; // Return silently - message won't be saved but request can continue
            }
            // Re-throw other SQL errors
            log.error("SQL error during saveMessage: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Graceful degradation for other unexpected errors
            log.warn("saveMessage failed - message will not be saved. Error: {}", e.getMessage());
            log.debug("saveMessage error details", e);
            // Return silently - message won't be saved but request can continue
        }
    }

    /**
     * Gets conversation history for a chat with pagination.
     */
    public List<ConversationMessage> getHistory(String chatId, int page, int size, String sort) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page must be non-negative, got: " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1, got: " + size);
        }
        if (sort == null || sort.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort cannot be null or empty");
        }

        // Parse sort parameter (e.g., "sequence_number,asc")
        String[] sortParts = sort.split(",");
        String sortColumn = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "asc";

        // Validate sort column is safe (prevent SQL injection)
        if (sortColumn == null || sortColumn.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort column cannot be empty");
        }
        // Validate sort direction
        if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc', got: " + sortDirection);
        }

        int offset = page * size;

        String sql = String.format("""
                SELECT id, chat_id, message_type, role, content, sequence_number, tokens_used, created_at
                FROM expertmatch.conversation_history
                WHERE chat_id = :chatId
                ORDER BY %s %s
                LIMIT :size OFFSET :offset
                """, sortColumn, sortDirection.toUpperCase());

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);
        params.put("size", size);
        params.put("offset", offset);

        try {
            return namedJdbcTemplate.query(sql, params, (rs, rowNum) ->
                    new ConversationMessage(
                            rs.getString("id"),
                            rs.getString("chat_id"),
                            rs.getString("message_type"),
                            rs.getString("role"),
                            rs.getString("content"),
                            rs.getInt("sequence_number"),
                            rs.getObject("tokens_used", Integer.class),
                            rs.getTimestamp("created_at").toInstant()
                    )
            );
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("getHistory failed due to aborted transaction - returning empty list. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return List.of(); // Return empty list
            }
            log.error("SQL error during getHistory", e);
            throw e;
        } catch (Exception e) {
            log.warn("getHistory failed - returning empty list. Error: {}", e.getMessage());
            log.debug("getHistory error details", e);
            return List.of(); // Return empty list
        }
    }

    /**
     * Gets total count of messages for a chat.
     */
    public int getTotalMessageCount(String chatId) {
        String sql = """
                SELECT COUNT(*)
                FROM expertmatch.conversation_history
                WHERE chat_id = :chatId
                """;

        Map<String, Object> params = Map.of("chatId", chatId);
        try {
            Integer count = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null ? count : 0;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("getTotalMessageCount failed due to aborted transaction - returning 0. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return 0; // Return default count
            }
            log.error("SQL error during getTotalMessageCount", e);
            throw e;
        } catch (Exception e) {
            log.warn("getTotalMessageCount failed - returning 0. Error: {}", e.getMessage());
            log.debug("getTotalMessageCount error details", e);
            return 0; // Return default count
        }
    }

    /**
     * Deletes all messages for a chat.
     */
    public boolean deleteAllMessages(String chatId) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }

        String sql = """
                DELETE FROM expertmatch.conversation_history
                WHERE chat_id = :chatId
                """;

        Map<String, Object> params = Map.of("chatId", chatId);

        try {
            int deletedCount = namedJdbcTemplate.update(sql, params);
            log.info("Deleted {} messages for chatId: {}", deletedCount, chatId);
            return deletedCount > 0;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("deleteAllMessages failed due to aborted transaction - returning false. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return false; // Return false to indicate failure
            }
            log.error("SQL error during deleteAllMessages: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("deleteAllMessages failed - returning false. Error: {}", e.getMessage());
            log.debug("deleteAllMessages error details", e);
            return false; // Return false to indicate failure
        }
    }

    /**
     * Gets next sequence number for a chat.
     */
    public int getNextSequenceNumber(String chatId) {
        String sql = """
                SELECT COALESCE(MAX(sequence_number), 0) + 1
                FROM expertmatch.conversation_history
                WHERE chat_id = :chatId
                """;

        Map<String, Object> params = Map.of("chatId", chatId);
        try {
            Integer next = namedJdbcTemplate.queryForObject(sql, params, Integer.class);
            return next != null ? next : 1;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("getNextSequenceNumber failed due to aborted transaction - returning default value 1. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return 1; // Return default sequence number
            }
            log.error("SQL error during getNextSequenceNumber", e);
            throw e;
        } catch (Exception e) {
            log.warn("getNextSequenceNumber failed - returning default value 1. Error: {}", e.getMessage());
            log.debug("getNextSequenceNumber error details", e);
            return 1; // Return default sequence number
        }
    }

    /**
     * Conversation message record.
     */
    public record ConversationMessage(
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

