package com.berdachuk.expertmatch.query.skills;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.llm.tools.ChatManagementTools;
import com.berdachuk.expertmatch.llm.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.llm.tools.RetrievalTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Expert Matching skill activation and tool invocation.
 * Tests that skills are discovered, activated, and correctly reference Java @Tool methods.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "expertmatch.skills.enabled=true",
        "expertmatch.tools.search.enabled=false"  // Disable TST for simpler testing
})
@DisplayName("Expert Matching Skill Integration Tests")
class ExpertMatchingSkillIT extends BaseIntegrationTest {

    private static final String EXPERT_MATCHING_SKILL_NAME = "expert-matching-hybrid-retrieval";

    @Autowired(required = false)
    @Qualifier("chatClientWithSkills")
    private ChatClient chatClientWithSkills;

    @Autowired(required = false)
    @Qualifier("skillsTool")
    private ToolCallback skillsTool;

    @Autowired(required = false)
    private ExpertMatchTools expertMatchTools;

    @Autowired(required = false)
    private ChatManagementTools chatManagementTools;

    @Autowired(required = false)
    private RetrievalTools retrievalTools;

    @Autowired(required = false)
    private FileSystemTools fileSystemTools;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("Expert matching skill should be activated when skills are enabled")
    void testExpertMatchingSkillActivation() {
        assertThat(chatClientWithSkills)
                .as("ChatClient with skills should be available")
                .isNotNull();
        assertThat(skillsTool)
                .as("SkillsTool should be available")
                .isNotNull();
    }

    @Test
    @DisplayName("ChatClient should be configured with skills and tools")
    void testChatClientWithSkillsConfiguration() {
        assertThat(chatClientWithSkills)
                .as("ChatClient with skills should be configured")
                .isNotNull();
        assertThat(skillsTool)
                .as("SkillsTool should be available for skill discovery")
                .isNotNull();
    }

    @Test
    @DisplayName("All required components should be ready for skill activation")
    void testSkillsInfrastructureReady() {
        assertThat(chatClientWithSkills)
                .as("ChatClient should be configured")
                .isNotNull();
        assertThat(skillsTool)
                .as("SkillsTool should be configured")
                .isNotNull();
        assertThat(fileSystemTools)
                .as("FileSystemTools should be configured")
                .isNotNull();
    }

    @Test
    @DisplayName("Expert matching skill file should exist")
    void testExpertMatchingSkillExists() {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + EXPERT_MATCHING_SKILL_NAME + "/SKILL.md");
        assertThat(skillResource.exists())
                .as("Expert matching skill should exist")
                .isTrue();
    }

    @Test
    @DisplayName("Expert matching skill should reference expertQuery tool")
    void testExpertMatchingSkillReferencesExpertQueryTool() {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + EXPERT_MATCHING_SKILL_NAME + "/SKILL.md");

        assertThat(skillResource.exists())
                .as("Skill file should exist")
                .isTrue();

        try {
            String content = readResourceContent(skillResource);

            // Check that skill references expertQuery tool
            assertThat(content)
                    .as("Skill should reference 'expertQuery' tool")
                    .containsIgnoringCase("expertQuery");

            // Check that ExpertMatchTools is available
            assertThat(expertMatchTools)
                    .as("ExpertMatchTools should be available for skill to reference")
                    .isNotNull();

        } catch (Exception e) {
            throw new AssertionError("Failed to read skill content", e);
        }
    }

    @Test
    @DisplayName("Expert matching skill should have valid structure")
    void testExpertMatchingSkillStructure() {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + EXPERT_MATCHING_SKILL_NAME + "/SKILL.md");

        try {
            String content = readResourceContent(skillResource);

            // Check for required sections
            assertThat(content)
                    .as("Skill should have 'When to Use' section")
                    .containsIgnoringCase("When to Use");

            assertThat(content)
                    .as("Skill should have 'Available Tools' section")
                    .containsIgnoringCase("Available Tools");

            // Check for tool usage instructions
            assertThat(content)
                    .as("Skill should provide tool usage instructions")
                    .matches(Pattern.compile("(?i).*(how to use|usage|example).*", Pattern.DOTALL));

        } catch (Exception e) {
            throw new AssertionError("Failed to validate skill structure", e);
        }
    }

    @Test
    @DisplayName("ExpertMatchTools should be available for skill to reference")
    void testExpertMatchToolsAvailable() {
        assertThat(expertMatchTools)
                .as("ExpertMatchTools should be available")
                .isNotNull();
    }

    @Test
    @DisplayName("All tool beans should be available for skill integration")
    void testAllToolBeansAvailable() {
        assertThat(expertMatchTools)
                .as("ExpertMatchTools should be available")
                .isNotNull();
        assertThat(chatManagementTools)
                .as("ChatManagementTools should be available")
                .isNotNull();
        assertThat(retrievalTools)
                .as("RetrievalTools should be available")
                .isNotNull();
    }

    @Test
    @DisplayName("Skills and tools should be integrated in ChatClient")
    void testSkillsAndToolsIntegration() {
        assertThat(chatClientWithSkills)
                .as("ChatClient with skills should be configured")
                .isNotNull();
        assertThat(skillsTool)
                .as("SkillsTool should be configured")
                .isNotNull();
        assertThat(expertMatchTools)
                .as("ExpertMatchTools should be available")
                .isNotNull();
        assertThat(fileSystemTools)
                .as("FileSystemTools should be available")
                .isNotNull();
    }

    @Test
    @DisplayName("Expert matching skill should have correct frontmatter")
    void testExpertMatchingSkillFrontmatter() {
        Resource skillResource = resourceLoader.getResource(
                "classpath:.claude/skills/" + EXPERT_MATCHING_SKILL_NAME + "/SKILL.md");

        try {
            String content = readResourceContent(skillResource);

            // Check frontmatter
            assertThat(content)
                    .as("Skill should start with frontmatter")
                    .startsWith("---");

            // Check name matches
            assertThat(content)
                    .as("Skill frontmatter should contain correct name")
                    .containsPattern(Pattern.compile("name:\\s*" + EXPERT_MATCHING_SKILL_NAME.replace("-", "[-_]")));

            // Check description exists
            assertThat(content)
                    .as("Skill frontmatter should contain description")
                    .contains("description:");

        } catch (Exception e) {
            throw new AssertionError("Failed to validate skill frontmatter", e);
        }
    }

    @Test
    @DisplayName("SkillsTool should implement ToolCallback interface")
    void testSkillsToolImplementsToolCallback() {
        assertThat(skillsTool)
                .as("SkillsTool should implement ToolCallback")
                .isInstanceOf(ToolCallback.class);
    }

    /**
     * Reads the content of a resource as a string.
     */
    private String readResourceContent(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}
