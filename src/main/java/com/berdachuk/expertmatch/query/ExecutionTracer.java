package com.berdachuk.expertmatch.query;

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
    private final List<ExecutionTrace.ExecutionStep> steps = new ArrayList<>();
    private final Map<String, StepContext> activeSteps = new HashMap<>();
    private final long startTime = System.currentTimeMillis();

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
    public void endStepWithLLM(String inputSummary, String outputSummary, String llmModel, ExecutionTrace.TokenUsage tokenUsage) {
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
            ExecutionTrace.ExecutionStep step = new ExecutionTrace.ExecutionStep(
                    context.name,
                    context.service,
                    context.method,
                    duration,
                    "SUCCESS",
                    inputSummary,
                    outputSummary,
                    llmModel,
                    tokenUsage
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

        ExecutionTrace.ExecutionStep step = new ExecutionTrace.ExecutionStep(
                name,
                service,
                method,
                duration,
                "FAILED",
                error,
                null,
                null,
                null
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
        ExecutionTrace.ExecutionStep step = new ExecutionTrace.ExecutionStep(
                name,
                null,
                null,
                0L,
                "SKIPPED",
                reason,
                null,
                null,
                null
        );
        steps.add(step);
    }

    /**
     * Builds the final execution trace with aggregated token usage.
     *
     * @return ExecutionTrace with all steps and aggregated information
     */
    public ExecutionTrace.ExecutionTraceData buildTrace() {
        long totalDuration = System.currentTimeMillis() - startTime;

        // Aggregate token usage across all LLM steps
        ExecutionTrace.TokenUsage totalTokenUsage = null;
        for (ExecutionTrace.ExecutionStep step : steps) {
            if (step.tokenUsage() != null) {
                totalTokenUsage = ExecutionTrace.TokenUsage.sum(totalTokenUsage, step.tokenUsage());
            }
        }

        return new ExecutionTrace.ExecutionTraceData(
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

