/**
 * Work Experience Module
 * <p>
 * Manages work experience data:
 * - WorkExperience domain entities
 * - WorkExperience repository (JDBC-based)
 * <p>
 * Exposes:
 * - WorkExperienceRepository (repository interface)
 */
@org.springframework.modulith.ApplicationModule(
        id = "workexperience",
        displayName = "Work Experience Management",
        allowedDependencies = {"employee", "project", "technology", "core"}
)
package com.berdachuk.expertmatch.workexperience;

