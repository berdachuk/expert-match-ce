package com.berdachuk.expertmatch.employee.repository;

import com.berdachuk.expertmatch.employee.domain.Employee;

import java.util.List;
import java.util.Optional;


/**
 * Repository interface for employee operations.
 */
public interface EmployeeRepository {

    /**
     * Finds an employee by their unique identifier.
     *
     * @param employeeId The unique identifier of the employee (19-digit numeric string)
     * @return Optional containing the employee if found, empty otherwise
     */
    Optional<Employee> findById(String employeeId);

    /**
     * Finds an employee by their email address.
     *
     * @param email The email address of the employee
     * @return Optional containing the employee if found, empty otherwise
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Finds multiple employees by their unique identifiers.
     *
     * @param employeeIds List of employee unique identifiers
     * @return List of employees found, empty list if none found
     */
    List<Employee> findByIds(List<String> employeeIds);

    /**
     * Finds employee identifiers by name using exact or partial matching.
     *
     * @param name       The name to search for
     * @param maxResults Maximum number of results to return
     * @return List of employee identifiers matching the name, empty list if none found
     */
    List<String> findEmployeeIdsByName(String name, int maxResults);

    /**
     * Finds employee identifiers by name using similarity matching.
     *
     * @param name                The name to search for
     * @param similarityThreshold Minimum similarity threshold (0.0 to 1.0)
     * @param maxResults          Maximum number of results to return
     * @return List of employee identifiers matching the name with similarity >= threshold, empty list if none found
     */
    List<String> findEmployeeIdsByNameSimilarity(String name, double similarityThreshold, int maxResults);

    /**
     * Creates or updates an employee.
     * Uses ON CONFLICT to handle duplicate IDs/emails gracefully.
     *
     * @param employee Employee entity to create/update
     * @return The employee ID
     */
    String createOrUpdate(Employee employee);

    /**
     * Finds all employee IDs, optionally limited by count.
     *
     * @param limit Maximum number of IDs to return (0 or negative for no limit)
     * @return List of employee IDs
     */
    List<String> findAllIds(int limit);

    /**
     * Deletes all employee records.
     * Warning: This is a destructive operation.
     *
     * @return Number of records deleted
     */
    int deleteAll();
}
