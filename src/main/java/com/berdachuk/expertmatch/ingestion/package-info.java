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
        allowedDependencies = {"employee", "embedding", "graph", "technology", "api", "core", "workexperience", "project"}
)
package com.berdachuk.expertmatch.ingestion;

