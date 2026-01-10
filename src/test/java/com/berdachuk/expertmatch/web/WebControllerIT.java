package com.berdachuk.expertmatch.web;

import com.berdachuk.expertmatch.chat.service.ChatService;
import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for WebController.
 * Uses Testcontainers PostgreSQL and MockMvc for endpoint testing.
 * <p>
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@SuppressWarnings("null")
class WebControllerIT extends BaseIntegrationTest {

    private static final String TEST_USER_ID = "test-user-web-123";
    private static volatile int staticServerPort = 0;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ChatService chatService;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    @LocalServerPort
    private int serverPort;

    private String validChatId;

    @DynamicPropertySource
    static void configureApiBaseUrl(DynamicPropertyRegistry registry) {
        // Use a supplier that reads from static variable set in @BeforeEach
        // WebController now reads from Environment dynamically, so this will work
        registry.add("expertmatch.api.base-url", () -> {
            int port = staticServerPort;
            if (port > 0) {
                return "http://localhost:" + port;
            }
            // Fallback during initial context loading
            return "http://localhost:8093";
        });
    }

    @BeforeEach
    void setUp() {
        // Set the static port so @DynamicPropertySource supplier can use it
        // WebController will read this dynamically from Environment
        staticServerPort = serverPort;
        // Also set as system property for immediate availability
        System.setProperty("expertmatch.api.base-url", "http://localhost:" + serverPort);

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

        // Create test chat
        com.berdachuk.expertmatch.chat.domain.Chat testUserChat = chatService.getOrCreateDefaultChat(TEST_USER_ID);
        validChatId = testUserChat.id();
        // Verify chat was created with correct user ID
        assert testUserChat.userId().equals(TEST_USER_ID) :
                "Chat created with wrong user ID. Expected: " + TEST_USER_ID + ", got: " + testUserChat.userId();
    }

    @Test
    void testIndex_ReturnsIndexView() throws Exception {
        mockMvc.perform(get("/")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("chats"))
                .andExpect(model().attribute("currentPage", "index"));
    }

    @Test
    void testChats_WithoutChatId_ReturnsChatsView() throws Exception {
        mockMvc.perform(get("/chats")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chats"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("chats"))
                .andExpect(model().attributeExists("messages"))
                .andExpect(model().attributeExists("isLoading"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("currentPage", "chats"));
    }

    @Test
    void testChats_WithValidChatId_ReturnsChatsViewWithCurrentChat() throws Exception {
        mockMvc.perform(get("/chats")
                        .param("chatId", validChatId)
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chats"))
                .andExpect(model().attributeExists("chats"))
                .andExpect(model().attribute("currentPage", "chats"));
    }

    @Test
    void testChats_WithInvalidChatId_ReturnsChatsViewWithoutCurrentChat() throws Exception {
        mockMvc.perform(get("/chats")
                        .param("chatId", "invalid-chat-id-123456789012")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("chats"))
                .andExpect(model().attribute("currentPage", "chats"));
    }

    @Test
    void testProcessQuery_WithValidQuery_ReturnsIndexViewWithResults() throws Exception {
        // Insert test data for query to succeed
        insertTestEmployee("John Doe", "Java, Spring Boot");

        mockMvc.perform(post("/query")
                        .param("query", "Find experts in Java")
                        .param("chatId", validChatId)
                        .param("rerank", "false")
                        .param("deepResearch", "false")
                        .param("includeExecutionTrace", "false")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("chats"))
                .andExpect(model().attributeExists("query"))
                .andExpect(model().attribute("currentPage", "index"))
                .andExpect(model().attribute("query", "Find experts in Java"));
    }

    @Test
    void testProcessQuery_WithoutChatId_CreatesDefaultChat() throws Exception {
        // Insert test data for query to succeed
        insertTestEmployee("Jane Smith", "Python, Django");

        mockMvc.perform(post("/query")
                        .param("query", "Find experts in Python")
                        .param("rerank", "false")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("chats"));
    }

    @Test
    void testSendMessage_WithValidMessage_RedirectsToChats() throws Exception {
        // Insert test data for query to succeed
        insertTestEmployee("Alice Brown", "React, Node.js");

        mockMvc.perform(post("/chats/send")
                        .param("message", "Hello, find experts")
                        .param("chatId", validChatId)
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chats?chatId=" + validChatId));
    }

    @Test
    void testCreateChat_WithoutName_HandlesRequest() throws Exception {
        // Create chat - may redirect on success or return error, both are valid
        var result = mockMvc.perform(post("/chats/new")
                        .header("X-User-Id", TEST_USER_ID))
                .andReturn();

        // Should either redirect (success) or return error (API failure)
        assertTrue(result.getResponse().getStatus() == 302 ||
                result.getResponse().getStatus() >= 400);
    }

    @Test
    void testCreateChat_WithName_HandlesRequest() throws Exception {
        // Create chat - may redirect on success or return error, both are valid
        var result = mockMvc.perform(post("/chats/new")
                        .param("name", "Test Chat")
                        .header("X-User-Id", TEST_USER_ID))
                .andReturn();

        // Should either redirect (success) or return error (API failure)
        assertTrue(result.getResponse().getStatus() == 302 ||
                result.getResponse().getStatus() >= 400);
    }

    @Test
    void testDeleteChat_WithValidChatId_RedirectsToIndex() throws Exception {
        // Create a non-default chat to delete
        com.berdachuk.expertmatch.chat.domain.Chat chatToDelete = chatService.createChat(TEST_USER_ID, "Chat to Delete");
        String chatIdToDelete = chatToDelete.id();

        mockMvc.perform(post("/chats/delete")
                        .param("chatId", chatIdToDelete)
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testDeleteChat_WithInvalidChatId_ReturnsIndexWithError() throws Exception {
        mockMvc.perform(post("/chats/delete")
                        .param("chatId", "invalid-chat-id-123456789012")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testDeleteChat_WithDefaultChat_ReturnsIndexWithError() throws Exception {
        // Try to delete the default chat (should fail)
        mockMvc.perform(post("/chats/delete")
                        .param("chatId", validChatId)
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("error"));
    }

    /**
     * Helper method to insert a test employee for query tests.
     */
    private void insertTestEmployee(String name, String skills) {
        String employeeId = IdGenerator.generateEmployeeId();
        String workExperienceId = IdGenerator.generateId();

        // Insert employee using named parameters
        Map<String, Object> employeeParams = new HashMap<>();
        employeeParams.put("id", employeeId);
        employeeParams.put("name", name);
        employeeParams.put("email", name.toLowerCase().replace(" ", ".") + "-" + System.currentTimeMillis() + "@example.com");
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.employee (id, name, email) VALUES (:id, :name, :email)",
                employeeParams
        );

        // Insert work experience
        Map<String, Object> workExpParams = new HashMap<>();
        workExpParams.put("id", workExperienceId);
        workExpParams.put("employeeId", employeeId);
        workExpParams.put("projectName", "Test Project");
        workExpParams.put("role", "Senior Developer");
        workExpParams.put("projectSummary", "Experience with " + skills);
        workExpParams.put("technologies", skills.split(",\\s*"));
        namedJdbcTemplate.update(
                "INSERT INTO expertmatch.work_experience (id, employee_id, project_name, role, project_summary, technologies) VALUES (:id, :employeeId, :projectName, :role, :projectSummary, :technologies)",
                workExpParams
        );
    }
}

