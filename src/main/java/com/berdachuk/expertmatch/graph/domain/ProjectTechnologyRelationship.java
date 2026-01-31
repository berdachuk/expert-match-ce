package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents a relationship between a project and a technology.
 *
 * @param projectId      the unique identifier of the project
 * @param technologyName the name of the technology
 */
public record ProjectTechnologyRelationship(String projectId, String technologyName) {
}
