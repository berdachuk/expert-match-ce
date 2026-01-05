package com.berdachuk.expertmatch.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP JSON-RPC 2.0 response.
 */
public record McpResponse(
        @JsonProperty("jsonrpc")
        String jsonrpc,

        @JsonProperty("id")
        String id,

        @JsonProperty("result")
        Object result,

        @JsonProperty("error")
        McpError error
) {
    public McpResponse {
        if (jsonrpc == null) {
            jsonrpc = "2.0";
        }
    }

    public static McpResponse success(String id, Object result) {
        return new McpResponse("2.0", id, result, null);
    }

    public static McpResponse error(String id, int code, String message) {
        return new McpResponse("2.0", id, null, new McpError(code, message, null));
    }
}

