package com.berdachuk.expertmatch.workexperience.repository;

import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;

import java.util.List;
import java.util.Map;


/**
 * Repository interface for workexperience operations.
 */
public interface WorkExperienceRepository {

    /**
     * Finds all work experience records for an employee.
     *
     * @param employeeId The unique identifier of the employee
     * @return List of work experience records, empty list if none found
     */
    List<WorkExperience> findByEmployeeId(String employeeId);

    /**
     * Finds work experience records for multiple employees.
     * Uses batch loading to efficiently retrieve data for multiple employees.
     *
     * @param employeeIds List of employee unique identifiers
     * @return Map of employee ID to list of work experience records, empty map if none found
     */
    Map<String, List<WorkExperience>> findByEmployeeIds(List<String> employeeIds);

    /**
     * Finds employee identifiers who have experience with the specified technologies.
     *
     * @param technologies List of technology names to search for
     * @return List of employee identifiers who have experience with any of the specified technologies, empty list if none found
     */
    List<String> findEmployeeIdsByTechnologies(List<String> technologies);

}
