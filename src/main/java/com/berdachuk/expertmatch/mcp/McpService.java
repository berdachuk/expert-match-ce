package com.berdachuk.expertmatch.mcp;

import com.berdachuk.expertmatch.llm.tools.PgVectorToolSearcher;
import com.berdachuk.expertmatch.mcp.model.McpRequest;
import com.berdachuk.expertmatch.mcp.model.McpResource;
import com.berdachuk.expertmatch.mcp.model.McpResponse;
import com.berdachuk.expertmatch.mcp.model.McpTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for handling MCP protocol operations.
 */
@Service
@ConditionalOnProperty(name = "expertmatch.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(McpToolHandler.class)
public class McpService {

    private final McpToolHandler toolHandler;
    private final McpResourceHandler resourceHandler;
    private final PgVectorToolSearcher toolSearcher;
    private final ObjectMapper objectMapper;

    public McpService(
            McpToolHandler toolHandler,
            McpResourceHandler resourceHandler,
            PgVectorToolSearcher toolSearcher,
            ObjectMapper objectMapper
    ) {
        this.toolHandler = toolHandler;
        this.resourceHandler = resourceHandler;
        this.toolSearcher = toolSearcher;
        this.objectMapper = objectMapper;
    }

    /**
     * Process MCP request.
     */
    public McpResponse processRequest(McpRequest request) {
        try {
            String method = request.method();
            String id = request.id();

            switch (method) {
                case "initialize":
                    return initialize(id, request.params());
                case "tools/list":
                    return listTools(id);
                case "tools/search":
                    return searchTools(id, request.params());
                case "resources/list":
                    return listResources(id);
                case "tools/call":
                    return callTool(id, request.params());
                case "resources/read":
                    return readResource(id, request.params());
                default:
                    return McpResponse.error(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            return McpResponse.error(request.id(), -32603, "Internal error: " + e.getMessage());
        }
    }

    /**
     * Initialize MCP connection.
     */
    private McpResponse initialize(String id, Object params) {
        Map<String, Object> result = Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of("listChanged", true),
                        "resources", Map.of("subscribe", true, "listChanged", true)
                ),
                "serverInfo", Map.of(
                        "name", "expertmatch-mcp-server",
                        "version", "1.0.0"
                )
        );
        return McpResponse.success(id, result);
    }

    /**
     * List available tools.
     */
    private McpResponse listTools(String id) {
        List<McpTool> tools = toolHandler.listTools();
        return McpResponse.success(id, Map.of("tools", tools));
    }

    /**
     * Search for tools using semantic search.
     */
    private McpResponse searchTools(String id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String query = (String) paramsMap.get("query");
            Integer maxResults = paramsMap.get("maxResults") != null
                    ? ((Number) paramsMap.get("maxResults")).intValue()
                    : 5;

            if (query == null || query.isBlank()) {
                return McpResponse.error(id, -32602, "Query parameter is required");
            }

            // Search for tools using PgVectorToolSearcher
            List<Map<String, Object>> searchResults = toolSearcher.search(query, maxResults);

            // Get all tools and filter by search results
            List<McpTool> allTools = toolHandler.listTools();
            List<McpTool> matchedTools = allTools.stream()
                    .filter(tool -> {
                        String toolName = tool.name();
                        // Check if tool name matches any search result
                        return searchResults.stream()
                                .anyMatch(result -> {
                                    String resultToolName = (String) result.get("toolName");
                                    return toolName.equals("expertmatch_" + resultToolName);
                                });
                    })
                    .limit(maxResults)
                    .collect(Collectors.toList());

            return McpResponse.success(id, Map.of("tools", matchedTools));
        } catch (Exception e) {
            return McpResponse.error(id, -32603, "Tool search error: " + e.getMessage());
        }
    }

    /**
     * List available resources.
     */
    private McpResponse listResources(String id) {
        List<McpResource> resources = resourceHandler.listResources();
        return McpResponse.success(id, Map.of("resources", resources));
    }

    /**
     * Call a tool.
     */
    private McpResponse callTool(String id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String toolName = (String) paramsMap.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

            Object result = toolHandler.callTool(toolName, arguments);
            return McpResponse.success(id, Map.of("content", List.of(
                    Map.of("type", "text", "text", objectMapper.writeValueAsString(result))
            )));
        } catch (Exception e) {
            return McpResponse.error(id, -32603, "Tool execution error: " + e.getMessage());
        }
    }

    /**
     * Read a resource.
     */
    private McpResponse readResource(String id, Object params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paramsMap = (Map<String, Object>) params;
            String uri = (String) paramsMap.get("uri");

            String content = resourceHandler.readResource(uri);
            return McpResponse.success(id, Map.of("contents", List.of(
                    Map.of("uri", uri, "mimeType", "application/json", "text", content)
            )));
        } catch (Exception e) {
            return McpResponse.error(id, -32603, "Resource read error: " + e.getMessage());
        }
    }
}

