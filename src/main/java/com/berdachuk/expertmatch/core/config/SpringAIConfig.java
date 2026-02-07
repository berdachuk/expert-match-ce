package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

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
     * Chat client configuration (no tools).
     * Created only when chatClientWithTools is not present (e.g. when Tool Search or Agent Skills
     * provide their own ChatClient). When Tool Search is disabled and Agent Skills are disabled,
     * ToolConfiguration creates chatClientWithTools with ExpertMatch tools (getRetrievedExperts etc.);
     * that client must be used for answer generation so the LLM's tool calls are executed and the
     * final answer text is returned instead of raw tool_calls JSON.
     * <p>
     * Selection: When chatClientWithTools exists it is @Primary. This no-tools client is NOT
     *
     * @Primary so that when both exist (e.g. config load order), chatClientWithTools is chosen.
     */
    @Bean
    @ConditionalOnProperty(
            name = "expertmatch.tools.search.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = {"chatClientWithSkills", "chatClientWithTools"})
    public ChatClient chatClient(@Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        log.info("Configuring ChatClient (no tools) with @Primary ChatModel: {}", primaryChatModel.getClass().getSimpleName());
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
            // Create custom EmbeddingModel - OpenAI-compatible providers only
            if (!"openai".equalsIgnoreCase(embeddingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + embeddingProvider);
            }
            log.info("Creating custom EmbeddingModel with provider: {}, base URL: {}", embeddingProvider, embeddingBaseUrl);

            log.info("REAL LLM CREATION: Creating OpenAiApi for EmbeddingModel! Base URL: {} ", embeddingBaseUrl);
            OpenAiApi embeddingApi = OpenAiApi.builder()
                    .baseUrl(embeddingBaseUrl)
                    .apiKey(embeddingApiKey != null ? embeddingApiKey : "")
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

        // Fall back to auto-configured models
        if (models.isEmpty()) {
            // This will likely cause PgVectorStore to fail, but we provide a clear error here
            throw new IllegalStateException("No EmbeddingModel bean found. Please configure 'spring.ai.openai' (or OpenAI-compatible) properties.");
        }

        if (models.size() == 1) {
            return models.get(0);
        }

        // Select based on active profile
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevProfile = Arrays.asList(activeProfiles).contains("dev") ||
                Arrays.asList(activeProfiles).contains("staging") ||
                Arrays.asList(activeProfiles).contains("prod");

        // Prefer OpenAI-compatible model
        EmbeddingModel selected = models.stream()
                .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                .findFirst()
                .orElse(models.get(0));

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
            // Create custom ChatModel - OpenAI-compatible providers only
            if (!"openai".equalsIgnoreCase(chatProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported. Provider: " + chatProvider);
            }
            log.info("Creating custom ChatModel with provider: {}, base URL: {}", chatProvider, chatBaseUrl);

            log.info("REAL LLM CREATION: Creating OpenAiApi for ChatModel! Base URL: {} ", chatBaseUrl);
            OpenAiApi chatApi = OpenAiApi.builder()
                    .baseUrl(chatBaseUrl)
                    .apiKey(chatApiKey != null ? chatApiKey : "")
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

            log.info("REAL LLM CREATION: Creating OpenAiChatModel! ");
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .openAiApi(chatApi)
                    .defaultOptions(optionsBuilder.build())
                    .build();
            log.info("REAL LLM CREATION: OpenAiChatModel created! Type: {} ", model.getClass().getName());
            return model;
        }

        // Fall back to auto-configured models
        // Get ChatModel beans by name from BeanFactory, excluding our own beans
        String[] beanNames = beanFactory.getBeanNamesForType(ChatModel.class);
        List<ChatModel> models = Arrays.stream(beanNames)
                .filter(name -> !name.equals("primaryChatModel") && !name.equals("rerankingChatModel"))
                .map(name -> beanFactory.getBean(name, ChatModel.class))
                .toList();

        if (models.isEmpty()) {
            throw new IllegalStateException("No ChatModel bean found. Please configure 'spring.ai.openai' (or OpenAI-compatible) properties.");
        }

        ChatModel primary = models.get(0);
        if (models.size() > 1) {
            primary = models.stream()
                    .filter(m -> m.getClass().getSimpleName().toLowerCase().contains("openai"))
                    .findFirst()
                    .orElse(models.get(0));
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
    public ChatModel rerankingChatModel(Environment environment) {
        String rerankingBaseUrl = environment.getProperty("spring.ai.custom.reranking.base-url");
        String rerankingProvider = environment.getProperty("spring.ai.custom.reranking.provider", "openai");
        String rerankingModel = environment.getProperty("spring.ai.custom.reranking.model");
        String rerankingApiKey = environment.getProperty("spring.ai.custom.reranking.api-key");
        String rerankingTemperature = environment.getProperty("spring.ai.custom.reranking.temperature", "0.1");

        if (rerankingBaseUrl != null && !rerankingBaseUrl.isEmpty() && rerankingModel != null && !rerankingModel.isEmpty()) {
            // Create custom reranking ChatModel with separate base URL and provider
            log.info("Creating custom reranking ChatModel with provider: {}, base URL: {}, model: {}",
                    rerankingProvider, rerankingBaseUrl, rerankingModel);

            if (!"openai".equalsIgnoreCase(rerankingProvider)) {
                throw new IllegalArgumentException("Only OpenAI-compatible providers are supported for reranking. Provider: " + rerankingProvider);
            }
            OpenAiApi rerankingApi = OpenAiApi.builder()
                    .baseUrl(rerankingBaseUrl)
                    .apiKey(rerankingApiKey != null ? rerankingApiKey : "")
                    .build();
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .model(rerankingModel);
            try {
                optionsBuilder.temperature(Double.parseDouble(rerankingTemperature));
            } catch (NumberFormatException e) {
                log.warn("Invalid reranking temperature: {}. Using default 0.1.", rerankingTemperature);
                optionsBuilder.temperature(0.1);
            }
            return OpenAiChatModel.builder()
                    .openAiApi(rerankingApi)
                    .defaultOptions(optionsBuilder.build())
                    .build();
        }
        log.warn("Reranking model not configured. Reranking will use placeholder implementation.");
        return null;
    }
}
