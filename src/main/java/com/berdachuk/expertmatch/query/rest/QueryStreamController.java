package com.berdachuk.expertmatch.query.rest;

import com.berdachuk.expertmatch.chat.service.ChatService;
import com.berdachuk.expertmatch.core.api.ApiMapper;
import com.berdachuk.expertmatch.core.domain.QueryRequest;
import com.berdachuk.expertmatch.core.domain.QueryResponse;
import com.berdachuk.expertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.expertmatch.core.util.ValidationUtils;
import com.berdachuk.expertmatch.query.service.QueryService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller for streaming query endpoints using Server-Sent Events (SSE).
 * Note: SSE endpoints return SseEmitter directly, not wrapped in ResponseEntity.
 * The generated QueryStreamApi interface returns ResponseEntity<Resource>, so we don't implement it.
 * Instead, we use the same path and annotations to ensure proper OpenAPI documentation.
 */
@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "expertmatch.query.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(QueryService.class)
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class QueryStreamController {

    private final QueryService queryService;
    private final ChatService chatService;
    private final ApiMapper apiMapper;
    private final HeaderBasedUserContext userContext;
    private final Validator validator;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Process natural language query with streaming response using Server-Sent Events.
     * Note: Returns SseEmitter directly for proper SSE support.
     * This method handles the /query-stream endpoint defined in the OpenAPI specification.
     */
    @io.swagger.v3.oas.annotations.Operation(
            operationId = "processQueryStream",
            summary = "Process query with streaming response",
            description = "Processes a natural language query and streams the response using Server-Sent Events (SSE).\nEmits events at each processing stage: parsing, retrieving, reranking, generating, and complete.\nIf chatId is not provided, uses or creates the user's default chat for conversation context.\nIf X-User-Id is not provided, an anonymous user will be used.",
            tags = {"Query Streaming"}
    )
    @org.springframework.web.bind.annotation.PostMapping(
            value = "/query-stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public SseEmitter processQueryStream(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @Valid @RequestBody com.berdachuk.expertmatch.api.model.QueryRequest queryRequest,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 minute timeout

        CompletableFuture.runAsync(() -> {
            try {
                // Get user ID from HTTP headers populated by Spring Gateway
                // Use xUserId from parameter or fallback to userContext for anonymous users
                String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();

                // Convert API model to domain model
                QueryRequest domainRequest =
                        apiMapper.toDomainQueryRequest(queryRequest);

                // Validate SGR pattern combinations
                validateSGRPatterns(domainRequest);

                // Get or create default chat if chatId not provided
                String chatId = domainRequest.chatId();
                if (chatId == null || chatId.isBlank()) {
                    var defaultChat = chatService.getOrCreateDefaultChat(userId);
                    chatId = defaultChat.id();
                } else {
                    // Validate chatId format
                    if (!ValidationUtils.isValidId(chatId)) {
                        sendError(emitter, "Invalid chat ID format: " + chatId);
                        return;
                    }

                    // Validate chatId ownership
                    var chat = chatService.findChat(chatId);
                    if (chat.isEmpty()) {
                        sendError(emitter, "Chat not found: " + chatId);
                        return;
                    }
                    if (!chat.get().userId().equals(userId)) {
                        sendError(emitter, "Access denied to chat: " + chatId);
                        return;
                    }
                }

                // Stream processing stages
                sendEvent(emitter, "parsing", "Query parsing started", null);

                // Process query (this will internally handle all stages)
                // For now, we'll process synchronously and emit events
                // In a more advanced implementation, we could make QueryService support callbacks
                QueryResponse domainResponse =
                        queryService.processQuery(domainRequest, chatId, userId);

                sendEvent(emitter, "retrieving", "Retrieval in progress", null);
                sendEvent(emitter, "reranking", "Reranking in progress", null);
                sendEvent(emitter, "generating", "LLM generation in progress", null);

                // Convert domain response to API response for the complete event
                com.berdachuk.expertmatch.api.model.QueryResponse apiResponse =
                        apiMapper.toApiQueryResponse(domainResponse);
                sendEvent(emitter, "complete", "Query processing complete", apiResponse);

                emitter.complete();

            } catch (Exception e) {
                sendError(emitter, "Error processing query: " + e.getMessage());
                emitter.completeWithError(e);
            }
        }, executorService);

        return emitter;
    }

    /**
     * Sends an SSE event.
     */
    private void sendEvent(SseEmitter emitter, String event, String data, Object payload) {
        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name(event != null ? event : "")
                    .data(data != null ? data : "");

            if (payload != null) {
                // For complete event, include the full response
                if (payload instanceof com.berdachuk.expertmatch.api.model.QueryResponse) {
                    eventBuilder.data(payload);
                }
            }

            emitter.send(eventBuilder);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * Sends an error event.
     */
    private void sendError(SseEmitter emitter, String errorMessage) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorMessage != null ? errorMessage : "Unknown error"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * Validates SGR pattern combinations in the domain request.
     * Throws ValidationException if Cascade and Cycle patterns are both enabled.
     */
    private void validateSGRPatterns(QueryRequest domainRequest) {
        java.util.Set<ConstraintViolation<QueryRequest>> violations = validator.validate(domainRequest);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid SGR pattern combination");
            throw new com.berdachuk.expertmatch.core.exception.ValidationException(errorMessage);
        }
    }
}
