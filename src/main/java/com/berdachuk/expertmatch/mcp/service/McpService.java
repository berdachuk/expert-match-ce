package com.berdachuk.expertmatch.mcp.service;

import com.berdachuk.expertmatch.mcp.model.McpRequest;
import com.berdachuk.expertmatch.mcp.model.McpResponse;


/**
 * Service interface for mcp operations.
 */
public interface McpService {
    /**
     * Processes an MCP (Model Context Protocol) request.
     *
     * @param request The MCP request containing the operation and parameters
     * @return MCP response containing the result of the operation
     */
    McpResponse processRequest(McpRequest request);
}
