package com.berdachuk.expertmatch.ingestion.service.impl;

import com.berdachuk.expertmatch.api.model.TestDataStatsResponse;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.ingestion.service.TestDataStatisticsService;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.technology.repository.TechnologyRepository;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import org.springframework.stereotype.Service;

/**
 * Aggregates current test data counts from repositories.
 */
@Service
public class TestDataStatisticsServiceImpl implements TestDataStatisticsService {

    private final EmployeeRepository employeeRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;

    public TestDataStatisticsServiceImpl(
            EmployeeRepository employeeRepository,
            WorkExperienceRepository workExperienceRepository,
            ProjectRepository projectRepository,
            TechnologyRepository technologyRepository) {
        this.employeeRepository = employeeRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.projectRepository = projectRepository;
        this.technologyRepository = technologyRepository;
    }

    @Override
    public TestDataStatsResponse getStatistics() {
        long employees = employeeRepository.count();
        long workExperiences = workExperienceRepository.count();
        long projects = projectRepository.count();
        long technologies = technologyRepository.count();
        return new TestDataStatsResponse(
                (int) employees,
                (int) workExperiences,
                (int) projects,
                (int) technologies
        );
    }
}
