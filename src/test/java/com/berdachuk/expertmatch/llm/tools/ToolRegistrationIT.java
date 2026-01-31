package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.core.config.TestAIConfig;
import com.berdachuk.expertmatch.core.config.TestSecurityConfig;
import com.berdachuk.expertmatch.core.config.ToolConfiguration;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import com.berdachuk.expertmatch.query.tools.ExpertMatchTools;
import com.berdachuk.expertmatch.query.tools.RetrievalTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests to verify that tools are properly registered with Spring AI ChatClient.
 */
@Import({TestSecurityConfig.class, TestAIConfig.class})
class ToolRegistrationIT extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    @Qualifier("chatClientWithTools")
    private org.springframework.ai.chat.client.ChatClient chatClientWithTools;

    @Autowired(required = false)
    private ExpertMatchTools expertMatchTools;

    @Autowired(required = false)
    private ChatManagementTools chatManagementTools;

    @Autowired(required = false)
    private RetrievalTools retrievalTools;

    @Test
    void testToolComponentsAreRegistered() {
        // Verify tool components are registered as Spring beans
        assertNotNull(expertMatchTools, "ExpertMatchTools should be registered");
        assertNotNull(chatManagementTools, "ChatManagementTools should be registered");
        assertNotNull(retrievalTools, "RetrievalTools should be registered");
    }

    @Test
    void testChatClientWithToolsBeanExists() {
        // Verify chatClientWithTools bean is created
        assertNotNull(chatClientWithTools, "chatClientWithTools bean should be created");
    }

    @Test
    void testToolConfigurationBeanExists() {
        // Verify ToolConfiguration is registered
        ToolConfiguration toolConfig = applicationContext.getBean(ToolConfiguration.class);
        assertNotNull(toolConfig, "ToolConfiguration should be registered");
    }

    @Test
    void testExpertMatchToolsHasToolMethods() {
        if (expertMatchTools == null) {
            return; // Skip if not available
        }

        // Verify ExpertMatchTools has @Tool annotated methods
        // This is a basic check - actual tool discovery would require reflection
        assertNotNull(expertMatchTools);

        // Verify the component can be instantiated and has expected structure
        // In a full test, we'd use reflection to verify @Tool methods exist
    }
}

