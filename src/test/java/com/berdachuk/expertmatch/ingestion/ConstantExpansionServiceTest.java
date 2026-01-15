package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.ingestion.service.ConstantExpansionService;
import com.berdachuk.expertmatch.ingestion.service.impl.ConstantExpansionServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConstantExpansionService.
 */
@ExtendWith(MockitoExtension.class)
class ConstantExpansionServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private PromptTemplate technologiesExpansionTemplate;

    @Mock
    private PromptTemplate toolsExpansionTemplate;

    @Mock
    private PromptTemplate projectTypesExpansionTemplate;

    @Mock
    private PromptTemplate teamNamesExpansionTemplate;

    @Mock
    private PromptTemplate technologyCategoriesExpansionTemplate;

    @Mock
    private PromptTemplate technologySynonymsExpansionTemplate;

    private ObjectMapper objectMapper;
    private ConstantExpansionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ConstantExpansionServiceImpl(
                chatClient,
                technologiesExpansionTemplate,
                toolsExpansionTemplate,
                projectTypesExpansionTemplate,
                teamNamesExpansionTemplate,
                technologyCategoriesExpansionTemplate,
                technologySynonymsExpansionTemplate,
                objectMapper
        );
    }

    @Test
    void testExpandTechnologies_Success() {
        // Given
        List<String> existing = Arrays.asList("Java", "Python", "Node.js");
        String mockResponse = "[\"C#\", \".NET\", \"Go\", \"Rust\"]";

        when(technologiesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        List<String> result = service.expandTechnologies(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
        assertTrue(result.containsAll(existing));
        assertTrue(result.contains("C#"));
        assertTrue(result.contains(".NET"));
    }

    @Test
    void testExpandTechnologies_FallbackOnError() {
        // Given
        List<String> existing = Arrays.asList("Java", "Python");

        when(technologiesExpansionTemplate.render(any())).thenThrow(new RuntimeException("LLM error"));

        // When
        List<String> result = service.expandTechnologies(existing);

        // Then
        assertNotNull(result);
        assertEquals(existing.size(), result.size());
        assertTrue(result.containsAll(existing));
    }

    @Test
    void testExpandTechnologies_Caching() {
        // Given
        List<String> existing = Arrays.asList("Java", "Python");
        String mockResponse = "[\"C#\", \".NET\"]";

        when(technologiesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When - call twice
        List<String> result1 = service.expandTechnologies(existing);
        List<String> result2 = service.expandTechnologies(existing);

        // Then - should only call LLM once (cached on second call)
        verify(technologiesExpansionTemplate, times(1)).render(any());
        assertEquals(result1.size(), result2.size());
    }

    @Test
    void testExpandTools_Success() {
        // Given
        List<String> existing = Arrays.asList("IntelliJ IDEA", "VS Code", "Git");
        String mockResponse = "[\"PyCharm\", \"Eclipse\", \"Sublime Text\"]";

        when(toolsExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        List<String> result = service.expandTools(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
        assertTrue(result.containsAll(existing));
    }

    @Test
    void testExpandProjectTypes_Success() {
        // Given
        List<String> existing = Arrays.asList("Web Application", "Microservices");
        String mockResponse = "[\"REST API\", \"GraphQL API\", \"Mobile App\"]";

        when(projectTypesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        List<String> result = service.expandProjectTypes(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
    }

    @Test
    void testExpandTeamNames_Success() {
        // Given
        List<String> existing = Arrays.asList("Backend Team", "Frontend Team");
        String mockResponse = "[\"Mobile Team\", \"Cloud Team\", \"Security Team\"]";

        when(teamNamesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        List<String> result = service.expandTeamNames(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
    }

    @Test
    void testExpandTechnologyCategories_Success() {
        // Given
        List<String> technologies = Arrays.asList("C#", ".NET", "MongoDB");
        Map<String, String> existing = new HashMap<>();
        existing.put("Java", "Programming Language");
        String mockResponse = "{\"C#\": \"Programming Language\", \".NET\": \"Framework\", \"MongoDB\": \"Database\"}";

        when(technologyCategoriesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        Map<String, String> result = service.expandTechnologyCategories(technologies, existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
        assertTrue(result.containsKey("Java"));
        assertTrue(result.containsKey("C#"));
    }

    @Test
    void testExpandTechnologySynonyms_Success() {
        // Given
        List<String> technologies = Arrays.asList("C#", "MongoDB");
        Map<String, String[]> existing = new HashMap<>();
        existing.put("Java", new String[]{"Java Programming"});
        String mockResponse = "{\"C#\": [\"C Sharp\", \"CSharp\"], \"MongoDB\": [\"Mongo\"]}";

        when(technologySynonymsExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        Map<String, String[]> result = service.expandTechnologySynonyms(technologies, existing);

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= existing.size());
        assertTrue(result.containsKey("Java"));
        assertTrue(result.containsKey("C#"));
    }

    @Test
    void testParseJsonArray_WithMarkdown() {
        // Given - response wrapped in markdown code block
        String jsonWithMarkdown = "```json\n[\"Technology1\", \"Technology2\"]\n```";
        List<String> existing = List.of("Java");

        when(technologiesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(jsonWithMarkdown);

        // When
        List<String> result = service.expandTechnologies(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Technology1"));
        assertTrue(result.contains("Technology2"));
    }

    @Test
    void testParseJsonArray_WithoutMarkdown() {
        // Given - plain JSON
        String plainJson = "[\"Technology1\", \"Technology2\"]";
        List<String> existing = List.of("Java");

        when(technologiesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(plainJson);

        // When
        List<String> result = service.expandTechnologies(existing);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Technology1"));
        assertTrue(result.contains("Technology2"));
    }

    @Test
    void testParseJsonObject_Success() {
        // Given
        List<String> technologies = List.of("C#");
        Map<String, String> existing = new HashMap<>();
        String mockResponse = "{\"C#\": \"Programming Language\"}";

        when(technologyCategoriesExpansionTemplate.render(any())).thenReturn("mock prompt");
        mockChatClientResponse(mockResponse);

        // When
        Map<String, String> result = service.expandTechnologyCategories(technologies, existing);

        // Then
        assertNotNull(result);
        assertEquals("Programming Language", result.get("C#"));
    }

    /**
     * Helper method to mock ChatClient fluent API and return a ChatResponse.
     * Uses the same pattern as QueryParserTest to handle Spring AI's fluent API.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockChatClientResponse(String responseContent) {
        ChatResponse mockChatResponse = createMockChatResponse(responseContent);

        lenient().when(chatClient.prompt()).thenAnswer((Answer) invocation -> {
            Class<?> promptReturnType = invocation.getMethod().getReturnType();
            Object requestMock = mock(promptReturnType, withSettings().defaultAnswer(inv -> {
                String methodName = inv.getMethod().getName();
                if (methodName.equals("user") && inv.getArguments().length == 1 && inv.getArguments()[0] instanceof String) {
                    Class<?> userReturnType = inv.getMethod().getReturnType();
                    Object requestSpecMock = mock(userReturnType, withSettings().defaultAnswer(inv2 -> {
                        String methodName2 = inv2.getMethod().getName();
                        if (methodName2.equals("call")) {
                            Class<?> callReturnType = inv2.getMethod().getReturnType();
                            Object responseMock = mock(callReturnType, withSettings().defaultAnswer(inv3 -> {
                                String methodName3 = inv3.getMethod().getName();
                                if (methodName3.equals("chatResponse")) {
                                    return mockChatResponse;
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

    /**
     * Helper method to create a mock ChatResponse.
     */
    private ChatResponse createMockChatResponse(String content) {
        AssistantMessage assistantMessage = new AssistantMessage(content);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }
}

