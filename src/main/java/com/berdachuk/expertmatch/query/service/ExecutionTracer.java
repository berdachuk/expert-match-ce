package com.berdachuk.expertmatch.query.service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to track execution steps for query processing.
 * Thread-safe for single-threaded use within a single request.
 */
@Slf4j
public class ExecutionTracer {
    /**
     * ThreadLocal storage for current ExecutionTracer instance.
     * Allows advisors to access the tracer without passing it as a parameter.
     */
    private static final ThreadLocal<ExecutionTracer> currentTracer = new ThreadLocal<>();
    private final List<com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep> steps = new ArrayList<>();
    private final Map<String, StepContext> activeSteps = new HashMap<>();
    private final long startTime = System.currentTimeMillis();

    /**
     * Gets the current ExecutionTracer for this thread.
     *
     * @return The current ExecutionTracer instance, or null if not set
     */
    public static ExecutionTracer getCurrent() {
        return currentTracer.get();
    }

    /**
     * Sets the current ExecutionTracer for this thread.
     *
     * @param tracer The ExecutionTracer instance to set
     */
    public static void setCurrent(ExecutionTracer tracer) {
        currentTracer.set(tracer);
    }

    /**
     * Clears the current ExecutionTracer for this thread.
     */
    public static void clear() {
        currentTracer.remove();
    }

    /**
     * Starts timing a step.
     *
     * @param name    Step name
     * @param service Service name
     * @param method  Method name
     */
    public void startStep(String name, String service, String method) {
        String key = name + ":" + service + ":" + method;
        activeSteps.put(key, new StepContext(name, service, method, System.currentTimeMillis()));
    }

    /**
     * Ends a step without LLM (no model or token usage).
     *
     * @param inputSummary  Input summary
     * @param outputSummary Output summary
     */
    public void endStep(String inputSummary, String outputSummary) {
        endStepWithLLM(inputSummary, outputSummary, null, null);
    }

    /**
     * Ends a step with LLM information.
     *
     * @param inputSummary  Input summary
     * @param outputSummary Output summary
     * @param llmModel      LLM model name (null if no LLM)
     * @param tokenUsage    Token usage (null if not available)
     */
    public void endStepWithLLM(String inputSummary, String outputSummary, String llmModel, com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage) {
        // Find the most recently started step (simple approach - use last entry)
        if (activeSteps.isEmpty()) {
            log.warn("Attempted to end step but no active step found");
            return;
        }

        // Get the last started step (most recent)
        StepContext context = null;
        String lastKey = null;
        for (Map.Entry<String, StepContext> entry : activeSteps.entrySet()) {
            lastKey = entry.getKey();
            context = entry.getValue();
        }

        if (context != null && lastKey != null) {
            long duration = System.currentTimeMillis() - context.startTime;
            com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step = new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep(
                    context.name,
                    context.service,
                    context.method,
                    duration,
                    "SUCCESS",
                    inputSummary,
                    outputSummary,
                    llmModel,
                    tokenUsage,
                    null // toolCall - null for regular steps
            );
            steps.add(step);
            activeSteps.remove(lastKey);
        }
    }

    /**
     * Records a failed step.
     *
     * @param name    Step name
     * @param service Service name
     * @param method  Method name
     * @param error   Error message
     */
    public void failStep(String name, String service, String method, String error) {
        // Remove from active if present
        String key = name + ":" + service + ":" + method;
        StepContext context = activeSteps.remove(key);
        long duration = context != null ? System.currentTimeMillis() - context.startTime : 0;

        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step = new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep(
                name,
                service,
                method,
                duration,
                "FAILED",
                error,
                null,
                null,
                null,
                null // toolCall - null for failed steps
        );
        steps.add(step);
    }

    /**
     * Records a skipped step.
     *
     * @param name   Step name
     * @param reason Reason for skipping
     */
    public void skipStep(String name, String reason) {
        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step = new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep(
                name,
                null,
                null,
                0L,
                "SKIPPED",
                reason,
                null,
                null,
                null,
                null // toolCall - null for skipped steps
        );
        steps.add(step);
    }

    /**
     * Records a tool call step.
     *
     * @param toolName   Tool name (e.g., "Skill", "expertQuery")
     * @param toolType   Tool type (AGENT_SKILL, JAVA_TOOL, FILE_SYSTEM_TOOL)
     * @param parameters Tool parameters (JSON string)
     * @param response   Tool response (JSON string)
     * @param skillName  Skill name (if toolType is AGENT_SKILL, null otherwise)
     * @param durationMs Tool call duration in milliseconds
     */
    public void recordToolCall(String toolName, String toolType, String parameters,
                               String response, String skillName, long durationMs) {
        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ToolCallInfo toolCallInfo =
                new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ToolCallInfo(
                        toolName,
                        toolType,
                        parameters,
                        response,
                        skillName
                );

        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step =
                new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep(
                        "Tool Call: " + toolName,
                        "ChatClient",
                        "toolCall",
                        durationMs,
                        "SUCCESS",
                        "Tool: " + toolName + (skillName != null ? ", Skill: " + skillName : ""),
                        "Response received",
                        null, // No LLM model for tool calls
                        null, // Token usage tracked separately
                        toolCallInfo
                );
        steps.add(step);
    }

    /**
     * Records a failed tool call.
     *
     * @param toolName   Tool name (e.g., "Skill", "expertQuery")
     * @param toolType   Tool type (AGENT_SKILL, JAVA_TOOL, FILE_SYSTEM_TOOL)
     * @param parameters Tool parameters (JSON string)
     * @param error      Error message
     * @param skillName  Skill name (if toolType is AGENT_SKILL, null otherwise)
     * @param durationMs Tool call duration in milliseconds
     */
    public void recordToolCallFailure(String toolName, String toolType, String parameters,
                                      String error, String skillName, long durationMs) {
        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ToolCallInfo toolCallInfo =
                new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ToolCallInfo(
                        toolName,
                        toolType,
                        parameters,
                        "ERROR: " + error,
                        skillName
                );

        com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step =
                new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep(
                        "Tool Call: " + toolName,
                        "ChatClient",
                        "toolCall",
                        durationMs,
                        "FAILED",
                        "Tool: " + toolName + (skillName != null ? ", Skill: " + skillName : ""),
                        "Error: " + error,
                        null, // No LLM model for tool calls
                        null, // Token usage tracked separately
                        toolCallInfo
                );
        steps.add(step);
    }

    /**
     * Builds the final execution trace with aggregated token usage.
     *
     * @return ExecutionTrace with all steps and aggregated information
     */
    public com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionTraceData buildTrace() {
        long totalDuration = System.currentTimeMillis() - startTime;

        // Aggregate token usage across all LLM steps
        com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage totalTokenUsage = null;
        for (com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionStep step : steps) {
            if (step.tokenUsage() != null) {
                totalTokenUsage = com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage.sum(totalTokenUsage, step.tokenUsage());
            }
        }

        return new com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionTraceData(
                new ArrayList<>(steps),
                totalDuration,
                totalTokenUsage
        );
    }

    /**
     * Internal context for tracking active steps.
     */
    private record StepContext(String name, String service, String method, long startTime) {
    }
}

