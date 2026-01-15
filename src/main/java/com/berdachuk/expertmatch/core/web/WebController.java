package com.berdachuk.expertmatch.core.web;

import com.berdachuk.expertmatch.api.client.ApiClient;
import com.berdachuk.expertmatch.api.client.ChatManagementApi;
import com.berdachuk.expertmatch.api.client.QueryApi;
import com.berdachuk.expertmatch.api.client.model.*;
import com.berdachuk.expertmatch.chat.service.ChatService;
import com.berdachuk.expertmatch.core.security.HeaderBasedUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/")
public class WebController {

    private final QueryApi queryApi;
    private final ChatManagementApi chatManagementApi;
    private final ChatService chatService;
    private final HeaderBasedUserContext userContext;
    private final Environment environment;

    public WebController(
            Environment environment,
            HeaderBasedUserContext userContext,
            ChatService chatService,
            ObjectMapper objectMapper) {
        this.environment = environment;
        this.userContext = userContext;
        this.chatService = chatService;

        // Initialize API client with base URL from environment
        // This allows the URL to be resolved dynamically (e.g., in tests with random port)
        String apiBaseUrl = environment.getProperty("expertmatch.api.base-url", "http://localhost:8093");

        // Create RestTemplate with configured ObjectMapper that includes JsonNullableModule
        // This is critical for deserializing JsonNullable fields in execution trace
        // Use HttpComponentsClientHttpRequestFactory to support PATCH method
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        // Replace ALL message converters with ones using our configured ObjectMapper
        restTemplate.getMessageConverters().clear(); // Clear all default converters
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        restTemplate.getMessageConverters().add(jsonConverter); // Add our configured converter
        // Verify JsonNullableModule is registered by checking module IDs
        boolean hasJsonNullableModule = objectMapper.getRegisteredModuleIds().stream()
                .map(Object::toString)
                .anyMatch(id -> id.contains("JsonNullableModule") || id.contains("jackson.nullable"));
        log.info("Configured RestTemplate with ObjectMapper that includes JsonNullableModule (module registered: {})", hasJsonNullableModule);

        // Create ApiClient with the configured RestTemplate
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(apiBaseUrl + "/api/v1");

        // Verify the RestTemplate still has our configured ObjectMapper after ApiClient initialization
        // ApiClient.init() might modify the RestTemplate, so we re-apply our converter if needed
        boolean hasConfiguredConverter = restTemplate.getMessageConverters().stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .anyMatch(converter -> converter.getObjectMapper() == objectMapper);
        if (!hasConfiguredConverter) {
            log.warn("ApiClient modified RestTemplate converters, re-applying configured ObjectMapper");
            restTemplate.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
            restTemplate.getMessageConverters().add(0, new MappingJackson2HttpMessageConverter(objectMapper));
        }

        this.queryApi = new QueryApi(apiClient);
        this.chatManagementApi = new ChatManagementApi(apiClient);
    }

    private String getApiBaseUrl() {
        // Read from environment each time to support dynamic port resolution in tests
        // Also check system property as fallback (for tests that set it dynamically)
        String fromEnv = environment.getProperty("expertmatch.api.base-url");
        if (fromEnv != null && !fromEnv.contains(":0") && !fromEnv.contains("localhost:0")) {
            return fromEnv;
        }
        // Fallback to system property or default
        return System.getProperty("expertmatch.api.base-url", "http://localhost:8093");
    }

    private void updateApiClientBasePath() {
        // Update API client base path if it has changed (e.g., in tests with random port)
        String apiBaseUrl = getApiBaseUrl();
        log.debug("Updating API client base path to: {}", apiBaseUrl);
        queryApi.getApiClient().setBasePath(apiBaseUrl + "/api/v1");
        chatManagementApi.getApiClient().setBasePath(apiBaseUrl + "/api/v1");
    }

