/**
 * Chat Management Module
 * <p>
 * Manages chat conversations:
 * - Default chat creation
 * - Chat CRUD operations
 * - Conversation history persistence
 * - Chat-specific memory
 * <p>
 * Exposes:
 * - ChatService (service layer)
 * - ChatController (REST API)
 */
@org.springframework.modulith.ApplicationModule(
        id = "chat",
        displayName = "Chat Management",
        allowedDependencies = {"core", "api"}
)
package com.berdachuk.expertmatch.chat;

