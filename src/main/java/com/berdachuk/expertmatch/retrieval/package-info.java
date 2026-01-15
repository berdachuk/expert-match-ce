/**
 * Retrieval Module
 * <p>
 * Hybrid GraphRAG retrieval engine combining:
 * - Vector search (PgVector)
 * - Graph traversal (Apache AGE)
 * - Keyword search (PostgreSQL full-text)
 * - Semantic reranking
 * <p>
 * Exposes:
 * - HybridRetrievalService (service layer)
 * - VectorSearchService (service layer)
 * - GraphSearchService (service layer)
 * - KeywordSearchService (service layer)
 */
@org.springframework.modulith.ApplicationModule(
    id = "retrieval",
    displayName = "Hybrid Retrieval",
    allowedDependencies = {"employee::API", "embedding::API", "graph::API", "core"}
)
package com.berdachuk.expertmatch.retrieval;

import org.springframework.modulith.ApplicationModule;
