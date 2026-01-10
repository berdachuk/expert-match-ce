package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.llm.tools.ExpertMatchTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AI tool integration.
 * Creates a ChatClient bean with tools enabled.
 */
@Configuration
public class ToolConfiguration {

    /**
     * Creates a ChatClient with ExpertMatch tools enabled.
     * This is a separate bean from the default ChatClient to allow
     * tool-enabled interactions while maintaining backward compatibility.
     */
    @Bean("chatClientWithTools")
    public ChatClient chatClientWithTools(
            ChatClient.Builder builder,
            ExpertMatchTools tools
    ) {
        return builder
                .defaultTools(tools)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}

