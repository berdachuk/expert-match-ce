package com.berdachuk.expertmatch.system.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive health indicator that verifies all required infrastructure:
 * - Database connectivity
 * - LLM models (ChatModel)
 * - Embedding models
 * - Vector store (PgVector)
 * <p>
 * Note: Expensive LLM checks are cached to avoid frequent API calls.
 */
@Slf4j
@Component
public class ComprehensiveHealthIndicator implements HealthIndicator {

    // Cache expensive LLM checks for 5 minutes to avoid costs
    private static final Duration LLM_CHECK_CACHE_DURATION = Duration.ofMinutes(5);

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final Environment environment;

    // Cache for expensive checks
    private final AtomicReference<CachedHealthResult> llmHealthCache = new AtomicReference<>();
    private final AtomicReference<CachedHealthResult> embeddingHealthCache = new AtomicReference<>();

    @Autowired(required = false)
    public ComprehensiveHealthIndicator(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ChatModel chatModel,
            EmbeddingModel embeddingModel,
            Environment environment) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.environment = environment;
    }

    @Override
    public Health health() {
        Instant startTime = Instant.now();
        Map<String, Object> details = new HashMap<>();
        boolean isUp = true;

        log.debug("Starting comprehensive health check...");

        // 1. Database Health Check (always checked - fast and free)
        Health dbHealth = checkDatabase();
        details.put("database", dbHealth.getDetails());
        if (!dbHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Database health check failed: {}", dbHealth.getStatus());
        } else {
            log.debug("Database health check passed");
        }

        // 2. Vector Store Health Check (always checked - fast and free)
        Health vectorHealth = checkVectorStore();
        details.put("vectorStore", vectorHealth.getDetails());
        if (!vectorHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Vector store health check failed: {}", vectorHealth.getStatus());
        } else {
            log.debug("Vector store health check passed");
        }

        // 3. LLM Model Health Check (cached to avoid costs)
        Health llmHealth = checkLlmModel();
        details.put("llm", llmHealth.getDetails());
        if (!llmHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("LLM health check failed: {}", llmHealth.getStatus());
        } else {
            log.debug("LLM health check passed");
        }

        // 4. Embedding Model Health Check (cached to avoid costs)
        Health embeddingHealth = checkEmbeddingModel();
        details.put("embedding", embeddingHealth.getDetails());
        if (!embeddingHealth.getStatus().getCode().equals("UP")) {
            isUp = false;
            log.warn("Embedding health check failed: {}", embeddingHealth.getStatus());
        } else {
            log.debug("Embedding health check passed");
        }

        // 5. Reranking Model Configuration (from environment, not a health check)
        Map<String, Object> rerankingConfig = extractRerankingModelConfig();
        if (!rerankingConfig.isEmpty()) {
            details.put("reranking", rerankingConfig);
        }

        Duration duration = Duration.between(startTime, Instant.now());
        details.put("checkDuration", duration.toMillis() + "ms");
        details.put("timestamp", Instant.now().toString());

        Health.Builder healthBuilder = isUp ? Health.up() : Health.down();
        healthBuilder.withDetails(details);

        Health result = healthBuilder.build();

        if (isUp) {
            log.info("Comprehensive health check passed in {}ms - Database: {}, VectorStore: {}, LLM: {}, Embedding: {}",
                    duration.toMillis(),
                    dbHealth.getStatus().getCode(),
                    vectorHealth.getStatus().getCode(),
                    llmHealth.getStatus().getCode(),
                    embeddingHealth.getStatus().getCode());
        } else {
            log.error("Comprehensive health check failed in {}ms - Database: {}, VectorStore: {}, LLM: {}, Embedding: {}",
                    duration.toMillis(),
                    dbHealth.getStatus().getCode(),
                    vectorHealth.getStatus().getCode(),
                    llmHealth.getStatus().getCode(),
                    embeddingHealth.getStatus().getCode());
        }

        return result;
    }

    /**
     * Checks database connectivity and basic functionality.
     */
    private Health checkDatabase() {
        try {
            // Simple query to verify database connectivity
            Integer result = namedJdbcTemplate.queryForObject("SELECT 1", Collections.emptyMap(), Integer.class);
            if (result != null && result == 1) {
                // Check if expertmatch schema exists
                Integer schemaCount = namedJdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'expertmatch'",
                        Collections.emptyMap(),
                        Integer.class);

                Map<String, Object> details = new HashMap<>();
                details.put("status", "UP");
                details.put("schemaExists", schemaCount != null && schemaCount > 0);

                return Health.up().withDetails(details).build();
            } else {
                return Health.down()
                        .withDetail("error", "Database query returned unexpected result")
                        .build();
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * Checks vector store (PgVector) functionality.
     */
    private Health checkVectorStore() {
        try {
            // Check if vector extension is available
            Integer extensionCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                    Collections.emptyMap(),
                    Integer.class);

            // Check if work_experience table with embedding column exists
            Integer tableCount = namedJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                            "WHERE table_schema = 'expertmatch' " +
                            "AND table_name = 'work_experience' " +
                            "AND column_name = 'embedding'",
                    Collections.emptyMap(),
                    Integer.class);

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("vectorExtensionAvailable", extensionCount != null && extensionCount > 0);
            details.put("embeddingColumnExists", tableCount != null && tableCount > 0);

            if (extensionCount == null || extensionCount == 0) {
                return Health.down()
                        .withDetail("error", "PgVector extension not available")
                        .withDetails(details)
                        .build();
            }

            if (tableCount == null || tableCount == 0) {
                return Health.down()
                        .withDetail("error", "Embedding column not found")
                        .withDetails(details)
                        .build();
            }

            return Health.up().withDetails(details).build();
        } catch (Exception e) {
            log.error("Vector store health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * Checks LLM model availability (cached to avoid API costs).
     */
    private Health checkLlmModel() {
        // Check cache first
        CachedHealthResult cached = llmHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached LLM health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (chatModel == null) {
                Health result = Health.down()
                        .withDetail("error", "ChatModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(llmHealthCache, result);
                return result;
            }

            // Simple test to verify model is accessible
            // Note: We don't make an actual API call to avoid costs
            // Just verify the bean is available and configured
            String modelType = chatModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);

            // Extract model configuration details
            Map<String, Object> modelConfig = extractChatModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(llmHealthCache, result);

            log.info("LLM health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("LLM health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(llmHealthCache, result);
            return result;
        }
    }

    /**
     * Checks embedding model availability (cached to avoid API costs).
     */
    private Health checkEmbeddingModel() {
        // Check cache first
        CachedHealthResult cached = embeddingHealthCache.get();
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached embedding health check result (age: {}ms)", cached.getAge().toMillis());
            return cached.health();
        }

        // Perform actual check
        try {
            if (embeddingModel == null) {
                Health result = Health.down()
                        .withDetail("error", "EmbeddingModel bean not available")
                        .withDetail("cached", false)
                        .build();
                cacheResult(embeddingHealthCache, result);
                return result;
            }

            // Simple test to verify model is accessible
            // Note: We don't make an actual API call to avoid costs
            // Just verify the bean is available and configured
            String modelType = embeddingModel.getClass().getSimpleName();

            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("modelType", modelType);
            details.put("cached", false);

            // Extract model configuration details
            Map<String, Object> modelConfig = extractEmbeddingModelConfig();
            if (!modelConfig.isEmpty()) {
                details.putAll(modelConfig);
            }

            Health result = Health.up().withDetails(details).build();
            cacheResult(embeddingHealthCache, result);

            log.info("Embedding health check passed - Model: {}", modelType);
            return result;
        } catch (Exception e) {
            log.error("Embedding health check failed", e);
            Health result = Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .withDetail("cached", false)
                    .build();
            cacheResult(embeddingHealthCache, result);
            return result;
        }
    }

    /**
     * Extracts chat model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractChatModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.chat.*)
            String model = environment.getProperty("spring.ai.custom.chat.model");
            String baseUrl = environment.getProperty("spring.ai.custom.chat.base-url");
            String provider = environment.getProperty("spring.ai.custom.chat.provider", "openai");

            // Fallback to legacy configuration if custom not available
            if (model == null || model.isEmpty()) {
                String modelType = chatModel != null ? chatModel.getClass().getSimpleName().toLowerCase() : "";
                if (modelType.contains("ollama")) {
                    model = environment.getProperty("spring.ai.ollama.chat.options.model");
                    baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
                    provider = "ollama";
                } else if (modelType.contains("openai")) {
                    model = environment.getProperty("spring.ai.openai.chat.options.model");
                    baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
                    provider = "openai";
                }
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract chat model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Extracts embedding model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractEmbeddingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.embedding.*)
            String model = environment.getProperty("spring.ai.custom.embedding.model");
            String baseUrl = environment.getProperty("spring.ai.custom.embedding.base-url");
            String provider = environment.getProperty("spring.ai.custom.embedding.provider", "openai");

            // Fallback to legacy configuration if custom not available
            if (model == null || model.isEmpty()) {
                String modelType = embeddingModel != null ? embeddingModel.getClass().getSimpleName().toLowerCase() : "";
                if (modelType.contains("ollama")) {
                    model = environment.getProperty("spring.ai.ollama.embedding.embedding.options.model");
                    baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
                    provider = "ollama";
                } else if (modelType.contains("openai")) {
                    model = environment.getProperty("spring.ai.openai.embedding.options.model");
                    baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
                    provider = "openai";
                }
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract embedding model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Extracts reranking model configuration details from environment properties.
     *
     * @return Map containing model name, base URL, and provider
     */
    private Map<String, Object> extractRerankingModelConfig() {
        Map<String, Object> config = new HashMap<>();
        if (environment == null) {
            return config;
        }

        try {
            // Try custom configuration first (spring.ai.custom.reranking.*)
            String model = environment.getProperty("spring.ai.custom.reranking.model");
            String baseUrl = environment.getProperty("spring.ai.custom.reranking.base-url");
            String provider = environment.getProperty("spring.ai.custom.reranking.provider", "ollama");

            // Fallback to legacy configuration if custom not available
            if (model == null || model.isEmpty()) {
                String legacyModel = environment.getProperty("spring.ai.ollama.reranking.options.model");
                if (legacyModel != null && !legacyModel.isEmpty()) {
                    model = legacyModel;
                    baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
                    provider = "ollama";
                }
            }

            if (model != null && !model.isEmpty()) {
                config.put("model", model);
            }
            if (baseUrl != null && !baseUrl.isEmpty()) {
                config.put("baseUrl", baseUrl);
            }
            if (provider != null && !provider.isEmpty()) {
                config.put("provider", provider);
            }
        } catch (Exception e) {
            log.debug("Failed to extract reranking model configuration: {}", e.getMessage());
        }

        return config;
    }

    /**
     * Caches a health check result.
     */
    private void cacheResult(AtomicReference<CachedHealthResult> cache, Health health) {
        // Cache successful checks for 5 minutes, failures for 30 seconds
        Duration cacheDuration = health.getStatus().getCode().equals("UP")
                ? LLM_CHECK_CACHE_DURATION
                : Duration.ofSeconds(30);

        cache.set(new CachedHealthResult(health, Instant.now(), cacheDuration));
    }

    /**
     * Cached health check result.
     */
    private record CachedHealthResult(Health health, Instant timestamp, Duration cacheDuration) {

        public Duration getAge() {
            return Duration.between(timestamp, Instant.now());
        }

        public boolean isExpired() {
            return getAge().compareTo(cacheDuration) >= 0;
        }
    }
}

