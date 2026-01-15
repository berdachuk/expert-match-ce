package com.berdachuk.expertmatch.query.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelInfoExtractorTest {

    @Test
    void testExtractModelInfoWithOllama() {
        ChatModel mockChatModel = new MockOllamaChatModel();

        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.ollama.chat.options.model", "not configured"))
                .thenReturn("qwen3:4b-instruct-2507-q4_K_M");

        String modelInfo = ModelInfoExtractor.extractModelInfo(mockChatModel, mockEnv);

        assertNotNull(modelInfo);
        assertTrue(modelInfo.contains("MockOllamaChatModel") || modelInfo.contains("Ollama"));
        assertTrue(modelInfo.contains("qwen3:4b-instruct-2507-q4_K_M"));
    }

    @Test
    void testExtractModelInfoWithOpenAI() {
        ChatModel mockChatModel = new MockOpenAiChatModel();

        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.openai.chat.options.model", "not configured"))
                .thenReturn("gpt-4");

        String modelInfo = ModelInfoExtractor.extractModelInfo(mockChatModel, mockEnv);

        assertNotNull(modelInfo);
        assertTrue(modelInfo.contains("MockOpenAiChatModel") || modelInfo.contains("OpenAI"));
        assertTrue(modelInfo.contains("gpt-4"));
    }

    @Test
    void testExtractModelInfoWithNullEnvironment() {
        ChatModel mockChatModel = new MockOllamaChatModel();

        String modelInfo = ModelInfoExtractor.extractModelInfo(mockChatModel, null);

        assertNotNull(modelInfo);
        assertEquals("MockOllamaChatModel", modelInfo);
    }

    @Test
    void testExtractModelInfoWithNullModel() {
        Environment mockEnv = mock(Environment.class);
        String modelInfo = ModelInfoExtractor.extractModelInfo(null, mockEnv);
        assertNull(modelInfo);
    }

    @Test
    void testExtractEmbeddingModelInfo() {
        EmbeddingModel mockEmbeddingModel = mock(EmbeddingModel.class);

        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.ollama.embedding.embedding.options.model", "not configured"))
                .thenReturn("qwen3-embedding-8b");

        String modelInfo = ModelInfoExtractor.extractEmbeddingModelInfo(mockEmbeddingModel, mockEnv);

        assertNotNull(modelInfo);
        // Just verify it returns something, exact format may vary
        assertFalse(modelInfo.isEmpty());
    }

    @Test
    void testExtractRerankingModelInfo() {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.ollama.reranking.options.model"))
                .thenReturn("dengcao/Qwen3-Reranker-8B:Q4_K_M");

        String modelInfo = ModelInfoExtractor.extractRerankingModelInfo(mockEnv);

        assertNotNull(modelInfo);
        assertTrue(modelInfo.contains("OllamaChatModel"));
        assertTrue(modelInfo.contains("dengcao/Qwen3-Reranker-8B:Q4_K_M"));
    }

    @Test
    void testExtractRerankingModelInfoWithOpenAI() {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.ollama.reranking.options.model")).thenReturn(null);
        when(mockEnv.getProperty("spring.ai.openai.reranking.options.model"))
                .thenReturn("gpt-4-rerank");

        String modelInfo = ModelInfoExtractor.extractRerankingModelInfo(mockEnv);

        assertNotNull(modelInfo);
        assertTrue(modelInfo.contains("OpenAiChatModel"));
        assertTrue(modelInfo.contains("gpt-4-rerank"));
    }

    @Test
    void testExtractRerankingModelInfoNotConfigured() {
        Environment mockEnv = mock(Environment.class);
        when(mockEnv.getProperty("spring.ai.ollama.reranking.options.model")).thenReturn(null);
        when(mockEnv.getProperty("spring.ai.openai.reranking.options.model")).thenReturn(null);

        String modelInfo = ModelInfoExtractor.extractRerankingModelInfo(mockEnv);
        assertNull(modelInfo);
    }

    @Test
    void testExtractRerankingModelInfoWithNullEnvironment() {
        String modelInfo = ModelInfoExtractor.extractRerankingModelInfo(null);
        assertNull(modelInfo);
    }

    // Mock classes for testing
    static class MockOllamaChatModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt prompt) {
            return null;
        }
    }

    static class MockOpenAiChatModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(org.springframework.ai.chat.prompt.Prompt prompt) {
            return null;
        }
    }
}

