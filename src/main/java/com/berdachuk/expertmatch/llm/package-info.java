/**
 * LLM Orchestration Module
 * <p>
 * Handles LLM orchestration using Spring AI:
 * - RAG pattern implementation
 * - Prompt construction
 * - Structured output generation
 * - SGR patterns (Cascade, Routing, Cycle)
 * <p>
 * Exposes:
 * - LLMService (service layer)
 * - SGR patterns (service layer)
 */
@org.springframework.modulith.ApplicationModule(
    id = "llm",
    displayName = "LLM Orchestration",
    allowedDependencies = {"retrieval::API", "core"}
)
package com.berdachuk.expertmatch.llm;

import org.springframework.modulith.ApplicationModule;
