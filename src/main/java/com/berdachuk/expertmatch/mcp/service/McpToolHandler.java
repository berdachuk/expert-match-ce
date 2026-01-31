package com.berdachuk.expertmatch.mcp.service;

import com.berdachuk.expertmatch.llm.tools.ChatManagementTools;
import com.berdachuk.expertmatch.mcp.model.McpTool;
import com.berdachuk.expertmatch.query.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.query.tools.RetrievalTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handler for MCP tool operations.
 * Delegates to Spring AI @Tool annotated methods for actual implementation.
 */
@Component
@ConditionalOnProperty(name = "expertmatch.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class McpToolHandler {

    private final ExpertMatchTools expertMatchTools;
    private final ChatManagementTools chatManagementTools;
    private final RetrievalTools retrievalTools;
    private final ToolMetadataConverter toolMetadataConverter;
    private final ObjectMapper objectMapper;

    public McpToolHandler(
            ExpertMatchTools expertMatchTools,
            ChatManagementTools chatManagementTools,
            RetrievalTools retrievalTools,
            ToolMetadataConverter toolMetadataConverter,
            ObjectMapper objectMapper
    ) {
        this.expertMatchTools = expertMatchTools;
        this.chatManagementTools = chatManagementTools;
        this.retrievalTools = retrievalTools;
        this.toolMetadataConverter = toolMetadataConverter;
        this.objectMapper = objectMapper;
    }

    /**
     * List all available MCP tools.
     * Converts Spring AI @Tool methods to MCP format.
     */
    public List<McpTool> listTools() {
        List<McpTool> tools = new ArrayList<>();

        // Convert tools from all tool components
        tools.addAll(toolMetadataConverter.convertToMcpTools(expertMatchTools));
        tools.addAll(toolMetadataConverter.convertToMcpTools(chatManagementTools));
        tools.addAll(toolMetadataConverter.convertToMcpTools(retrievalTools));

        return tools;
    }

    /**
     * Call a tool by name.
     * Delegates to Spring AI @Tool methods.
     */
    public Object callTool(String toolName, Map<String, Object> arguments) {
        // Extract method name from MCP tool name (remove "expertmatch_" prefix)
        String methodName = toolName.startsWith("expertmatch_")
                ? toolName.substring("expertmatch_".length())
                : toolName;

        // Try to find and invoke the method in tool components
        Object result = invokeToolMethod(expertMatchTools, methodName, arguments);
        if (result != null) {
            return result;
        }

        result = invokeToolMethod(chatManagementTools, methodName, arguments);
        if (result != null) {
            return result;
        }

        result = invokeToolMethod(retrievalTools, methodName, arguments);
        if (result != null) {
            return result;
        }

        throw new IllegalArgumentException("Unknown tool: " + toolName);
    }

    /**
     * Invokes a tool method using reflection.
     */
    private Object invokeToolMethod(Object toolComponent, String methodName, Map<String, Object> arguments) {
        try {
            Class<?> clazz = toolComponent.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    // Build method arguments from MCP arguments
                    Parameter[] parameters = method.getParameters();
                    Object[] methodArgs = new Object[parameters.length];

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter param = parameters[i];
                        String paramName = param.getName();

                        // Try to get argument value (handle snake_case to camelCase conversion)
                        Object value = arguments.get(paramName);
                        if (value == null) {
                            // Try snake_case version
                            value = arguments.get(toSnakeCase(paramName));
                        }

                        // Convert to appropriate type
                        methodArgs[i] = convertValue(value, param.getType());
                    }

                    return method.invoke(toolComponent, methodArgs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke tool method: " + methodName, e);
        }

        return null;
    }

    /**
     * Converts value to target type.
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        // Handle common conversions
        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }

        // For complex types, try JSON conversion
        if (value instanceof Map) {
            return objectMapper.convertValue(value, targetType);
        }

        return value;
    }

    /**
     * Converts camelCase to snake_case.
     */
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}

