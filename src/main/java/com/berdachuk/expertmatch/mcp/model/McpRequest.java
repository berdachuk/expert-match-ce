package com.berdachuk.expertmatch.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP JSON-RPC 2.0 request.
 */
public record McpRequest(
        @JsonProperty("jsonrpc")
        String jsonrpc,

        @JsonProperty("id")
        String id,

        @JsonProperty("method")
        String method,

        @JsonProperty("params")
        Object params
) {
    public McpRequest {
        if (jsonrpc == null) {
            jsonrpc = "2.0";
        }
    }
}

