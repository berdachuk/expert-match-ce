/**
 * Project Module
 * <p>
 * Manages project data:
 * - Project domain entities
 * - Project repository (JDBC-based)
 * <p>
 * Exposes:
 * - ProjectRepository (repository interface)
 */
@org.springframework.modulith.ApplicationModule(
        id = "project",
        displayName = "Project Management",
        allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.project;

