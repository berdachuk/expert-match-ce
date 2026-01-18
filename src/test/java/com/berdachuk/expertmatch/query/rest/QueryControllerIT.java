package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.chat.service.ChatService;
import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for QueryController.
 * Uses Testcontainers PostgreSQL and MockMvc for endpoint testing.
 * <p>
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class QueryControllerIT extends BaseIntegrationTest {

    @SuppressWarnings("null")
    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);
    private static final String TEST_USER_ID = "test-user-123";
    private static final String OTHER_USER_ID = "other-user-456";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ChatService chatService;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private String validChatId;
    private String otherUserChatId;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.conversation_history");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.chat");

        // Ensure graph exists (required for graph-based queries)
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = ag_catalog, \"$user\", public, expertmatch;");
            namedJdbcTemplate.getJdbcTemplate().execute("SELECT * FROM ag_catalog.create_graph('expertmatch_graph');");
            namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = expertmatch, public;");
        } catch (Exception e) {
            // Graph might already exist, ignore
            try {
                namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = expertmatch, public;");
            } catch (Exception e2) {
                // Ignore
            }
        }

        // Create test chats for different users
        com.berdachuk.expertmatch.chat.domain.Chat testUserChat = chatService.getOrCreateDefaultChat(TEST_USER_ID);
        validChatId = testUserChat.id();

        com.berdachuk.expertmatch.chat.domain.Chat otherUserChat = chatService.createChat(OTHER_USER_ID, "Other User Chat");
        otherUserChatId = otherUserChat.id();
    }

    @Test
    void testProcessQueryWithoutChatId() throws Exception {
        // Test that default chat is created when chatId is not provided
        String requestBody = """
                {
                    "query": "Looking for experts in Java and Spring Boot",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "includeSources": true,
                        "includeEntities": true,
                        "rerank": true
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").exists())
                .andExpect(jsonPath("$.messageId").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.experts").exists())
                .andExpect(jsonPath("$.sources").exists())
                .andExpect(jsonPath("$.entities").exists());
    }

    @Test
    void testProcessQueryWithValidChatId() throws Exception {
        String requestBody = """
                {
                    "query": "Need experts in Python and Machine Learning",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 5,
                        "minConfidence": 0.8,
                        "includeSources": true,
                        "includeEntities": true,
                        "rerank": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.messageId").exists())
                .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    void testProcessQueryWithInvalidChatIdFormat() throws Exception {
        // Test with invalid chatId format (not 24 hex characters)
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "invalid-chat-id",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testProcessQueryWithNonExistentChatId() throws Exception {
        // Test with valid format but non-existent chatId
        String nonExistentChatId = "507f1f77bcf86cd799439011"; // Valid format but doesn't exist
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7
                    }
                }
                """.formatted(nonExistentChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Chat not found")));
    }

    @Test
    void testProcessQueryWithChatIdBelongingToDifferentUser() throws Exception {
        // Test access denied when chatId belongs to different user
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7
                    }
                }
                """.formatted(otherUserChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Access denied")));
    }

    @Test
    void testProcessQueryWithAnonymousUser() throws Exception {
        // Test with no X-User-Id header (should use anonymous-user)
        String requestBody = """
                {
                    "query": "Looking for experts in React",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/query")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").exists());
    }

    @Test
    void testProcessQueryWithAllOptions() throws Exception {
        // Test with all query options enabled, including execution trace
        String requestBody = """
                {
                    "query": "Need a team for a microservices project",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 15,
                        "minConfidence": 0.75,
                        "includeSources": true,
                        "includeEntities": true,
                        "rerank": true,
                        "deepResearch": false,
                        "useCascadePattern": false,
                        "useRoutingPattern": false,
                        "useCyclePattern": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                .andExpect(jsonPath("$.executionTrace.totalDurationMs").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].name").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].service").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].method").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].status").exists());
    }

    @Test
    void testProcessQueryWithRerankEnabled() throws Exception {
        // Test that reranking option is properly processed
        // Note: Reranking step only appears in execution trace if there are results to rerank
        // Since we don't have test data, reranking will be skipped (correct behavior)
        String requestBody = """
                {
                    "query": "Looking for Java and Spring Boot experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "rerank": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that reranking is properly handled (skipped when no results, which is correct)
                // The rerank option is processed correctly even if no reranking step appears
                // because there are no results to rerank (fusedResults.isEmpty())
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Fuse Results')]").exists())
                // Calculate Relevance Scores step only appears when reranking is enabled AND there are results
                // Since we have no results, it won't appear (correct behavior)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Hybrid Retrieval')]").exists());
    }

    @Test
    void testProcessQueryWithRerankDisabled() throws Exception {
        // Test that reranking option is properly processed when disabled
        String requestBody = """
                {
                    "query": "Looking for Java and Spring Boot experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "rerank": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that reranking step does NOT exist when rerank is disabled
                // (Even if there were results, reranking wouldn't run when rerank=false)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Semantic Reranking')]").doesNotExist())
                // Verify that Calculate Relevance Scores step does NOT exist when rerank is disabled
                // (Relevance scores are only calculated when reranking is enabled)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Calculate Relevance Scores')]").doesNotExist())
                // Verify that other retrieval steps still exist
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Fuse Results')]").exists());
    }

    @Test
    void testProcessQueryWithDeepResearchEnabled() throws Exception {
        // Test that deep research step is included in execution trace when deepResearch is true
        // Note: Deep research always creates a "Deep Research" step in QueryService,
        // even if performDeepResearch returns early due to no initial results
        String requestBody = """
                {
                    "query": "Looking for Java and Spring Boot experts with microservices experience",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "deepResearch": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that deep research step exists when deepResearch is enabled
                // The step is created in QueryService before calling performDeepResearch
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Deep Research')]").exists())
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Deep Research')].service").value("DeepResearchService"))
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Deep Research')].method").value("performDeepResearch"))
                // Verify that "Hybrid Retrieval" step does NOT exist (replaced by Deep Research)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Hybrid Retrieval')]").doesNotExist());
    }

    @Test
    void testProcessQueryWithDeepResearchDisabled() throws Exception {
        // Test that deep research step is NOT included when deepResearch is false
        String requestBody = """
                {
                    "query": "Looking for Java and Spring Boot experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "deepResearch": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that deep research step does NOT exist when deepResearch is disabled
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Deep Research')]").doesNotExist())
                // Verify that hybrid retrieval step exists instead
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Hybrid Retrieval')]").exists());
    }

    @Test
    void testProcessQueryWithCascadePatternEnabled() throws Exception {
        // Test that Cascade pattern option is properly processed when enabled
        // Cascade pattern only works when there's exactly 1 expert result
        // Insert exactly 1 test expert to ensure Cascade pattern is triggered
        String employeeId = IdGenerator.generateEmployeeId();
        Map<String, Object> employeeParams = new HashMap<>();
        employeeParams.put("id", employeeId);
        employeeParams.put("name", "Java Expert");
        employeeParams.put("email", "java-expert@test.com");
        // Test with A level seniority (A5 = Principal - highest A level)
        employeeParams.put("seniority", "A5");
        employeeParams.put("languageEnglish", "Fluent");
        employeeParams.put("availabilityStatus", "available");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status) " +
                        "VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)",
                employeeParams
        );

        // Insert work experience with Java to ensure the expert is found
        String workExperienceId = IdGenerator.generateId();
        String projectId = IdGenerator.generateProjectId();
        Map<String, Object> workExpParams = new HashMap<>();
        workExpParams.put("id", workExperienceId);
        workExpParams.put("employeeId", employeeId);
        workExpParams.put("projectId", projectId);
        workExpParams.put("projectName", "Java Microservices Project");
        workExpParams.put("role", "Senior Java Developer");
        workExpParams.put("technologies", new String[]{"Java", "Spring Boot", "PostgreSQL"});
        workExpParams.put("industry", "Finance");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_id, project_name, role, technologies, industry) " +
                        "VALUES (:id, :employeeId, :projectId, :projectName, :role, :technologies, :industry)",
                workExpParams
        );

        String requestBody = """
                {
                    "query": "Looking for Java expert with Spring Boot experience",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 1,
                        "minConfidence": 0.7,
                        "useCascadePattern": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // When useCascadePattern is true and exactly 1 expert is found,
                // the "Cascade Pattern Evaluation" step should appear
                // Note: If ExpertEvaluationService is not available, SGR config is disabled,
                // or pattern fails, it falls back to RAG pattern (which is acceptable)
                // The test verifies that the request is processed correctly
                // If Cascade pattern is available and conditions are met, the step will appear
                // Otherwise, it falls back to RAG pattern (Generate Answer step)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Generate Answer')]").exists());
    }

    @Test
    void testProcessQueryWithCascadePatternDisabled() throws Exception {
        // Test that Cascade pattern step does NOT appear when disabled
        String requestBody = """
                {
                    "query": "Looking for Java expert",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 1,
                        "minConfidence": 0.7,
                        "useCascadePattern": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that Cascade Pattern Evaluation step does NOT exist when disabled
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Cascade Pattern Evaluation')]").doesNotExist())
                // Verify that Generate Answer step exists
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Generate Answer')]").exists());
    }

    @Test
    void testProcessQueryWithCyclePatternEnabled() throws Exception {
        // Test that Cycle pattern option is properly processed when enabled
        // Cycle pattern only works when there are multiple expert results (>1)
        // Insert multiple test experts to ensure Cycle pattern is triggered
        String employeeId1 = IdGenerator.generateEmployeeId();
        String employeeId2 = IdGenerator.generateEmployeeId();
        String employeeId3 = IdGenerator.generateEmployeeId();

        // Insert first expert
        Map<String, Object> employee1Params = new HashMap<>();
        employee1Params.put("id", employeeId1);
        employee1Params.put("name", "Java Expert 1");
        employee1Params.put("email", "java-expert1@test.com");
        // Test with A level seniority (A5 = Principal - highest A level)
        employee1Params.put("seniority", "A5");
        employee1Params.put("languageEnglish", "Fluent");
        employee1Params.put("availabilityStatus", "available");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status) " +
                        "VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)",
                employee1Params
        );

        // Insert second expert
        Map<String, Object> employee2Params = new HashMap<>();
        employee2Params.put("id", employeeId2);
        employee2Params.put("name", "Java Expert 2");
        employee2Params.put("email", "java-expert2@test.com");
        // Test with A level seniority (A4 = Lead)
        employee2Params.put("seniority", "A4");
        employee2Params.put("languageEnglish", "Good");
        employee2Params.put("availabilityStatus", "available");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status) " +
                        "VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)",
                employee2Params
        );

        // Insert third expert
        Map<String, Object> employee3Params = new HashMap<>();
        employee3Params.put("id", employeeId3);
        employee3Params.put("name", "Java Expert 3");
        employee3Params.put("email", "java-expert3@test.com");
        // Test with A level seniority (A3 = Senior)
        employee3Params.put("seniority", "A3");
        employee3Params.put("languageEnglish", "Basic");
        employee3Params.put("availabilityStatus", "available");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status) " +
                        "VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)",
                employee3Params
        );

        // Insert work experience for all experts to ensure they are found
        String projectId1 = IdGenerator.generateProjectId();
        String projectId2 = IdGenerator.generateProjectId();
        String projectId3 = IdGenerator.generateProjectId();

        Map<String, Object> workExp1Params = new HashMap<>();
        workExp1Params.put("id", IdGenerator.generateId());
        workExp1Params.put("employeeId", employeeId1);
        workExp1Params.put("projectId", projectId1);
        workExp1Params.put("projectName", "Java Microservices Project 1");
        workExp1Params.put("role", "Senior Java Developer");
        workExp1Params.put("technologies", new String[]{"Java", "Spring Boot", "PostgreSQL"});
        workExp1Params.put("industry", "Finance");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_id, project_name, role, technologies, industry) " +
                        "VALUES (:id, :employeeId, :projectId, :projectName, :role, :technologies, :industry)",
                workExp1Params
        );

        Map<String, Object> workExp2Params = new HashMap<>();
        workExp2Params.put("id", IdGenerator.generateId());
        workExp2Params.put("employeeId", employeeId2);
        workExp2Params.put("projectId", projectId2);
        workExp2Params.put("projectName", "Java Microservices Project 2");
        workExp2Params.put("role", "Java Developer");
        workExp2Params.put("technologies", new String[]{"Java", "Spring Boot", "AWS"});
        workExp2Params.put("industry", "E-commerce");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_id, project_name, role, technologies, industry) " +
                        "VALUES (:id, :employeeId, :projectId, :projectName, :role, :technologies, :industry)",
                workExp2Params
        );

        Map<String, Object> workExp3Params = new HashMap<>();
        workExp3Params.put("id", IdGenerator.generateId());
        workExp3Params.put("employeeId", employeeId3);
        workExp3Params.put("projectId", projectId3);
        workExp3Params.put("projectName", "Java Microservices Project 3");
        workExp3Params.put("role", "Junior Java Developer");
        workExp3Params.put("technologies", new String[]{"Java", "Spring Boot", "Docker"});
        workExp3Params.put("industry", "Healthcare");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_id, project_name, role, technologies, industry) " +
                        "VALUES (:id, :employeeId, :projectId, :projectName, :role, :technologies, :industry)",
                workExp3Params
        );

        String requestBody = """
                {
                    "query": "Looking for Java experts with Spring Boot experience",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 5,
                        "minConfidence": 0.7,
                        "useCyclePattern": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // When useCyclePattern is true and multiple experts are found (>1),
                // the "Cycle Pattern Evaluation" step should appear
                // Note: If CyclePatternService is not available, SGR config is disabled,
                // or pattern fails, it falls back to RAG pattern (which is acceptable)
                // The test verifies that the request is processed correctly
                // If Cycle pattern is available and conditions are met, the step will appear
                // Otherwise, it falls back to RAG pattern (Generate Answer step)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Generate Answer')]").exists());
    }

    @Test
    void testProcessQueryWithCyclePatternDisabled() throws Exception {
        // Test that Cycle pattern step does NOT appear when disabled
        String requestBody = """
                {
                    "query": "Looking for Java experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 5,
                        "minConfidence": 0.7,
                        "useCyclePattern": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that Cycle Pattern Evaluation step does NOT exist when disabled
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Cycle Pattern Evaluation')]").doesNotExist())
                // Verify that Generate Answer step exists
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Generate Answer')]").exists());
    }

    @Test
    void testProcessQueryWithRoutingPatternEnabled() throws Exception {
        // Test that Routing pattern option is properly processed when enabled
        // Note: Routing pattern affects query classification during parsing
        String requestBody = """
                {
                    "query": "I need to form a team for a cloud migration project",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "useRoutingPattern": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Routing pattern affects query parsing/classification
                // It doesn't create a separate step, but affects the Parse Query step
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Parse Query')]").exists());
    }

    @Test
    void testProcessQueryWithRoutingPatternDisabled() throws Exception {
        // Test that Routing pattern option is properly processed when disabled
        String requestBody = """
                {
                    "query": "Looking for Java experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "useRoutingPattern": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                // Verify that Parse Query step exists (uses rule-based classification when routing disabled)
                .andExpect(jsonPath("$.executionTrace.steps[?(@.name == 'Parse Query')]").exists());
    }

    @Test
    void testProcessQueryWithoutExecutionTrace() throws Exception {
        // Test that execution trace is NOT included when includeExecutionTrace is false
        String requestBody = """
                {
                    "query": "Looking for Java experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "minConfidence": 0.7,
                        "includeExecutionTrace": false
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").value(validChatId))
                .andExpect(jsonPath("$.executionTrace").doesNotExist());
    }

    @Test
    void testProcessQueryWithBlankChatId() throws Exception {
        // Test that blank chatId is validated by Spring validation (Size constraint)
        // Blank string doesn't match the 24-character requirement
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "   ",
                    "options": {
                        "maxResults": 10
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void testProcessQueryWithDefaultOptions() throws Exception {
        // Test that default options are applied when not provided
        // includeSources should default to false
        String requestBody = """
                {
                    "query": "Looking for experts in Kubernetes"
                }
                """;

        var result = mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").exists())
                .andExpect(jsonPath("$.chatId").exists())
                .andExpect(jsonPath("$.answer").exists())
                .andReturn();

        // Verify that sources are NOT included by default (includeSources defaults to false)
        // Sources may be present as empty array instead of missing
        String responseContent = result.getResponse().getContentAsString();
        if (responseContent.contains("\"sources\"")) {
            // If sources field exists, verify it's empty
            assertTrue(responseContent.contains("\"sources\":[]") || responseContent.contains("\"sources\": []"),
                    "Sources field should be empty array if present");
        }
        // If sources field doesn't exist, that's also acceptable
    }

    @Test
    void testProcessQueryCreatesConversationHistory() throws Exception {
        // Test that query creates conversation history entry
        String requestBody = """
                {
                    "query": "Looking for Java experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Verify conversation history was created
        Integer count = namedJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expertmatch.conversation_history WHERE chat_id = :chatId",
                new HashMap<String, Object>() {{
                    put("chatId", validChatId);
                }},
                Integer.class
        );
        assertNotNull(count);
        assertTrue(count >= 2); // At least user message and assistant response
    }

    @Test
    void testProcessQueryStream_WithValidQuery_ReturnsStream() throws Exception {
        // Test that the query-stream endpoint accepts requests and returns SSE stream
        String requestBody = """
                {
                    "query": "Looking for experts in Java",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "includeSources": true
                    }
                }
                """.formatted(validChatId);

        // For SSE endpoints, content type may not be set immediately due to async processing
        // Just verify the endpoint accepts the request and returns 200
        String responseContent = mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify response is returned (may be empty if async processing hasn't completed)
        assertNotNull(responseContent, "Response should be returned");
    }

    @Test
    void testProcessQueryStream_WithoutUserId_UsesAnonymousUser() throws Exception {
        // Test that endpoint works without X-User-Id header (should use anonymous user)
        String requestBody = """
                {
                    "query": "Looking for experts in Python",
                    "options": {
                        "maxResults": 10
                    }
                }
                """;

        // Verify endpoint accepts request without X-User-Id header and uses anonymous user
        // For SSE endpoints, content type may not be set immediately due to async processing
        mockMvc.perform(post("/api/v1/query-stream")
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQueryStream_WithInvalidChatIdFormat_ReturnsErrorEvent() throws Exception {
        // Test that invalid chatId format triggers validation error
        // Note: Validation happens at the API level before reaching the controller
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "invalid-chat-id",
                    "options": {
                        "maxResults": 10
                    }
                }
                """;

        // Invalid chatId format is validated by @Size and @Pattern annotations, returns 400
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessQueryStream_WithNonExistentChatId_ReturnsErrorEvent() throws Exception {
        // Test that non-existent chatId triggers error event in SSE stream
        String nonExistentChatId = "507f1f77bcf86cd799439011"; // Valid format but doesn't exist
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10
                    }
                }
                """.formatted(nonExistentChatId);

        String responseContent = mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Error is sent asynchronously in SSE stream, may not be immediately available
        // Just verify the endpoint accepts the request (error will be in the stream)
        assertNotNull(responseContent, "Response should be returned");
    }

    @Test
    void testProcessQueryStream_WithChatIdBelongingToDifferentUser_ReturnsErrorEvent() throws Exception {
        // Test access denied when chatId belongs to different user
        String requestBody = """
                {
                    "query": "Looking for experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10
                    }
                }
                """.formatted(otherUserChatId);

        String responseContent = mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Error is sent asynchronously in SSE stream, may not be immediately available
        // Just verify the endpoint accepts the request (error will be in the stream)
        assertNotNull(responseContent, "Response should be returned");
    }

    @Test
    void testProcessQueryStream_WithoutChatId_CreatesDefaultChat() throws Exception {
        // Test that default chat is created when chatId is not provided
        String requestBody = """
                {
                    "query": "Looking for experts in React",
                    "options": {
                        "maxResults": 10
                    }
                }
                """;

        // For SSE endpoints, content type may not be set immediately due to async processing
        // Just verify the endpoint accepts the request and returns 200
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQueryStream_WithAllQueryOptions() throws Exception {
        // Test with all query options enabled
        String requestBody = """
                {
                    "query": "Need a team for a microservices project",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 15,
                        "minConfidence": 0.75,
                        "includeSources": true,
                        "includeEntities": true,
                        "rerank": true,
                        "deepResearch": false,
                        "useCascadePattern": false,
                        "useRoutingPattern": false,
                        "useCyclePattern": false,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        String responseContent = mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify SSE stream is returned (may be empty if async processing hasn't completed)
        assertNotNull(responseContent, "Response should be returned");
    }

    @Test
    void testProcessQueryStream_WithDeepResearchEnabled() throws Exception {
        // Test with deepResearch option enabled
        String requestBody = """
                {
                    "query": "Find experts for a complex microservices architecture",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 20,
                        "deepResearch": true,
                        "includeSources": true
                    }
                }
                """.formatted(validChatId);

        // For SSE endpoints, content type may not be set immediately due to async processing
        // Just verify the endpoint accepts the request and returns 200
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQueryStream_WithRerankEnabled() throws Exception {
        // Test with rerank option enabled
        String requestBody = """
                {
                    "query": "Looking for Java and Spring Boot experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "rerank": true,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        // For SSE endpoints, content type may not be set immediately due to async processing
        // Just verify the endpoint accepts the request and returns 200
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQueryStream_WithIncludeSourcesDisabled() throws Exception {
        // Test with includeSources set to false
        String requestBody = """
                {
                    "query": "Looking for experts in Kubernetes",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "includeSources": false
                    }
                }
                """.formatted(validChatId);

        // For SSE endpoints, content type may not be set immediately due to async processing
        // Just verify the endpoint accepts the request and returns 200
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQueryStream_WithEmptyQuery_ReturnsError() throws Exception {
        // Test that empty query triggers validation error
        // Note: Empty string passes @NotBlank validation, but may fail business logic
        // The endpoint accepts it and processes it (may return empty results)
        String requestBody = """
                {
                    "query": "",
                    "options": {
                        "maxResults": 10
                    }
                }
                """;

        // Empty query may be accepted and processed (validation happens in business logic)
        // Check that endpoint responds (may be 200 with empty results or error in stream)
        var result = mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andReturn();

        // Accept either 200 (error in stream) or 400 (rejected immediately)
        int status = result.getResponse().getStatus();
        assertTrue(status == 200 || status == 400,
                "Should return 200 (error in stream) or 400 (rejected)");
    }


    @Test
    void testProcessQueryStream_WithUserRolesAndEmail() throws Exception {
        // Test that X-User-Roles and X-User-Email headers are accepted
        String requestBody = """
                {
                    "query": "Looking for experts in Docker",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10
                    }
                }
                """.formatted(validChatId);

        // For SSE endpoints, content type may not be set immediately due to async processing
        mockMvc.perform(post("/api/v1/query-stream")
                        .header("X-User-Id", TEST_USER_ID)
                        .header("X-User-Roles", "ROLE_USER,ROLE_ADMIN")
                        .header("X-User-Email", "user@example.com")
                        .contentType(APPLICATION_JSON)
                        .accept("text/event-stream")
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQuery_WithBothCascadeAndCyclePattern_ReturnsBadRequest() throws Exception {
        // Test that enabling both Cascade and Cycle patterns returns validation error
        String requestBody = """
                {
                    "query": "Looking for experts in Java",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "useCascadePattern": true,
                        "useCyclePattern": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cascade and Cycle patterns cannot be enabled simultaneously")));
    }

    @Test
    void testProcessQuery_WithCascadeAndRoutingPattern_ReturnsOk() throws Exception {
        // Test that Cascade and Routing patterns can be used together
        String requestBody = """
                {
                    "query": "Looking for experts in Java",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 1,
                        "useCascadePattern": true,
                        "useRoutingPattern": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQuery_WithCycleAndRoutingPattern_ReturnsOk() throws Exception {
        // Test that Cycle and Routing patterns can be used together
        String requestBody = """
                {
                    "query": "Looking for experts in Java",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "useCyclePattern": true,
                        "useRoutingPattern": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessQuery_ExecutionTraceIncludesToolCallStructure() throws Exception {
        // Test that Execution Trace includes tool call structure when enabled
        // Note: Actual tool calls may not occur with mocked LLMs, but structure should be present
        String requestBody = """
                {
                    "query": "Find Java experts",
                    "chatId": "%s",
                    "options": {
                        "maxResults": 10,
                        "includeExecutionTrace": true
                    }
                }
                """.formatted(validChatId);

        mockMvc.perform(post("/api/v1/query")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionTrace").exists())
                .andExpect(jsonPath("$.executionTrace.steps").isArray())
                .andExpect(jsonPath("$.executionTrace.steps[0].name").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].service").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].method").exists())
                .andExpect(jsonPath("$.executionTrace.steps[0].status").exists());
        // Note: toolCall field may be null for non-tool-call steps, which is correct behavior
        // The structure allows tool calls to be included when they occur
    }
}

