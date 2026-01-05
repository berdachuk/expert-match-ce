package com.berdachuk.expertmatch.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP resource definition.
 */
public record McpResource(
        @JsonProperty("uri")
        String uri,

        @JsonProperty("name")
        String name,

        @JsonProperty("description")
        String description,

        @JsonProperty("mimeType")
        String mimeType
) {
}

