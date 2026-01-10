package com.berdachuk.expertmatch.graph.service;

import java.util.List;

/**
 * Service interface for graph builder operations.
 */
public interface GraphBuilderService {
    /**
     * Builds the complete graph from database data.
     * Creates all vertices (experts, projects, technologies, domains, customers) and relationships.
     */
    void buildGraph();

    /**
     * Creates an expert vertex in the graph.
     *
     * @param expertId  The unique identifier of the expert
     * @param name      The name of the expert
     * @param email     The email address of the expert
     * @param seniority The seniority level of the expert
     */
    void createExpertVertex(String expertId, String name, String email, String seniority);

    /**
     * Creates a project vertex in the graph.
     *
     * @param projectId   The unique identifier of the project
     * @param projectName The name of the project
     * @param projectType The type of the project
     */
    void createProjectVertex(String projectId, String projectName, String projectType);

    /**
     * Creates a technology vertex in the graph.
     *
     * @param technologyName The name of the technology
     */
    void createTechnologyVertex(String technologyName);

    /**
     * Creates a domain vertex in the graph.
     *
     * @param domainName The name of the domain
     */
    void createDomainVertex(String domainName);

    /**
     * Creates a customer vertex in the graph.
     *
     * @param customerId   The unique identifier of the customer
     * @param customerName The name of the customer
     */
    void createCustomerVertex(String customerId, String customerName);

    /**
     * Creates an "IN_DOMAIN" relationship between a project and a domain.
     *
     * @param projectId  The unique identifier of the project
     * @param domainName The name of the domain
     */
    void createInDomainRelationship(String projectId, String domainName);

    /**
     * Creates a "PARTICIPATION" relationship between an expert and a project.
     *
     * @param expertId  The unique identifier of the expert
     * @param projectId The unique identifier of the project
     * @param role      The role of the expert in the project
     */
    void createParticipationRelationship(String expertId, String projectId, String role);

    /**
     * Creates multiple "PARTICIPATION" relationships in batch.
     *
     * @param relationships List of participation relationships to create
     */
    void createParticipationRelationshipsBatch(List<ParticipationRelationship> relationships);

    /**
     * Creates multiple "USES" relationships between projects and technologies in batch.
     *
     * @param relationships List of project-technology relationships to create
     */
    void createUsesRelationshipsBatch(List<ProjectTechnologyRelationship> relationships);

    /**
     * Creates multiple "PROJECT_CUSTOMER" relationships in batch.
     *
     * @param relationships List of project-customer relationships to create
     */
    void createProjectCustomerRelationshipsBatch(List<ProjectCustomerRelationship> relationships);

    /**
     * Creates multiple "EXPERT_CUSTOMER" relationships in batch.
     *
     * @param relationships List of expert-customer relationships to create
     */
    void createExpertCustomerRelationshipsBatch(List<ExpertCustomerRelationship> relationships);

    /**
     * Creates a "USES" relationship between a project and a technology.
     *
     * @param projectId      The unique identifier of the project
     * @param technologyName The name of the technology
     */
    void createUsesRelationship(String projectId, String technologyName);

    /**
     * Participation relationship.
     */
    record ParticipationRelationship(String expertId, String projectId, String role) {
    }

    /**
     * Project-technology relationship.
     */
    record ProjectTechnologyRelationship(String projectId, String technologyName) {
    }

    /**
     * Expert-customer relationship.
     */
    record ExpertCustomerRelationship(String expertId, String customerId) {
    }

    /**
     * Project-customer relationship.
     */
    record ProjectCustomerRelationship(String projectId, String customerId) {
    }
}
