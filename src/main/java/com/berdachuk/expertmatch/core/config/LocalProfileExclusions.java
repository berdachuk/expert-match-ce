package com.berdachuk.expertmatch.core.config;

import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration to exclude ALL Spring AI auto-configurations for ALL non-test profiles.
 * This ensures that only custom configuration (SpringAIConfig) is used.
 * Applies to: local, dev, staging, prod, debug profiles (all except test)
 */
@Configuration
@Profile("!test")
@EnableAutoConfiguration(exclude = {
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class
})
public class LocalProfileExclusions {
    // This class exists solely to exclude ALL Spring AI auto-configurations for ALL non-test profiles
    // All models are created via SpringAIConfig.java custom configuration
    // Test profile uses TestProfileExclusions instead
}

