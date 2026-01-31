/**
 * Core Module
 * <p>
 * Provides core utilities and infrastructure:
 * - API mapping utilities
 * - SQL injection infrastructure
 * - ID generation utilities
 * - Configuration
 * - Exception handling
 * - Security
 * <p>
 * This is a shared infrastructure module used by other modules.
 * Orchestration services like configuration classes may depend on multiple
 * domain modules to coordinate workflows (e.g., tool registration, configuration).
 */
@org.springframework.modulith.ApplicationModule(
        id = "core",
        displayName = "Core Infrastructure",
        allowedDependencies = {"api", "query", "llm", "chat"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.berdachuk.expertmatch.core;

