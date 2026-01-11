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
 */
@org.springframework.modulith.ApplicationModule(
    id = "employee",
    displayName = "Employee Management",
    allowedDependencies = {"core"}
)
package com.berdachuk.expertmatch.employee;

import org.springframework.modulith.ApplicationModule;
