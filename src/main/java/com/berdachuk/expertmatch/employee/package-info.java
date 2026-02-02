/**
 * Employee Module
 * <p>
 * Manages employee/expert data:
 * - Employee domain entities
 * - Employee repository (JDBC-based)
 * - Employee service layer
 * <p>
 * Exposes:
 * - EmployeeService (service layer)
 * - EmployeeRepository (repository interface)
 * - ExpertEnrichmentService (service layer)
 */
@org.springframework.modulith.ApplicationModule(
        id = "employee",
        displayName = "Employee Management",
        allowedDependencies = {"core", "workexperience", "technology"}
)
package com.berdachuk.expertmatch.employee;

