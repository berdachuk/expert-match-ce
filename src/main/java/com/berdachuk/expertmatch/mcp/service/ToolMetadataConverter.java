package com.berdachuk.expertmatch.mcp.service;

import com.berdachuk.expertmatch.mcp.model.McpTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Spring AI @Tool annotated methods to MCP tool format.
 */
@Component
public class ToolMetadataConverter {

    /**
     * Converts a Spring AI tool component to MCP tools.
     */
    public List<McpTool> convertToMcpTools(Object toolComponent) {
        List<McpTool> mcpTools = new ArrayList<>();
        Class<?> clazz = toolComponent.getClass();

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                McpTool mcpTool = convertMethodToMcpTool(method, toolAnnotation, toolComponent);
                mcpTools.add(mcpTool);
            }
        }

        return mcpTools;
    }

    /**
     * Converts a single @Tool method to MCP tool format.
     */
    private McpTool convertMethodToMcpTool(Method method, Tool toolAnnotation, Object toolComponent) {
        String toolName = method.getName();
        String description = toolAnnotation.description();

        // Build MCP tool schema
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            ToolParam toolParam = param.getAnnotation(ToolParam.class);
            String paramName = param.getName();
            String paramDescription = toolParam != null ? toolParam.description() : "";

            // Determine parameter type
            Class<?> paramType = param.getType();
            Map<String, Object> paramSchema = buildParameterSchema(paramType, paramDescription);

            properties.put(paramName, paramSchema);

            // Mark as required if no default value (simplified - in real implementation, check for Optional, nullable, etc.)
            if (toolParam == null || !paramDescription.contains("optional") && !paramDescription.contains("Optional")) {
                required.add(paramName);
            }
        }

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        // Map Spring AI tool name to MCP tool name (add prefix for consistency)
        String mcpToolName = "expertmatch_" + toolName;

        return new McpTool(mcpToolName, description, schema);
    }

    /**
     * Builds parameter schema for MCP format.
     */
    private Map<String, Object> buildParameterSchema(Class<?> paramType, String description) {
        Map<String, Object> schema = new HashMap<>();

        if (paramType == String.class) {
            schema.put("type", "string");
        } else if (paramType == Integer.class || paramType == int.class) {
            schema.put("type", "integer");
        } else if (paramType == Double.class || paramType == double.class) {
            schema.put("type", "number");
        } else if (paramType == Boolean.class || paramType == boolean.class) {
            schema.put("type", "boolean");
        } else if (List.class.isAssignableFrom(paramType)) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "string")); // Default to string array
        } else if (Map.class.isAssignableFrom(paramType)) {
            schema.put("type", "object");
        } else {
            // For complex types, use object
            schema.put("type", "object");
        }

        if (description != null && !description.isBlank()) {
            schema.put("description", description);
        }

        return schema;
    }
}

