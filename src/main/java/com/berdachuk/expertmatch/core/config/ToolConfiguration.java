package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.query.tools.ExpertMatchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for Spring AI tool integration.
 * Creates a ChatClient bean with tools enabled.
 */
@Configuration
public class ToolConfiguration {

    /**
     * Creates ToolCallTracingAdvisor bean for tracking tool calls in Execution Trace.
     * Only created if not already defined in AgentSkillsConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean(ToolCallTracingAdvisor.class)
    public ToolCallTracingAdvisor toolCallTracingAdvisor() {
        return new ToolCallTracingAdvisor();
    }

    /**
     * Creates a ChatClient with ExpertMatch tools enabled (getRetrievedExperts etc.).
     * Marked as @Primary so answer generation uses this client; when the LLM returns a tool call,
     * Spring AI executes it and continues until the final text answer is produced. Without this,
     * the no-tools ChatClient would be used and the user would see raw tool_calls JSON.
     * Only created when no other specialized ChatClient beans exist (skills/tool search disabled).
     * Excluded from test profile so tests can use the mock testChatClient as the single primary.
     */
    @Bean("chatClientWithTools")
    @Primary
    @Profile("!test")
    @ConditionalOnMissingBean(name = {"chatClientWithSkills", "chatClientWithToolSearch", "chatClientWithSkillsAndTools"})
    public ChatClient chatClientWithTools(
            ChatClient.Builder builder,
            ExpertMatchTools tools,
            ToolCallTracingAdvisor toolCallTracingAdvisor
    ) {
        return builder
                .defaultTools(tools)
                .defaultAdvisors(toolCallTracingAdvisor, new SimpleLoggerAdvisor())
                .build();
    }
}

