package com.berdachuk.expertmatch.query.domain;

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
 * Unit tests for EntityExtractor.
 */
@ExtendWith(MockitoExtension.class)
class EntityExtractorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatModel chatModel;

    @Mock
    private Environment environment;

    @Mock
    private PromptTemplate domainExtractionPromptTemplate;

    @Mock
    private PromptTemplate personExtractionPromptTemplate;

    @Mock
    private PromptTemplate organizationExtractionPromptTemplate;

    @Mock
    private PromptTemplate technologyEntityExtractionPromptTemplate;

    @Mock
    private PromptTemplate projectExtractionPromptTemplate;

    private ObjectMapper objectMapper;
    private EntityExtractor entityExtractor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(domainExtractionPromptTemplate.render(any())).thenReturn("domain extraction prompt");
        lenient().when(personExtractionPromptTemplate.render(any())).thenReturn("person extraction prompt");
        lenient().when(organizationExtractionPromptTemplate.render(any())).thenReturn("organization extraction prompt");
        lenient().when(technologyEntityExtractionPromptTemplate.render(any())).thenReturn("technology extraction prompt");
        lenient().when(projectExtractionPromptTemplate.render(any())).thenReturn("project extraction prompt");
    }

    @Test
    void testExtractDomains_Success() {
        String query = "Need expert for banking application";
        String domainsJson = "[{\"type\": \"domain\", \"name\": \"banking\", \"id\": \"banking\"}]";

        mockChatClientResponse(domainsJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(1, result.domains().size());
        assertEquals("banking", result.domains().get(0).name());
    }

    @Test
    void testExtractPersons_Success() {
        String query = "Need expert like John Doe";
        String personsJson = "[{\"type\": \"person\", \"name\": \"John Doe\", \"id\": \"john-doe\"}]";

        mockChatClientResponse(personsJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(1, result.persons().size());
        assertEquals("John Doe", result.persons().get(0).name());
        assertEquals("john-doe", result.persons().get(0).id());
    }

    @Test
    void testExtractOrganizations_Success() {
        String query = "Need expert for Acme Corp project";
        String organizationsJson = "[{\"type\": \"organization\", \"name\": \"Acme Corp\", \"id\": \"acme-corp\"}]";

        mockChatClientResponse(organizationsJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(1, result.organizations().size());
        assertEquals("Acme Corp", result.organizations().get(0).name());
    }

    @Test
    void testExtractTechnologies_Success() {
        String query = "Need expert in Java and Spring Boot";
        String technologiesJson = "[{\"type\": \"technology\", \"name\": \"Java\", \"id\": \"java\"}, {\"type\": \"technology\", \"name\": \"Spring Boot\", \"id\": \"spring-boot\"}]";

        mockChatClientResponse(technologiesJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(2, result.technologies().size());
        assertTrue(result.technologies().stream().anyMatch(e -> e.name().equals("Java")));
        assertTrue(result.technologies().stream().anyMatch(e -> e.name().equals("Spring Boot")));
    }

    @Test
    void testExtractProjects_Success() {
        String query = "Need expert for Project Alpha";
        String projectsJson = "[{\"type\": \"project\", \"name\": \"Project Alpha\", \"id\": \"project-alpha\"}]";

        mockChatClientResponse(projectsJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(1, result.projects().size());
        assertEquals("Project Alpha", result.projects().get(0).name());
    }

    @Test
    void testExtractEntities_WithMarkdownCodeBlock() {
        String query = "Need expert like John Doe for Acme Corp";
        String personsJson = "```json\n[{\"type\": \"person\", \"name\": \"John Doe\", \"id\": \"john-doe\"}]\n```";
        String organizationsJson = "```json\n[{\"type\": \"organization\", \"name\": \"Acme Corp\", \"id\": \"acme-corp\"}]\n```";

        // Mock multiple ChatClient calls - one for persons, one for organizations
        mockChatClientResponses(personsJson, organizationsJson);

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        EntityExtractor.ExtractedEntities result = entityExtractor.extract(query);

        assertNotNull(result);
        assertEquals(1, result.persons().size());
        assertEquals("John Doe", result.persons().get(0).name());
    }

    @Test
    void testExtractEntities_EmptyResponse_ThrowsException() {
        String query = "Need expert for banking";

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

        entityExtractor = new EntityExtractor(
                chatClient,
                chatModel,
                objectMapper,
                environment,
                domainExtractionPromptTemplate,
                personExtractionPromptTemplate,
                organizationExtractionPromptTemplate,
                technologyEntityExtractionPromptTemplate,
                projectExtractionPromptTemplate
        );

        assertThrows(RuntimeException.class, () -> entityExtractor.extract(query));
    }

    /**
     * Helper method to mock ChatClient response.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockChatClientResponse(String responseText) {
        ChatResponse mockResponse = createChatResponse(responseText);

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

        lenient().when(chatClient.prompt()).thenAnswer((Answer) invocation -> {
            int currentCall = callCount[0]++;
            final ChatResponse resp = currentCall == 0 ? firstMockResponse : secondMockResponse;

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

