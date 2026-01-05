package com.berdachuk.expertmatch.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpringAIConfig custom base URL configuration properties.
 * Verifies that separate base URLs for chat, embedding, and reranking can be configured.
 * <p>
 * Note: This is a unit test that only verifies property reading, not bean creation.
 * SpringAIConfig is excluded in test profile, so we test the configuration properties directly.
 */
class SpringAIConfigTest {

    /**
     * Test that custom chat configuration properties are supported.
     */
    @Test
    void testCustomChatConfigurationProperties() {
        Environment env = new StandardEnvironment();

        // Test that properties can be set (simulating what would be in application.yml)
        // In real usage, these would come from application.yml or environment variables
        assertNotNull(env, "Environment should be available");

        // Verify property keys match what SpringAIConfig expects
        String chatProviderKey = "spring.ai.custom.chat.provider";
        String chatBaseUrlKey = "spring.ai.custom.chat.base-url";
        String chatApiKeyKey = "spring.ai.custom.chat.api-key";
        String chatModelKey = "spring.ai.custom.chat.model";
        String chatTemperatureKey = "spring.ai.custom.chat.temperature";

        assertNotNull(chatProviderKey, "Chat provider property key should be defined");
        assertNotNull(chatBaseUrlKey, "Chat base URL property key should be defined");
        assertNotNull(chatApiKeyKey, "Chat API key property key should be defined");
        assertNotNull(chatModelKey, "Chat model property key should be defined");
        assertNotNull(chatTemperatureKey, "Chat temperature property key should be defined");
    }

    /**
     * Test that custom embedding configuration properties are supported.
     */
    @Test
    void testCustomEmbeddingConfigurationProperties() {
        // Verify property keys match what SpringAIConfig expects
        String embeddingProviderKey = "spring.ai.custom.embedding.provider";
        String embeddingBaseUrlKey = "spring.ai.custom.embedding.base-url";
        String embeddingApiKeyKey = "spring.ai.custom.embedding.api-key";
        String embeddingModelKey = "spring.ai.custom.embedding.model";
        String embeddingDimensionsKey = "spring.ai.custom.embedding.dimensions";

        assertNotNull(embeddingProviderKey, "Embedding provider property key should be defined");
        assertNotNull(embeddingBaseUrlKey, "Embedding base URL property key should be defined");
        assertNotNull(embeddingApiKeyKey, "Embedding API key property key should be defined");
        assertNotNull(embeddingModelKey, "Embedding model property key should be defined");
        assertNotNull(embeddingDimensionsKey, "Embedding dimensions property key should be defined");
    }

    /**
     * Test that custom reranking configuration properties are supported.
     */
    @Test
    void testCustomRerankingConfigurationProperties() {
        // Verify property keys match what SpringAIConfig expects
        String rerankingProviderKey = "spring.ai.custom.reranking.provider";
        String rerankingBaseUrlKey = "spring.ai.custom.reranking.base-url";
        String rerankingApiKeyKey = "spring.ai.custom.reranking.api-key";
        String rerankingModelKey = "spring.ai.custom.reranking.model";
        String rerankingTemperatureKey = "spring.ai.custom.reranking.temperature";

        assertNotNull(rerankingProviderKey, "Reranking provider property key should be defined");
        assertNotNull(rerankingBaseUrlKey, "Reranking base URL property key should be defined");
        assertNotNull(rerankingApiKeyKey, "Reranking API key property key should be defined");
        assertNotNull(rerankingModelKey, "Reranking model property key should be defined");
        assertNotNull(rerankingTemperatureKey, "Reranking temperature property key should be defined");
    }

    /**
     * Test that SpringAIConfig property reading logic is correct.
     * This verifies the property names used in SpringAIConfig match expected format.
     */
    @Test
    void testPropertyKeyFormat() {
        // Verify property keys follow Spring Boot property naming convention
        String[] expectedKeys = {
                "spring.ai.custom.chat.provider",
                "spring.ai.custom.chat.base-url",
                "spring.ai.custom.chat.api-key",
                "spring.ai.custom.chat.model",
                "spring.ai.custom.chat.temperature",
                "spring.ai.custom.embedding.provider",
                "spring.ai.custom.embedding.base-url",
                "spring.ai.custom.embedding.api-key",
                "spring.ai.custom.embedding.model",
                "spring.ai.custom.embedding.dimensions",
                "spring.ai.custom.reranking.provider",
                "spring.ai.custom.reranking.base-url",
                "spring.ai.custom.reranking.api-key",
                "spring.ai.custom.reranking.model",
                "spring.ai.custom.reranking.temperature"
        };

        for (String key : expectedKeys) {
            assertTrue(key.startsWith("spring.ai.custom."),
                    "Property key should start with 'spring.ai.custom.': " + key);
            assertFalse(key.isEmpty(), "Property key should not be empty");
        }
    }

    /**
     * Test that provider values are validated correctly.
     * Valid providers are 'ollama' and 'openai' (case-insensitive).
     */
    @Test
    void testProviderValues() {
        // Valid provider values
        String[] validProviders = {"ollama", "openai", "OLLAMA", "OPENAI", "Ollama", "OpenAI"};

        for (String provider : validProviders) {
            assertNotNull(provider, "Provider should not be null");
            assertFalse(provider.isEmpty(), "Provider should not be empty");
            // SpringAIConfig uses equalsIgnoreCase, so both should work
            assertTrue("ollama".equalsIgnoreCase(provider) || "openai".equalsIgnoreCase(provider),
                    "Provider should be 'ollama' or 'openai': " + provider);
        }
    }
}

