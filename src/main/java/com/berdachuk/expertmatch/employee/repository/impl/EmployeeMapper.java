package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Row mapper for Employee entity.
 * Maps ResultSet rows to Employee records.
 * <p>
 * This mapper is reusable across all EmployeeRepository methods,
 * providing consistent mapping logic and easier maintenance.
 * <p>
 * Benefits:
 * - Centralized mapping logic
 * - Reusable across multiple repository methods
 * - Easier to test independently
 * - Consistent with WCA-Backend patterns
 */
@Component
public class EmployeeMapper implements RowMapper<Employee> {

    @Override
    public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Employee(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("seniority"),
                rs.getString("language_english"),
                rs.getString("availability_status")
        );
    }
}
