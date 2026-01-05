package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.chat.ChatService;
import com.berdachuk.expertmatch.data.ChatRepository;
import com.berdachuk.expertmatch.security.HeaderBasedUserContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI tools for chat management operations.
 */
@Component
public class ChatManagementTools {

    private final ChatService chatService;
    private final HeaderBasedUserContext userContext;

    public ChatManagementTools(
            ChatService chatService,
            HeaderBasedUserContext userContext
    ) {
        this.chatService = chatService;
        this.userContext = userContext;
    }

    @Tool(description = "Create a new chat session. Use this when the user wants to start a new conversation thread.")
    public ChatInfo createChat(
            @ToolParam(description = "Chat name (optional, defaults to generated name)") String name
    ) {
        String userId = userContext.getUserIdOrAnonymous();
        ChatRepository.Chat chat = chatService.createChat(userId, name);
        return toChatInfo(chat);
    }

    @Tool(description = "List all chat sessions for the current user.")
    public List<ChatInfo> listChats() {
        String userId = userContext.getUserIdOrAnonymous();
        List<ChatRepository.Chat> chats = chatService.listChats(userId);
        return chats.stream()
                .map(this::toChatInfo)
                .collect(Collectors.toList());
    }

    @Tool(description = "Get details for a specific chat by chat ID.")
    public ChatInfo getChat(
            @ToolParam(description = "Chat ID (24-character hex string)") String chatId
    ) {
        return chatService.findChat(chatId)
                .map(this::toChatInfo)
                .orElse(null);
    }

    @Tool(description = "Update chat name. Use this when the user wants to rename a chat.")
    public boolean updateChatName(
            @ToolParam(description = "Chat ID (24-character hex string)") String chatId,
            @ToolParam(description = "New chat name") String name
    ) {
        return chatService.updateChat(chatId, name);
    }

    @Tool(description = "Delete a chat session. Cannot delete the default chat.")
    public boolean deleteChat(
            @ToolParam(description = "Chat ID (24-character hex string)") String chatId
    ) {
        return chatService.deleteChat(chatId);
    }

    @Tool(description = "Get or create the default chat for the current user.")
    public ChatInfo getOrCreateDefaultChat() {
        String userId = userContext.getUserIdOrAnonymous();
        ChatRepository.Chat chat = chatService.getOrCreateDefaultChat(userId);
        return toChatInfo(chat);
    }

    private ChatInfo toChatInfo(ChatRepository.Chat chat) {
        return new ChatInfo(
                chat.id(),
                chat.name(),
                chat.isDefault(),
                chat.createdAt().toString(),
                chat.updatedAt().toString(),
                chat.lastActivityAt().toString()
        );
    }

    /**
     * Chat information DTO for tool responses.
     */
    public record ChatInfo(
            String id,
            String name,
            boolean isDefault,
            String createdAt,
            String updatedAt,
            String lastActivityAt
    ) {
    }
}

