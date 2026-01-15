package com.berdachuk.expertmatch.embedding.service.impl;

import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating vector embeddings.
 * Uses the primary EmbeddingModel bean (configured in SpringAIConfig),
 * which automatically selects the appropriate model based on profile configuration:
 * - Local profile: Ollama (if configured)
 * - Dev profile: OpenAI/DIAL (if configured)
 * - Other profiles: Based on available models
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Constructor that uses the primary EmbeddingModel bean.
     * The primary bean is selected by SpringAIConfig based on available models and profile.
     */
    @Autowired
    public EmbeddingServiceImpl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates embedding for text.
     *
     * @param text Input text to embed
     * @return List of embedding values (as Double for compatibility)
     */
    @Override
    public List<Double> generateEmbedding(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not configured");
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

        if (response.getResults().isEmpty()) {
            return List.of();
        }

        float[] output = response.getResults().get(0).getOutput();
        List<Double> result = new ArrayList<>(output.length);
        for (float value : output) {
            result.add((double) value);
        }
        return result;
    }

    /**
     * Generates embeddings for multiple texts (batch).
     *
     * @param texts List of texts to embed
     * @return List of embedding vectors
     */
    @Override
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not configured");
        }

        if (texts.isEmpty()) {
            return List.of();
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);

        return response.getResults().stream()
                .map(result -> {
                    float[] output = result.getOutput();
                    List<Double> embedding = new ArrayList<>(output.length);
                    for (float value : output) {
                        embedding.add((double) value);
                    }
                    return embedding;
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates embedding as float array (more efficient for vector operations).
     */
    @Override
    public float[] generateEmbeddingAsFloatArray(String text) {
        List<Double> embedding = generateEmbedding(text);
        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}

