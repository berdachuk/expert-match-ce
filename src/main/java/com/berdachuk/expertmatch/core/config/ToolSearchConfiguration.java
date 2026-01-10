package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.llm.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.tool.search.ToolSearchToolCallAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration for Tool Search Tool integration.
 * Enables dynamic tool discovery using PgVector-based semantic search.
 * <p>
 * This configuration is only active when expertmatch.tools.search.enabled=true.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "expertmatch.tools.search.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ToolSearchConfiguration {

    private final ToolMetadataService toolMetadataService;
    private final ObjectProvider<ExpertMatchTools> expertMatchToolsProvider;
    private final ObjectProvider<ChatManagementTools> chatManagementToolsProvider;
    private final ObjectProvider<RetrievalTools> retrievalToolsProvider;
    private final int maxResults;

    public ToolSearchConfiguration(
            ToolMetadataService toolMetadataService,
            ObjectProvider<ExpertMatchTools> expertMatchToolsProvider,
            ObjectProvider<ChatManagementTools> chatManagementToolsProvider,
            ObjectProvider<RetrievalTools> retrievalToolsProvider,
            @Value("${expertmatch.tools.search.max-results:5}") int maxResults
    ) {
        this.toolMetadataService = toolMetadataService;
        this.expertMatchToolsProvider = expertMatchToolsProvider;
        this.chatManagementToolsProvider = chatManagementToolsProvider;
        this.retrievalToolsProvider = retrievalToolsProvider;
        this.maxResults = maxResults;
        log.info("ToolSearchConfiguration initialized with maxResults: {}", maxResults);
    }

    /**
     * Indexes tools on application startup.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void indexToolsOnStartup() {
        log.info("Indexing tools on application startup...");
        // Use ObjectProvider to get beans lazily, breaking circular dependency
        ExpertMatchTools expertMatchTools = expertMatchToolsProvider.getIfAvailable();
        ChatManagementTools chatManagementTools = chatManagementToolsProvider.getIfAvailable();
        RetrievalTools retrievalTools = retrievalToolsProvider.getIfAvailable();

        if (expertMatchTools != null) {
            toolMetadataService.indexTools(expertMatchTools);
        }
        if (chatManagementTools != null) {
            toolMetadataService.indexTools(chatManagementTools);
        }
        if (retrievalTools != null) {
            toolMetadataService.indexTools(retrievalTools);
        }
        log.info("Tool indexing completed");
    }

    /**
     * Creates ToolSearchToolCallAdvisor bean for dynamic tool discovery.
     */
    @Bean
    public ToolSearchToolCallAdvisor toolSearchToolCallAdvisor(PgVectorToolSearcher toolSearcher) {
        log.info("Creating ToolSearchToolCallAdvisor with maxResults: {}", maxResults);
        return ToolSearchToolCallAdvisor.builder()
                .toolSearcher(toolSearcher)
                .maxResults(maxResults)
                .build();
    }

    /**
     * Creates a ChatClient with Tool Search Tool enabled.
     * This ChatClient uses ToolSearchToolCallAdvisor for dynamic tool discovery.
     * All tools are registered but only Tool Search Tool is sent initially to LLM.
     * When LLM needs capabilities, it calls Tool Search Tool which discovers and adds relevant tools.
     */
    @Bean("chatClientWithToolSearch")
    @Primary
    @ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public ChatClient chatClientWithToolSearch(
            ChatClient.Builder builder,
            ObjectProvider<ExpertMatchTools> expertToolsProvider,
            ObjectProvider<ChatManagementTools> chatToolsProvider,
            ObjectProvider<RetrievalTools> retrievalToolsProvider,
            ToolSearchToolCallAdvisor toolSearchAdvisor
    ) {
        log.info("Creating chatClientWithToolSearch with Tool Search Tool enabled");
        // Use ObjectProvider to break circular dependency - get beans lazily
        ExpertMatchTools expertTools = expertToolsProvider.getIfAvailable();
        ChatManagementTools chatTools = chatToolsProvider.getIfAvailable();
        RetrievalTools retrievalTools = retrievalToolsProvider.getIfAvailable();

        if (expertTools == null || chatTools == null || retrievalTools == null) {
            throw new IllegalStateException("Required tool beans are not available");
        }

        return builder
                .defaultTools(expertTools, chatTools, retrievalTools)  // All tools registered
                .defaultAdvisors(toolSearchAdvisor, new SimpleLoggerAdvisor())  // Tool Search Tool advisor activates dynamic discovery
                .build();
    }
}

