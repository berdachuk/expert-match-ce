UPDATE expertmatch.chat
SET last_activity_at = CURRENT_TIMESTAMP
WHERE id = :chatId
