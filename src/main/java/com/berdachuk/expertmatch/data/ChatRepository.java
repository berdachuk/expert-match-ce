package com.berdachuk.expertmatch.data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for chat management operations.
 */
@Slf4j
@Repository
public class ChatRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public ChatRepository(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * Creates a new chat.
     */
    public Chat createChat(String userId, String name, boolean isDefault) {
        // Validate input parameters before executing query
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        String id = IdGenerator.generateId();
        Instant now = Instant.now();

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("userId", userId);
        params.put("name", name != null ? name : "Default Chat");
        params.put("isDefault", isDefault);
        // Convert Instant to Timestamp for PostgreSQL compatibility
        params.put("createdAt", java.sql.Timestamp.from(now));
        params.put("updatedAt", java.sql.Timestamp.from(now));
        params.put("lastActivityAt", java.sql.Timestamp.from(now));
        params.put("messageCount", 0);

        String sql = """
                    INSERT INTO expertmatch.chat 
                (id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count)
                VALUES (:id, :userId, :name, :isDefault, :createdAt, :updatedAt, :lastActivityAt, :messageCount)
                """;

        try {
            namedJdbcTemplate.update(sql, params);
            return new Chat(id, userId, name, isDefault, now, now, now, 0);
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("createChat failed due to aborted transaction - returning null. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return null; // Return null to indicate failure
            }
            log.error("SQL error during createChat: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("createChat failed - returning null. Error: {}", e.getMessage());
            log.debug("createChat error details", e);
            return null; // Return null to indicate failure
        }
    }

    /**
     * Finds default chat for a user.
     */
    public Optional<Chat> findDefaultChat(String userId) {
        // Validate input parameters before executing query
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        String sql = """
                SELECT id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count
                    FROM expertmatch.chat
                WHERE user_id = :userId AND is_default = TRUE
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        try {
            List<Chat> results = namedJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToChat(rs));
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("findDefaultChat failed due to aborted transaction - returning empty. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return Optional.empty(); // Return empty to allow graceful degradation
            }
            log.error("SQL error during findDefaultChat: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("findDefaultChat failed - returning empty. Error: {}", e.getMessage());
            log.debug("findDefaultChat error details", e);
            return Optional.empty(); // Return empty to allow graceful degradation
        }
    }

    /**
     * Finds chat by ID.
     */
    public Optional<Chat> findById(String chatId) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }

        String sql = """
                SELECT id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count
                    FROM expertmatch.chat
                WHERE id = :chatId
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            List<Chat> results = namedJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToChat(rs));
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("findById failed due to aborted transaction - returning empty. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return Optional.empty(); // Return empty to allow graceful degradation
            }
            log.error("SQL error during findById: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("findById failed - returning empty. Error: {}", e.getMessage());
            log.debug("findById error details", e);
            return Optional.empty(); // Return empty to allow graceful degradation
        }
    }

    /**
     * Lists all chats for a user.
     */
    public List<Chat> findAllByUserId(String userId) {
        // Validate input parameters before executing query
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        String sql = """
                SELECT id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count
                    FROM expertmatch.chat
                WHERE user_id = :userId
                ORDER BY last_activity_at DESC
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        try {
            return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToChat(rs));
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("findAllByUserId failed due to aborted transaction - returning empty list. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return List.of(); // Return empty list to allow graceful degradation
            }
            log.error("SQL error during findAllByUserId: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("findAllByUserId failed - returning empty list. Error: {}", e.getMessage());
            log.debug("findAllByUserId error details", e);
            return List.of(); // Return empty list to allow graceful degradation
        }
    }

    /**
     * Updates chat name.
     */
    public boolean updateChatName(String chatId, String name) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }
        if (name == null) {
            throw new IllegalArgumentException("Chat name cannot be null");
        }

        String sql = """
                    UPDATE expertmatch.chat
                SET name = :name, updated_at = CURRENT_TIMESTAMP
                WHERE id = :chatId
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);
        params.put("name", name);

        try {
            return namedJdbcTemplate.update(sql, params) > 0;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("updateChatName failed due to aborted transaction - returning false. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return false; // Return false to indicate failure
            }
            log.error("SQL error during updateChatName: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("updateChatName failed - returning false. Error: {}", e.getMessage());
            log.debug("updateChatName error details", e);
            return false; // Return false to indicate failure
        }
    }

    /**
     * Deletes a chat.
     */
    public boolean deleteChat(String chatId) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }

        String sql = "DELETE FROM expertmatch.chat WHERE id = :chatId AND is_default = FALSE";
        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            return namedJdbcTemplate.update(sql, params) > 0;
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("deleteChat failed due to aborted transaction - returning false. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return false; // Return false to indicate failure
            }
            log.error("SQL error during deleteChat: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("deleteChat failed - returning false. Error: {}", e.getMessage());
            log.debug("deleteChat error details", e);
            return false; // Return false to indicate failure
        }
    }

    /**
     * Updates last activity timestamp.
     */
    public void updateLastActivity(String chatId) {
        // Validate input parameters before executing query
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }

        String sql = """
                    UPDATE expertmatch.chat
                SET last_activity_at = CURRENT_TIMESTAMP
                WHERE id = :chatId
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            namedJdbcTemplate.update(sql, params);
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("updateLastActivity failed due to aborted transaction - ignoring. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return; // Return silently - activity update is not critical
            }
            log.error("SQL error during updateLastActivity: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.warn("updateLastActivity failed - ignoring. Error: {}", e.getMessage());
            log.debug("updateLastActivity error details", e);
            // Return silently - activity update is not critical
        }
    }

    private Chat mapRowToChat(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Chat(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getBoolean("is_default"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("last_activity_at").toInstant(),
                rs.getInt("message_count")
        );
    }

    /**
     * Chat entity record.
     */
    public record Chat(
            String id,
            String userId,
            String name,
            boolean isDefault,
            Instant createdAt,
            Instant updatedAt,
            Instant lastActivityAt,
            int messageCount
    ) {
    }
}

