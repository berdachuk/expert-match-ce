package com.berdachuk.expertmatch.query;

import com.berdachuk.expertmatch.api.ApiMapper;
import com.berdachuk.expertmatch.api.QueryApi;
import com.berdachuk.expertmatch.chat.ChatService;
import com.berdachuk.expertmatch.exception.ResourceNotFoundException;
import com.berdachuk.expertmatch.exception.ValidationException;
import com.berdachuk.expertmatch.security.HeaderBasedUserContext;
import com.berdachuk.expertmatch.util.ValidationUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for query endpoints.
 * Implements generated API interface from OpenAPI specification.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "expertmatch.query.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(QueryService.class)
public class QueryController implements QueryApi {

    private final QueryService queryService;
    private final ChatService chatService;
    private final ApiMapper apiMapper;
    private final HeaderBasedUserContext userContext;
    private final QueryExamplesService queryExamplesService;
    private final Validator validator;

    public QueryController(QueryService queryService, ChatService chatService, ApiMapper apiMapper, HeaderBasedUserContext userContext, QueryExamplesService queryExamplesService, Validator validator) {
        this.queryService = queryService;
        this.chatService = chatService;
        this.apiMapper = apiMapper;
        this.userContext = userContext;
        this.queryExamplesService = queryExamplesService;
        this.validator = validator;
    }

    /**
     * Process natural language query for expert discovery.
     */
    @Override
    public ResponseEntity<com.berdachuk.expertmatch.api.model.QueryResponse> processQuery(
            @Valid @RequestBody com.berdachuk.expertmatch.api.model.QueryRequest queryRequest,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String xUserRoles,
            @RequestHeader(value = "X-User-Email", required = false) String xUserEmail) {
        // Get user ID from HTTP headers populated by Spring Gateway
        // Use xUserId from parameter or fallback to userContext for anonymous users
        String userId = xUserId != null && !xUserId.isBlank() ? xUserId : userContext.getUserIdOrAnonymous();
        log.info("processQuery: xUserId param={}, resolved userId={}", xUserId, userId);

        // Convert API model to domain model
        com.berdachuk.expertmatch.query.QueryRequest domainRequest =
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
                throw new ValidationException(
                        "Invalid chat ID format: " + chatId
                );
            }

            // Validate chatId ownership
            var chat = chatService.findChat(chatId);
            if (chat.isEmpty()) {
                throw new ResourceNotFoundException(
                        "Chat not found: " + chatId
                );
            }
            String chatUserId = chat.get().userId();
            log.info("processQuery: Validating chat ownership - chatId={}, chat.userId={}, request.userId={}",
                    chatId, chatUserId, userId);
            if (!chatUserId.equals(userId)) {
                log.warn("processQuery: Access denied - chat.userId={} != request.userId={}", chatUserId, userId);
                throw new ValidationException(
                        "Access denied to chat: " + chatId
                );
            }
        }

        // Process query using domain model
        com.berdachuk.expertmatch.query.QueryResponse domainResponse =
                queryService.processQuery(domainRequest, chatId, userId);

        // Convert domain model to API model
        com.berdachuk.expertmatch.api.model.QueryResponse apiResponse = apiMapper.toApiQueryResponse(domainResponse);

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Validates SGR pattern combinations in the domain request.
     * Throws ValidationException if Cascade and Cycle patterns are both enabled.
     */
    private void validateSGRPatterns(com.berdachuk.expertmatch.query.QueryRequest domainRequest) {
        java.util.Set<ConstraintViolation<com.berdachuk.expertmatch.query.QueryRequest>> violations = validator.validate(domainRequest);
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid SGR pattern combination");
            throw new ValidationException(errorMessage);
        }
    }

    /**
     * Get list of example queries organized by categories.
     * Implements QueryApi interface method.
     */
    @Override
    public ResponseEntity<com.berdachuk.expertmatch.api.model.QueryExamplesResponse> getQueryExamples() {
        log.debug("Getting query examples");
        List<QueryExamplesService.QueryExample> serviceExamples = queryExamplesService.getExamples();

        // Convert service QueryExample records to API model QueryExample objects
        List<com.berdachuk.expertmatch.api.model.QueryExample> apiExamples = serviceExamples.stream()
                .map(serviceExample -> {
                    com.berdachuk.expertmatch.api.model.QueryExample apiExample = new com.berdachuk.expertmatch.api.model.QueryExample();
                    apiExample.setCategory(serviceExample.category());
                    apiExample.setTitle(serviceExample.title());
                    apiExample.setQuery(serviceExample.query());
                    return apiExample;
                })
                .toList();

        com.berdachuk.expertmatch.api.model.QueryExamplesResponse response = new com.berdachuk.expertmatch.api.model.QueryExamplesResponse();
        response.setExamples(apiExamples);

        return ResponseEntity.ok(response);
    }

    // QueryStreamApi is now generated by OpenAPI Generator
    // QueryStreamController handles /query-stream endpoint directly
    // No need to override processQueryStream here
}

