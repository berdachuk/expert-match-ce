SELECT id, chat_id, message_type, role, content, sequence_number, tokens_used, created_at
FROM expertmatch.conversation_history
WHERE chat_id = :chatId
ORDER BY {sortColumn} {sortDirection}
LIMIT :size OFFSET :offset
