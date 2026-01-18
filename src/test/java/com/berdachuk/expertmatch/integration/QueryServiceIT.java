package com.berdachuk.expertmatch.integration;

import com.berdachuk.expertmatch.chat.service.ChatService;
import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.query.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for QueryService.
 * <p>
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Uses "test" profile which excludes SpringAIConfig (via @Profile("!test"))
 * - TestAIConfig provides @Primary mocks for ChatModel and EmbeddingModel
 * - BaseIntegrationTest disables Spring AI auto-configuration
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
class QueryServiceIT extends BaseIntegrationTest {

    private final String userId = "test-user-id";
    @Autowired
    private QueryService queryService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private String chatId;

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

        // Create a test chat before each test
        var chat = chatService.getOrCreateDefaultChat(userId);
        chatId = chat.id();
    }

    @Test
    void testProcessQuery() {
        QueryRequest request = new QueryRequest(
                "Looking for experts in Java and Spring Boot",
                null,
                new com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions(10, 0.7, true, true, true, false, false, false, false, false)
        );

        QueryResponse response = queryService.processQuery(request, chatId, userId);

        assertNotNull(response);
        assertNotNull(response.queryId());
        assertNotNull(response.chatId());
        assertNotNull(response.messageId());
        assertNotNull(response.answer());
        assertNotNull(response.experts());
        assertNotNull(response.sources());
        assertNotNull(response.entities());
        assertNotNull(response.summary());
    }

    @Test
    void testProcessQueryWithOptions() {
        QueryRequest request = new QueryRequest(
                "Need a team for a banking app",
                null,
                new com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions(5, 0.8, true, true, true, false, false, false, false, false)
        );

        QueryResponse response = queryService.processQuery(request, chatId, userId);

        assertNotNull(response);
        assertNotNull(response.queryId());
        assertNotNull(response.chatId());
        assertEquals(chatId, response.chatId());
        assertNotNull(response.messageId());
        assertNotNull(response.answer());
        assertNotNull(response.experts());
        assertNotNull(response.sources());
        assertNotNull(response.entities());
        assertNotNull(response.summary());
        // Verify that maxResults option is respected
        assertTrue(response.experts().size() <= 5,
                "Number of experts should not exceed maxResults option");
    }

    @Test
    void testProcessQueryWithCascadePattern() {
        // Test Cascade pattern with single expert (domain model directly)
        // Note: useCascadePattern is not yet in API model, so we test via domain model
        QueryRequest request = new QueryRequest(
                "Looking for Java expert",
                null,
                new com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions(1, 0.7, true, true, true, false, true, false, false, false)
        );

        QueryResponse response = queryService.processQuery(request, chatId, userId);

        assertNotNull(response);
        assertNotNull(response.queryId());
        assertNotNull(response.chatId());
        assertNotNull(response.messageId());
        assertNotNull(response.answer());
        assertNotNull(response.experts());
        // Note: Cascade pattern only works when there's exactly 1 expert
        // If no experts found, it falls back to RAG pattern
        // If multiple experts found, it falls back to RAG pattern
    }

    @Test
    void testProcessQueryWithCyclePattern() {
        // Test Cycle pattern with multiple experts (domain model directly)
        // Note: useCyclePattern is not yet in API model, so we test via domain model
        QueryRequest request = new QueryRequest(
                "Looking for Java experts",
                null,
                new com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions(5, 0.7, true, true, true, false, false, false, true, false)
        );

        QueryResponse response = queryService.processQuery(request, chatId, userId);

        assertNotNull(response);
        assertNotNull(response.queryId());
        assertNotNull(response.chatId());
        assertNotNull(response.messageId());
        assertNotNull(response.answer());
        assertNotNull(response.experts());
        // Note: Cycle pattern only works when there are multiple experts (>1)
        // If no experts found, it falls back to RAG pattern
        // If only 1 expert found, it falls back to RAG pattern
    }
}

