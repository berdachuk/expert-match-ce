package com.berdachuk.expertmatch.query.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.env.Environment;

/**
 * Utility to extract model information from ChatModel and EmbeddingModel instances.
 */
@Slf4j
public class ModelInfoExtractor {

    /**
     * Extracts model information from a ChatModel.
     * Returns formatted string like "OpenAiChatModel (gpt-4)".
     *
     * @param chatModel   The ChatModel instance
     * @param environment Spring Environment for accessing configuration
     * @return Formatted model string or null if extraction fails
     */
    public static String extractModelInfo(ChatModel chatModel, Environment environment) {
        if (chatModel == null) {
            return null;
        }

        try {
            String modelType = chatModel.getClass().getSimpleName();
            String modelName = extractModelName(modelType, false, environment);

            if (modelName != null && !modelName.equals("not configured")) {
                return String.format("%s (%s)", modelType, modelName);
            } else {
                return modelType;
            }
        } catch (Exception e) {
            log.debug("Failed to extract model info from ChatModel: {}", e.getMessage());
            return chatModel.getClass().getSimpleName();
        }
    }

    /**
     * Extracts model information from an EmbeddingModel.
     * Returns formatted string like "OpenAiEmbeddingModel (text-embedding-3-large)".
     *
     * @param embeddingModel The EmbeddingModel instance
     * @param environment    Spring Environment for accessing configuration
     * @return Formatted model string or null if extraction fails
     */
    public static String extractEmbeddingModelInfo(EmbeddingModel embeddingModel, Environment environment) {
        if (embeddingModel == null) {
            return null;
        }

        try {
            String modelType = embeddingModel.getClass().getSimpleName();
            String modelName = extractModelName(modelType, true, environment);

            if (modelName != null && !modelName.equals("not configured")) {
                return String.format("%s (%s)", modelType, modelName);
            } else {
                return modelType;
            }
        } catch (Exception e) {
            log.debug("Failed to extract model info from EmbeddingModel: {}", e.getMessage());
            return embeddingModel.getClass().getSimpleName();
        }
    }

    /**
     * Extracts reranking model information from environment configuration.
     *
     * @param environment Spring Environment for accessing configuration
     * @return Formatted model string or null if not configured
     */
    public static String extractRerankingModelInfo(Environment environment) {
        if (environment == null) {
            return null;
        }

        try {
            String rerankingModel = environment.getProperty("spring.ai.custom.reranking.model");
            if (rerankingModel != null && !rerankingModel.isEmpty()) {
                return String.format("OpenAiChatModel (%s)", rerankingModel);
            }
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract reranking model info: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts model name from environment properties based on model type.
     *
     * @param modelType        The model class simple name (e.g., "OpenAiChatModel")
     * @param isEmbeddingModel Whether this is an embedding model
     * @param environment      Spring Environment
     * @return Model name or "not configured" if not found
     */
    private static String extractModelName(String modelType, boolean isEmbeddingModel, Environment environment) {
        if (environment == null) {
            return "not configured";
        }

        String modelTypeLower = modelType.toLowerCase();

        if (modelTypeLower.contains("openai")) {
            String modelProperty = isEmbeddingModel ?
                    "spring.ai.openai.embedding.options.model" :
                    "spring.ai.openai.chat.options.model";
            return environment.getProperty(modelProperty, "not configured");
        }
        return "not configured";
    }
}

