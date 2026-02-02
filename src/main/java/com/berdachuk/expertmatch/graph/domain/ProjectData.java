package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents project data fetched from the work experience table.
 *
 * @param projectId   the unique identifier of the project
 * @param projectName the name of the project
 * @param projectType the type/industry of the project
 */
public record ProjectData(String projectId, String projectName, String projectType) {
}