SELECT COALESCE(MAX(sequence_number), 0) + 1
FROM expertmatch.conversation_history
WHERE chat_id = :chatId
