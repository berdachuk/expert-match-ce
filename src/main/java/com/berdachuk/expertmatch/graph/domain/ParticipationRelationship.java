package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents a participation relationship between an expert and a project.
 *
 * @param expertId  the unique identifier of the expert
 * @param projectId the unique identifier of the project
 * @param role      the role of the expert in the project
 */
public record ParticipationRelationship(String expertId, String projectId, String role) {
}
