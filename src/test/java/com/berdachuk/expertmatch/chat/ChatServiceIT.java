package com.berdachuk.expertmatch.chat;

import com.berdachuk.expertmatch.data.ChatRepository;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ChatService.
 * Uses Testcontainers PostgreSQL database.
 *
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
class ChatServiceIT extends BaseIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.conversation_history");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.chat");
    }

    @Test
    void testGetOrCreateDefaultChat() {
        String userId = "test-user-001";

        // Get or create default chat
        ChatRepository.Chat chat = chatService.getOrCreateDefaultChat(userId);

        assertNotNull(chat);
        assertNotNull(chat.id());
        assertEquals(userId, chat.userId());
        assertTrue(chat.isDefault());
        assertNotNull(chat.name());
    }

    @Test
    void testGetOrCreateDefaultChatIdempotent() {
        String userId = "test-user-002";

        // Call twice - should return same chat
        ChatRepository.Chat chat1 = chatService.getOrCreateDefaultChat(userId);
        ChatRepository.Chat chat2 = chatService.getOrCreateDefaultChat(userId);

        assertEquals(chat1.id(), chat2.id());
        assertTrue(chat1.isDefault());
        assertTrue(chat2.isDefault());
    }

    @Test
    void testCreateChat() {
        String userId = "test-user-003";
        String chatName = "My Custom Chat";

        ChatRepository.Chat chat = chatService.createChat(userId, chatName);

        assertNotNull(chat);
        assertNotNull(chat.id());
        assertEquals(userId, chat.userId());
        assertEquals(chatName, chat.name());
        assertFalse(chat.isDefault());
    }

    @Test
    void testGetChatById() {
        String userId = "test-user-004";
        ChatRepository.Chat createdChat = chatService.createChat(userId, "Test Chat");

        Optional<ChatRepository.Chat> foundChat = chatService.findChat(createdChat.id());

        assertTrue(foundChat.isPresent());
        assertEquals(createdChat.id(), foundChat.get().id());
        assertEquals("Test Chat", foundChat.get().name());
    }

    @Test
    void testGetChatByIdNotFound() {
        Optional<ChatRepository.Chat> chat = chatService.findChat("507f1f77bcf86cd799439999");

        assertTrue(chat.isEmpty());
    }

    @Test
    void testGetUserChats() {
        String userId = "test-user-008";

        // Ensure default chat exists (it's created on first access)
        ChatRepository.Chat defaultChat = chatService.getOrCreateDefaultChat(userId);
        assertNotNull(defaultChat);

        // Create multiple chats
        ChatRepository.Chat chat1 = chatService.createChat(userId, "Chat 1");
        ChatRepository.Chat chat2 = chatService.createChat(userId, "Chat 2");
        ChatRepository.Chat chat3 = chatService.createChat(userId, "Chat 3");

        List<ChatRepository.Chat> chats = chatService.listChats(userId);

        assertNotNull(chats);
        assertTrue(chats.size() >= 3, "Should have at least 3 custom chats, but got: " + chats.size());

        // Verify all created chats are in the list
        assertTrue(chats.stream().anyMatch(c -> c.id().equals(chat1.id())));
        assertTrue(chats.stream().anyMatch(c -> c.id().equals(chat2.id())));
        assertTrue(chats.stream().anyMatch(c -> c.id().equals(chat3.id())));

        // Verify default chat is in the list
        assertTrue(chats.stream().anyMatch(c -> c.id().equals(defaultChat.id())));
    }

    @Test
    void testUpdateChat() {
        String userId = "test-user-009";
        ChatRepository.Chat chat = chatService.createChat(userId, "Original Name");

        boolean updated = chatService.updateChat(chat.id(), "Updated Name");

        assertTrue(updated);

        Optional<ChatRepository.Chat> found = chatService.findChat(chat.id());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().name());
    }

    @Test
    void testDeleteChat() {
        String userId = "test-user-010";
        ChatRepository.Chat chat = chatService.createChat(userId, "To Be Deleted");

        boolean deleted = chatService.deleteChat(chat.id());

        assertTrue(deleted);

        Optional<ChatRepository.Chat> found = chatService.findChat(chat.id());
        assertTrue(found.isEmpty());
    }

    @Test
    void testDeleteDefaultChatShouldFail() {
        String userId = "test-user-011";
        ChatRepository.Chat defaultChat = chatService.getOrCreateDefaultChat(userId);

        // Should not be able to delete default chat - returns false
        boolean deleted = chatService.deleteChat(defaultChat.id());
        assertFalse(deleted);

        // Chat should still exist
        Optional<ChatRepository.Chat> found = chatService.findChat(defaultChat.id());
        assertTrue(found.isPresent());
    }
}

