package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import com.berdachuk.expertmatch.llm.sgr.impl.CyclePatternServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CyclePatternServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private PromptTemplate cycleEvaluationPromptTemplate;

    private ObjectMapper objectMapper;
    private CyclePatternService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SGRPatternConfig config = mock(SGRPatternConfig.class);
        SGRPatternConfig.CycleConfig cycleConfig = mock(SGRPatternConfig.CycleConfig.class);

        lenient().when(config.getCycle()).thenReturn(cycleConfig);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(cycleConfig.isEnabled()).thenReturn(true);
        lenient().when(cycleEvaluationPromptTemplate.render(any())).thenReturn("test prompt");

        service = new CyclePatternServiceImpl(chatClient, objectMapper, config, cycleEvaluationPromptTemplate);
    }

    // Note: testEvaluateMultipleExperts_Success is skipped due to complex Spring AI ChatClient mocking
    // The service implementation is tested through integration tests

    @Test
    void testEvaluateMultipleExperts_WhenDisabled_ThrowsException() {
        // Arrange - create new service with disabled config to avoid unnecessary stubbings
        SGRPatternConfig disabledConfig = mock(SGRPatternConfig.class);
        when(disabledConfig.isEnabled()).thenReturn(false);
        CyclePatternService disabledService = new CyclePatternServiceImpl(chatClient, objectMapper, disabledConfig, cycleEvaluationPromptTemplate);

        String query = "Find experts";
        List<AnswerGenerationService.ExpertContext> expertContexts = List.of();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            disabledService.evaluateMultipleExperts(query, expertContexts);
        });
    }

    @Test
    void testEvaluateMultipleExperts_WhenCycleDisabled_ThrowsException() {
        // Arrange - create new service with cycle disabled config to avoid unnecessary stubbings
        SGRPatternConfig cycleDisabledConfig = mock(SGRPatternConfig.class);
        SGRPatternConfig.CycleConfig disabledCycleConfig = mock(SGRPatternConfig.CycleConfig.class);
        when(cycleDisabledConfig.getCycle()).thenReturn(disabledCycleConfig);
        when(cycleDisabledConfig.isEnabled()).thenReturn(true);
        when(disabledCycleConfig.isEnabled()).thenReturn(false);
        CyclePatternService cycleDisabledService = new CyclePatternServiceImpl(chatClient, objectMapper, cycleDisabledConfig, cycleEvaluationPromptTemplate);

        String query = "Find experts";
        List<AnswerGenerationService.ExpertContext> expertContexts = List.of();

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            cycleDisabledService.evaluateMultipleExperts(query, expertContexts);
        });
    }

    @Test
    void testEvaluateMultipleExperts_WhenEmptyContexts_ReturnsEmptyList() {
        // Arrange
        String query = "Find experts";
        List<AnswerGenerationService.ExpertContext> expertContexts = List.of();

        // Act
        List<ExpertEvaluation> result = service.evaluateMultipleExperts(query, expertContexts);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testEvaluateMultipleExperts_WhenNullContexts_ReturnsEmptyList() {
        // Arrange
        String query = "Find experts";

        // Act
        List<ExpertEvaluation> result = service.evaluateMultipleExperts(query, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

