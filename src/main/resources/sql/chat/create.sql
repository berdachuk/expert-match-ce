INSERT INTO expertmatch.chat 
(id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count)
VALUES (:id, :userId, :name, :isDefault, :createdAt, :updatedAt, :lastActivityAt, :messageCount)
