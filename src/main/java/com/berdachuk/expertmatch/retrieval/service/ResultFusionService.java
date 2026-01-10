package com.berdachuk.expertmatch.retrieval.service;

import java.util.List;
import java.util.Map;


/**
 * Service interface for resultfusion operations.
 */
public interface ResultFusionService {
    /**
     * Fuses multiple result lists using weighted scoring.
     *
     * @param results Map of source name to list of expert IDs
     * @param weights Map of source name to weight (0.0 to 1.0)
     * @return Fused list of expert IDs ordered by combined score
     */
    List<String> fuseResults(Map<String, List<String>> results, Map<String, Double> weights);

    /**
     * Fuses multiple result lists using equal weights.
     *
     * @param results Map of source name to list of expert IDs
     * @return Fused list of expert IDs ordered by combined score
     */
    List<String> fuseResults(Map<String, List<String>> results);
}
