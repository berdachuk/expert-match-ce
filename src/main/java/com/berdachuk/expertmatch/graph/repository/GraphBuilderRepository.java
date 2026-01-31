package com.berdachuk.expertmatch.graph.repository;

import com.berdachuk.expertmatch.graph.domain.*;

import java.util.List;

/**
 * Repository interface for graph builder data access operations.
 * <p>
 * Provides methods to fetch data from the database for building the graph structure.
 * All data access for graph building operations should go through this interface.
 */
public interface GraphBuilderRepository {

    /**
     * Finds all expert data from the employee table.
     *
     * @return List of expert data, empty list if none found
     */
    List<ExpertData> findAllExperts();

    /**
     * Finds all project data from the work experience table.
     *
     * @return List of project data, empty list if none found
     */
    List<ProjectData> findAllProjects();

    /**
     * Finds all technology names from the work experience table.
     *
     * @return List of technology names, empty list if none found
     */
    List<String> findAllTechnologies();

    /**
     * Finds all domain names (industries) from the work experience table.
     *
     * @return List of domain names, empty list if none found
     */
    List<String> findAllDomains();

    /**
     * Finds all customer data from the work experience table.
     *
     * @return List of customer data, empty list if none found
     */
    List<CustomerData> findAllCustomers();

    /**
     * Finds all expert-project participation relationships from the work experience table.
     *
     * @return List of participation relationships, empty list if none found
     */
    List<ParticipationRelationship> findAllExpertProjectRelationships();

    /**
     * Finds all expert-customer relationships from the work experience table.
     *
     * @return List of expert-customer relationships, empty list if none found
     */
    List<ExpertCustomerRelationship> findAllExpertCustomerRelationships();

    /**
     * Finds all project-technology relationships from the work experience table.
     *
     * @return List of project-technology relationships, empty list if none found
     */
    List<ProjectTechnologyRelationship> findAllProjectTechnologyRelationships();

    /**
     * Finds all project-domain relationships from the work experience table.
     * Returns relationships in "projectId|domainName" format for backward compatibility.
     *
     * @return List of project-domain relationships in pipe-delimited format, empty list if none found
     */
    List<String> findAllProjectDomainRelationships();

    /**
     * Finds all project-customer relationships from the work experience table.
     *
     * @return List of project-customer relationships, empty list if none found
     */
    List<ProjectCustomerRelationship> findAllProjectCustomerRelationships();
}