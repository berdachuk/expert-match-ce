package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;
import java.util.Map;

/**
 * Test configuration listener to log which AI providers are actually being used in tests.
 * This helps verify that mocks are being used instead of real LLM providers.
 */
@Slf4j
@TestConfiguration
@Profile("test")
public class TestAIConfigListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired(required = false)
    private List<ChatModel> chatModels;

    @Autowired(required = false)
    private List<EmbeddingModel> embeddingModels;

    @Override
    public void onApplicationEvent(@org.springframework.lang.NonNull ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();

        log.info("========================================");
        log.info("AI PROVIDER USAGE IN TESTS:");
        log.info("========================================");

        // Check for SpringAIConfig bean (should NOT exist in test profile)
        try {
            SpringAIConfig springAIConfig = context.getBean(SpringAIConfig.class);
            log.error("REAL LLM CONFIG DETECTED: SpringAIConfig bean found in test profile! This should NOT happen! ");
        } catch (Exception e) {
            log.info("✓ SpringAIConfig not found (correct - should be excluded in test profile)");
        }

        // Check for OpenAiApi beans (should NOT exist in test profile)
        try {
            Map<String, OpenAiApi> openAiApis = context.getBeansOfType(OpenAiApi.class);
            if (!openAiApis.isEmpty()) {
                log.error("REAL LLM API DETECTED: {} OpenAiApi bean(s) found in test profile! ", openAiApis.size());
                openAiApis.forEach((name, api) -> {
                    log.error("  ✗ OpenAiApi bean: {} - Type: {}", name, api.getClass().getName());
                });
            } else {
                log.info("✓ No OpenAiApi beans found (correct)");
            }
        } catch (Exception e) {
            log.info("✓ No OpenAiApi beans found (correct)");
        }

        if (chatModels != null && !chatModels.isEmpty()) {
            log.info("ChatModel beans found: {}", chatModels.size());
            for (ChatModel model : chatModels) {
                String className = model.getClass().getName();
                String simpleName = model.getClass().getSimpleName();
                if (className.contains("MockitoMock") || className.contains("$MockitoMock")) {
                    log.info("  ✓ ChatModel: {} - MOCK (correct)", simpleName);
                } else if (className.contains("OpenAi")) {
                    log.error("  ✗ ChatModel: {} - REAL OPENAI (should be mock!)", simpleName);
                } else {
                    log.warn("  ? ChatModel: {} - Unknown type: {}", simpleName, className);
                }
            }
        } else {
            log.warn("No ChatModel beans found");
        }

        if (embeddingModels != null && !embeddingModels.isEmpty()) {
            log.info("EmbeddingModel beans found: {}", embeddingModels.size());
            for (EmbeddingModel model : embeddingModels) {
                String className = model.getClass().getName();
                String simpleName = model.getClass().getSimpleName();
                if (className.contains("MockitoMock") || className.contains("$MockitoMock")) {
                    log.info("  ✓ EmbeddingModel: {} - MOCK (correct)", simpleName);
                } else if (className.contains("OpenAi")) {
                    log.error("  ✗ EmbeddingModel: {} - REAL OPENAI (should be mock!)", simpleName);
                } else {
                    log.warn("  ? EmbeddingModel: {} - Unknown type: {}", simpleName, className);
                }
            }
        } else {
            log.warn("No EmbeddingModel beans found");
        }

        log.info("========================================");
    }
}