    @ModelAttribute("userId")
    public String getUserId() {
        return userContext.getUserIdOrAnonymous();
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String chatId,
            Model model) {
        model.addAttribute("currentPage", "index");

        List<Chat> chats = getChats();
        model.addAttribute("chats", chats);

        Chat currentChat = null;
        if (chatId != null && !chatId.isBlank()) {
            currentChat = chats.stream()
                    .filter(chat -> chatId.equals(chat.getId()))
                    .findFirst()
                    .orElse(null);
        } else if (!chats.isEmpty()) {
            currentChat = chats.get(0);
        }

        model.addAttribute("currentChat", currentChat);

        // Load chat history if chat is selected
        if (currentChat != null) {
            try {
                String userId = getUserId();
                updateApiClientBasePath();
                ConversationHistoryResponse history = chatManagementApi.getHistory(
                        currentChat.getId(),
                        userId,
                        null,
                        null,
                        0,  // page
                        100, // size (load up to 100 messages)
                        "sequence_number,asc"  // sort
                );
                model.addAttribute("messages", history.getMessages() != null ? history.getMessages() : new ArrayList<>());
            } catch (RestClientException e) {
                log.error("Error loading chat history", e);
                model.addAttribute("messages", new ArrayList<>());
            }
        } else {
            model.addAttribute("messages", new ArrayList<>());
        }

        model.addAttribute("isLoading", false);
        model.addAttribute("error", "");

        // Add external system person profile URL template
        String personProfileUrlTemplate = environment.getProperty("expertmatch.external.person-profile-url-template", "");
        model.addAttribute("personProfileUrlTemplate", personProfileUrlTemplate);

        return "index";
    }

    @GetMapping("/chats")
    public String chats(
            @RequestParam(required = false) String chatId,
            Model model) {
        model.addAttribute("currentPage", "chats");

        List<Chat> chats = getChats();
        model.addAttribute("chats", chats);

        Chat currentChat = null;
        if (chatId != null && !chatId.isBlank()) {
            currentChat = chats.stream()
                    .filter(chat -> chatId.equals(chat.getId()))
                    .findFirst()
                    .orElse(null);
        } else if (!chats.isEmpty()) {
            currentChat = chats.get(0);
        }

        model.addAttribute("currentChat", currentChat);

        // Load chat history if chat is selected
        if (currentChat != null) {
            try {
                String userId = getUserId();
                updateApiClientBasePath();
                ConversationHistoryResponse history = chatManagementApi.getHistory(
                        currentChat.getId(),
                        userId,
                        null,
                        null,
                        0,  // page
                        100, // size (load up to 100 messages)
                        "sequence_number,asc"  // sort
                );
                model.addAttribute("messages", history.getMessages() != null ? history.getMessages() : new ArrayList<>());
            } catch (RestClientException e) {
                log.error("Error loading chat history", e);
                model.addAttribute("messages", new ArrayList<>());
            }
        } else {
            model.addAttribute("messages", new ArrayList<>());
        }

        model.addAttribute("isLoading", false);
        model.addAttribute("error", "");

        // Add external system person profile URL template
        String personProfileUrlTemplate = environment.getProperty("expertmatch.external.person-profile-url-template", "");
        model.addAttribute("personProfileUrlTemplate", personProfileUrlTemplate);

        return "chats";
    }

