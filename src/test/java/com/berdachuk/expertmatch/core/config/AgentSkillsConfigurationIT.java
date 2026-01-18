package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.llm.tools.ChatManagementTools;
import com.berdachuk.expertmatch.llm.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.llm.tools.RetrievalTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Agent Skills configuration.
 * Tests skills discovery, bean creation, ChatClient configuration, and skill content validation.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "expertmatch.skills.enabled=true"
})
@DisplayName("Agent Skills Configuration Integration Tests")
class AgentSkillsConfigurationIT extends BaseIntegrationTest {

    private static final List<String> EXPECTED_SKILLS = List.of(
            "expert-matching-hybrid-retrieval",
            "rag-answer-generation",
            "person-name-matching",
            "query-classification",
            "rfp-response-generation",
            "team-formation"
    );

    @Autowired(required = false)
    @Qualifier("skillsTool")
    private ToolCallback skillsTool;

    @Autowired(required = false)
    private FileSystemTools fileSystemTools;

    @Autowired(required = false)
    @Qualifier("chatClientWithSkills")
    private ChatClient chatClientWithSkills;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private ExpertMatchTools expertMatchTools;

    @Autowired(required = false)
    private ChatManagementTools chatManagementTools;

    @Autowired(required = false)
    private RetrievalTools retrievalTools;

    @Test
    @DisplayName("SkillsTool bean should be created when skills are enabled")
    void testSkillsToolBeanExists() {
        assertThat(skillsTool)
                .as("SkillsTool bean should exist when expertmatch.skills.enabled=true")
                .isNotNull();
    }

    @Test
    @DisplayName("FileSystemTools bean should be created when skills are enabled")
    void testFileSystemToolsBeanExists() {
        assertThat(fileSystemTools)
                .as("FileSystemTools bean should exist when skills are enabled")
                .isNotNull();
    }

    @Test
    @DisplayName("ChatClient with skills bean should be created when skills are enabled")
    void testChatClientWithSkillsBeanExists() {
        assertThat(chatClientWithSkills)
                .as("chatClientWithSkills bean should exist when skills are enabled")
                .isNotNull();
    }

