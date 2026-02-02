package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents a relationship between a project and a customer.
 *
 * @param projectId  the unique identifier of the project
 * @param customerId the unique identifier of the customer
 */
public record ProjectCustomerRelationship(String projectId, String customerId) {
}
