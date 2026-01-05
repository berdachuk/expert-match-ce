package com.berdachuk.expertmatch.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP tool definition.
 */
public record McpTool(
        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("inputSchema")
        Object inputSchema
) {
}

