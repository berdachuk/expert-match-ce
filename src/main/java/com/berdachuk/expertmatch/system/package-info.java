/**
 * System Module
 * <p>
 * System-level endpoints and utilities:
 * - Health checks
 * - System information
 * - Monitoring endpoints
 * <p>
 * Exposes:
 * - SystemController (REST API)
 */
@org.springframework.modulith.ApplicationModule(
    id = "system",
    displayName = "System",
    allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.system;

import org.springframework.modulith.ApplicationModule;
