package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.llm.tools.ChatManagementTools;
import com.berdachuk.expertmatch.llm.tools.PgVectorToolSearcher;
import com.berdachuk.expertmatch.llm.tools.ToolMetadataService;
import com.berdachuk.expertmatch.query.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.query.tools.RetrievalTools;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.tool.search.ToolSearchToolCallAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * <p>
 * <b>NOTE:</b> Tool Search Tool is currently DISABLED by default because:
 * <ul>
 *   <li>Incompatible with Spring AI 1.1.0 (ToolCallAdvisor is final, cannot be extended)</li>
 *   <li>Conflicts with Agent Skills (both try to be @Primary ChatClient)</li>
 *   <li>Requires Spring AI 2.0.0+ for full compatibility</li>
 * </ul>
 * Agent Skills are the recommended approach for Spring AI 1.1.0.
 * <p>
 * <b>IMPORTANT:</b> This configuration uses @ConditionalOnClass to prevent class loading
 * when Tool Search classes are not available or incompatible. This avoids IncompatibleClassChangeError
 * when Tool Search is disabled.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "expertmatch.tools.search.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
        name = "org.springaicommunity.tool.search.ToolSearchToolCallAdvisor"
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
     * Creates ToolCallTracingAdvisor bean for tracking tool calls in Execution Trace.
     * Only created if not already defined in AgentSkillsConfiguration or ToolConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean(ToolCallTracingAdvisor.class)
    public ToolCallTracingAdvisor toolCallTracingAdvisor() {
        log.info("Creating ToolCallTracingAdvisor bean for tool call tracking");
        return new ToolCallTracingAdvisor();
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
            ToolSearchToolCallAdvisor toolSearchAdvisor,
            ToolCallTracingAdvisor toolCallTracingAdvisor
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
                .defaultAdvisors(toolSearchAdvisor, toolCallTracingAdvisor, new SimpleLoggerAdvisor())  // Tool Search Tool advisor activates dynamic discovery
                .build();
    }

    /**
     * Creates a ChatClient with both Tool Search Tool and Agent Skills enabled.
     * This ChatClient integrates SkillsTool for skill discovery with ToolSearchToolCallAdvisor
     * for dynamic tool discovery, providing a hybrid approach.
     * <p>
     * This bean is only created when both expertmatch.tools.search.enabled=true
     * and expertmatch.skills.enabled=true.
     */
    @Bean("chatClientWithSkillsAndTools")
    @Primary
    @ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    @ConditionalOnBean(name = "skillsTool")
    public ChatClient chatClientWithSkillsAndTools(
            ChatClient.Builder builder,
            ObjectProvider<ExpertMatchTools> expertToolsProvider,
            ObjectProvider<ChatManagementTools> chatToolsProvider,
            ObjectProvider<RetrievalTools> retrievalToolsProvider,
            ToolSearchToolCallAdvisor toolSearchAdvisor,
            @org.springframework.beans.factory.annotation.Qualifier("skillsTool") ToolCallback skillsTool,
            FileSystemTools fileSystemTools,
            ToolCallTracingAdvisor toolCallTracingAdvisor
    ) {
        log.info("Creating chatClientWithSkillsAndTools with Tool Search Tool and Agent Skills enabled");
        // Use ObjectProvider to break circular dependency - get beans lazily
        ExpertMatchTools expertTools = expertToolsProvider.getIfAvailable();
        ChatManagementTools chatTools = chatToolsProvider.getIfAvailable();
        RetrievalTools retrievalTools = retrievalToolsProvider.getIfAvailable();

        if (expertTools == null || chatTools == null || retrievalTools == null) {
            throw new IllegalStateException("Required tool beans are not available");
        }

        return builder
                // Agent Skills (new) - provides knowledge/instructions
                .defaultToolCallbacks(skillsTool)  // Skills discovery (ToolCallback)
                .defaultTools(fileSystemTools)  // File reading tools
                // Java @Tool methods (existing) - provides actions/business logic
                .defaultTools(expertTools, chatTools, retrievalTools)
                // Existing Advisors
                .defaultAdvisors(toolSearchAdvisor, toolCallTracingAdvisor, new SimpleLoggerAdvisor())
                .build();
    }
}