    @Test
    @DisplayName("Skills directory should exist in classpath")
    void testSkillsResourceExists() {
        Resource skillsResource = resourceLoader.getResource("classpath:.claude/skills");
        assertThat(skillsResource.exists())
                .as("Skills directory should exist in classpath")
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "expert-matching-hybrid-retrieval",
            "rag-answer-generation",
            "person-name-matching",
            "query-classification",
            "rfp-response-generation",
            "team-formation"
    })
    @DisplayName("Core skill should exist in classpath")
    void testCoreSkillExists(String skillName) {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + skillName + "/SKILL.md");
        assertThat(skillResource.exists())
                .as("Skill '%s' should exist in classpath", skillName)
                .isTrue();
    }

    @Test
    @DisplayName("All expected core skills should exist")
    void testAllCoreSkillsExist() {
        for (String skillName : EXPECTED_SKILLS) {
            Resource skillResource = resourceLoader.getResource(
                    "classpath:.claude/skills/" + skillName + "/SKILL.md");
            assertThat(skillResource.exists())
                    .as("Skill '%s' should exist in classpath", skillName)
                    .isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "expert-matching-hybrid-retrieval",
            "rag-answer-generation",
            "person-name-matching",
            "query-classification",
            "rfp-response-generation",
            "team-formation"
    })
    @DisplayName("Skill should have valid frontmatter with name and description")
    void testSkillHasValidFrontmatter(String skillName) {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + skillName + "/SKILL.md");

        assertThat(skillResource.exists())
                .as("Skill '%s' should exist", skillName)
                .isTrue();

        try {
            String content = readResourceContent(skillResource);

            // Check for frontmatter markers
            assertThat(content)
                    .as("Skill '%s' should start with frontmatter delimiter", skillName)
                    .startsWith("---");

            // Check for name field in frontmatter
            assertThat(content)
                    .as("Skill '%s' should have 'name' field in frontmatter", skillName)
                    .contains("name:");

            // Check for description field in frontmatter
            assertThat(content)
                    .as("Skill '%s' should have 'description' field in frontmatter", skillName)
                    .contains("description:");

            // Verify name matches directory name
            String namePattern = "name:\\s*" + skillName.replace("-", "[-_]");
            assertThat(content)
                    .as("Skill '%s' frontmatter name should match directory name", skillName)
                    .containsPattern(namePattern);

        } catch (Exception e) {
            throw new AssertionError("Failed to read skill content: " + skillName, e);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "expert-matching-hybrid-retrieval",
            "rag-answer-generation",
            "person-name-matching",
            "query-classification",
            "rfp-response-generation",
            "team-formation"
    })
    @DisplayName("Skill should have valid content structure")
    void testSkillHasValidContentStructure(String skillName) {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + skillName + "/SKILL.md");

        try {
            String content = readResourceContent(skillResource);

            // Check for main heading
            assertThat(content)
                    .as("Skill '%s' should have a main heading (#)", skillName)
                    .contains("#");

            // Check for overview or purpose section
            boolean hasOverview = content.contains("## Overview") ||
                    content.contains("## Purpose") ||
                    content.contains("## When to Use");
            assertThat(hasOverview)
                    .as("Skill '%s' should have an Overview, Purpose, or When to Use section", skillName)
                    .isTrue();

            // Check for content after frontmatter
            int frontmatterEnd = content.indexOf("---", 3);
            if (frontmatterEnd > 0) {
                String contentAfterFrontmatter = content.substring(frontmatterEnd + 3).trim();
                assertThat(contentAfterFrontmatter)
                        .as("Skill '%s' should have content after frontmatter", skillName)
                        .isNotEmpty();
            }

        } catch (Exception e) {
            throw new AssertionError("Failed to validate skill structure: " + skillName, e);
        }
    }

    @Test
    @DisplayName("SkillsTool should be configured to load from classpath")
    void testSkillsToolConfiguration() {
        assertThat(skillsTool)
                .as("SkillsTool should be configured")
                .isNotNull();

        // SkillsTool should be a ToolCallback (Spring AI 1.1.0 compatibility)
        assertThat(skillsTool)
                .as("SkillsTool should implement ToolCallback")
                .isInstanceOf(ToolCallback.class);
    }

    @Test
    @DisplayName("All skill directories should exist")
    void testSkillsDirectoryStructure() {
        for (String skillName : EXPECTED_SKILLS) {
            Resource skillDir = resourceLoader.getResource(
                    "classpath:.claude/skills/" + skillName);
            assertThat(skillDir.exists())
                    .as("Skill directory '%s' should exist", skillName)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("ChatClient should be configured with all required tools")
    void testChatClientWithSkillsConfiguration() {
        assertThat(chatClientWithSkills)
                .as("chatClientWithSkills should be configured")
                .isNotNull();

        // Verify that Java @Tool beans are available
        assertThat(expertMatchTools)
                .as("ExpertMatchTools should be available for ChatClient")
                .isNotNull();

        assertThat(chatManagementTools)
                .as("ChatManagementTools should be available for ChatClient")
                .isNotNull();

        assertThat(retrievalTools)
                .as("RetrievalTools should be available for ChatClient")
                .isNotNull();
    }

    @Test
    @DisplayName("AgentSkillsConfiguration bean should exist")
    void testAgentSkillsConfigurationBeanExists() {
        AgentSkillsConfiguration config = applicationContext.getBean(AgentSkillsConfiguration.class);
        assertThat(config)
                .as("AgentSkillsConfiguration should be registered")
                .isNotNull();
    }

    @Test
    @DisplayName("SkillsTool should be registered with correct qualifier")
    void testSkillsToolQualifier() {
        ToolCallback bean = applicationContext.getBean("skillsTool", ToolCallback.class);
        assertThat(bean)
                .as("SkillsTool should be accessible via 'skillsTool' qualifier")
                .isNotNull();
        assertThat(bean)
                .as("SkillsTool bean should be the same instance")
                .isSameAs(skillsTool);
    }

    @Test
    @DisplayName("FileSystemTools should be configured correctly")
    void testFileSystemToolsConfiguration() {
        assertThat(fileSystemTools)
                .as("FileSystemTools should be configured")
                .isNotNull();
    }

    @Test
    @DisplayName("No duplicate skills should exist")
    void testNoDuplicateSkills() {
        long uniqueSkillCount = EXPECTED_SKILLS.stream()
                .distinct()
                .count();
        assertThat(uniqueSkillCount)
                .as("All skills should be unique")
                .isEqualTo(EXPECTED_SKILLS.size());
    }

    @Test
    @DisplayName("Skills should not have empty content")
    void testSkillsHaveNonEmptyContent() {
        for (String skillName : EXPECTED_SKILLS) {
            Resource skillResource = resourceLoader.getResource(
                    "classpath:.claude/skills/" + skillName + "/SKILL.md");

            try {
                String content = readResourceContent(skillResource);

                // Remove frontmatter and whitespace
                int frontmatterEnd = content.indexOf("---", 3);
                String actualContent = frontmatterEnd > 0
                        ? content.substring(frontmatterEnd + 3).trim()
                        : content.trim();

                assertThat(actualContent)
                        .as("Skill '%s' should have non-empty content", skillName)
                        .isNotEmpty();

            } catch (Exception e) {
                throw new AssertionError("Failed to check skill content: " + skillName, e);
            }
        }
    }

    /**
     * Reads the content of a resource as a string.
     */
    private String readResourceContent(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
