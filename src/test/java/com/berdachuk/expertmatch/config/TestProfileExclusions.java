package com.berdachuk.expertmatch.config;

import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration to exclude Spring AI auto-configurations for test profile.
 * This ensures that no real LLM models are created during tests.
 * TestAIConfig provides @Primary mocks for all LLM services.
 */
@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {
        // Explicitly exclude Spring AI auto-configuration classes to prevent real LLM models from being created
        // This is in addition to the enabled=false properties in BaseIntegrationTest
        OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class,
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class
})
public class TestProfileExclusions {
    // This class exists solely to exclude Spring AI auto-configurations for test profile
}

