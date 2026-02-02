package com.berdachuk.expertmatch.graph.domain;

/**
 * Represents expert data fetched from the employee table.
 *
 * @param id        the unique identifier of the expert
 * @param name      the name of the expert
 * @param email     the email address of the expert
 * @param seniority the seniority level of the expert
 */
public record ExpertData(String id, String name, String email, String seniority) {
}