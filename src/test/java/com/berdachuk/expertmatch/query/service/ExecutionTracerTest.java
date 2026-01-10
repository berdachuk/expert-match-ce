package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.query.domain.ExecutionTrace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionTracerTest {

    @Test
    void testStartAndEndStep() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.startStep("Test Step", "TestService", "testMethod");
        tracer.endStep("Input: test", "Output: success");

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertEquals(1, trace.steps().size());
        ExecutionTrace.ExecutionStep step = trace.steps().get(0);
        assertEquals("Test Step", step.name());
        assertEquals("TestService", step.service());
        assertEquals("testMethod", step.method());
        assertEquals("SUCCESS", step.status());
        assertEquals("Input: test", step.inputSummary());
        assertEquals("Output: success", step.outputSummary());
        assertNull(step.llmModel());
        assertNull(step.tokenUsage());
        assertTrue(step.durationMs() >= 0);
    }

    @Test
    void testStartAndEndStepWithLLM() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.startStep("LLM Step", "LLMService", "generate");
        ExecutionTrace.TokenUsage tokenUsage = new ExecutionTrace.TokenUsage(45, 12, 57);
        tracer.endStepWithLLM("Query: test", "Answer: success", "OllamaChatModel (test-model)", tokenUsage);

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertEquals(1, trace.steps().size());
        ExecutionTrace.ExecutionStep step = trace.steps().get(0);
        assertEquals("LLM Step", step.name());
        assertEquals("OllamaChatModel (test-model)", step.llmModel());
        assertNotNull(step.tokenUsage());
        assertEquals(45, step.tokenUsage().inputTokens());
        assertEquals(12, step.tokenUsage().outputTokens());
        assertEquals(57, step.tokenUsage().totalTokens());
    }

    @Test
    void testMultipleSteps() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.startStep("Step 1", "Service1", "method1");
        tracer.endStep("Input 1", "Output 1");

        tracer.startStep("Step 2", "Service2", "method2");
        ExecutionTrace.TokenUsage tokenUsage = new ExecutionTrace.TokenUsage(10, 5, 15);
        tracer.endStepWithLLM("Input 2", "Output 2", "Model1", tokenUsage);

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertEquals(2, trace.steps().size());
        assertEquals("Step 1", trace.steps().get(0).name());
        assertEquals("Step 2", trace.steps().get(1).name());
    }

    @Test
    void testFailStep() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.failStep("Failed Step", "FailedService", "failedMethod", "Error message");

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertEquals(1, trace.steps().size());
        ExecutionTrace.ExecutionStep step = trace.steps().get(0);
        assertEquals("Failed Step", step.name());
        assertEquals("FAILED", step.status());
        assertEquals("Error message", step.inputSummary());
        assertNull(step.outputSummary());
    }

    @Test
    void testSkipStep() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.skipStep("Skipped Step", "Not needed");

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertEquals(1, trace.steps().size());
        ExecutionTrace.ExecutionStep step = trace.steps().get(0);
        assertEquals("Skipped Step", step.name());
        assertEquals("SKIPPED", step.status());
        assertEquals("Not needed", step.inputSummary());
        assertEquals(0L, step.durationMs());
    }

    @Test
    void testTotalDuration() {
        ExecutionTracer tracer = new ExecutionTracer();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        tracer.startStep("Step", "Service", "method");
        tracer.endStep("Input", "Output");

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertTrue(trace.totalDurationMs() >= 10);
    }

    @Test
    void testAggregatedTokenUsage() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.startStep("Step 1", "Service1", "method1");
        ExecutionTrace.TokenUsage usage1 = new ExecutionTrace.TokenUsage(10, 5, 15);
        tracer.endStepWithLLM("Input 1", "Output 1", "Model1", usage1);

        tracer.startStep("Step 2", "Service2", "method2");
        ExecutionTrace.TokenUsage usage2 = new ExecutionTrace.TokenUsage(20, 10, 30);
        tracer.endStepWithLLM("Input 2", "Output 2", "Model2", usage2);

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertNotNull(trace.totalTokenUsage());
        assertEquals(30, trace.totalTokenUsage().inputTokens());
        assertEquals(15, trace.totalTokenUsage().outputTokens());
        assertEquals(45, trace.totalTokenUsage().totalTokens());
    }

    @Test
    void testAggregatedTokenUsageWithNull() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.startStep("Step 1", "Service1", "method1");
        ExecutionTrace.TokenUsage usage1 = new ExecutionTrace.TokenUsage(10, 5, 15);
        tracer.endStepWithLLM("Input 1", "Output 1", "Model1", usage1);

        tracer.startStep("Step 2", "Service2", "method2");
        tracer.endStep("Input 2", "Output 2"); // No LLM, no tokens

        ExecutionTrace.ExecutionTraceData trace = tracer.buildTrace();

        assertNotNull(trace.totalTokenUsage());
        assertEquals(10, trace.totalTokenUsage().inputTokens());
        assertEquals(5, trace.totalTokenUsage().outputTokens());
        assertEquals(15, trace.totalTokenUsage().totalTokens());
    }

    @Test
    void testTokenUsageSum() {
        ExecutionTrace.TokenUsage a = new ExecutionTrace.TokenUsage(10, 5, 15);
        ExecutionTrace.TokenUsage b = new ExecutionTrace.TokenUsage(20, 10, 30);

        ExecutionTrace.TokenUsage sum = ExecutionTrace.TokenUsage.sum(a, b);

        assertEquals(30, sum.inputTokens());
        assertEquals(15, sum.outputTokens());
        assertEquals(45, sum.totalTokens());
    }

    @Test
    void testTokenUsageSumWithNull() {
        ExecutionTrace.TokenUsage a = new ExecutionTrace.TokenUsage(10, 5, 15);

        ExecutionTrace.TokenUsage sum1 = ExecutionTrace.TokenUsage.sum(a, null);
        assertEquals(a, sum1);

        ExecutionTrace.TokenUsage sum2 = ExecutionTrace.TokenUsage.sum(null, a);
        assertEquals(a, sum2);

        ExecutionTrace.TokenUsage sum3 = ExecutionTrace.TokenUsage.sum(null, null);
        assertNull(sum3);
    }
}

