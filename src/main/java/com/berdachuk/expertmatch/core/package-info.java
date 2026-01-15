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
 */
@org.springframework.modulith.ApplicationModule(
    id = "core",
    displayName = "Core Infrastructure",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.berdachuk.expertmatch.core;

import org.springframework.modulith.ApplicationModule;
