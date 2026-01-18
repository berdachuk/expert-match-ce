package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.llm.tools.ChatManagementTools;
import com.berdachuk.expertmatch.llm.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.llm.tools.RetrievalTools;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

/**
 * Configuration for Spring AI Agent Skills integration.
 * <p>
 * Agent Skills provide modular knowledge management through Markdown-based skills
 * that complement existing Java @Tool methods. This configuration enables skills
 * discovery and loading on demand.
 * <p>
 * Skills are loaded from classpath:.claude/skills for packaged applications.
 * This configuration is only active when expertmatch.skills.enabled=true.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "expertmatch.skills.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AgentSkillsConfiguration {

    private final ResourceLoader resourceLoader;

    public AgentSkillsConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Creates SkillsTool bean for discovering and loading skills on demand.
     * Skills are loaded from classpath for packaged applications.
     * <p>
     * Note: SkillsTool.Builder.build() returns ToolCallback in Spring AI 1.1.0,
     * so we return ToolCallback and use @Qualifier to identify it as SkillsTool.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("skillsTool")
    public ToolCallback skillsTool() {
        log.info("Creating SkillsTool bean for Agent Skills");
        SkillsTool.Builder builder = SkillsTool.builder();
        boolean skillsAdded = false;

        // First, try to load from local filesystem directory (for development)
        // This takes precedence as it's easier to modify during development
        try {
            java.io.File skillsDir = new java.io.File(".claude/skills");
            if (skillsDir.exists() && skillsDir.isDirectory()) {
                builder.addSkillsDirectory(".claude/skills");
                log.info("Added local filesystem skills directory: .claude/skills");
                skillsAdded = true;
            }
        } catch (Exception e) {
            log.debug("Local skills directory not found, skipping: {}", e.getMessage());
        }

        // Then, try to load from classpath (for packaged JAR applications)
        // This works when running from a JAR file
        if (!skillsAdded) {
            try {
                org.springframework.core.io.Resource skillsResource = resourceLoader.getResource("classpath:.claude/skills");
                if (skillsResource.exists()) {
                    builder.addSkillsResource(skillsResource);
                    log.info("Added classpath skills resource: classpath:.claude/skills");
                    skillsAdded = true;
                } else {
                    log.warn("Classpath skills resource not found: classpath:.claude/skills");
                }
            } catch (Exception e) {
                log.warn("Failed to load classpath skills resource: {}", e.getMessage());
            }
        }

        if (!skillsAdded) {
            log.error("No skills found! Please ensure skills are available in either:");
            log.error("  1. Local filesystem: .claude/skills/");
            log.error("  2. Classpath: classpath:.claude/skills/");
            throw new IllegalStateException("At least one skill must be configured. " +
                    "Please ensure skills are available in .claude/skills/ (filesystem) or " +
                    "classpath:.claude/skills/ (JAR)");
        }

        // builder.build() returns ToolCallback in Spring AI 1.1.0
        return builder.build();
    }

    /**
     * Creates FileSystemTools bean for reading reference files and assets.
     * Used by skills to load additional documentation and templates.
     */
    @Bean
    public FileSystemTools fileSystemTools() {
        log.info("Creating FileSystemTools bean for reading skill references");
        return FileSystemTools.builder()
                .build();
    }

    /**
     * Creates ToolCallTracingAdvisor bean for tracking tool calls in Execution Trace.
     */
    @Bean
    public ToolCallTracingAdvisor toolCallTracingAdvisor() {
        log.info("Creating ToolCallTracingAdvisor bean for tool call tracking");
        return new ToolCallTracingAdvisor();
    }

    /**
     * Creates a ChatClient with Agent Skills enabled.
     * This ChatClient includes SkillsTool for skill discovery and FileSystemTools
     * for reading reference materials. Java @Tool methods are registered separately.
     * <p>
     * Note: This is a standalone ChatClient for skills-only scenarios.
     * For integration with existing Tool Search Tool, see ToolSearchConfiguration.
     * <p>
     * This bean is marked as @Primary when Agent Skills are enabled and Tool Search is disabled,
     * ensuring that AnswerGenerationService uses the ChatClient with tools.
     */
    @Bean("chatClientWithSkills")
    @Primary
    @ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatClient chatClientWithSkills(
            ChatClient.Builder builder,
            @org.springframework.beans.factory.annotation.Qualifier("skillsTool") ToolCallback skillsTool,
            FileSystemTools fileSystemTools,
            ExpertMatchTools expertTools,
            ChatManagementTools chatTools,
            RetrievalTools retrievalTools,
            ToolCallTracingAdvisor toolCallTracingAdvisor
    ) {
        log.info("Creating chatClientWithSkills with Agent Skills enabled");
        return builder
                .defaultToolCallbacks(skillsTool)  // Agent Skills discovery (ToolCallback)
                .defaultTools(fileSystemTools)  // File reading tools
                .defaultTools(expertTools, chatTools, retrievalTools)  // Java @Tool methods
                .defaultAdvisors(toolCallTracingAdvisor, new SimpleLoggerAdvisor())
                .build();
    }
}
