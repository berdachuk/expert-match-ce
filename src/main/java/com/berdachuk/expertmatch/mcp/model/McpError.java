package com.berdachuk.expertmatch.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP JSON-RPC 2.0 error.
 */
public record McpError(
        @JsonProperty("code")
        int code,

        @JsonProperty("message")
        String message,

        @JsonProperty("data")
        Object data
) {
}

