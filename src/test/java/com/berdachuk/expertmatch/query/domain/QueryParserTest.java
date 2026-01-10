package com.berdachuk.expertmatch.query.domain;

import com.berdachuk.expertmatch.llm.sgr.SGRPatternConfig;
import com.berdachuk.expertmatch.query.sgr.QueryClassificationService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for com.berdachuk.expertmatch.query.domain.QueryParser.
 */
@ExtendWith(MockitoExtension.class)
class QueryParserTest {

    @Mock
    private QueryClassificationService queryClassificationService;

    @Mock
    private SGRPatternConfig sgrConfig;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatModel chatModel;

    @Mock
    private Environment environment;

    @Mock
    private PromptTemplate skillExtractionPromptTemplate;

    @Mock
    private PromptTemplate seniorityExtractionPromptTemplate;

    @Mock
    private PromptTemplate languageExtractionPromptTemplate;

    @Mock
    private PromptTemplate technologyExtractionPromptTemplate;

    private ObjectMapper objectMapper;
    private QueryParser queryParser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(skillExtractionPromptTemplate.render(any())).thenReturn("skill extraction prompt");
        lenient().when(seniorityExtractionPromptTemplate.render(any())).thenReturn("seniority extraction prompt");
        lenient().when(languageExtractionPromptTemplate.render(any())).thenReturn("language extraction prompt");
        lenient().when(technologyExtractionPromptTemplate.render(any())).thenReturn("technology extraction prompt");
    }

    @Test
    void testParseSkills_Success() {
        String query = "Looking for experts in Java, Spring Boot, AWS, and MongoDB";
        String skillsJson = "[\"Java\", \"Spring Boot\", \"AWS\", \"MongoDB\"]";
        String seniorityJson = "[]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(4, result.skills().size());
        assertTrue(result.skills().contains("Java"));
        assertTrue(result.skills().contains("Spring Boot"));
        assertTrue(result.skills().contains("AWS"));
        assertTrue(result.skills().contains("MongoDB"));
    }

    @Test
    void testParseSeniority_Success() {
        String query = "Need A4 or A5 level expert";
        String skillsJson = "[]";
        String seniorityJson = "[\"A4\", \"A5\"]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.seniorityLevels().size());
        assertTrue(result.seniorityLevels().contains("A4"));
        assertTrue(result.seniorityLevels().contains("A5"));
    }

    @Test
    void testParseSeniority_BLevels_Success() {
        String query = "Need B2 or B3 level manager";
        String skillsJson = "[]";
        String seniorityJson = "[\"B2\", \"B3\"]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.seniorityLevels().size());
        assertTrue(result.seniorityLevels().contains("B2"));
        assertTrue(result.seniorityLevels().contains("B3"));
    }

    @Test
    void testParseSeniority_CLevels_Success() {
        String query = "Need C1 or C2 level executive";
        String skillsJson = "[]";
        String seniorityJson = "[\"C1\", \"C2\"]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.seniorityLevels().size());
        assertTrue(result.seniorityLevels().contains("C1"));
        assertTrue(result.seniorityLevels().contains("C2"));
    }

    @Test
    void testParseSeniority_MixedLevels_Success() {
        String query = "Need A5 or B1 level expert";
        String skillsJson = "[]";
        String seniorityJson = "[\"A5\", \"B1\"]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.seniorityLevels().size());
        assertTrue(result.seniorityLevels().contains("A5"));
        assertTrue(result.seniorityLevels().contains("B1"));
    }

    @Test
    void testParseLanguage_Success() {
        String query = "Need expert with English C1+";
        String skillsJson = "[]";
        String seniorityJson = "[]";
        String languageJson = "{\"language\": \"English\", \"proficiency\": \"C1\"}";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals("C1", result.language());
    }

    @Test
    void testParseTechnologies_Success() {
        String query = "Looking for Java 21+ and Spring Boot 3.5";
        String skillsJson = "[]";
        String seniorityJson = "[]";
        String languageJson = "null";
        String technologiesJson = "[\"Java 21+\", \"Spring Boot 3.5\"]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.technologies().size());
        assertTrue(result.technologies().contains("Java 21+"));
        assertTrue(result.technologies().contains("Spring Boot 3.5"));
    }

    @Test
    void testParseIntent() {
        String query = "Need a team for a banking app";
        String skillsJson = "[]";
        String seniorityJson = "[]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsJson, seniorityJson, languageJson, technologiesJson);

        QueryParser parser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = parser.parse(query);

        assertNotNull(result);
        assertEquals("team_formation", result.intent());
    }

    @Test
    void testParseSkills_WithMarkdownCodeBlock() {
        String query = "Looking for experts in Java and Spring Boot";
        String skillsResponse = "```json\n[\"Java\", \"Spring Boot\"]\n```";
        String seniorityJson = "[]";
        String languageJson = "null";
        String technologiesJson = "[]";

        mockChatClientResponses(skillsResponse, seniorityJson, languageJson, technologiesJson);

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery result = queryParser.parse(query);

        assertNotNull(result);
        assertEquals(2, result.skills().size());
        assertTrue(result.skills().contains("Java"));
        assertTrue(result.skills().contains("Spring Boot"));
    }

    @Test
    void testParseSkills_EmptyResponse_ThrowsException() {
        String query = "Looking for experts in Java";

        // Mock ChatClient to return null response
        lenient().when(chatClient.prompt()).thenAnswer(invocation -> {
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
                                    return null; // Return null to trigger exception
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

        queryParser = new QueryParser(
                queryClassificationService,
                sgrConfig,
                chatClient,
                chatModel,
                objectMapper,
                environment,
                skillExtractionPromptTemplate,
                seniorityExtractionPromptTemplate,
                languageExtractionPromptTemplate,
                technologyExtractionPromptTemplate
        );

        assertThrows(RuntimeException.class, () -> queryParser.parse(query));
    }

    /**
     * Helper method to mock ChatClient responses for all extraction methods.
     * Order: skills, seniority, language, technologies
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockChatClientResponses(String skillsResponse, String seniorityResponse, String languageResponse, String technologiesResponse) {
        ChatResponse skillsMockResponse = createChatResponse(skillsResponse);
        ChatResponse seniorityMockResponse = createChatResponse(seniorityResponse);
        ChatResponse languageMockResponse = createChatResponse(languageResponse);
        ChatResponse technologiesMockResponse = createChatResponse(technologiesResponse);
        final int[] callCount = {0};

        lenient().when(chatClient.prompt()).thenAnswer((Answer) invocation -> {
            int currentCall = callCount[0]++;
            final ChatResponse resp;
            if (currentCall == 0) {
                resp = skillsMockResponse;
            } else if (currentCall == 1) {
                resp = seniorityMockResponse;
            } else if (currentCall == 2) {
                resp = languageMockResponse;
            } else {
                resp = technologiesMockResponse;
            }

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
        // Verify the structure is correct
        if (response.getResult() == null) {
            throw new IllegalStateException("ChatResponse.getResult() is null - this indicates a Spring AI version mismatch");
        }
        if (response.getResult().getOutput() == null) {
            throw new IllegalStateException("ChatResponse.getResult().getOutput() is null - this indicates a Spring AI version mismatch");
        }
        return response;
    }
}

