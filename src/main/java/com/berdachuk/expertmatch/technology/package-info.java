/**
 * Technology Module
 * <p>
 * Manages technology/skill data:
 * - Technology domain entities
 * - Technology repository (JDBC-based)
 * <p>
 * Exposes:
 * - TechnologyRepository (repository interface)
 */
@org.springframework.modulith.ApplicationModule(
    id = "technology",
    displayName = "Technology Management",
    allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.technology;

import org.springframework.modulith.ApplicationModule;
