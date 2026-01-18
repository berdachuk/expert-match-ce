package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.retrieval.service.impl.SemanticRerankerImpl;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SemanticReranker.
 * Tests LLM-based reranking functionality with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class SemanticRerankerTest {

    private static final String TEST_QUERY = "Looking for Java and Spring Boot experts";
    private static final List<String> TEST_EXPERT_IDS = List.of("expert1", "expert2", "expert3");
    @Mock
    private ChatModel rerankingChatModel;
    @Mock
    private PromptTemplate rerankingPromptTemplate;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private WorkExperienceRepository workExperienceRepository;
    @Mock
    private ObjectMapper objectMapper;
    private SemanticRerankerImpl semanticReranker;

    @BeforeEach
    void setUp() {
        semanticReranker = new SemanticRerankerImpl(
                rerankingChatModel,
                rerankingPromptTemplate,
                employeeRepository,
                workExperienceRepository,
                new ObjectMapper() // Use real ObjectMapper for JSON parsing
        );
    }

    @Test
    void testRerank_WithLLMModel_ReturnsRerankedResults() throws Exception {
        // Arrange
        String rerankingResponse = """
                [
                  {"expertId": "expert2", "score": 0.95, "reason": "Strong match"},
                  {"expertId": "expert1", "score": 0.87, "reason": "Good match"},
                  {"expertId": "expert3", "score": 0.75, "reason": "Partial match"}
                ]
                """;

        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenReturn(createChatResponse(rerankingResponse));

        // Mock employee data
        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 10);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        // Should be sorted by score (descending): expert2, expert1, expert3
        assertEquals("expert2", result.get(0));
        assertEquals("expert1", result.get(1));
        assertEquals("expert3", result.get(2));

        verify(rerankingPromptTemplate).render(anyMap());
        verify(rerankingChatModel).call(any(Prompt.class));
    }

    @Test
    void testRerank_WithLLMModel_RespectsMaxResults() throws Exception {
        // Arrange
        String rerankingResponse = """
                [
                  {"expertId": "expert2", "score": 0.95, "reason": "Strong match"},
                  {"expertId": "expert1", "score": 0.87, "reason": "Good match"},
                  {"expertId": "expert3", "score": 0.75, "reason": "Partial match"}
                ]
                """;

        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenReturn(createChatResponse(rerankingResponse));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 2);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("expert2", result.get(0));
        assertEquals("expert1", result.get(1));
    }

    @Test
    void testRerank_WithoutLLMModel_ReturnsOriginalOrder() {
        // Arrange - Create reranker without LLM model
        SemanticRerankerImpl rerankerWithoutModel = new SemanticRerankerImpl(
                null, // No LLM model
                rerankingPromptTemplate,
                employeeRepository,
                workExperienceRepository,
                new ObjectMapper()
        );

        // Act
        List<String> result = rerankerWithoutModel.rerank(TEST_QUERY, TEST_EXPERT_IDS, 10);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        // Should return in original order (fallback behavior)
        assertEquals("expert1", result.get(0));
        assertEquals("expert2", result.get(1));
        assertEquals("expert3", result.get(2));

        // Should not call LLM
        verify(rerankingChatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testRerank_WithLLMError_FallsBackToOriginalOrder() {
        // Arrange
        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM error"));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 10);

        // Assert - Should fallback to original order
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("expert1", result.get(0));
    }

    @Test
    void testRerank_WithInvalidJSON_FallsBackToOriginalOrder() {
        // Arrange
        String invalidResponse = "This is not valid JSON";

        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenReturn(createChatResponse(invalidResponse));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 10);

        // Assert - When JSON parsing fails, should fallback to original order
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("expert1", result.get(0));
        assertEquals("expert2", result.get(1));
        assertEquals("expert3", result.get(2));
    }

    @Test
    void testRerank_WithEmptyExpertIds_ReturnsEmptyList() {
        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, List.of(), 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Should not call LLM or repositories
        verify(rerankingChatModel, never()).call(any(Prompt.class));
        verify(employeeRepository, never()).findByIds(any());
    }

    @Test
    void testRerank_WithNullQuery_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            semanticReranker.rerank(null, TEST_EXPERT_IDS, 10);
        });
    }

    @Test
    void testRerank_WithBlankQuery_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            semanticReranker.rerank("   ", TEST_EXPERT_IDS, 10);
        });
    }

    @Test
    void testRerank_WithInvalidMaxResults_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 0);
        });
    }

    @Test
    void testCalculateRelevanceScores_WithLLMModel_ReturnsScores() throws Exception {
        // Arrange
        String rerankingResponse = """
                [
                  {"expertId": "expert2", "score": 0.95, "reason": "Strong match"},
                  {"expertId": "expert1", "score": 0.87, "reason": "Good match"},
                  {"expertId": "expert3", "score": 0.75, "reason": "Partial match"}
                ]
                """;

        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenReturn(createChatResponse(rerankingResponse));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        Map<String, Double> scores = semanticReranker.calculateRelevanceScores(TEST_QUERY, TEST_EXPERT_IDS);

        // Assert
        assertNotNull(scores);
        assertEquals(3, scores.size());
        assertEquals(0.95, scores.get("expert2"), 0.01);
        assertEquals(0.87, scores.get("expert1"), 0.01);
        assertEquals(0.75, scores.get("expert3"), 0.01);

        verify(rerankingPromptTemplate).render(anyMap());
        verify(rerankingChatModel).call(any(Prompt.class));
    }

    @Test
    void testCalculateRelevanceScores_WithoutLLMModel_ReturnsPlaceholderScores() {
        // Arrange - Create reranker without LLM model
        SemanticRerankerImpl rerankerWithoutModel = new SemanticRerankerImpl(
                null, // No LLM model
                rerankingPromptTemplate,
                employeeRepository,
                workExperienceRepository,
                new ObjectMapper()
        );

        // Act
        Map<String, Double> scores = rerankerWithoutModel.calculateRelevanceScores(TEST_QUERY, TEST_EXPERT_IDS);

        // Assert
        assertNotNull(scores);
        assertEquals(3, scores.size());
        // Should return placeholder scores (0.8)
        assertEquals(0.8, scores.get("expert1"), 0.01);
        assertEquals(0.8, scores.get("expert2"), 0.01);
        assertEquals(0.8, scores.get("expert3"), 0.01);

        // Should not call LLM
        verify(rerankingChatModel, never()).call(any(Prompt.class));
    }

    @Test
    void testCalculateRelevanceScores_WithLLMError_ReturnsPlaceholderScores() {
        // Arrange
        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("LLM error"));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        Map<String, Double> scores = semanticReranker.calculateRelevanceScores(TEST_QUERY, TEST_EXPERT_IDS);

        // Assert - Should return placeholder scores
        assertNotNull(scores);
        assertEquals(3, scores.size());
        assertEquals(0.8, scores.get("expert1"), 0.01);
    }

    @Test
    void testCalculateRelevanceScores_WithEmptyExpertIds_ReturnsEmptyMap() {
        // Act
        Map<String, Double> scores = semanticReranker.calculateRelevanceScores(TEST_QUERY, List.of());

        // Assert
        assertNotNull(scores);
        assertTrue(scores.isEmpty());

        // Should not call LLM or repositories
        verify(rerankingChatModel, never()).call(any(Prompt.class));
        verify(employeeRepository, never()).findByIds(any());
    }

    @Test
    void testCalculateRelevanceScores_WithNullQuery_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            semanticReranker.calculateRelevanceScores(null, TEST_EXPERT_IDS);
        });
    }

    @Test
    void testCalculateRelevanceScores_WithBlankQuery_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            semanticReranker.calculateRelevanceScores("   ", TEST_EXPERT_IDS);
        });
    }

    @Test
    void testRerank_WithMarkdownCodeBlocks_ParsesCorrectly() throws Exception {
        // Arrange - Response wrapped in markdown code blocks
        String rerankingResponse = "```json\n" +
                "[\n" +
                "  {\"expertId\": \"expert2\", \"score\": 0.95, \"reason\": \"Strong match\"},\n" +
                "  {\"expertId\": \"expert1\", \"score\": 0.87, \"reason\": \"Good match\"}\n" +
                "]\n" +
                "```";

        when(rerankingPromptTemplate.render(anyMap())).thenReturn("test prompt");
        when(rerankingChatModel.call(any(Prompt.class))).thenReturn(createChatResponse(rerankingResponse));

        when(employeeRepository.findByIds(TEST_EXPERT_IDS)).thenReturn(createTestEmployees());
        when(workExperienceRepository.findByEmployeeIds(TEST_EXPERT_IDS)).thenReturn(createTestWorkExperiences());

        // Act
        List<String> result = semanticReranker.rerank(TEST_QUERY, TEST_EXPERT_IDS, 10);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertEquals("expert2", result.get(0));
        assertEquals("expert1", result.get(1));
    }

    // Helper methods

    private ChatResponse createChatResponse(String content) {
        AssistantMessage assistantMessage = new AssistantMessage(content);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }

    private List<com.berdachuk.expertmatch.employee.domain.Employee> createTestEmployees() {
        return List.of(
                new com.berdachuk.expertmatch.employee.domain.Employee("expert1", "Expert One", "expert1@test.com", "A4", "Fluent", "Available"),
                new com.berdachuk.expertmatch.employee.domain.Employee("expert2", "Expert Two", "expert2@test.com", "A5", "Fluent", "Available"),
                new com.berdachuk.expertmatch.employee.domain.Employee("expert3", "Expert Three", "expert3@test.com", "A3", "Fluent", "Available")
        );
    }

    private Map<String, List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience>> createTestWorkExperiences() {
        return Map.of(
                "expert1", List.of(createWorkExperience("expert1", "Java Project", "Java", "Spring Boot")),
                "expert2", List.of(createWorkExperience("expert2", "Spring Boot Project", "Java", "Spring Boot", "Microservices")),
                "expert3", List.of(createWorkExperience("expert3", "Python Project", "Python", "Django"))
        );
    }

    private com.berdachuk.expertmatch.workexperience.domain.WorkExperience createWorkExperience(
            String employeeId, String projectName, String... technologies) {
        String weId = "we-" + employeeId;
        String projectId = "project-" + employeeId;
        String customerId = "customer-" + employeeId;
        String customerName = "Customer " + employeeId;
        String industry = "Technology";
        String role = "Senior Developer";
        java.time.Instant startDate = java.time.Instant.now().minusSeconds(365 * 24 * 60 * 60); // 1 year ago
        java.time.Instant endDate = java.time.Instant.now();
        String summary = "Project summary for " + projectName;
        String responsibilities = "Responsibilities for " + projectName;
        List<String> techList = List.of(technologies);

        return new com.berdachuk.expertmatch.workexperience.domain.WorkExperience(
                weId,
                employeeId,
                projectId,
                customerId,
                projectName,
                customerName,
                industry,
                role,
                startDate,
                endDate,
                summary,
                responsibilities,
                techList
        );
    }
}

