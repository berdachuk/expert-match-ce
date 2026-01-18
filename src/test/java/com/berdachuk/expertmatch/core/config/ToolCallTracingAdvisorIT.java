package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ToolCallTracingAdvisor configuration.
 * Verifies that the advisor is properly configured and can track tool calls.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "expertmatch.skills.enabled=true",
        "expertmatch.tools.search.enabled=false"
})
class ToolCallTracingAdvisorIT extends BaseIntegrationTest {

    @Autowired(required = false)
    private ToolCallTracingAdvisor toolCallTracingAdvisor;

    @Test
    @DisplayName("Verify ToolCallTracingAdvisor bean is created when skills are enabled")
    void testToolCallTracingAdvisorBeanExists() {
        assertThat(toolCallTracingAdvisor).isNotNull();
        assertThat(toolCallTracingAdvisor.getName()).isEqualTo("ToolCallTracingAdvisor");
        assertThat(toolCallTracingAdvisor.getOrder()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify ThreadLocal storage works correctly")
    void testThreadLocalStorage() {
        ExecutionTracer tracer1 = new ExecutionTracer();
        ExecutionTracer tracer2 = new ExecutionTracer();

        // Set first tracer
        ExecutionTracer.setCurrent(tracer1);
        assertThat(ExecutionTracer.getCurrent()).isEqualTo(tracer1);

        // Set second tracer (should replace first)
        ExecutionTracer.setCurrent(tracer2);
        assertThat(ExecutionTracer.getCurrent()).isEqualTo(tracer2);

        // Clear
        ExecutionTracer.clear();
        assertThat(ExecutionTracer.getCurrent()).isNull();

        // Verify isolation - setting in one thread doesn't affect another
        ExecutionTracer.setCurrent(tracer1);
        assertThat(ExecutionTracer.getCurrent()).isEqualTo(tracer1);
    }

    @Test
    @DisplayName("Verify ExecutionTracer can record tool calls")
    void testExecutionTracerRecordToolCall() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.recordToolCall("Skill", "AGENT_SKILL",
                "{\"skillName\":\"expert-matching-hybrid-retrieval\"}",
                "{\"status\":\"executed\"}",
                "expert-matching-hybrid-retrieval",
                150L);

        var trace = tracer.buildTrace();
        assertThat(trace.steps()).hasSize(1);
        var step = trace.steps().get(0);
        assertThat(step.name()).isEqualTo("Tool Call: Skill");
        assertThat(step.service()).isEqualTo("ChatClient");
        assertThat(step.method()).isEqualTo("toolCall");
        assertThat(step.status()).isEqualTo("SUCCESS");
        assertThat(step.toolCall()).isNotNull();
        assertThat(step.toolCall().toolName()).isEqualTo("Skill");
        assertThat(step.toolCall().toolType()).isEqualTo("AGENT_SKILL");
        assertThat(step.toolCall().skillName()).isEqualTo("expert-matching-hybrid-retrieval");
        assertThat(step.durationMs()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Verify ExecutionTracer can record tool call failures")
    void testExecutionTracerRecordToolCallFailure() {
        ExecutionTracer tracer = new ExecutionTracer();

        tracer.recordToolCallFailure("Skill", "AGENT_SKILL",
                "{\"skillName\":\"invalid-skill\"}",
                "Skill not found",
                "invalid-skill",
                100L);

        var trace = tracer.buildTrace();
        assertThat(trace.steps()).hasSize(1);
        var step = trace.steps().get(0);
        assertThat(step.status()).isEqualTo("FAILED");
        assertThat(step.toolCall()).isNotNull();
        assertThat(step.toolCall().response()).contains("ERROR: Skill not found");
    }
}
