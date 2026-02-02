/**
 * Query Processing Module
 * <p>
 * Handles query parsing, requirements extraction, entity extraction,
 * and intent classification.
 * <p>
 * Exposes:
 * - QueryService (service layer)
 * - QueryController (REST API)
 */
@org.springframework.modulith.ApplicationModule(
        id = "query",
        displayName = "Query Processing",
        allowedDependencies = {"core", "llm", "retrieval", "chat", "employee", "workexperience", "embedding", "api"}
)
package com.berdachuk.expertmatch.query;

