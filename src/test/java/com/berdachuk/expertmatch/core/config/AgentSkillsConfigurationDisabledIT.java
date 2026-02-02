package com.berdachuk.expertmatch.core.config;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Agent Skills configuration when disabled.
 * Verifies that skills-related beans are not created when expertmatch.skills.enabled=false.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "expertmatch.skills.enabled=false"
})
@DisplayName("Agent Skills Configuration Disabled Integration Tests")
class AgentSkillsConfigurationDisabledIT extends BaseIntegrationTest {

    @Autowired(required = false)
    @Qualifier("skillsTool")
    private ToolCallback skillsTool;

    @Autowired(required = false)
    @Qualifier("chatClientWithSkills")
    private ChatClient chatClientWithSkills;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("SkillsTool bean should not exist when skills are disabled")
    void testSkillsToolBeanDoesNotExist() {
        assertThat(skillsTool)
                .as("SkillsTool bean should not exist when expertmatch.skills.enabled=false")
                .isNull();

        assertThatThrownBy(() -> applicationContext.getBean("skillsTool", ToolCallback.class))
                .as("Getting skillsTool bean should throw NoSuchBeanDefinitionException")
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("ChatClient with skills bean should not exist when skills are disabled")
    void testChatClientWithSkillsBeanDoesNotExist() {
        assertThat(chatClientWithSkills)
                .as("chatClientWithSkills bean should not exist when skills are disabled")
                .isNull();

        assertThatThrownBy(() -> applicationContext.getBean("chatClientWithSkills", ChatClient.class))
                .as("Getting chatClientWithSkills bean should throw NoSuchBeanDefinitionException")
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("AgentSkillsConfiguration should not be registered when skills are disabled")
    void testAgentSkillsConfigurationBeanDoesNotExist() {
        assertThatThrownBy(() -> applicationContext.getBean(AgentSkillsConfiguration.class))
                .as("AgentSkillsConfiguration should not be registered when skills are disabled")
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("Regular ChatClient should still be available when skills are disabled")
    void testRegularChatClientAvailable() {
        // Regular ChatClient should still be available (from SpringAIConfig)
        ChatClient regularChatClient = applicationContext.getBean(ChatClient.class);
        assertThat(regularChatClient)
                .as("Regular ChatClient should be available even when skills are disabled")
                .isNotNull();
    }
}
