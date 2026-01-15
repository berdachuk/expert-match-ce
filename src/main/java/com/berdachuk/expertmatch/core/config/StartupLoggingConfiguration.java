package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * Configuration for logging application startup information including REST endpoints, Swagger UI, and LLM models.
 */
@Slf4j
@Component
public class StartupLoggingConfiguration {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ApplicationProperties applicationProperties;
    private final Environment environment;
    private final List<ChatModel> chatModels;
    private final List<EmbeddingModel> embeddingModels;

    public StartupLoggingConfiguration(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RequestMappingHandlerMapping requestMappingHandlerMapping,
            ApplicationProperties applicationProperties,
            Environment environment,
            @Lazy List<ChatModel> chatModels,
            @Lazy List<EmbeddingModel> embeddingModels) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.applicationProperties = applicationProperties;
        this.environment = environment;
        this.chatModels = chatModels;
        this.embeddingModels = embeddingModels;
    }

    /**
     * Log application information when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationInfo() {
        log.info("=================================================================");
        log.info("ExpertMatch Application Started Successfully!");
        log.info("=================================================================");

        // Log server information
        log.info("Server Information:");
        log.info("  Port: {}", applicationProperties.getServerPort());
        log.info("  Context Path: {}", applicationProperties.getServerContextPath());
        log.info("  Active Profiles: {}", String.join(", ", environment.getActiveProfiles()));

        // Log database information
        logDatabaseInformation();

        // Log LLM Model information
        logLLMInformation();

        // Log REST API endpoints (only if Spring MVC is available)
        if (requestMappingHandlerMapping != null) {
            log.info("REST API Endpoints:");
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
            Set<RequestMappingInfo> endpoints = handlerMethods.keySet();

            List<String> apiEndpoints = new ArrayList<>();
            List<String> otherEndpoints = new ArrayList<>();

            for (RequestMappingInfo endpoint : endpoints) {
                if (endpoint.getPatternsCondition() != null) {
                    Set<String> patterns = endpoint.getPatternsCondition().getPatterns();
                    for (String pattern : patterns) {
                        if (pattern.contains("/api/")) {
                            apiEndpoints.add(pattern);
                        } else if (!pattern.contains("/error") &&
                                !pattern.contains("/actuator") &&
                                !pattern.contains("/mcp")) {
                            otherEndpoints.add(pattern);
                        }
                    }
                }
            }

            // Sort endpoints for consistent output
            apiEndpoints.sort(String::compareTo);
            otherEndpoints.sort(String::compareTo);

            if (!apiEndpoints.isEmpty()) {
                log.info("  API Endpoints:");
                for (String endpoint : apiEndpoints) {
                    log.info("    {}", endpoint);
                }
            }

            if (!otherEndpoints.isEmpty()) {
                log.info("  Other Endpoints:");
                for (String endpoint : otherEndpoints) {
                    log.info("    {}", endpoint);
                }
            }
        } else {
            log.info("\nREST API Endpoints: Not available (Spring MVC not enabled)");
        }

        // Log Actuator endpoints
        log.info("Actuator Endpoints:");
        log.info("  Health Check: http://localhost:{}{}/actuator/health",
                applicationProperties.getServerPort(),
                applicationProperties.getServerContextPath());

        // Log Swagger/OpenAPI information
        log.info("API Documentation:");
        log.info("  OpenAPI Spec: http://localhost:{}{}/api/v1/openapi.json",
                applicationProperties.getServerPort(),
                applicationProperties.getServerContextPath());
        log.info("  Swagger UI: http://localhost:{}{}/swagger-ui.html",
                applicationProperties.getServerPort(),
                applicationProperties.getServerContextPath());

        log.info("=================================================================");
        log.info("Application is ready to accept requests!");
        log.info("=================================================================");
    }

    /**
     * Log database configuration information.
     */
    private void logDatabaseInformation() {
        log.debug("Database Configuration:");
        String dbUrl = environment.getProperty("spring.datasource.url", "not configured");
        String dbUsername = environment.getProperty("spring.datasource.username", "not configured");
        log.debug("  URL: {}", dbUrl);
        log.debug("  Username: {}", dbUsername);
    }

    /**
     * Log LLM model and API configuration information.
     */
    private void logLLMInformation() {
        log.info("LLM Configuration:");

        // Log Chat Models
        if (!chatModels.isEmpty()) {
            log.info("  Chat Models ({}):", chatModels.size());
            for (ChatModel model : chatModels) {
                String modelName = model.getClass().getSimpleName();
                log.info("    - {}: {}", modelName, getModelDetails(modelName, false));
            }
        } else {
            log.info("  Chat Models: None configured");
        }

        // Log Embedding Models (filter out duplicates)
        if (!embeddingModels.isEmpty()) {
            // Use LinkedHashSet to maintain order while removing duplicates
            Set<String> uniqueModels = new LinkedHashSet<>();
            List<EmbeddingModel> filteredModels = new ArrayList<>();

            for (EmbeddingModel model : embeddingModels) {
                String modelKey = model.getClass().getSimpleName() + "@" + model.hashCode();
                if (uniqueModels.add(modelKey)) {
                    filteredModels.add(model);
                }
            }

            log.info("  Embedding Models ({}):", filteredModels.size());
            for (EmbeddingModel model : filteredModels) {
                String modelName = model.getClass().getSimpleName();
                log.info("    - {}: {}", modelName, getModelDetails(modelName, true));
            }
        } else {
            log.info("  Embedding Models: None configured");
        }

        // Log Reranking Model Configuration
        log.info("  Reranking Models:");
        String rerankingModel = environment.getProperty("spring.ai.ollama.reranking.options.model");
        if (rerankingModel != null && !rerankingModel.isEmpty()) {
            String baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
            log.info("    - OllamaRerankingModel: URL: {}, Model: {}", baseUrl, rerankingModel);
        } else {
            String fallbackRerankingModel = environment.getProperty("expertmatch.query.reranking.model");
            if (fallbackRerankingModel != null && !fallbackRerankingModel.isEmpty()) {
                String baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11434");
                log.info("    - OllamaRerankingModel: URL: {}, Model: {}", baseUrl, fallbackRerankingModel);
            } else {
                log.info("    - OllamaRerankingModel: Not configured (reranking disabled)");
            }
        }
    }

    /**
     * Get model-specific details based on model type.
     */
    private String getModelDetails(String modelName, boolean isEmbeddingModel) {
        if (modelName.toLowerCase().contains("ollama")) {
            String baseUrl = environment.getProperty("spring.ai.ollama.base-url", "http://localhost:11435");
            String modelProperty = isEmbeddingModel ?
                    "spring.ai.ollama.embedding.embedding.options.model" :
                    "spring.ai.ollama.chat.options.model";
            String model = environment.getProperty(modelProperty, "not configured");
            return String.format("URL: %s, Model: %s", baseUrl, model);
        } else if (modelName.toLowerCase().contains("openai")) {
            String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
            String modelProperty = isEmbeddingModel ?
                    "spring.ai.openai.embedding.options.model" :
                    "spring.ai.openai.chat.options.model";
            String model = environment.getProperty(modelProperty, "not configured");
            return String.format("URL: %s, Model: %s", baseUrl, model);
        } else {
            return "Configuration not available";
        }
    }
}
