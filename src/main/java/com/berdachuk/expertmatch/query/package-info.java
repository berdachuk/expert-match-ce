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
        allowedDependencies = {"retrieval::API", "llm::API", "employee::API", "core"}
)
package com.berdachuk.expertmatch.query;

