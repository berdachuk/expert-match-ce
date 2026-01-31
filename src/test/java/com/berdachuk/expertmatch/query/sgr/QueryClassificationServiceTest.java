package com.berdachuk.expertmatch.query.sgr;

import com.berdachuk.expertmatch.llm.sgr.SGRPatternConfig;
import com.berdachuk.expertmatch.llm.sgr.StructuredOutputHelper;
import com.berdachuk.expertmatch.query.sgr.impl.QueryClassificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.PromptTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryClassificationServiceTest {

    @Mock
    private StructuredOutputHelper structuredOutputHelper;

    @Mock
    private SGRPatternConfig config;

    @Mock
    private SGRPatternConfig.RoutingConfig routingConfig;

    @Mock
    private PromptTemplate queryClassificationPromptTemplate;

    private QueryClassificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(config.getRouting()).thenReturn(routingConfig);
        lenient().when(config.isEnabled()).thenReturn(true);
        lenient().when(routingConfig.isEnabled()).thenReturn(true);
        lenient().when(queryClassificationPromptTemplate.render(any())).thenReturn("test prompt");

        service = new QueryClassificationServiceImpl(structuredOutputHelper, config, queryClassificationPromptTemplate);
    }

    @Test
    void testClassifyWithRouting_Success() {
        // Arrange
        String query = "Find experts in Java and Spring Boot";
        QueryClassification expectedClassification = new QueryClassification(
                QueryIntent.EXPERT_SEARCH,
                95,
                "Query is clearly looking for individual experts with specific skills",
                java.util.Map.of("skills", java.util.List.of("Java", "Spring Boot"))
        );

        when(structuredOutputHelper.callWithStructuredOutput(anyString(), eq(QueryClassification.class)))
                .thenReturn(expectedClassification);

        // Act
        QueryClassification result = service.classifyWithRouting(query);

        // Assert
        assertNotNull(result);
        assertEquals(QueryIntent.EXPERT_SEARCH, result.intent());
        assertEquals(95, result.confidence());
        verify(structuredOutputHelper).callWithStructuredOutput(anyString(), eq(QueryClassification.class));
    }

    @Test
    void testClassifyWithRouting_WhenDisabled_ThrowsException() {
        // Arrange - create new service with disabled config to avoid unnecessary stubbings
        SGRPatternConfig disabledConfig = mock(SGRPatternConfig.class);
        when(disabledConfig.isEnabled()).thenReturn(false);
        QueryClassificationService disabledService = new QueryClassificationServiceImpl(structuredOutputHelper, disabledConfig, queryClassificationPromptTemplate);

        String query = "Find experts";

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            disabledService.classifyWithRouting(query);
        });
    }

    @Test
    void testClassifyWithRouting_WhenRoutingDisabled_ThrowsException() {
        // Arrange - create new service with routing disabled config to avoid unnecessary stubbings
        SGRPatternConfig routingDisabledConfig = mock(SGRPatternConfig.class);
        SGRPatternConfig.RoutingConfig disabledRoutingConfig = mock(SGRPatternConfig.RoutingConfig.class);
        when(routingDisabledConfig.getRouting()).thenReturn(disabledRoutingConfig);
        when(routingDisabledConfig.isEnabled()).thenReturn(true);
        when(disabledRoutingConfig.isEnabled()).thenReturn(false);
        QueryClassificationService routingDisabledService = new QueryClassificationServiceImpl(structuredOutputHelper, routingDisabledConfig, queryClassificationPromptTemplate);

        String query = "Find experts";

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            routingDisabledService.classifyWithRouting(query);
        });
    }

    @Test
    void testClassifyWithRouting_WhenStructuredOutputFails_ThrowsException() {
        // Arrange
        String query = "Find experts";

        when(structuredOutputHelper.callWithStructuredOutput(anyString(), eq(QueryClassification.class)))
                .thenThrow(new StructuredOutputHelper.StructuredOutputException("Parsing failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            service.classifyWithRouting(query);
        });
    }
}

