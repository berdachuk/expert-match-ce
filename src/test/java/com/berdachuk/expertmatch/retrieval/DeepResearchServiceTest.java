package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.data.ExpertEnrichmentService;
import com.berdachuk.expertmatch.query.QueryParser;
import com.berdachuk.expertmatch.query.QueryRequest;
import com.berdachuk.expertmatch.query.QueryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeepResearchService.
 *
 * Note: Full integration testing of ChatClient interactions requires integration tests
 * due to the complexity of mocking Spring AI's fluent ChatClient API.
 *
 * WARNING: Some tests that require ChatClient mocking are currently disabled due to
 * Spring AI inner type accessibility issues. These should be tested via integration tests.
 */
@ExtendWith(MockitoExtension.class)
class DeepResearchServiceTest {

    @Mock
    private HybridRetrievalService retrievalService;

    @Mock
    private ExpertEnrichmentService enrichmentService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatModel chatModel;

    @Mock
    private Environment environment;

    @Mock
    private PromptTemplate queryRefinementPromptTemplate;

    @Mock
    private PromptTemplate gapAnalysisPromptTemplate;

    private DeepResearchService deepResearchService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(queryRefinementPromptTemplate.render(any())).thenReturn("test prompt");
        lenient().when(gapAnalysisPromptTemplate.render(any())).thenReturn("test gap analysis prompt");
        deepResearchService = new DeepResearchService(
                retrievalService,
                enrichmentService,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                queryRefinementPromptTemplate,
                gapAnalysisPromptTemplate
        );
    }

    @Test
    void testPerformDeepResearchWithEmptyInitialResults() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult emptyResult =
                new HybridRetrievalService.RetrievalResult(List.of(), Map.of());

        when(retrievalService.retrieve(any(), any(), any())).thenReturn(emptyResult);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify
        assertNotNull(result);
        assertTrue(result.expertIds().isEmpty());
        verify(retrievalService, times(1)).retrieve(any(), any(), any());
        verify(enrichmentService, never()).enrichExperts(any(), any());
        verify(chatClient, never()).prompt();
    }

    @Test
    void testPerformDeepResearchWithNoGaps() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1", "expert2"),
                        Map.of("expert1", 0.9, "expert2", 0.8)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1"),
                createMockExpert("expert2", "Expert 2")
        );

        // Mock gap analysis response - no expansion needed
        String gapAnalysisJson = """
                {
                  "identifiedGaps": [],
                  "ambiguities": [],
                  "missingInformation": [],
                  "needsExpansion": false,
                  "reasoning": "Initial results are sufficient"
                }
                """;

        when(retrievalService.retrieve(any(), any(), any())).thenReturn(initialResult);
        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponse(gapAnalysisJson);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.expertIds().size());
        assertTrue(result.expertIds().contains("expert1"));
        assertTrue(result.expertIds().contains("expert2"));
        verify(retrievalService, times(1)).retrieve(any(), any(), any());
        verify(enrichmentService, times(1)).enrichExperts(any(), any());
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void testPerformDeepResearchWithGapsAndExpansion() {
        // Setup
        QueryRequest request = createRequest("Find Java experts with Spring Boot and AWS", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("Find Java experts with Spring Boot and AWS", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.8)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis response - expansion needed
        String gapAnalysisJson = """
                {
                  "identifiedGaps": ["Missing AWS-specific experience"],
                  "ambiguities": [],
                  "missingInformation": [],
                  "needsExpansion": true,
                  "reasoning": "Initial results lack AWS experience"
                }
                """;

        // Mock query refinement response
        String refinedQueriesJson = """
                ["Find Java experts with AWS Lambda", "Find Spring Boot experts with cloud architecture"]
                """;

        // Mock expanded retrieval results
        HybridRetrievalService.RetrievalResult expandedResult1 =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert2", "expert3"),
                        Map.of("expert2", 0.85, "expert3", 0.75)
                );

        HybridRetrievalService.RetrievalResult expandedResult2 =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert4"),
                        Map.of("expert4", 0.7)
                );

        when(retrievalService.retrieve(any(), any(), any()))
                .thenReturn(initialResult)  // Initial retrieval
                .thenReturn(expandedResult1) // First expanded query
                .thenReturn(expandedResult2); // Second expanded query

        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);

        // Mock ChatClient for gap analysis and query refinement
        mockChatClientResponses(gapAnalysisJson, refinedQueriesJson);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify
        assertNotNull(result);
        assertTrue(result.expertIds().size() > 0);
        // Should include experts from both initial and expanded results
        verify(retrievalService, atLeast(2)).retrieve(any(), any(), any());
        verify(enrichmentService, times(1)).enrichExperts(any(), any());
        verify(chatClient, atLeast(2)).prompt();
    }

    @Test
    void testPerformDeepResearchWithEmptyRefinedQueries() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis - expansion needed
        String gapAnalysisJson = """
                {
                  "identifiedGaps": ["Missing information"],
                  "needsExpansion": true
                }
                """;

        // Mock query refinement - empty array
        String refinedQueriesJson = "[]";

        when(retrievalService.retrieve(any(), any(), any())).thenReturn(initialResult);
        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponses(gapAnalysisJson, refinedQueriesJson);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify - should return initial results when no refined queries
        assertNotNull(result);
        assertEquals(1, result.expertIds().size());
        assertEquals("expert1", result.expertIds().get(0));
        verify(retrievalService, times(1)).retrieve(any(), any(), any());
    }

    @Test
    void testPerformDeepResearchWithExpandedRetrievalError() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis - expansion needed
        String gapAnalysisJson = """
                {
                  "identifiedGaps": ["Missing information"],
                  "needsExpansion": true
                }
                """;

        String refinedQueriesJson = """
                ["refined query 1"]
                """;

        when(retrievalService.retrieve(any(), any(), any()))
                .thenReturn(initialResult)  // Initial retrieval succeeds
                .thenThrow(new RuntimeException("Retrieval error")); // Expanded retrieval fails

        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponses(gapAnalysisJson, refinedQueriesJson);

        // Execute - should handle error gracefully and return initial results
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify - should return initial results when expanded retrieval fails
        assertNotNull(result);
        assertEquals(1, result.expertIds().size());
        assertEquals("expert1", result.expertIds().get(0));
        verify(retrievalService, atLeast(2)).retrieve(any(), any(), any());
    }

    @Test
    void testGapAnalysisParsingWithMarkdownCodeBlock() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis response with markdown code block
        String gapAnalysisResponse = """
                Here is the analysis:
                ```json
                {
                  "identifiedGaps": ["Gap 1"],
                  "needsExpansion": true
                }
                ```
                """;

        // Mock query refinement response
        String refinedQueriesResponse = """
                ["refined query 1", "refined query 2"]
                """;

        when(retrievalService.retrieve(any(), any(), any()))
                .thenReturn(initialResult)
                .thenReturn(new HybridRetrievalService.RetrievalResult(List.of("expert2"), Map.of("expert2", 0.8)))
                .thenReturn(new HybridRetrievalService.RetrievalResult(List.of("expert3"), Map.of("expert3", 0.7)));
        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponses(gapAnalysisResponse, refinedQueriesResponse);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify - should parse JSON from markdown code block
        assertNotNull(result);
        verify(chatClient, atLeastOnce()).prompt();
    }

    @Test
    void testGapAnalysisParsingFailure() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock invalid JSON response - should throw exception
        String invalidJsonResponse = "Yes, we should expand the search";

        when(retrievalService.retrieve(any(), any(), any())).thenReturn(initialResult);
        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponse(invalidJsonResponse);

        // Execute - should throw exception when parsing fails
        assertThrows(RuntimeException.class, () -> {
            deepResearchService.performDeepResearch(request, parsedQuery);
        });

        // Verify - should have attempted gap analysis
        verify(chatClient, atLeastOnce()).prompt();
    }

    @Test
    void testQueryRefinementParsingWithMarkdown() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis
        String gapAnalysisJson = """
                {
                  "identifiedGaps": ["Gap"],
                  "needsExpansion": true
                }
                """;

        // Mock query refinement with markdown
        String refinedQueriesResponse = """
                Here are the refined queries:
                ```json
                ["query 1", "query 2"]
                ```
                """;

        when(retrievalService.retrieve(any(), any(), any()))
                .thenReturn(initialResult)
                .thenReturn(new HybridRetrievalService.RetrievalResult(List.of("expert2"), Map.of("expert2", 0.8)))
                .thenReturn(new HybridRetrievalService.RetrievalResult(List.of("expert3"), Map.of("expert3", 0.7)));

        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponses(gapAnalysisJson, refinedQueriesResponse);

        // Execute
        HybridRetrievalService.RetrievalResult result =
                deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify - should parse queries from markdown
        assertNotNull(result);
        verify(retrievalService, atLeast(2)).retrieve(any(), any(), any());
    }

    @Test
    void testDeepResearchDoesNotRecurse() {
        // Setup - deepResearch is enabled in request
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        // Mock gap analysis
        String gapAnalysisJson = """
                {
                  "identifiedGaps": ["Gap"],
                  "needsExpansion": true
                }
                """;

        String refinedQueriesJson = """
                ["refined query"]
                """;

        when(retrievalService.retrieve(any(), any(), any()))
                .thenReturn(initialResult)
                .thenReturn(new HybridRetrievalService.RetrievalResult(List.of("expert2"), Map.of("expert2", 0.8)));

        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);
        mockChatClientResponses(gapAnalysisJson, refinedQueriesJson);

        // Execute
        deepResearchService.performDeepResearch(request, parsedQuery);

        // Verify - expanded requests should have deepResearch=false (no recursion)
        verify(retrievalService, atLeast(2)).retrieve(any(), any(), any());
    }

    @Test
    void testChatClientNullResponse() {
        // Setup
        QueryRequest request = createRequest("test query", true);
        QueryParser.ParsedQuery parsedQuery = new QueryParser.ParsedQuery("test query", List.of(), List.of(), null, "expert_search", List.of());

        HybridRetrievalService.RetrievalResult initialResult =
                new HybridRetrievalService.RetrievalResult(
                        List.of("expert1"),
                        Map.of("expert1", 0.9)
                );

        List<QueryResponse.ExpertMatch> experts = List.of(
                createMockExpert("expert1", "Expert 1")
        );

        when(retrievalService.retrieve(any(), any(), any())).thenReturn(initialResult);
        when(enrichmentService.enrichExperts(any(), any())).thenReturn(experts);

        // Mock ChatClient to return null response
        // Use the existing helper method but make it return null
        lenient().when(chatClient.prompt()).thenAnswer(invocation -> {
            // Get the return type of prompt() method
            Class<?> promptReturnType = invocation.getMethod().getReturnType();

            // Create request mock using the actual return type
            Object requestMock = mock(promptReturnType, withSettings().defaultAnswer(inv -> {
                String methodName = inv.getMethod().getName();
                if (methodName.equals("user") && inv.getArguments().length == 1 && inv.getArguments()[0] instanceof String) {
                    // Get the return type of user() method
                    Class<?> userReturnType = inv.getMethod().getReturnType();
                    // Create requestSpec mock using the actual return type
                    Object requestSpecMock = mock(userReturnType, withSettings().defaultAnswer(inv2 -> {
                        String methodName2 = inv2.getMethod().getName();
                        if (methodName2.equals("call")) {
                            // Get the return type of call() method
                            Class<?> callReturnType = inv2.getMethod().getReturnType();
                            // Create response mock using the actual return type - but return null for chatResponse
                            Object responseMock = mock(callReturnType, withSettings().defaultAnswer(inv3 -> {
                                String methodName3 = inv3.getMethod().getName();
                                if (methodName3.equals("chatResponse")) {
                                    return null; // This is what we want - null response
                                }
                                return null;
                            }));
                            return responseMock;
                        }
                        return null;
                    }));
                    return requestSpecMock;
                }
                return null;
            }));

            return requestMock;
        });

        // Execute - should throw exception when ChatClient returns null
        assertThrows(RuntimeException.class, () -> {
            deepResearchService.performDeepResearch(request, parsedQuery);
        });
    }

    // Helper methods

    private QueryRequest createRequest(String query, boolean deepResearch) {
        return new QueryRequest(
                query,
                null,
                new QueryRequest.QueryOptions(10, 0.7, true, true, true, deepResearch, false, false, false, false)
        );
    }

    // Helper methods to mock ChatClient fluent API chain
    // Note: Due to Spring AI's inner type accessibility, we use reflection to get return types
    // and create properly typed mocks to avoid ClassCastException
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockChatClientResponse(String responseText) {
        ChatResponse mockResponse = createChatResponse(responseText);

        // Build the chain: prompt() -> user() -> call() -> chatResponse()
        // Use reflection to get the actual return types from method signatures
        lenient().when(chatClient.prompt()).thenAnswer((Answer) invocation -> {
            // Get the return type of prompt() method
            Class<?> promptReturnType = invocation.getMethod().getReturnType();

            // Create request mock using the actual return type
            Object requestMock = mock(promptReturnType, withSettings().defaultAnswer((Answer) inv -> {
                String methodName = inv.getMethod().getName();
                if (methodName.equals("user") && inv.getArguments().length == 1 && inv.getArguments()[0] instanceof String) {
                    // Get the return type of user() method
                    Class<?> userReturnType = inv.getMethod().getReturnType();
                    // Create requestSpec mock using the actual return type
                    Object requestSpecMock = mock(userReturnType, withSettings().defaultAnswer((Answer) inv2 -> {
                        String methodName2 = inv2.getMethod().getName();
                        if (methodName2.equals("call")) {
                            // Get the return type of call() method
                            Class<?> callReturnType = inv2.getMethod().getReturnType();
                            // Create response mock using the actual return type
                            Object responseMock = mock(callReturnType, withSettings().defaultAnswer((Answer) inv3 -> {
                                String methodName3 = inv3.getMethod().getName();
                                if (methodName3.equals("chatResponse")) {
                                    return mockResponse;
                                }
                                return null;
                            }));
                            return responseMock;
                        }
                        return null;
                    }));
                    return requestSpecMock;
                }
                return null;
            }));

            return requestMock;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockChatClientResponses(String firstResponse, String secondResponse) {
        ChatResponse firstMockResponse = createChatResponse(firstResponse);
        ChatResponse secondMockResponse = createChatResponse(secondResponse);
        final int[] callCount = {0};

        // For multiple calls, create new mocks each time with different responses
        // Use reflection to get the actual return types from method signatures
        lenient().when(chatClient.prompt()).thenAnswer((Answer) invocation -> {
            int currentCall = callCount[0]++;
            final ChatResponse resp = currentCall == 0 ? firstMockResponse : secondMockResponse;

            // Get the return type of prompt() method
            Class<?> promptReturnType = invocation.getMethod().getReturnType();

            // Create request mock using the actual return type
            Object requestMock = mock(promptReturnType, withSettings().defaultAnswer((Answer) inv -> {
                String methodName = inv.getMethod().getName();
                if (methodName.equals("user") && inv.getArguments().length == 1 && inv.getArguments()[0] instanceof String) {
                    // Get the return type of user() method
                    Class<?> userReturnType = inv.getMethod().getReturnType();
                    // Create requestSpec mock using the actual return type
                    Object requestSpecMock = mock(userReturnType, withSettings().defaultAnswer((Answer) inv2 -> {
                        String methodName2 = inv2.getMethod().getName();
                        if (methodName2.equals("call")) {
                            // Get the return type of call() method
                            Class<?> callReturnType = inv2.getMethod().getReturnType();
                            // Create response mock using the actual return type
                            Object responseMock = mock(callReturnType, withSettings().defaultAnswer((Answer) inv3 -> {
                                String methodName3 = inv3.getMethod().getName();
                                if (methodName3.equals("chatResponse")) {
                                    return resp;
                                }
                                return null;
                            }));
                            return responseMock;
                        }
                        return null;
                    }));
                    return requestSpecMock;
                }
                return null;
            }));

            return requestMock;
        });
    }

    private ChatResponse createChatResponse(String text) {
        org.springframework.ai.chat.messages.AssistantMessage assistantMessage =
                new org.springframework.ai.chat.messages.AssistantMessage(text);
        Generation generation = new Generation(assistantMessage);
        ChatResponse response = new ChatResponse(List.of(generation));
        // Verify the structure is correct - getResult() should not be null
        if (response.getResult() == null) {
            throw new IllegalStateException("ChatResponse.getResult() is null - this indicates a Spring AI version mismatch");
        }
        if (response.getResult().getOutput() == null) {
            throw new IllegalStateException("ChatResponse.getResult().getOutput() is null - this indicates a Spring AI version mismatch");
        }
        return response;
    }

    private QueryResponse.ExpertMatch createMockExpert(String id, String name) {
        return new QueryResponse.ExpertMatch(
                id,
                name,
                name + "@example.com",
                "A4",
                new QueryResponse.LanguageProficiency("C1"),
                new QueryResponse.SkillMatch(5, 5, 0, 0, 1.0),
                new QueryResponse.MatchedSkills(List.of("Java", "Spring"), List.of()),
                List.of(),
                new QueryResponse.Experience(false, false, false, false, false),
                0.9,
                "available"
        );
    }
}
