package com.berdachuk.expertmatch.retrieval.service;

import java.util.List;
import java.util.Map;

/**
 * Service for semantic reranking of retrieval results using LLM.
 */
public interface SemanticReranker {

    /**
     * Reranks expert results using semantic similarity via LLM.
     *
     * @param queryText  Original query text
     * @param expertIds  List of expert IDs to rerank
     * @param maxResults Maximum number of results after reranking
     * @return Reranked list of expert IDs
     */
    List<String> rerank(String queryText, List<String> expertIds, int maxResults);

    /**
     * Calculates relevance scores for experts using LLM.
     *
     * @param queryText Original query text
     * @param expertIds List of expert IDs to score
     * @return Map of expert IDs to relevance scores
     */
    Map<String, Double> calculateRelevanceScores(String queryText, List<String> expertIds);
}
