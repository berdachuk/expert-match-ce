package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.core.service.ExecutionTracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advisor that intercepts tool calls and adds them to Execution Trace.
 * <p>
 * This advisor tracks all tool calls (Agent Skills, Java @Tool methods, FileSystemTools)
 * and records them in the Execution Trace when tracing is enabled.
 */
@Slf4j
public class ToolCallTracingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Long> toolCallStartTimes = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0; // Run after other advisors to capture final tool calls
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ExecutionTracer tracer = ExecutionTracer.getCurrent();

        log.info("ToolCallTracingAdvisor.adviseCall() called, tracer: {}", tracer != null ? "present" : "null");

        // Execute the chain to get the response
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        // Process tool calls if tracing is enabled
        if (tracer != null) {
            log.info("Processing tool calls with tracer present");
            processToolCalls(chatClientResponse, tracer);
        } else {
            log.debug("No tracer present, skipping tool call processing");
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        ExecutionTracer tracer = ExecutionTracer.getCurrent();

        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

        // Process tool calls in streaming responses
        if (tracer != null) {
            return chatClientResponses.doOnNext(response -> processToolCalls(response, tracer));
        }

        return chatClientResponses;
    }

    /**
     * Processes tool calls from a ChatClientResponse and records them in Execution Trace.
     */
    private void processToolCalls(ChatClientResponse chatClientResponse, ExecutionTracer tracer) {
        try {
            if (chatClientResponse == null) {
                return;
            }

            // Get the ChatResponse from ChatClientResponse using reflection
            // In Spring AI 1.1.0, ChatClientResponse may not expose ChatResponse directly
            // We need to check what methods are available
            ChatResponse chatResponse = null;
            try {
                // Try all possible method names to access ChatResponse
                java.lang.reflect.Method[] methods = chatClientResponse.getClass().getMethods();
                for (java.lang.reflect.Method method : methods) {
                    String methodName = method.getName();
                    // Look for methods that return ChatResponse
                    if (method.getReturnType().equals(ChatResponse.class) &&
                            method.getParameterCount() == 0) {
                        try {
                            chatResponse = (ChatResponse) method.invoke(chatClientResponse);
                            log.debug("Successfully accessed ChatResponse via method: {}", methodName);
                            break;
                        } catch (Exception e) {
                            log.debug("Failed to invoke method {}: {}", methodName, e.getMessage());
                        }
                    }
                }

                // If still null, try common method names
                if (chatResponse == null) {
                    String[] methodNames = {"getResponse", "response", "getChatResponse", "chatResponse"};
                    for (String methodName : methodNames) {
                        try {
                            java.lang.reflect.Method method = chatClientResponse.getClass().getMethod(methodName);
                            if (ChatResponse.class.isAssignableFrom(method.getReturnType())) {
                                chatResponse = (ChatResponse) method.invoke(chatClientResponse);
                                log.debug("Successfully accessed ChatResponse via method: {}", methodName);
                                break;
                            }
                        } catch (NoSuchMethodException e) {
                            // Continue to next method name
                            continue;
                        } catch (Exception e) {
                            log.debug("Failed to invoke method {}: {}", methodName, e.getMessage());
                        }
                    }
                }

                if (chatResponse == null) {
                    log.warn("Could not access ChatResponse from ChatClientResponse. Available methods: {}",
                            java.util.Arrays.stream(chatClientResponse.getClass().getMethods())
                                    .map(java.lang.reflect.Method::getName)
                                    .filter(name -> name.toLowerCase().contains("response"))
                                    .collect(java.util.stream.Collectors.joining(", ")));
                    return;
                }
            } catch (Exception e) {
                log.warn("Failed to get ChatResponse from ChatClientResponse: {}", e.getMessage(), e);
                return;
            }

            if (chatResponse == null) {
                return;
            }

            // Check all results for tool calls
            // In Spring AI, tool calls can appear in any Generation's output (AssistantMessage)
            log.info("🔍 Processing ChatResponse for tool calls, ChatResponse class: {}", chatResponse.getClass().getName());

            try {
                // Try getResults() first (returns List<Generation>)
                java.lang.reflect.Method getResultsMethod = chatResponse.getClass().getMethod("getResults");
                @SuppressWarnings("unchecked")
                java.util.List<?> results = (java.util.List<?>) getResultsMethod.invoke(chatResponse);

                log.debug("getResults() returned: {} (size: {})", results != null ? "not null" : "null",
                        results != null ? results.size() : 0);

                if (results != null && !results.isEmpty()) {
                    log.info("🔍 Checking {} results for tool calls", results.size());
                    for (int i = 0; i < results.size(); i++) {
                        Object result = results.get(i);
                        if (result == null) {
                            log.warn("⚠️  Result {} is null", i);
                            continue;
                        }

                        log.info("🔍 Processing result {} of type {}", i, result.getClass().getName());

                        // Get output from result (AssistantMessage)
                        try {
                            java.lang.reflect.Method getOutputMethod = result.getClass().getMethod("getOutput");
                            Object output = getOutputMethod.invoke(result);

                            log.info("🔍 Result {} output: {} (type: {})", i, output != null ? "not null" : "null",
                                    output != null ? output.getClass().getName() : "N/A");

                            if (output != null) {
                                // Check for tool calls in the output message
                                processToolCallsFromMessage(output, tracer);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️  Failed to get output from result {}: {}", i, e.getMessage());
                        }
                    }
                } else {
                    log.warn("⚠️  Results list is null or empty, trying getResult()");
                }

                // Also check the single result if getResults() doesn't work or returns empty
                try {
                    java.lang.reflect.Method getResultMethod = chatResponse.getClass().getMethod("getResult");
                    Object result = getResultMethod.invoke(chatResponse);
                    log.debug("getResult() returned: {} (type: {})", result != null ? "not null" : "null",
                            result != null ? result.getClass().getName() : "N/A");

                    if (result != null) {
                        try {
                            java.lang.reflect.Method getOutputMethod = result.getClass().getMethod("getOutput");
                            Object output = getOutputMethod.invoke(result);
                            log.debug("Single result output: {} (type: {})", output != null ? "not null" : "null",
                                    output != null ? output.getClass().getName() : "N/A");

                            if (output != null) {
                                processToolCallsFromMessage(output, tracer);
                            }
                        } catch (Exception e) {
                            log.debug("Failed to get output from single result: {}", e.getMessage());
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // getResult() not available, that's okay - we already checked getResults()
                    log.debug("getResult() method not available");
                }
            } catch (Exception e) {
                log.warn("Failed to get results from ChatResponse: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.warn("Error processing tool calls in advisor: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes tool calls from a message (AssistantMessage).
     */
    private void processToolCallsFromMessage(Object message, ExecutionTracer tracer) {
        if (message == null || tracer == null) {
            return;
        }

        try {
            // Use reflection to access getToolCalls() method on the message
            java.lang.reflect.Method getToolCallsMethod = message.getClass().getMethod("getToolCalls");
            @SuppressWarnings("unchecked")
            java.util.List<?> toolCalls = (java.util.List<?>) getToolCallsMethod.invoke(message);

            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.debug("Found {} tool calls in message", toolCalls.size());
                for (Object toolCall : toolCalls) {
                    try {
                        String toolCallId = extractToolCallId(toolCall);
                        String toolName = extractToolCallName(toolCall);
                        long startTime = toolCallStartTimes.getOrDefault(toolCallId, System.currentTimeMillis());
                        long durationMs = System.currentTimeMillis() - startTime;

                        String toolType = determineToolType(toolName);
                        String skillName = extractSkillName(toolCall, toolType);
                        String parameters = serializeToolCallArguments(toolCall);
                        String responseJson = serializeToolCallResponse(toolCall);

                        log.debug("Recording tool call: {} ({}), skill: {}", toolName, toolType, skillName);
                        tracer.recordToolCall(toolName, toolType, parameters, responseJson, skillName, durationMs);
                        toolCallStartTimes.remove(toolCallId);
                    } catch (Exception e) {
                        log.warn("Failed to record tool call in execution trace: {}", e.getMessage(), e);
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // getToolCalls() method not available - this message doesn't have tool calls
            log.info("Message does not have getToolCalls() method: {}", message.getClass().getName());
        } catch (Exception e) {
            log.warn("Failed to get tool calls from message: {}", e.getMessage(), e);
        }
    }

    /**
     * Determines the tool type based on tool name.
     */
    private String determineToolType(String toolName) {
        if (toolName == null) {
            return "UNKNOWN";
        }
        // Check if it's Agent Skills (Skill tool)
        if (toolName.equals("Skill")) {
            return "AGENT_SKILL";
        }
        // Check if it's FileSystemTools
        if (toolName.startsWith("readFile") || toolName.startsWith("listFiles") ||
                toolName.startsWith("read") || toolName.startsWith("list")) {
            return "FILE_SYSTEM_TOOL";
        }
        // Otherwise, it's a Java @Tool method
        return "JAVA_TOOL";
    }

    /**
     * Extracts tool call ID using reflection.
     */
    private String extractToolCallId(Object toolCall) {
        try {
            java.lang.reflect.Method getIdMethod = toolCall.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(toolCall);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to extract tool call ID: {}", e.getMessage());
        }
        return extractToolCallName(toolCall) + "_" + System.currentTimeMillis();
    }

    /**
     * Extracts tool call name using reflection.
     */
    private String extractToolCallName(Object toolCall) {
        try {
            java.lang.reflect.Method getNameMethod = toolCall.getClass().getMethod("getName");
            Object name = getNameMethod.invoke(toolCall);
            if (name != null) {
                return name.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to extract tool call name: {}", e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Extracts skill name from tool call if it's an Agent Skill.
     */
    private String extractSkillName(Object toolCall, String toolType) {
        if (!"AGENT_SKILL".equals(toolType)) {
            return null;
        }
        try {
            java.lang.reflect.Method getArgumentsMethod = toolCall.getClass().getMethod("getArguments");
            Object arguments = getArgumentsMethod.invoke(toolCall);
            if (arguments != null) {
                // Try to parse arguments as JSON and extract skillName
                String argsJson = arguments.toString();
                Map<String, Object> args = objectMapper.readValue(argsJson,
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                Object skillName = args.get("skillName");
                if (skillName != null) {
                    return skillName.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract skill name from tool call: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Serializes tool call arguments to JSON string.
     */
    private String serializeToolCallArguments(Object toolCall) {
        try {
            java.lang.reflect.Method getArgumentsMethod = toolCall.getClass().getMethod("getArguments");
            Object arguments = getArgumentsMethod.invoke(toolCall);
            if (arguments != null) {
                String argsJson = arguments.toString();
                // Try to pretty print JSON for readability
                try {
                    Object json = objectMapper.readValue(argsJson, Object.class);
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                } catch (Exception e) {
                    // If not valid JSON, return as-is
                    return argsJson;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to serialize tool call arguments: {}", e.getMessage());
        }
        return "{}";
    }

    /**
     * Serializes tool call response to JSON string.
     * Note: The actual response might not be available in the ChatResponse,
     * as tool execution happens separately. This captures what we can see.
     */
    private String serializeToolCallResponse(Object toolCall) {
        // Tool call response is typically not available in the ChatResponse
        // It would be in a subsequent message. For now, we'll indicate it was called.
        return "{\"status\":\"executed\"}";
    }
}
