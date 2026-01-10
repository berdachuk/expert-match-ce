package com.berdachuk.expertmatch.chat.service;

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
 * <p>
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
        com.berdachuk.expertmatch.chat.domain.Chat chat = chatService.getOrCreateDefaultChat(userId);

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
        com.berdachuk.expertmatch.chat.domain.Chat chat1 = chatService.getOrCreateDefaultChat(userId);
        com.berdachuk.expertmatch.chat.domain.Chat chat2 = chatService.getOrCreateDefaultChat(userId);

        assertEquals(chat1.id(), chat2.id());
        assertTrue(chat1.isDefault());
        assertTrue(chat2.isDefault());
    }

    @Test
    void testCreateChat() {
        String userId = "test-user-003";
        String chatName = "My Custom Chat";

        com.berdachuk.expertmatch.chat.domain.Chat chat = chatService.createChat(userId, chatName);

        assertNotNull(chat);
        assertNotNull(chat.id());
        assertEquals(userId, chat.userId());
        assertEquals(chatName, chat.name());
        assertFalse(chat.isDefault());
    }

    @Test
    void testGetChatById() {
        String userId = "test-user-004";
        com.berdachuk.expertmatch.chat.domain.Chat createdChat = chatService.createChat(userId, "Test Chat");

        Optional<com.berdachuk.expertmatch.chat.domain.Chat> foundChat = chatService.findChat(createdChat.id());

        assertTrue(foundChat.isPresent());
        assertEquals(createdChat.id(), foundChat.get().id());
        assertEquals("Test Chat", foundChat.get().name());
    }

    @Test
    void testGetChatByIdNotFound() {
        Optional<com.berdachuk.expertmatch.chat.domain.Chat> chat = chatService.findChat("507f1f77bcf86cd799439999");

        assertTrue(chat.isEmpty());
    }

    @Test
    void testGetUserChats() {
        String userId = "test-user-008";

        // Ensure default chat exists (it's created on first access)
        com.berdachuk.expertmatch.chat.domain.Chat defaultChat = chatService.getOrCreateDefaultChat(userId);
        assertNotNull(defaultChat);

        // Create multiple chats
        com.berdachuk.expertmatch.chat.domain.Chat chat1 = chatService.createChat(userId, "Chat 1");
        com.berdachuk.expertmatch.chat.domain.Chat chat2 = chatService.createChat(userId, "Chat 2");
        com.berdachuk.expertmatch.chat.domain.Chat chat3 = chatService.createChat(userId, "Chat 3");

        List<com.berdachuk.expertmatch.chat.domain.Chat> chats = chatService.listChats(userId);

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
        com.berdachuk.expertmatch.chat.domain.Chat chat = chatService.createChat(userId, "Original Name");

        boolean updated = chatService.updateChat(chat.id(), "Updated Name");

        assertTrue(updated);

        Optional<com.berdachuk.expertmatch.chat.domain.Chat> found = chatService.findChat(chat.id());
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().name());
    }

    @Test
    void testDeleteChat() {
        String userId = "test-user-010";
        com.berdachuk.expertmatch.chat.domain.Chat chat = chatService.createChat(userId, "To Be Deleted");

        boolean deleted = chatService.deleteChat(chat.id());

        assertTrue(deleted);

        Optional<com.berdachuk.expertmatch.chat.domain.Chat> found = chatService.findChat(chat.id());
        assertTrue(found.isEmpty());
    }

    @Test
    void testDeleteDefaultChatShouldFail() {
        String userId = "test-user-011";
        com.berdachuk.expertmatch.chat.domain.Chat defaultChat = chatService.getOrCreateDefaultChat(userId);

        // Should not be able to delete default chat - returns false
        boolean deleted = chatService.deleteChat(defaultChat.id());
        assertFalse(deleted);

        // Chat should still exist
        Optional<com.berdachuk.expertmatch.chat.domain.Chat> found = chatService.findChat(defaultChat.id());
        assertTrue(found.isPresent());
    }
}

