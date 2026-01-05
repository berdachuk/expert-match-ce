package com.berdachuk.expertmatch.query;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenUsageExtractorTest {

    @Test
    void testExtractTokenUsageWithValidUsage() {
        // Create mock Usage
        Usage mockUsage = mock(Usage.class);
        when(mockUsage.getPromptTokens()).thenReturn(45);
        when(mockUsage.getCompletionTokens()).thenReturn(12);
        when(mockUsage.getTotalTokens()).thenReturn(57);

        // Create mock metadata - use the actual interface from Spring AI
        org.springframework.ai.chat.metadata.ChatResponseMetadata mockMetadata =
                mock(org.springframework.ai.chat.metadata.ChatResponseMetadata.class);
        when(mockMetadata.getUsage()).thenReturn(mockUsage);

        // Create mock ChatResponse
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenReturn(mockMetadata);

        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);

        assertNotNull(tokenUsage);
        assertEquals(45, tokenUsage.inputTokens());
        assertEquals(12, tokenUsage.outputTokens());
        assertEquals(57, tokenUsage.totalTokens());
    }

    @Test
    void testExtractTokenUsageWithNullResponse() {
        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(null);
        assertNull(tokenUsage);
    }

    @Test
    void testExtractTokenUsageWithNullMetadata() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenReturn(null);

        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);
        assertNull(tokenUsage);
    }

    @Test
    void testExtractTokenUsageWithNullUsage() {
        org.springframework.ai.chat.metadata.ChatResponseMetadata mockMetadata =
                mock(org.springframework.ai.chat.metadata.ChatResponseMetadata.class);
        when(mockMetadata.getUsage()).thenReturn(null);

        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenReturn(mockMetadata);

        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);
        assertNull(tokenUsage);
    }

    @Test
    void testExtractTokenUsageWithPartialTokens() {
        Usage mockUsage = mock(Usage.class);
        when(mockUsage.getPromptTokens()).thenReturn(45);
        when(mockUsage.getCompletionTokens()).thenReturn(null);
        when(mockUsage.getTotalTokens()).thenReturn(null);

        org.springframework.ai.chat.metadata.ChatResponseMetadata mockMetadata =
                mock(org.springframework.ai.chat.metadata.ChatResponseMetadata.class);
        when(mockMetadata.getUsage()).thenReturn(mockUsage);

        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenReturn(mockMetadata);

        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);

        assertNotNull(tokenUsage);
        assertEquals(45, tokenUsage.inputTokens());
        assertNull(tokenUsage.outputTokens());
        assertNull(tokenUsage.totalTokens());
    }

    @Test
    void testExtractTokenUsageWithAllNullTokens() {
        Usage mockUsage = mock(Usage.class);
        when(mockUsage.getPromptTokens()).thenReturn(null);
        when(mockUsage.getCompletionTokens()).thenReturn(null);
        when(mockUsage.getTotalTokens()).thenReturn(null);

        org.springframework.ai.chat.metadata.ChatResponseMetadata mockMetadata =
                mock(org.springframework.ai.chat.metadata.ChatResponseMetadata.class);
        when(mockMetadata.getUsage()).thenReturn(mockUsage);

        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenReturn(mockMetadata);

        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);
        assertNull(tokenUsage);
    }

    @Test
    void testExtractTokenUsageHandlesException() {
        ChatResponse mockResponse = mock(ChatResponse.class);
        when(mockResponse.getMetadata()).thenThrow(new RuntimeException("Test exception"));

        // Should not throw, but return null
        ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(mockResponse);
        assertNull(tokenUsage);
    }
}
