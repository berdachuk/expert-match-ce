package com.berdachuk.expertmatch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Spring AI configuration for LLM and embedding models.
 * Excluded in test profile to allow TestAIConfig to provide mocks.
 */
@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class SpringAIConfig {
    private final Environment environment;
    private final ListableBeanFactory beanFactory;

    public SpringAIConfig(Environment environment, ListableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
        String[] activeProfiles = environment.getActiveProfiles();
        log.info("REAL LLM CONFIG DETECTED: SpringAIConfig is being instantiated! Active profiles: {} ",
                Arrays.toString(activeProfiles));
        if (Arrays.asList(activeProfiles).contains("test")) {
            throw new IllegalStateException("SpringAIConfig should NOT be active in test profile! This will create real LLM models!");
        }
    }

    /**
     * Chat client configuration.
     * Handles ambiguity if multiple ChatModels are present (e.g., Ollama + OpenAI/DIAL).
     * Selection priority:
     * 1. Use @Primary ChatModel if available (respects primaryChatModel bean)
     * 2. Exclude rerankingChatModel from selection (it's for reranking only)
     * 3. If multiple models remain, select based on profile
     * <p>
     * Note: This bean is created as @Primary only when Tool Search Tool is disabled.
     * When Tool Search Tool is enabled, chatClientWithToolSearch from ToolSearchConfiguration
     * becomes the primary ChatClient.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatClient chatClient(@Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        // Always use the @Primary ChatModel (primaryChatModel bean)
        // This ensures we use the custom-configured model, not auto-configured ones
        log.info("Configuring ChatClient with @Primary ChatModel: {}", primaryChatModel.getClass().getSimpleName());
        return ChatClient.builder(primaryChatModel).build();
    }

    /**
     * Primary EmbeddingModel configuration.
     * Resolves ambiguity for PgVectorStore when multiple embedding models are present.
     * Supports separate base URLs for embedding service via spring.ai.custom.embedding.* properties.
     */
    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(List<EmbeddingModel> models) {
        // Check if custom embedding configuration is provided (separate base URL and provider)
        String embeddingBaseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
        String embeddingProvider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");
        String embeddingApiKey = environment.getProperty("spring.ai.custom.embedding.api-key");
        String embeddingModel = environment.getProperty("spring.ai.custom.embedding.model");
        String embeddingDimensions = environment.getProperty("spring.ai.custom.embedding.dimensions");

        if (embeddingBaseUrl != null && !embeddingBaseUrl.isEmpty()) {
            // Create custom EmbeddingModel with separate base URL and provider
            log.info("Creating custom EmbeddingModel with provider: {}, base URL: {}", embeddingProvider, embeddingBaseUrl);

            if ("ollama".equalsIgnoreCase(embeddingProvider)) {
                // Create Ollama EmbeddingModel
                log.info("REAL LLM CREATION: Creating OllamaApi for EmbeddingModel! Base URL: {} ", embeddingBaseUrl);
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(embeddingBaseUrl)
                        .build();
                log.info("REAL LLM CREATION: OllamaApi created! ");

                OllamaEmbeddingOptions.Builder optionsBuilder = OllamaEmbeddingOptions.builder();
                if (embeddingModel != null && !embeddingModel.isEmpty()) {
                    optionsBuilder.model(embeddingModel);
                }

                // Use builder pattern for OllamaEmbeddingModel
                log.info("REAL LLM CREATION: Creating OllamaEmbeddingModel! ");
                OllamaEmbeddingModel model = OllamaEmbeddingModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(optionsBuilder.build())
                        .build();
                log.info("REAL LLM CREATION: OllamaEmbeddingModel created! Type: {} ", model.getClass().getName());
                return model;
            } else {
                // Create OpenAI-compatible EmbeddingModel (default)
                log.info("REAL LLM CREATION: Creating OpenAiApi for EmbeddingModel! Base URL: {} ", embeddingBaseUrl);
                OpenAiApi embeddingApi = OpenAiApi.builder()
                        .baseUrl(embeddingBaseUrl)
                        .apiKey(embeddingApiKey != null ? embeddingApiKey : "ollama")
                        .build();
                log.info("REAL LLM CREATION: OpenAiApi created! ");

                OpenAiEmbeddingOptions.Builder optionsBuilder = OpenAiEmbeddingOptions.builder();
                if (embeddingModel != null && !embeddingModel.isEmpty()) {
                    optionsBuilder.model(embeddingModel);
                }
                if (embeddingDimensions != null && !embeddingDimensions.isEmpty()) {
                    try {
                        optionsBuilder.dimensions(Integer.parseInt(embeddingDimensions));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid embedding dimensions: {}. Using default.", embeddingDimensions);
                    }
                }

                log.info("REAL LLM CREATION: Creating OpenAiEmbeddingModel! ");
                OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
                        embeddingApi,
                        MetadataMode.EMBED,
                        optionsBuilder.build());
                log.info("REAL LLM CREATION: OpenAiEmbeddingModel created! Type: {} ", model.getClass().getName());
                return model;
            }
        }

        // Fall back to auto-configured models
        if (models.isEmpty()) {
            // This will likely cause PgVectorStore to fail, but we provide a clear error here
            throw new IllegalStateException("No EmbeddingModel bean found. Please configure 'spring.ai.ollama' or 'spring.ai.openai' properties.");
        }

        if (models.size() == 1) {
            return models.get(0);
        }

        // Select based on active profile
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevProfile = Arrays.asList(activeProfiles).contains("dev") ||
                Arrays.asList(activeProfiles).contains("staging") ||
                Arrays.asList(activeProfiles).contains("prod");

        EmbeddingModel selected;
        if (isDevProfile) {
            // For dev/staging/prod: Prefer OpenAI/DIAL
            selected = models.stream()
                    .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                    .findFirst()
                    .orElse(models.get(0));
        } else {
            // For local: Prefer Ollama
            selected = models.stream()
                    .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("ollama"))
                    .findFirst()
                    .orElse(models.get(0));
        }

        log.info("Multiple EmbeddingModel beans found: {}. Selected primary: {}",
                models.stream().map(m -> m.getClass().getSimpleName()).toList(),
                selected.getClass().getSimpleName());

        return selected;
    }

    /**
     * Primary ChatModel configuration.
     * Supports separate base URLs for chat service via spring.ai.custom.chat.* properties.
     * If custom configuration is provided, creates a custom ChatModel with separate base URL.
     * Otherwise, wraps the auto-configured ChatModel to mark it as @Primary.
     */
    @Bean
    @Primary
    @Lazy
    public ChatModel primaryChatModel() {
        // Check if custom chat configuration is provided (separate base URL and provider)
        String chatBaseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
        String chatProvider = environment.getProperty("spring.ai.custom.chat.provider", "openai");
        String chatApiKey = environment.getProperty("spring.ai.custom.chat.api-key");
        String chatModel = environment.getProperty("spring.ai.custom.chat.model");
        String chatTemperature = environment.getProperty("spring.ai.custom.chat.temperature");
        // Read max-tokens from custom config, fall back to openai.chat.options.max-tokens, then default to 6000
        String chatMaxTokens = environment.getProperty("spring.ai.custom.chat.max-tokens",
                environment.getProperty("spring.ai.openai.chat.options.max-tokens", "6000"));

        if (chatBaseUrl != null && !chatBaseUrl.isEmpty()) {
            // Create custom ChatModel with separate base URL and provider
            log.info("Creating custom ChatModel with provider: {}, base URL: {}", chatProvider, chatBaseUrl);

            if ("ollama".equalsIgnoreCase(chatProvider)) {
                // Create Ollama ChatModel
                log.info("REAL LLM CREATION: Creating OllamaApi for ChatModel! Base URL: {} ", chatBaseUrl);
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(chatBaseUrl)
                        .build();
                log.info("REAL LLM CREATION: OllamaApi created! ");

                OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder();
                if (chatModel != null && !chatModel.isEmpty()) {
                    optionsBuilder.model(chatModel);
                }
                if (chatTemperature != null && !chatTemperature.isEmpty()) {
                    try {
                        optionsBuilder.temperature(Double.parseDouble(chatTemperature));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid chat temperature: {}. Using default.", chatTemperature);
                    }
                }
                if (chatMaxTokens != null && !chatMaxTokens.isEmpty()) {
                    try {
                        optionsBuilder.numPredict(Integer.parseInt(chatMaxTokens));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid chat max-tokens: {}. Using default.", chatMaxTokens);
                    }
                }

                return OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(optionsBuilder.build())
                        .build();
            } else {
                // Create OpenAI-compatible ChatModel (default)
                log.info("REAL LLM CREATION: Creating OpenAiApi for ChatModel! Base URL: {} ", chatBaseUrl);
                OpenAiApi chatApi = OpenAiApi.builder()
                        .baseUrl(chatBaseUrl)
                        .apiKey(chatApiKey != null ? chatApiKey : "ollama")
                        .build();
                log.info("REAL LLM CREATION: OpenAiApi created! ");

                OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();
                if (chatModel != null && !chatModel.isEmpty()) {
                    optionsBuilder.model(chatModel);
                }
                if (chatTemperature != null && !chatTemperature.isEmpty()) {
                    try {
                        optionsBuilder.temperature(Double.parseDouble(chatTemperature));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid chat temperature: {}. Using default.", chatTemperature);
                    }
                }
                if (chatMaxTokens != null && !chatMaxTokens.isEmpty()) {
                    try {
                        optionsBuilder.maxTokens(Integer.parseInt(chatMaxTokens));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid chat max-tokens: {}. Using default.", chatMaxTokens);
                    }
                }

                RetryTemplate retryTemplate = createRetryTemplate();
                // Use builder pattern for OpenAiChatModel
                log.info("REAL LLM CREATION: Creating OpenAiChatModel! ");
                OpenAiChatModel model = OpenAiChatModel.builder()
                        .openAiApi(chatApi)
                        .defaultOptions(optionsBuilder.build())
                        .retryTemplate(retryTemplate)
                        .build();
                log.info("REAL LLM CREATION: OpenAiChatModel created! Type: {} ", model.getClass().getName());
                return model;
            }
        }

        // Fall back to auto-configured models
        // Get ChatModel beans by name from BeanFactory, excluding our own beans
        String[] beanNames = beanFactory.getBeanNamesForType(ChatModel.class);
        List<ChatModel> models = Arrays.stream(beanNames)
                .filter(name -> !name.equals("primaryChatModel") && !name.equals("rerankingChatModel"))
                .map(name -> beanFactory.getBean(name, ChatModel.class))
                .toList();

        if (models.isEmpty()) {
            throw new IllegalStateException("No ChatModel bean found. Please configure 'spring.ai.ollama' or 'spring.ai.openai' properties.");
        }

        ChatModel primary = models.get(0);
        if (models.size() > 1) {
            // Select based on profile
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isDevProfile = Arrays.asList(activeProfiles).contains("dev") ||
                    Arrays.asList(activeProfiles).contains("staging") ||
                    Arrays.asList(activeProfiles).contains("prod");

            if (isDevProfile) {
                primary = models.stream()
                        .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                        .findFirst()
                        .orElse(models.get(0));
            } else {
                primary = models.stream()
                        .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("ollama"))
                        .findFirst()
                        .orElse(models.get(0));
            }
        }

        log.info("Primary ChatModel: {}", primary.getClass().getSimpleName());
        return primary;
    }

    /**
     * Reranking ChatModel configuration.
     * Creates a dedicated ChatModel for semantic reranking using the configured reranking model.
     * Uses Ollama reranking model (e.g., dengcao/Qwen3-Reranker-8B:Q4_K_M) when configured.
     * <p>
     * Note: For now, returns null if reranking model is not configured.
     * SemanticReranker will handle this gracefully by using placeholder implementation.
     * <p>
     * TODO: Implement proper reranking ChatModel creation when Spring AI provides better support
     * for multiple ChatModel beans with different configurations.
     */
    @Bean
    @Qualifier("rerankingChatModel")
    public ChatModel rerankingChatModel(Environment environment, ObjectProvider<ChatModel> chatModelProvider) {
        // Check if custom reranking configuration is provided (separate base URL and provider)
        String rerankingBaseUrl = environment.getProperty("spring.ai.custom.reranking.base-url");
        String rerankingProvider = environment.getProperty("spring.ai.custom.reranking.provider", "ollama");
        String rerankingModel = environment.getProperty("spring.ai.custom.reranking.model");
        String rerankingApiKey = environment.getProperty("spring.ai.custom.reranking.api-key");
        String rerankingTemperature = environment.getProperty("spring.ai.custom.reranking.temperature", "0.1");

        if (rerankingBaseUrl != null && !rerankingBaseUrl.isEmpty() && rerankingModel != null && !rerankingModel.isEmpty()) {
            // Create custom reranking ChatModel with separate base URL and provider
            log.info("Creating custom reranking ChatModel with provider: {}, base URL: {}, model: {}",
                    rerankingProvider, rerankingBaseUrl, rerankingModel);

            if ("ollama".equalsIgnoreCase(rerankingProvider)) {
                // Create Ollama ChatModel for reranking
                log.info("REAL LLM CREATION: Creating OllamaApi for reranking ChatModel! Base URL: {} ", rerankingBaseUrl);
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(rerankingBaseUrl)
                        .build();
                log.info("REAL LLM CREATION: OllamaApi created! ");

                OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                        .model(rerankingModel);
                try {
                    optionsBuilder.temperature(Double.parseDouble(rerankingTemperature));
                } catch (NumberFormatException e) {
                    log.warn("Invalid reranking temperature: {}. Using default 0.1.", rerankingTemperature);
                    optionsBuilder.temperature(0.1);
                }

                log.info("REAL LLM CREATION: Creating OllamaChatModel for reranking! ");
                OllamaChatModel rerankingModelInstance = OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(optionsBuilder.build())
                        .build();
                log.info("REAL LLM CREATION: OllamaChatModel created! Type: {} ", rerankingModelInstance.getClass().getName());
                return rerankingModelInstance;
            } else {
                // Create OpenAI-compatible ChatModel for reranking
                log.info("REAL LLM CREATION: Creating OpenAiApi for reranking ChatModel! Base URL: {} ", rerankingBaseUrl);
                OpenAiApi rerankingApi = OpenAiApi.builder()
                        .baseUrl(rerankingBaseUrl)
                        .apiKey(rerankingApiKey != null ? rerankingApiKey : "ollama")
                        .build();
                log.info("REAL LLM CREATION: OpenAiApi created! ");

                OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                        .model(rerankingModel);
                try {
                    optionsBuilder.temperature(Double.parseDouble(rerankingTemperature));
                } catch (NumberFormatException e) {
                    log.warn("Invalid reranking temperature: {}. Using default 0.1.", rerankingTemperature);
                    optionsBuilder.temperature(0.1);
                }

                RetryTemplate retryTemplate = createRetryTemplate();

                log.info("REAL LLM CREATION: Creating OpenAiChatModel for reranking! ");
                OpenAiChatModel rerankingChatModelInstance = OpenAiChatModel.builder()
                        .openAiApi(rerankingApi)
                        .defaultOptions(optionsBuilder.build())
                        .retryTemplate(retryTemplate)
                        .build();
                log.info("REAL LLM CREATION: OpenAiChatModel created! Type: {} ", rerankingChatModelInstance.getClass().getName());
                return rerankingChatModelInstance;
            }
        }

        // Fall back to legacy configuration (spring.ai.ollama.reranking.options.model)
        String legacyRerankingModel = environment.getProperty("spring.ai.ollama.reranking.options.model");
        if (legacyRerankingModel != null && !legacyRerankingModel.isEmpty()) {
            // Get ChatModels excluding our own beans to avoid circular dependency
            List<ChatModel> chatModels = chatModelProvider.stream()
                    .filter(m -> !m.getClass().getSimpleName().equals("SpringAIConfig$$SpringCGLIB$$0"))
                    .toList();

            // For now, use the primary ChatModel but SemanticReranker will override model at runtime
            // This is a temporary solution until we can properly configure a separate ChatModel bean
            if (!chatModels.isEmpty()) {
                ChatModel primaryModel = chatModels.get(0);
                log.info("Using primary ChatModel for reranking with model override: {}", legacyRerankingModel);
                return primaryModel;
            }
        }

        log.warn("Reranking model not configured. Reranking will use placeholder implementation.");
        return null; // Will be handled gracefully in SemanticReranker
    }

    /**
     * Creates a RetryTemplate with exponential backoff for handling LLM API timeouts and transient errors.
     * Configuration:
     * - Max attempts: 3
     * - Initial delay: 1 second
     * - Multiplier: 2.0 (exponential backoff)
     * - Max delay: 10 seconds
     */
    private RetryTemplate createRetryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0); // Double the delay each retry
        backOffPolicy.setMaxInterval(10000); // Max 10 seconds between retries

        return new RetryTemplateBuilder()
                .maxAttempts(3)
                .customBackoff(backOffPolicy)
                .build();
    }
}
