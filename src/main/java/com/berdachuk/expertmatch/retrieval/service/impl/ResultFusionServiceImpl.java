package com.berdachuk.expertmatch.retrieval.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for fusing results from multiple retrieval methods using Reciprocal Rank Fusion (RRF).
 */
@Service
public class ResultFusionServiceImpl implements ResultFusionService {

    // RRF constant (typically 60)
    private static final int RRF_K = 60;

    /**
     * Fuses multiple result lists using Reciprocal Rank Fusion.
     *
     * @param results Map of method name to list of expert IDs (ranked)
     * @param weights Map of method name to weight
     * @return Fused and ranked list of expert IDs
     */
    @Override
    public List<String> fuseResults(Map<String, List<String>> results, Map<String, Double> weights) {
        // Validate input parameters
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Results cannot be null or empty");
        }
        if (weights == null) {
            throw new IllegalArgumentException("Weights cannot be null");
        }

        Map<String, Double> scores = new HashMap<>();

        // Calculate RRF scores for each method
        for (Map.Entry<String, List<String>> entry : results.entrySet()) {
            String method = entry.getKey();
            List<String> expertIds = entry.getValue();
            double weight = weights.getOrDefault(method, 1.0);

            for (int rank = 0; rank < expertIds.size(); rank++) {
                String expertId = expertIds.get(rank);
                double rrfScore = weight / (RRF_K + rank + 1);
                scores.merge(expertId, rrfScore, Double::sum);
            }
        }

        // Sort by score descending
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Fuses results with default weights.
     */
    @Override
    public List<String> fuseResults(Map<String, List<String>> results) {
        // Validate input parameters
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Results cannot be null or empty");
        }

        Map<String, Double> defaultWeights = Map.of(
                "vector", 1.0,
                "graph", 0.8,
                "keyword", 0.6
        );
        return fuseResults(results, defaultWeights);
    }
}

