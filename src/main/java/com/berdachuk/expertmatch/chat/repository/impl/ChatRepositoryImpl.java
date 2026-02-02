package com.berdachuk.expertmatch.chat.repository.impl;

import com.berdachuk.expertmatch.chat.domain.Chat;
import com.berdachuk.expertmatch.chat.repository.ChatRepository;
import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
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
public class ChatRepositoryImpl implements ChatRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatMapper chatMapper;

    @InjectSql("/sql/chat/create.sql")
    private String createSql;

    @InjectSql("/sql/chat/findDefaultChat.sql")
    private String findDefaultChatSql;

    @InjectSql("/sql/chat/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/chat/findAllByUserId.sql")
    private String findAllByUserIdSql;

    @InjectSql("/sql/chat/updateChatName.sql")
    private String updateChatNameSql;

    @InjectSql("/sql/chat/deleteChat.sql")
    private String deleteChatSql;

    @InjectSql("/sql/chat/updateLastActivity.sql")
    private String updateLastActivitySql;

    public ChatRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate, ChatMapper chatMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chatMapper = chatMapper;
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

        try {
            namedJdbcTemplate.update(createSql, params);
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

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        try {
            List<Chat> results = namedJdbcTemplate.query(findDefaultChatSql, params, chatMapper);
            return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
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

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            List<Chat> results = namedJdbcTemplate.query(findByIdSql, params, chatMapper);
            return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
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

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        try {
            return namedJdbcTemplate.query(findAllByUserIdSql, params, chatMapper);
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
            // Log the actual PostgreSQL error details
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof java.sql.SQLException sqlEx) {
                    log.error("findAllByUserId SQL error - SQLState: {}, ErrorCode: {}, Message: {}",
                            sqlEx.getSQLState(), sqlEx.getErrorCode(), sqlEx.getMessage());
                }
                cause = cause.getCause();
            }
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

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);
        params.put("name", name);

        try {
            return namedJdbcTemplate.update(updateChatNameSql, params) > 0;
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

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            return namedJdbcTemplate.update(deleteChatSql, params) > 0;
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

        Map<String, Object> params = new HashMap<>();
        params.put("chatId", chatId);

        try {
            namedJdbcTemplate.update(updateLastActivitySql, params);
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

}

