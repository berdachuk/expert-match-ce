package com.berdachuk.expertmatch.chat.repository.impl;

import com.berdachuk.expertmatch.chat.domain.Chat;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Row mapper for Chat entity.
 * Maps ResultSet rows to Chat records.
 * <p>
 * This mapper handles timestamp conversion and null-safe operations
 * for chat-related queries.
 */
@Component
public class ChatMapper implements RowMapper<Chat> {

    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Chat(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getBoolean("is_default"),
                mapTimestamp(rs, "created_at"),
                mapTimestamp(rs, "updated_at"),
                mapTimestamp(rs, "last_activity_at"),
                rs.getInt("message_count")
        );
    }

    /**
     * Maps a timestamp column to Instant, handling null values.
     */
    private Instant mapTimestamp(ResultSet rs, String columnName) throws SQLException {
        var timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
