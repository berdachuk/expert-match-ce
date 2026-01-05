package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.AnswerGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertEvaluationServiceTest {

    @Mock
    private StructuredOutputHelper structuredOutputHelper;

    @Mock
    private SGRPatternConfig config;

    @Mock
    private SGRPatternConfig.CascadeConfig cascadeConfig;

    @Mock
    private PromptTemplate cascadeEvaluationPromptTemplate;

    private ExpertEvaluationService service;

    @BeforeEach
    void setUp() {
        lenient().when(config.getCascade()).thenReturn(cascadeConfig);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(cascadeConfig.isEnabled()).thenReturn(true);
        lenient().when(cascadeEvaluationPromptTemplate.render(any())).thenReturn("test prompt");

        service = new ExpertEvaluationService(structuredOutputHelper, config, cascadeEvaluationPromptTemplate);
    }

    @Test
    void testEvaluateWithCascade_Success() {
        // Arrange
        String query = "Find expert in Java and Spring Boot";
        AnswerGenerationService.ExpertContext expertContext = new AnswerGenerationService.ExpertContext(
                "expert1", "John Doe", "john@example.com", "A3",
                List.of("Java", "Spring Boot"), List.of("Project1"), null
        );

        ExpertEvaluation expectedEvaluation = new ExpertEvaluation(
                "Experienced Java developer",
                new SkillMatchAnalysis(8, 7, List.of(), List.of("Java", "Spring Boot")),
                new ExperienceAssessment(5, 3.5, true, true),
                new Recommendation(RecommendationType.STRONGLY_RECOMMEND, 90, "Strong match")
        );

        when(structuredOutputHelper.callWithStructuredOutput(anyString(), eq(ExpertEvaluation.class)))
                .thenReturn(expectedEvaluation);

        // Act
        ExpertEvaluation result = service.evaluateWithCascade(query, expertContext);

        // Assert
        assertNotNull(result);
        assertEquals(expectedEvaluation.expertSummary(), result.expertSummary());
        verify(structuredOutputHelper).callWithStructuredOutput(anyString(), eq(ExpertEvaluation.class));
    }

    @Test
    void testEvaluateWithCascade_WhenDisabled_ThrowsException() {
        // Arrange - create new service with disabled config to avoid unnecessary stubbings
        SGRPatternConfig disabledConfig = mock(SGRPatternConfig.class);
        when(disabledConfig.isEnabled()).thenReturn(false);
        ExpertEvaluationService disabledService = new ExpertEvaluationService(structuredOutputHelper, disabledConfig, cascadeEvaluationPromptTemplate);

        String query = "Find expert";
        AnswerGenerationService.ExpertContext expertContext = new AnswerGenerationService.ExpertContext(
                "expert1", "John Doe", null, null, List.of(), List.of(), null
        );

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            disabledService.evaluateWithCascade(query, expertContext);
        });
    }

    @Test
    void testEvaluateWithCascade_WhenCascadeDisabled_ThrowsException() {
        // Arrange - create new service with cascade disabled config to avoid unnecessary stubbings
        SGRPatternConfig cascadeDisabledConfig = mock(SGRPatternConfig.class);
        SGRPatternConfig.CascadeConfig disabledCascadeConfig = mock(SGRPatternConfig.CascadeConfig.class);
        when(cascadeDisabledConfig.getCascade()).thenReturn(disabledCascadeConfig);
        when(cascadeDisabledConfig.isEnabled()).thenReturn(true);
        when(disabledCascadeConfig.isEnabled()).thenReturn(false);
        ExpertEvaluationService cascadeDisabledService = new ExpertEvaluationService(structuredOutputHelper, cascadeDisabledConfig, cascadeEvaluationPromptTemplate);

        String query = "Find expert";
        AnswerGenerationService.ExpertContext expertContext = new AnswerGenerationService.ExpertContext(
                "expert1", "John Doe", null, null, List.of(), List.of(), null
        );

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            cascadeDisabledService.evaluateWithCascade(query, expertContext);
        });
    }

    @Test
    void testEvaluateWithCascade_WhenStructuredOutputFails_ThrowsException() {
        // Arrange
        String query = "Find expert";
        AnswerGenerationService.ExpertContext expertContext = new AnswerGenerationService.ExpertContext(
                "expert1", "John Doe", null, null, List.of(), List.of(), null
        );

        when(structuredOutputHelper.callWithStructuredOutput(anyString(), eq(ExpertEvaluation.class)))
                .thenThrow(new StructuredOutputHelper.StructuredOutputException("Parsing failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            service.evaluateWithCascade(query, expertContext);
        });
    }
}

