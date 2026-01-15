package com.berdachuk.expertmatch.mcp.rest;

import com.berdachuk.expertmatch.mcp.model.McpRequest;
import com.berdachuk.expertmatch.mcp.model.McpResponse;
import com.berdachuk.expertmatch.mcp.service.McpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for MCP (Model Context Protocol) server.
 * Exposes ExpertMatch functionality as MCP server for AI assistants.
 */
@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP Server", description = "Model Context Protocol server for AI assistant integration")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(name = "expertmatch.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(McpService.class)
public class McpServerController {

    private final McpService mcpService;
    private final ObjectMapper objectMapper;

    public McpServerController(McpService mcpService, ObjectMapper objectMapper) {
        this.mcpService = mcpService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle MCP JSON-RPC 2.0 request.
     */
    @Operation(
            summary = "MCP JSON-RPC 2.0 endpoint",
            description = "Processes MCP protocol requests (JSON-RPC 2.0). Supports tools and resources for expert discovery."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "MCP response",
                    content = @Content(schema = @Schema(implementation = McpResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid JSON-RPC request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpResponse> handleMcpRequest(
            @Parameter(description = "MCP JSON-RPC 2.0 request")
            @RequestBody McpRequest request,
            @Parameter(hidden = true)
            Authentication authentication) {

        try {
            McpResponse response = mcpService.processRequest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            McpResponse errorResponse = McpResponse.error(
                    request.id() != null ? request.id() : "unknown",
                    -32603,
                    "Internal error: " + e.getMessage()
            );
            return ResponseEntity.ok(errorResponse);
        }
    }
}

