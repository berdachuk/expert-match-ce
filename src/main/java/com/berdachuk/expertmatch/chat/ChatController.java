package com.berdachuk.expertmatch.chat;

import com.berdachuk.expertmatch.api.ApiMapper;
import com.berdachuk.expertmatch.api.ChatsApi;
import com.berdachuk.expertmatch.api.model.*;
import com.berdachuk.expertmatch.data.ChatRepository;
import com.berdachuk.expertmatch.security.HeaderBasedUserContext;
import com.berdachuk.expertmatch.util.ValidationUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for chat management endpoints.
 * Implements generated API interface from OpenAPI specification.
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController implements ChatsApi {

    private final ChatService chatService;
    private final ConversationHistoryRepository historyRepository;
    private final ApiMapper apiMapper;
    private final HeaderBasedUserContext userContext;

    public ChatController(ChatService chatService, ConversationHistoryRepository historyRepository, ApiMapper apiMapper, HeaderBasedUserContext userContext) {
        this.chatService = chatService;
        this.historyRepository = historyRepository;
        this.apiMapper = apiMapper;
        this.userContext = userContext;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * Create a new chat.
     */
    @Override
    public ResponseEntity<Chat> createChat(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail,
            @Valid @RequestBody(required = false) CreateChatRequest createChatRequest) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();
        String name = createChatRequest != null && createChatRequest.getName() != null
                ? createChatRequest.getName() : null;

        ChatRepository.Chat domainChat = chatService.createChat(userId, name);
        Chat apiChat = apiMapper.toApiChat(domainChat);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiChat);
    }

    /**
     * List all chats for the authenticated user.
     */
    @Override
    public ResponseEntity<ChatListResponse> listChats(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();
        List<ChatRepository.Chat> domainChats = chatService.listChats(userId);

        ChatListResponse response = new ChatListResponse()
                .chats(apiMapper.toApiChatList(domainChats))
                .total(domainChats.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get chat details.
     */
    @Override
    public ResponseEntity<Chat> getChat(
            @jakarta.validation.constraints.Pattern(regexp = "^[0-9a-fA-F]{24}$") @PathVariable("chatId") String chatId,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();

        // Validate chatId format
        if (!ValidationUtils.isValidId(chatId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Invalid chat ID format: " + chatId
            );
        }

        Optional<ChatRepository.Chat> domainChat = chatService.findChat(chatId);

        if (domainChat.isEmpty()) {
            throw new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                    "CHAT_NOT_FOUND",
                    "Chat not found: " + chatId
            );
        }

        // Verify chat belongs to user
        if (!domainChat.get().userId().equals(userId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Access denied to chat: " + chatId
            );
        }

        Chat apiChat = apiMapper.toApiChat(domainChat.get());
        return ResponseEntity.ok(apiChat);
    }

    /**
     * Update chat (e.g., rename).
     */
    @Override
    public ResponseEntity<Chat> updateChat(
            @jakarta.validation.constraints.Pattern(regexp = "^[0-9a-fA-F]{24}$") @PathVariable("chatId") String chatId,
            @Valid @RequestBody UpdateChatRequest updateChatRequest,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();

        // Validate chatId format
        if (!ValidationUtils.isValidId(chatId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Invalid chat ID format: " + chatId
            );
        }

        Optional<ChatRepository.Chat> domainChat = chatService.findChat(chatId);

        if (domainChat.isEmpty()) {
            throw new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                    "CHAT_NOT_FOUND",
                    "Chat not found: " + chatId
            );
        }

        // Verify chat belongs to user
        if (!domainChat.get().userId().equals(userId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Access denied to chat: " + chatId
            );
        }

        boolean updated = chatService.updateChat(chatId, updateChatRequest.getName());
        if (!updated) {
            throw new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                    "CHAT_NOT_FOUND",
                    "Chat not found after update: " + chatId
            );
        }

        return chatService.findChat(chatId)
                .map(apiMapper::toApiChat)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                        "CHAT_NOT_FOUND",
                        "Chat not found after update: " + chatId
                ));
    }

    /**
     * Delete a chat.
     */
    @Override
    public ResponseEntity<DeleteChatResponse> deleteChat(
            @jakarta.validation.constraints.Pattern(regexp = "^[0-9a-fA-F]{24}$") @PathVariable("chatId") String chatId,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();

        // Validate chatId format
        if (!ValidationUtils.isValidId(chatId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Invalid chat ID format: " + chatId
            );
        }

        Optional<ChatRepository.Chat> domainChat = chatService.findChat(chatId);

        if (domainChat.isEmpty()) {
            throw new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                    "CHAT_NOT_FOUND",
                    "Chat not found: " + chatId
            );
        }

        // Verify chat belongs to user
        if (!domainChat.get().userId().equals(userId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Access denied to chat: " + chatId
            );
        }

        // Check if trying to delete default chat
        if (domainChat.get().isDefault()) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Cannot delete default chat"
            );
        }

        boolean deleted = chatService.deleteChat(chatId);
        if (!deleted) {
            throw new com.berdachuk.expertmatch.exception.RetrievalException(
                    "DELETE_CHAT_ERROR",
                    "Failed to delete chat: " + chatId
            );
        }

        DeleteChatResponse response = new DeleteChatResponse()
                .success(true)
                .message("Chat deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get conversation history for a chat.
     */
    @Override
    public ResponseEntity<ConversationHistoryResponse> getHistory(
            @jakarta.validation.constraints.Pattern(regexp = "^[0-9a-fA-F]{24}$") @PathVariable("chatId") String chatId,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail,
            @jakarta.validation.constraints.Min(0) @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) @Valid @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @Valid @RequestParam(value = "sort", required = false, defaultValue = "sequence_number,asc") String sort) {
        // Use xUserId from parameter or fallback to userContext
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();

        // Validate chatId format
        if (!ValidationUtils.isValidId(chatId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Invalid chat ID format: " + chatId
            );
        }

        // Set defaults for pagination
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortParam = sort != null ? sort : "sequence_number,asc";

        // Validate pagination parameters
        if (pageNum < 0) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Page number must be non-negative: " + pageNum
            );
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Page size must be between 1 and 100: " + pageSize
            );
        }

        Optional<ChatRepository.Chat> domainChat = chatService.findChat(chatId);

        if (domainChat.isEmpty()) {
            throw new com.berdachuk.expertmatch.exception.ResourceNotFoundException(
                    "CHAT_NOT_FOUND",
                    "Chat not found: " + chatId
            );
        }

        // Verify chat belongs to user
        if (!domainChat.get().userId().equals(userId)) {
            throw new com.berdachuk.expertmatch.exception.ValidationException(
                    "Access denied to chat: " + chatId
            );
        }

        List<ConversationHistoryRepository.ConversationMessage> domainMessages =
                historyRepository.getHistory(chatId, pageNum, pageSize, sortParam);

        // Calculate pagination info
        int totalElements = historyRepository.getTotalMessageCount(chatId);
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        PageInfo pageInfo = new PageInfo()
                .number(pageNum)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages);

        ConversationHistoryResponse response = new ConversationHistoryResponse()
                .chatId(chatId)
                .messages(apiMapper.toApiConversationMessageList(domainMessages))
                .page(pageInfo);

        return ResponseEntity.ok(response);
    }
}
