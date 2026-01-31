/**
 * MCP Server Module
 * <p>
 * Model Context Protocol server for integration with AI assistants:
 * - MCP server implementation
 * - Data export through MCP protocol
 * - Integration with Cursor IDE and other AI tools
 * <p>
 * Exposes:
 * - MCPService (service layer)
 * - MCPController (REST API)
 */
@org.springframework.modulith.ApplicationModule(
        id = "mcp",
        displayName = "MCP Server",
        allowedDependencies = {"employee", "retrieval", "llm", "project", "workexperience", "core"}
)
package com.berdachuk.expertmatch.mcp;

