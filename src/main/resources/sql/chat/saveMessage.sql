INSERT INTO expertmatch.conversation_history
(id, chat_id, message_type, role, content, sequence_number, tokens_used, created_at)
VALUES (:id, :chatId, :messageType, :role, :content, :sequenceNumber, :tokensUsed, :createdAt)
