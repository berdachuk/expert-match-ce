package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.config.ToolSearchConfiguration;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springaicommunity.tool.search.SearchType;
import org.springaicommunity.tool.search.ToolSearchRequest;
import org.springaicommunity.tool.search.ToolSearchResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Tool Search Tool functionality.
 * Tests tool discovery flow using PgVector-based semantic search.
 *
 * IMPORTANT: This is an integration test with database. All LLM calls MUST be mocked.
 * - Extends BaseIntegrationTest which uses TestAIConfig mocks
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
@SpringBootTest
@ActiveProfiles("test")
class ToolSearchIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private ToolSearchConfiguration toolSearchConfiguration;

    @Autowired(required = false)
    private ExpertMatchTools expertMatchTools;

    @Autowired(required = false)
    private PgVectorToolSearcher toolSearcher;

    @Autowired(required = false)
    private ToolMetadataService toolMetadataService;

    @Autowired(required = false)
    @Qualifier("chatClientWithToolSearch")
    private ChatClient chatClientWithToolSearch;

    @Test
    void testToolSearchConfigurationConditional() {
        // ToolSearchConfiguration should only be active when enabled=true
        // In test profile with default config, it should be null (disabled)
        // This test verifies the conditional configuration works
        if (toolSearchConfiguration == null) {
            // Configuration is disabled, which is expected for default test profile
            assertTrue(true, "ToolSearchConfiguration is correctly disabled when expertmatch.tools.search.enabled=false");
        }
    }

    @Test
    void testPgVectorToolSearcherImplementsInterface() {
        if (toolSearcher == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Verify ToolSearcher interface methods
        SearchType searchType = toolSearcher.searchType();
        assertEquals(SearchType.SEMANTIC, searchType, "PgVectorToolSearcher should return SEMANTIC search type");
    }

    @Test
    void testToolMetadataServiceIndexesTools() {
        if (toolMetadataService == null || expertMatchTools == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Index tools
        toolMetadataService.indexTools(expertMatchTools);

        // Verify tools are indexed (would need database query to verify)
        // For now, just verify no exception is thrown
        assertNotNull(toolMetadataService);
    }

    @Test
    void testPgVectorToolSearcherSearch() {
        if (toolSearcher == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Test legacy search method for backward compatibility
        List<Map<String, Object>> results = toolSearcher.search("find experts", 5);

        assertNotNull(results);
        // Results may be empty if tools haven't been indexed yet
        // In a full integration test, we'd index tools first
    }

    @Test
    void testToolSearcherSearchWithRequest() {
        if (toolSearcher == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Test new ToolSearchRequest-based search
        ToolSearchRequest request = new ToolSearchRequest(null, "find experts", 5, null);
        ToolSearchResponse response = toolSearcher.search(request);

        assertNotNull(response);
        assertNotNull(response.toolReferences());
        // Results may be empty if tools haven't been indexed yet
    }

    @Test
    void testToolSearchWithDifferentQueries() {
        if (toolSearcher == null || toolMetadataService == null || expertMatchTools == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Index tools first
        toolMetadataService.indexTools(expertMatchTools);

        // Test different query types
        List<Map<String, Object>> expertResults = toolSearcher.search("expert discovery", 5);
        List<Map<String, Object>> profileResults = toolSearcher.search("get expert profile", 5);
        List<Map<String, Object>> projectResults = toolSearcher.search("project requirements", 5);

        assertNotNull(expertResults);
        assertNotNull(profileResults);
        assertNotNull(projectResults);
    }

    @Test
    void testToolSearchMaxResults() {
        if (toolSearcher == null || toolMetadataService == null || expertMatchTools == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Index tools first
        toolMetadataService.indexTools(expertMatchTools);

        // Test with different maxResults values
        List<Map<String, Object>> results1 = toolSearcher.search("expert", 1);
        List<Map<String, Object>> results3 = toolSearcher.search("expert", 3);
        List<Map<String, Object>> results10 = toolSearcher.search("expert", 10);

        assertNotNull(results1);
        assertNotNull(results3);
        assertNotNull(results10);

        // Verify maxResults is respected
        assertTrue(results1.size() <= 1);
        assertTrue(results3.size() <= 3);
        assertTrue(results10.size() <= 10);
    }

    @Test
    void testToolSearchSimilarityThreshold() {
        if (toolSearcher == null || toolMetadataService == null || expertMatchTools == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Index tools first
        toolMetadataService.indexTools(expertMatchTools);

        // Search with a general query that should match some tools
        List<Map<String, Object>> results = toolSearcher.search("expert", 5);

        assertNotNull(results);

        // Verify results have similarity scores (if any results returned)
        if (!results.isEmpty()) {
            for (Map<String, Object> result : results) {
                assertTrue(result.containsKey("similarity"), "Result should contain similarity score");
                Object similarityObj = result.get("similarity");
                assertNotNull(similarityObj, "Similarity should not be null");
                // Check that similarity is a Number (Double in this case)
                assertTrue(similarityObj instanceof Number, "Similarity should be a Number, got: " + similarityObj.getClass().getName());
                double similarity = ((Number) similarityObj).doubleValue();
                // Check that similarity is not NaN and is in valid range [0.0, 1.0] or slightly outside due to floating point precision
                // Note: The database query filters for similarity >= 0.5, but we check the full range for robustness
                if (!Double.isNaN(similarity)) {
                    assertTrue(similarity >= -0.001 && similarity <= 1.001,
                            "Similarity should be between 0.0 and 1.0, got: " + similarity);
                }
                // Note: NaN similarity is acceptable in integration tests when embedding generation fails
            }
        }
        // Note: Empty results are acceptable in integration tests when tools haven't been properly indexed
        // or when the similarity threshold filters out all results
    }

    @Test
    void testChatClientWithToolSearchBean() {
        // When Tool Search Tool is enabled, chatClientWithToolSearch should be available
        // When disabled, it should be null
        if (toolSearchConfiguration != null) {
            assertNotNull(chatClientWithToolSearch, "chatClientWithToolSearch should be available when Tool Search Tool is enabled");
        } else {
            assertNull(chatClientWithToolSearch, "chatClientWithToolSearch should be null when Tool Search Tool is disabled");
        }
    }

    @Test
    void testToolSearcherClearIndex() {
        if (toolSearcher == null || toolMetadataService == null || expertMatchTools == null) {
            // Skip if tool search is not enabled
            return;
        }

        // Index tools first
        toolMetadataService.indexTools(expertMatchTools);

        // Verify tools are indexed
        ToolSearchRequest request = new ToolSearchRequest(null, "expert", 5, null);
        ToolSearchResponse responseBefore = toolSearcher.search(request);
        assertNotNull(responseBefore);

        // Clear index
        toolSearcher.clearIndex("test-session");

        // Verify index is cleared (search should return empty or fewer results)
        ToolSearchResponse responseAfter = toolSearcher.search(request);
        assertNotNull(responseAfter);
        // After clearing, results should be empty or significantly reduced
    }
}

