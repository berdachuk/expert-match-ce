package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents customer data fetched from the work experience table.
 *
 * @param customerId   the unique identifier of the customer
 * @param customerName the name of the customer
 */
public record CustomerData(String customerId, String customerName) {
}