package com.berdachuk.expertmatch.retrieval;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResultFusionService.
 */
class ResultFusionServiceTest {

    private final ResultFusionService fusionService = new ResultFusionService();

    @Test
    void testFuseResults() {
        Map<String, List<String>> results = new HashMap<>();
        results.put("vector", List.of("expert1", "expert2", "expert3"));
        results.put("graph", List.of("expert2", "expert4", "expert5"));
        results.put("keyword", List.of("expert1", "expert3", "expert6"));

        List<String> fused = fusionService.fuseResults(results);

        assertNotNull(fused);
        assertTrue(fused.size() > 0);
        // Expert1 and expert2 should be high in results (appear in multiple lists)
        assertTrue(fused.contains("expert1"));
        assertTrue(fused.contains("expert2"));
    }

    @Test
    void testFuseResultsWithWeights() {
        Map<String, List<String>> results = new HashMap<>();
        results.put("vector", List.of("expert1", "expert2"));
        results.put("graph", List.of("expert2", "expert3"));

        Map<String, Double> weights = Map.of(
            "vector", 1.0,
            "graph", 0.8
        );

        List<String> fused = fusionService.fuseResults(results, weights);

        assertNotNull(fused);
        assertTrue(fused.size() > 0);
    }

    @Test
    void testFuseEmptyResults() {
        Map<String, List<String>> results = new HashMap<>();
        results.put("vector", List.of());
        results.put("graph", List.of());

        List<String> fused = fusionService.fuseResults(results);

        assertNotNull(fused);
        assertTrue(fused.isEmpty());
    }

    @Test
    void testFuseResultsWithNullResults() {
        assertThrows(IllegalArgumentException.class, () -> {
            fusionService.fuseResults(null);
        });
    }

    @Test
    void testFuseResultsWithEmptyResultsMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            fusionService.fuseResults(new HashMap<>());
        });
    }

    @Test
    void testFuseResultsWithNullWeights() {
        Map<String, List<String>> results = new HashMap<>();
        results.put("vector", List.of("expert1"));

        assertThrows(IllegalArgumentException.class, () -> {
            fusionService.fuseResults(results, null);
        });
    }
}

