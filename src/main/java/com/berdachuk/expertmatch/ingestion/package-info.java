/**
 * Data Ingestion Module
 * <p>
 * Handles data ingestion:
 * - Test data generation (Datafaker)
 * - LLM-based constant expansion (optional, via ConstantExpansionService)
 * - Entity extraction and normalization
 * - Embedding generation pipeline
 * - Graph relationship building
 * <p>
 * Exposes:
 * - IngestionService (service layer)
 * - TestDataGenerator (service layer)
 * - IngestionController (REST API)
 */
@org.springframework.modulith.ApplicationModule(
    id = "ingestion",
    displayName = "Data Ingestion",
    allowedDependencies = {"employee::API", "embedding::API", "graph::API", "core"}
)
package com.berdachuk.expertmatch.ingestion;

import org.springframework.modulith.ApplicationModule;
