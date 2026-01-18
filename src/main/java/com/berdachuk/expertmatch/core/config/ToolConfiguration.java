package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.llm.tools.ExpertMatchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * Creates a ChatClient with ExpertMatch tools enabled.
     * This is a separate bean from the default ChatClient to allow
     * tool-enabled interactions while maintaining backward compatibility.
     * Only created when no other specialized ChatClient beans exist (skills/tool search disabled).
     */
    @Bean("chatClientWithTools")
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

