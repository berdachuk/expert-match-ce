package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents a relationship between an expert and a customer.
 *
 * @param expertId   the unique identifier of the expert
 * @param customerId the unique identifier of the customer
 */
public record ExpertCustomerRelationship(String expertId, String customerId) {
}
