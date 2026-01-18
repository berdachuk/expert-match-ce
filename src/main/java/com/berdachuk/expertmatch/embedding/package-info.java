/**
 * Embedding Module
 * <p>
 * Vector embedding generation:
 * - Embedding model configuration
 * - Batch embedding processing
 * - Embedding storage
 * <p>
 * Exposes:
 * - EmbeddingService (service layer)
 */
@org.springframework.modulith.ApplicationModule(
        id = "embedding",
        displayName = "Embedding Generation",
        allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.embedding;

