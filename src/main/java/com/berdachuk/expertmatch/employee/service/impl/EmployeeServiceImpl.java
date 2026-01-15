package com.berdachuk.expertmatch.employee.service.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for employee/expert operations.
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    public Optional<Employee> findById(String employeeId) {
        return employeeRepository.findById(employeeId);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    @Override
    public List<Employee> findByIds(List<String> employeeIds) {
        return employeeRepository.findByIds(employeeIds);
    }

    @Override
    public List<String> findEmployeeIdsByName(String name, int maxResults) {
        return employeeRepository.findEmployeeIdsByName(name, maxResults);
    }

    @Override
    public List<String> findEmployeeIdsByNameSimilarity(String name, double similarityThreshold, int maxResults) {
        return employeeRepository.findEmployeeIdsByNameSimilarity(name, similarityThreshold, maxResults);
    }
}
