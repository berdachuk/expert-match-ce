package com.berdachuk.expertmatch.mcp.service;

import com.berdachuk.expertmatch.mcp.model.McpResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Handler for MCP resource operations.
 */
@Component
public class McpResourceHandler {

    private final ObjectMapper objectMapper;

    public McpResourceHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * List all available MCP resources.
     */
    public List<McpResource> listResources() {
        return List.of(
                new McpResource(
                        "expertmatch://experts/{expert_id}",
                        "Expert Profile",
                        "Expert profile resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://projects/{project_id}",
                        "Project Information",
                        "Project information resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://technologies/{technology_name}",
                        "Technology Usage",
                        "Technology usage resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://domains/{domain_name}",
                        "Domain Expertise",
                        "Domain expertise resource",
                        "application/json"
                )
        );
    }

    /**
     * Read a resource by URI.
     */
    public String readResource(String uri) {
        try {
            if (uri.startsWith("expertmatch://experts/")) {
                String expertId = uri.substring("expertmatch://experts/".length());
                return readExpertResource(expertId);
            } else if (uri.startsWith("expertmatch://projects/")) {
                String projectId = uri.substring("expertmatch://projects/".length());
                return readProjectResource(projectId);
            } else if (uri.startsWith("expertmatch://technologies/")) {
                String technologyName = uri.substring("expertmatch://technologies/".length());
                return readTechnologyResource(technologyName);
            } else if (uri.startsWith("expertmatch://domains/")) {
                String domainName = uri.substring("expertmatch://domains/".length());
                return readDomainResource(domainName);
            } else {
                throw new IllegalArgumentException("Unknown resource URI: " + uri);
            }
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
                return "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            }
        }
    }

    private String readExpertResource(String expertId) throws Exception {
        // TODO: Implement actual expert profile retrieval
        return objectMapper.writeValueAsString(Map.of(
                "id", expertId,
                "message", "Expert profile retrieval not fully implemented"
        ));
    }

    private String readProjectResource(String projectId) throws Exception {
        // TODO: Implement actual project information retrieval
        return objectMapper.writeValueAsString(Map.of(
                "id", projectId,
                "message", "Project information retrieval not fully implemented"
        ));
    }

    private String readTechnologyResource(String technologyName) throws Exception {
        // TODO: Implement actual technology usage retrieval
        return objectMapper.writeValueAsString(Map.of(
                "name", technologyName,
                "message", "Technology usage retrieval not fully implemented"
        ));
    }

    private String readDomainResource(String domainName) throws Exception {
        // TODO: Implement actual domain expertise retrieval
        return objectMapper.writeValueAsString(Map.of(
                "name", domainName,
                "message", "Domain expertise retrieval not fully implemented"
        ));
    }
}

