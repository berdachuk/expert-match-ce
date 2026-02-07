package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for Spring AI components.
 * Provides mock ChatClient, ChatModel, and EmbeddingModel for tests that don't require real LLM.
 * <p>
 * Note: SpringAIConfig is excluded from test context (via @Profile("!test")) to prevent multiple @Primary beans.
 * All LLM calls in integration tests should use these mocks to avoid real API calls.
 * <p>
 * Configuration:
 * - BaseIntegrationTest disables Spring AI auto-configuration (spring.ai.openai.enabled=false)
 * - This ensures only the mocked beans from TestAIConfig are used
 * - All @Primary annotations ensure mocks are selected over any auto-configured beans
 */
@Slf4j
@TestConfiguration
@Profile("test")
public class TestAIConfig {

    /**
     * Mock ChatModel for tests.
     * This replaces any auto-configured ChatModel beans.
     * Returns valid JSON responses for extraction calls.
     * <p>
     * Strategy: Return empty arrays for all list-based extractions (skills, seniority, technologies, entities).
     * Return null JSON object for language extraction.
     * This works because:
     * - List parsers expect arrays and will handle empty arrays correctly
     * - Language parser can handle both "null" string and JSON objects
     * - Entity parsers expect arrays of objects
     */
    @Bean
    @Primary
    public ChatModel testChatModel() {
        log.info("Creating MOCK ChatModel for tests - NO real LLM calls will be made");
        ChatModel mockModel = mock(ChatModel.class);

        // Shared counter for tracking LLM calls
        // We use a simple strategy: alternate between array and object responses
        // Most extractions need arrays, only language needs an object
        AtomicInteger callCount = new AtomicInteger(0);

        // Mock the call method to return appropriate JSON responses
        // Strategy: com.berdachuk.expertmatch.query.domain.QueryParser.parse() makes 4 calls (skills, seniority, language, technologies)
        //           EntityExtractor.extract() makes 5 calls (all arrays)
        //           Language is the 3rd call (index 2) in QueryParser cycle
        when(mockModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            log.info("MOCK ChatModel.call() invoked - using MOCK, NOT real LLM");
            int currentCall = callCount.getAndIncrement();
            String responseText;

            // QueryParser calls: 0=skills, 1=seniority, 2=language, 3=technologies
            // EntityExtractor calls: 4=persons, 5=organizations, 6=technologies, 7=projects, 8=domains
            // Only call 2 (language) needs object format, all others need arrays
            if (currentCall == 2) {
                // Language extraction (3rd call, index 2) - return JSON object
                responseText = "{\"language\": null, \"proficiency\": null}";
            } else {
                // All other extractions - return empty array
                responseText = "[]";
            }

            AssistantMessage assistantMessage = new AssistantMessage(responseText);
            Generation generation = new Generation(assistantMessage);
            ChatResponse response = new ChatResponse(List.of(generation));

            // Verify response structure
            if (response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("ChatResponse structure is invalid");
            }

            return response;
        });

        // For stream calls, return a default response
        AssistantMessage defaultMessage = new AssistantMessage("This is a test response from the mock ChatModel.");
        Generation defaultGeneration = new Generation(defaultMessage);
        ChatResponse defaultResponse = new ChatResponse(List.of(defaultGeneration));
        when(mockModel.stream(any(Prompt.class))).thenReturn(Flux.just(defaultResponse));

        return mockModel;
    }

    /**
     * Mock EmbeddingModel for tests.
     * This replaces any auto-configured EmbeddingModel beans.
     * Note: SpringAIConfig.primaryEmbeddingModel is excluded in test profile.
     * <p>
     * Returns 1536 dimensions (OpenAI-compatible default), which matches text-embedding-3-large.
     * <p>
     * IMPORTANT: This mock ensures that TestDataGenerator.generateEmbeddings() (if called)
     * will use the mock instead of making real LLM API calls. The @Primary annotation
     * ensures this mock is selected over any auto-configured EmbeddingModel beans.
     * <p>
     * Note: TestDataGenerator.generateTestData() does NOT call generateEmbeddings(),
     * so no LLM calls are made during test data generation. However, if generateEmbeddings()
     * is called separately, this mock will be used.
     */
    @Bean
    @Primary
    public EmbeddingModel testEmbeddingModel() {
        log.info("Creating MOCK EmbeddingModel for tests - NO real LLM calls will be made");
        EmbeddingModel mockModel = mock(EmbeddingModel.class);

        // Mock EmbeddingResponse with a default embedding vector (1536 dimensions, OpenAI-compatible)
        float[] defaultEmbedding = new float[1536];
        org.springframework.ai.embedding.Embedding embedding =
                new org.springframework.ai.embedding.Embedding(defaultEmbedding, 0);
        EmbeddingResponse mockResponse = new EmbeddingResponse(List.of(embedding));

        // Mock the embedForResponse method to return the mock response
        // This ensures no real LLM API calls are made during tests
        when(mockModel.embedForResponse(any(List.class))).thenAnswer(invocation -> {
            log.info("MOCK EmbeddingModel.embedForResponse() invoked - using MOCK, NOT real LLM");
            return mockResponse;
        });

        return mockModel;
    }

    /**
     * Mock reranking ChatModel for tests.
     * This provides a mock for the rerankingChatModel bean used by SemanticReranker.
     * Returns valid JSON responses for reranking calls.
     * <p>
     * Note: SemanticReranker uses @Qualifier("rerankingChatModel") to inject this bean.
     * This mock ensures that reranking operations use mocked LLM calls instead of real API calls.
     */
    @Bean
    @Qualifier("rerankingChatModel")
    public ChatModel testRerankingChatModel() {
        log.info("Creating MOCK rerankingChatModel for tests - NO real LLM calls will be made");
        ChatModel mockModel = mock(ChatModel.class);

        // Mock reranking response - returns JSON array of reranking results
        // Format: [{"expertId": "id1", "score": 0.9}, {"expertId": "id2", "score": 0.8}, ...]
        when(mockModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            log.info("MOCK rerankingChatModel.call() invoked - using MOCK, NOT real LLM");
            // Return empty array for reranking (no reranking results)
            // This matches the behavior when reranking is not needed or returns no results
            String responseText = "[]";

            AssistantMessage assistantMessage = new AssistantMessage(responseText);
            Generation generation = new Generation(assistantMessage);
            ChatResponse response = new ChatResponse(List.of(generation));

            return response;
        });

        // For stream calls, return a default response
        AssistantMessage defaultMessage = new AssistantMessage("[]");
        Generation defaultGeneration = new Generation(defaultMessage);
        ChatResponse defaultResponse = new ChatResponse(List.of(defaultGeneration));
        when(mockModel.stream(any(Prompt.class))).thenReturn(Flux.just(defaultResponse));

        return mockModel;
    }

    /**
     * Mock ChatClient for tests.
     * Only created when skills are disabled (expertmatch.skills.enabled=false or not set).
     * When skills are enabled, chatClientWithSkills from AgentSkillsConfiguration will be @Primary instead.
     * When tool search is enabled, chatClientWithToolSearch from ToolSearchConfiguration will be @Primary instead.
     */
    @Bean("testChatClient")
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "expertmatch.skills.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatClient testChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

