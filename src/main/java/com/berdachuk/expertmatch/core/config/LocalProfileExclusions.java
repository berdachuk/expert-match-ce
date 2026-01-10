package com.berdachuk.expertmatch.core.config;

import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration to exclude ALL Spring AI auto-configurations for ALL non-test profiles.
 * This ensures that only custom configuration (SpringAIConfig) is used.
 * This provides an additional layer of exclusion beyond YAML configuration.
 * <p>
 * Applies to: local, dev, staging, prod, debug profiles (all except test)
 * Test profile uses TestProfileExclusions instead.
 */
@Configuration
@Profile("!test")  // Apply to all profiles except test
@EnableAutoConfiguration(exclude = {
        // Exclude ALL OpenAI auto-configurations
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        // Exclude ALL Ollama auto-configurations
        OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class,
        OllamaApiAutoConfiguration.class
})
public class LocalProfileExclusions {
    // This class exists solely to exclude ALL Spring AI auto-configurations for ALL non-test profiles
    // All models are created via SpringAIConfig.java custom configuration
    // Test profile uses TestProfileExclusions instead
}

