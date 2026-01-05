package com.berdachuk.expertmatch.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for EmbeddingService.
 * Uses Mockito to mock EmbeddingModel.
 *
 * IMPORTANT: LLM providers are external services and MUST be mocked.
 * - Uses @Mock for EmbeddingModel to avoid real API calls
 * - All embedding generation uses mocked responses
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @SuppressWarnings("null")
        // Mockito matchers don't preserve null-safety annotations
    void testGenerateEmbedding() {
        // Mock embedding response - Spring AI 1.1.0 uses float[] for embeddings
        float[] mockEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

        EmbeddingResponse response = mock(EmbeddingResponse.class);
        Embedding embedding = mock(Embedding.class);
        when(embedding.getOutput()).thenReturn(mockEmbedding);
        when(response.getResults()).thenReturn(List.of(embedding));

        when(embeddingModel.embedForResponse(ArgumentMatchers.<List<String>>any())).thenReturn(response);

        List<Double> resultList = embeddingService.generateEmbedding("test text");

        assertNotNull(resultList);
        assertEquals(5, resultList.size());
        assertEquals(0.1, resultList.get(0), 0.001);
        assertEquals(0.5, resultList.get(4), 0.001);

        List<String> expectedTexts = new ArrayList<>();
        expectedTexts.add("test text");
        verify(embeddingModel, times(1)).embedForResponse(expectedTexts);
    }

    @Test
    @SuppressWarnings("null")
        // Mockito matchers don't preserve null-safety annotations
    void testGenerateEmbeddingEmptyResponse() {
        EmbeddingResponse response = mock(EmbeddingResponse.class);
        when(response.getResults()).thenReturn(List.of());

        when(embeddingModel.embedForResponse(ArgumentMatchers.<List<String>>any())).thenReturn(response);

        List<Double> result = embeddingService.generateEmbedding("test text");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("null")
        // Mockito matchers don't preserve null-safety annotations
    void testGenerateEmbeddingsBatch() {
        // Mock batch embedding response - Spring AI 1.1.0 uses float[]
        float[] embedding1 = new float[]{0.1f, 0.2f, 0.3f};
        float[] embedding2 = new float[]{0.4f, 0.5f, 0.6f};

        EmbeddingResponse response = mock(EmbeddingResponse.class);
        Embedding emb1 = mock(Embedding.class);
        Embedding emb2 = mock(Embedding.class);
        when(emb1.getOutput()).thenReturn(embedding1);
        when(emb2.getOutput()).thenReturn(embedding2);
        when(response.getResults()).thenReturn(List.of(emb1, emb2));

        when(embeddingModel.embedForResponse(ArgumentMatchers.<List<String>>any())).thenReturn(response);

        List<String> texts = new ArrayList<>();
        texts.add("text1");
        texts.add("text2");
        List<List<Double>> results = embeddingService.generateEmbeddings(texts);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(3, results.get(0).size());
        assertEquals(3, results.get(1).size());
        assertEquals(0.1, results.get(0).get(0), 0.001);
        assertEquals(0.4, results.get(1).get(0), 0.001);

        verify(embeddingModel, times(1)).embedForResponse(texts);
    }

    @Test
    @SuppressWarnings("null")
        // Mockito matchers don't preserve null-safety annotations
    void testGenerateEmbeddingsEmptyList() {
        List<List<Double>> results = embeddingService.generateEmbeddings(new ArrayList<>());

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(embeddingModel, never()).embedForResponse(ArgumentMatchers.<List<String>>any());
    }

    @Test
    @SuppressWarnings("null")
        // Mockito matchers don't preserve null-safety annotations
    void testGenerateEmbeddingAsFloatArray() {
        float[] mockEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

        EmbeddingResponse response = mock(EmbeddingResponse.class);
        Embedding embedding = mock(Embedding.class);
        when(embedding.getOutput()).thenReturn(mockEmbedding);
        when(response.getResults()).thenReturn(List.of(embedding));

        when(embeddingModel.embedForResponse(ArgumentMatchers.<List<String>>any())).thenReturn(response);

        float[] resultArray = embeddingService.generateEmbeddingAsFloatArray("test text");

        assertNotNull(resultArray);
        assertEquals(5, resultArray.length);
        assertEquals(0.1f, resultArray[0], 0.001f);
        assertEquals(0.5f, resultArray[4], 0.001f);
    }

    @Test
    void testGenerateEmbeddingNullClient() {
        EmbeddingService service = new EmbeddingService(null);

        assertThrows(IllegalStateException.class, () -> {
            service.generateEmbedding("test");
        });
    }

    @Test
    void testGenerateEmbeddingsNullClient() {
        EmbeddingService service = new EmbeddingService(null);

        List<String> testList = new ArrayList<>();
        testList.add("test");
        assertThrows(IllegalStateException.class, () -> {
            service.generateEmbeddings(testList);
        });
    }
}

