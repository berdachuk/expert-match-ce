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
 * - LLM Tools (tools package for AI agent integration)
 * <p>
 * Note: The tools package provides Spring AI tool annotations that can be called by LLMs.
 * These tools orchestrate across multiple domain modules to accomplish complex tasks.
 */
@org.springframework.modulith.ApplicationModule(
        id = "llm",
        displayName = "LLM Orchestration",
        allowedDependencies = {"core", "query", "employee", "workexperience", "embedding", "graph", "chat"}
)
package com.berdachuk.expertmatch.llm;