    @PostMapping("/query")
    public String processQuery(
            @RequestParam String query,
            @RequestParam(required = false) String chatId,
            @RequestParam(required = false, defaultValue = "false") boolean rerank,
            @RequestParam(required = false, defaultValue = "false") boolean deepResearch,
            @RequestParam(required = false, defaultValue = "false") boolean includeSources,
            @RequestParam(required = false, defaultValue = "false") boolean includeExecutionTrace,
            @RequestParam(required = false, defaultValue = "false") boolean useCascadePattern,
            @RequestParam(required = false, defaultValue = "false") boolean useRoutingPattern,
            @RequestParam(required = false, defaultValue = "false") boolean useCyclePattern,
            @RequestParam(required = false, defaultValue = "10") int maxResults,
            @RequestParam(required = false, defaultValue = "0.7") double minConfidence,
            Model model) {
        try {
            String userId = getUserId();

            // Get or create chat
            if (chatId == null || chatId.isBlank()) {
                chatId = getOrCreateDefaultChat(userId);
            }

            // Check if this is the first query in the chat (before processing)
            boolean isFirstQuery = false;
            Chat chatBeforeQuery = null;
            try {
                updateApiClientBasePath();
                chatBeforeQuery = chatManagementApi.getChat(chatId, userId, null, null);
                if (chatBeforeQuery != null && chatBeforeQuery.getMessageCount() != null && chatBeforeQuery.getMessageCount() == 0) {
                    isFirstQuery = true;
                }
            } catch (RestClientException e) {
                log.warn("Could not check chat message count before query, skipping auto-rename: {}", e.getMessage());
            }

            // Build query request
            QueryRequest queryRequest = new QueryRequest();
            queryRequest.setQuery(query);
            queryRequest.setChatId(chatId);

            // Validate SGR pattern combinations
            if (useCascadePattern && useCyclePattern) {
                model.addAttribute("error", "Invalid SGR pattern combination: Cascade and Cycle patterns cannot be enabled simultaneously. Cascade requires exactly 1 expert result, while Cycle requires multiple expert results (>1).");
                return "index";
            }

            QueryOptions options = new QueryOptions();
            options.setRerank(rerank);
            options.setDeepResearch(deepResearch);
            options.setIncludeSources(includeSources);
            options.setIncludeExecutionTrace(includeExecutionTrace);
            options.setUseCascadePattern(useCascadePattern);
            options.setUseRoutingPattern(useRoutingPattern);
            options.setUseCyclePattern(useCyclePattern);
            options.setMaxResults(maxResults);
            options.setMinConfidence(minConfidence);
            options.setIncludeEntities(true);
            queryRequest.setOptions(options);

            // Update API client base path in case it changed (e.g., in tests)
            updateApiClientBasePath();

            // Execute query - userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            QueryResponse response = queryApi.processQuery(
                    queryRequest,
                    userId, // X-User-Id parameter (generated client sets this as header)
                    null, // X-User-Roles
                    null  // X-User-Email
            );

            // Update chat name if this was the first query (including default chats)
            if (isFirstQuery && chatBeforeQuery != null) {
                try {
                    String generatedName = generateChatNameFromQuery(query);
                    UpdateChatRequest updateRequest = new UpdateChatRequest();
                    updateRequest.setName(generatedName);
                    updateApiClientBasePath();
                    chatManagementApi.updateChat(chatId, updateRequest, userId, null, null);
                    log.info("Auto-updated chat name to '{}' for chatId: {} (isDefault: {})",
                            generatedName, chatId, chatBeforeQuery.getIsDefault());
                } catch (RestClientException e) {
                    log.warn("Failed to auto-update chat name for chatId: {}, error: {}", chatId, e.getMessage());
                    // Don't fail the query if name update fails
                }
            }

            model.addAttribute("currentPage", "index");

            List<Chat> chats = getChats();
            model.addAttribute("chats", chats);

            // Preserve current chat selection
            final String finalChatId = chatId;
            Chat currentChat = null;
            if (finalChatId != null && !finalChatId.isBlank()) {
                currentChat = chats.stream()
                        .filter(chat -> finalChatId.equals(chat.getId()))
                        .findFirst()
                        .orElse(null);
            } else if (!chats.isEmpty()) {
                currentChat = chats.get(0);
            }
            model.addAttribute("currentChat", currentChat);

            // Load chat history if chat is selected
            if (currentChat != null) {
                try {
                    updateApiClientBasePath();
                    ConversationHistoryResponse history = chatManagementApi.getHistory(
                            currentChat.getId(),
                            userId,
                            null,
                            null,
                            0,  // page
                            100, // size (load up to 100 messages)
                            "sequence_number,asc"  // sort
                    );
                    model.addAttribute("messages", history.getMessages() != null ? history.getMessages() : new ArrayList<>());
                } catch (RestClientException e) {
                    log.error("Error loading chat history", e);
                    model.addAttribute("messages", new ArrayList<>());
                }
            } else {
                model.addAttribute("messages", new ArrayList<>());
            }

            model.addAttribute("queryResult", response);
            // Always add query attribute, even on error, so tests can check for it
            model.addAttribute("query", query);
            model.addAttribute("isLoading", false);
            model.addAttribute("error", "");

            // Add external system person profile URL template
            String personProfileUrlTemplate = environment.getProperty("expertmatch.external.person-profile-url-template", "");
            model.addAttribute("personProfileUrlTemplate", personProfileUrlTemplate);

            return "index";
        } catch (RestClientException | HttpMessageConversionException e) {
            log.error("Error processing query", e);
            model.addAttribute("currentPage", "index");

            List<Chat> chats = getChats();
            model.addAttribute("chats", chats);

            // Preserve current chat selection
            final String finalChatId = chatId;
            Chat currentChat = null;
            if (finalChatId != null && !finalChatId.isBlank()) {
                currentChat = chats.stream()
                        .filter(chat -> finalChatId.equals(chat.getId()))
                        .findFirst()
                        .orElse(null);
            } else if (!chats.isEmpty()) {
                currentChat = chats.get(0);
            }
            model.addAttribute("currentChat", currentChat);

            // Load chat history if chat is selected
            if (currentChat != null) {
                try {
                    String userId = getUserId();
                    updateApiClientBasePath();
                    ConversationHistoryResponse history = chatManagementApi.getHistory(
                            currentChat.getId(),
                            userId,
                            null,
                            null,
                            0,  // page
                            100, // size (load up to 100 messages)
                            "sequence_number,asc"  // sort
                    );
                    model.addAttribute("messages", history.getMessages() != null ? history.getMessages() : new ArrayList<>());
                } catch (RestClientException ex) {
                    log.error("Error loading chat history", ex);
                    model.addAttribute("messages", new ArrayList<>());
                }
            } else {
                model.addAttribute("messages", new ArrayList<>());
            }

            model.addAttribute("error", "Error processing query: " + e.getMessage());
            // Always add query attribute, even on error
            model.addAttribute("query", query);
            model.addAttribute("isLoading", false);

            // Add external system person profile URL template
            String personProfileUrlTemplate = environment.getProperty("expertmatch.external.person-profile-url-template", "");
            model.addAttribute("personProfileUrlTemplate", personProfileUrlTemplate);

            return "index";
        }
    }

    @PostMapping("/chats/send")
    public String sendMessage(
            @RequestParam String message,
            @RequestParam String chatId,
            Model model) {
        try {
            // Get user ID from request context (reads X-User-Id header)
            String userId = getUserId();
            log.info("sendMessage: userId={}, chatId={}, header read from context", userId, chatId);

            // Build query request
            QueryRequest queryRequest = new QueryRequest();
            queryRequest.setQuery(message);
            queryRequest.setChatId(chatId);

            QueryOptions options = new QueryOptions();
            options.setRerank(true);
            options.setMaxResults(10);
            options.setMinConfidence(0.7);
            options.setIncludeSources(true);
            options.setIncludeEntities(true);
            queryRequest.setOptions(options);

            // Update API client base path in case it changed (e.g., in tests)
            updateApiClientBasePath();

            // Execute query - userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            QueryResponse response = queryApi.processQuery(
                    queryRequest,
                    userId, // X-User-Id parameter (generated client sets this as header)
                    null, // X-User-Roles
                    null  // X-User-Email
            );

            // Success - redirect to chats page with chat selected
            return "redirect:/chats?chatId=" + chatId;
        } catch (RestClientException e) {
            log.error("Error sending message", e);
            // On error, redirect to chats page (error will be logged)
            return "redirect:/chats?chatId=" + chatId;
        }
    }

    @PostMapping("/chats/new")
    public String createChat(
            @RequestParam(required = false) String name,
            Model model) {
        try {
            String userId = getUserId();

            CreateChatRequest request = new CreateChatRequest();
            if (name != null && !name.isBlank()) {
                request.setName(name);
            }

            updateApiClientBasePath();
            // userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            Chat chat = chatManagementApi.createChat(userId, null, null, request);

            return "redirect:/?chatId=" + chat.getId();
        } catch (RestClientException e) {
            log.error("Error creating chat", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create chat", e);
        }
    }

    @PostMapping("/chats/history/clear")
    public String clearChatHistory(
            @RequestParam String chatId,
            Model model) {
        try {
            String userId = getUserId();
            log.info("clearChatHistory: userId={}, chatId={}", userId, chatId);

            // Verify chat belongs to user
            updateApiClientBasePath();
            Chat chat = chatManagementApi.getChat(chatId, userId, null, null);
            if (chat == null) {
                model.addAttribute("currentPage", "index");
                model.addAttribute("chats", getChats());
                model.addAttribute("error", "Chat not found");
                return "index";
            }

            // Clear history
            boolean cleared = chatService.clearHistory(chatId);
            if (!cleared) {
                log.warn("Failed to clear history for chatId: {}", chatId);
                model.addAttribute("currentPage", "index");
                model.addAttribute("chats", getChats());
                model.addAttribute("error", "Failed to clear chat history");
                return "index";
            }

            log.info("Successfully cleared history for chatId: {}", chatId);
            return "redirect:/?chatId=" + chatId + "&cleared=" + System.currentTimeMillis();
        } catch (RestClientException e) {
            log.error("Error clearing chat history: chatId={}, userId={}", chatId, getUserId(), e);
            model.addAttribute("currentPage", "index");
            model.addAttribute("chats", getChats());
            model.addAttribute("error", "Error clearing chat history: " + e.getMessage());
            return "index";
        }
    }

    @PostMapping("/chats/delete")
    public String deleteChat(
            @RequestParam String chatId,
            @RequestHeader(value = "Referer", required = false) String referer,
            Model model) {
        try {
            // Get user ID from request context (reads X-User-Id header)
            String userId = getUserId();
            log.info("deleteChat: userId={}, chatId={}, header read from context", userId, chatId);
            updateApiClientBasePath();
            // userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            chatManagementApi.deleteChat(chatId, userId, null, null);

            // Always redirect to index page to refresh the chat list
            // Using redirect with cache-busting parameter to ensure fresh data
            return "redirect:/?deleted=" + System.currentTimeMillis();
        } catch (HttpClientErrorException e) {
            log.warn("Error deleting chat: chatId={}, userId={}, status={}", chatId, getUserId(), e.getStatusCode());

            String errorMessage = "Failed to delete chat";
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // Try to extract error message from response body
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && responseBody.contains("Cannot delete default chat")) {
                    errorMessage = "Cannot delete default chat";
                } else {
                    errorMessage = "Cannot delete this chat (may be default chat or invalid ID)";
                }
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                errorMessage = "Chat not found";
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                errorMessage = "Access denied to this chat";
            }

            model.addAttribute("currentPage", "index");
            model.addAttribute("chats", getChats());
            model.addAttribute("error", errorMessage);
            return "index";
        } catch (HttpServerErrorException e) {
            log.error("Server error deleting chat: chatId={}, userId={}, status={}", chatId, getUserId(), e.getStatusCode(), e);

            model.addAttribute("currentPage", "index");
            model.addAttribute("chats", getChats());
            model.addAttribute("error", "Server error while deleting chat. Please try again.");
            return "index";
        } catch (RestClientException e) {
            log.error("Error deleting chat: chatId={}, userId={}", chatId, getUserId(), e);

            model.addAttribute("currentPage", "index");
            model.addAttribute("chats", getChats());
            model.addAttribute("error", "Error deleting chat: " + e.getMessage());
            return "index";
        }
    }

    private List<Chat> getChats() {
        try {
            String userId = getUserId();
            updateApiClientBasePath();
            // userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            ChatListResponse response = chatManagementApi.listChats(userId, null, null);
            return response.getChats() != null ? response.getChats() : new ArrayList<>();
        } catch (RestClientException e) {
            log.error("Error fetching chats", e);
            return new ArrayList<>();
        }
    }

    private String getOrCreateDefaultChat(String userId) {
        try {
            List<Chat> chats = getChats();
            if (!chats.isEmpty()) {
                return chats.get(0).getId();
            }

            // Create new chat
            CreateChatRequest request = new CreateChatRequest();
            updateApiClientBasePath();
            // userId is passed as parameter, which the generated client sets as X-User-Id header
            // Don't call setApiHeaders() here to avoid duplicate headers
            Chat chat = chatManagementApi.createChat(userId, null, null, request);
            return chat.getId();
        } catch (RestClientException e) {
            log.error("Error getting or creating default chat", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get or create chat", e);
        }
    }

    private void setApiHeaders(String userId) {
        // Set default header for API client
        // Note: The generated client also sets X-User-Id header from the method parameter
        // This default header serves as a fallback if the parameter is null
        queryApi.getApiClient().addDefaultHeader("X-User-Id", userId);
        chatManagementApi.getApiClient().addDefaultHeader("X-User-Id", userId);
    }

    /**
     * Generates a chat name from the query text.
     * Truncates to 50 characters and cleans up the text.
     */
    private String generateChatNameFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return "New Chat";
        }

        // Trim and normalize whitespace
        String cleaned = query.trim().replaceAll("\\s+", " ");

        // Truncate to 50 characters, but try to break at word boundary
        int maxLength = 50;
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        // Find the last space before maxLength
        String truncated = cleaned.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 20) { // Only break at word boundary if it's not too short
            truncated = truncated.substring(0, lastSpace);
        }

        // Add ellipsis if truncated
        return truncated + "...";
    }
}

