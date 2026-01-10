SELECT id, user_id, name, is_default, created_at, updated_at, last_activity_at, message_count
FROM expertmatch.chat
WHERE user_id = :userId
ORDER BY last_activity_at DESC
