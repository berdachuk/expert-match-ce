package com.berdachuk.expertmatch.data;

import com.berdachuk.expertmatch.query.QueryParser;
import com.berdachuk.expertmatch.query.QueryResponse;
import com.berdachuk.expertmatch.retrieval.HybridRetrievalService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for enriching expert recommendations with detailed data.
 */
@Service
public class ExpertEnrichmentService {

    private final EmployeeRepository employeeRepository;
    private final WorkExperienceRepository workExperienceRepository;

    public ExpertEnrichmentService(
            EmployeeRepository employeeRepository,
            WorkExperienceRepository workExperienceRepository) {
        this.employeeRepository = employeeRepository;
        this.workExperienceRepository = workExperienceRepository;
    }

    /**
     * Enriches retrieval results with expert details.
     */
    public List<QueryResponse.ExpertMatch> enrichExperts(
            HybridRetrievalService.RetrievalResult retrievalResult,
            QueryParser.ParsedQuery parsedQuery) {

        List<String> expertIds = retrievalResult.expertIds();
        Map<String, Double> relevanceScores = retrievalResult.relevanceScores();

        // Fetch employee data
        List<EmployeeRepository.Employee> employees = employeeRepository.findByIds(expertIds);
        Map<String, EmployeeRepository.Employee> employeeMap = employees.stream()
                .collect(Collectors.toMap(EmployeeRepository.Employee::id, e -> e));

        // Fetch work experience
        Map<String, List<WorkExperienceRepository.WorkExperience>> workExperienceMap =
                workExperienceRepository.findByEmployeeIds(expertIds);

        // Build expert matches
        List<QueryResponse.ExpertMatch> expertMatches = new ArrayList<>();

        for (String expertId : expertIds) {
            EmployeeRepository.Employee employee = employeeMap.get(expertId);
            if (employee == null) {
                continue; // Skip if employee not found
            }

            List<WorkExperienceRepository.WorkExperience> workExperiences =
                    workExperienceMap.getOrDefault(expertId, List.of());

            // Calculate skill match
            QueryResponse.SkillMatch skillMatch = calculateSkillMatch(
                    parsedQuery, workExperiences);

            // Build matched skills
            QueryResponse.MatchedSkills matchedSkills = buildMatchedSkills(
                    parsedQuery, workExperiences);

            // Build relevant projects
            List<QueryResponse.RelevantProject> relevantProjects = buildRelevantProjects(
                    workExperiences, parsedQuery);

            // Build experience indicators
            QueryResponse.Experience experience = buildExperience(workExperiences);

            // Build language proficiency
            QueryResponse.LanguageProficiency language = new QueryResponse.LanguageProficiency(
                    employee.languageEnglish()
            );

            Double relevanceScore = relevanceScores.getOrDefault(expertId, 0.0);

            QueryResponse.ExpertMatch expertMatch = new QueryResponse.ExpertMatch(
                    employee.id(),
                    employee.name(),
                    employee.email(),
                    employee.seniority(),
                    language,
                    skillMatch,
                    matchedSkills,
                    relevantProjects,
                    experience,
                    relevanceScore,
                    employee.availabilityStatus()
            );

            expertMatches.add(expertMatch);
        }

        return expertMatches;
    }

    /**
     * Calculates skill match score.
     */
    private QueryResponse.SkillMatch calculateSkillMatch(
            QueryParser.ParsedQuery parsedQuery,
            List<WorkExperienceRepository.WorkExperience> workExperiences) {

        List<String> requiredSkills = new ArrayList<>(parsedQuery.skills());
        requiredSkills.addAll(parsedQuery.technologies());

        // Extract all technologies from work experience
        List<String> expertTechnologies = workExperiences.stream()
                .flatMap(workExperience -> workExperience.technologies().stream())
                .distinct()
                .toList();

        // Count must-have matches
        long mustHaveMatched = requiredSkills.stream()
                .map(String::toLowerCase)
                .filter(skill -> expertTechnologies.stream()
                        .anyMatch(technology -> technology.toLowerCase().contains(skill.toLowerCase()) ||
                                skill.toLowerCase().contains(technology.toLowerCase())))
                .count();

        int mustHaveTotal = requiredSkills.size();

        // For MVP: nice-to-have skills are not parsed from query yet
        // In future, we can parse "nice to have", "preferred", "bonus" keywords
        // For now, nice-to-have is empty
        int niceToHaveTotal = 0;
        int niceToHaveMatched = 0;

        // Calculate match score based on must-have matches
        double matchScore = mustHaveTotal > 0
                ? (double) mustHaveMatched / mustHaveTotal
                : 0.0;

        return new QueryResponse.SkillMatch(
                (int) mustHaveMatched,
                mustHaveTotal,
                niceToHaveMatched,
                niceToHaveTotal,
                matchScore
        );
    }

    /**
     * Builds matched skills breakdown.
     */
    private QueryResponse.MatchedSkills buildMatchedSkills(
            QueryParser.ParsedQuery parsedQuery,
            List<WorkExperienceRepository.WorkExperience> workExperiences) {

        List<String> requiredSkills = new ArrayList<>(parsedQuery.skills());
        requiredSkills.addAll(parsedQuery.technologies());

        List<String> expertTechnologies = workExperiences.stream()
                .flatMap(workExperience -> workExperience.technologies().stream())
                .distinct()
                .toList();

        List<String> mustHave = requiredSkills.stream()
                .filter(skill -> expertTechnologies.stream()
                        .anyMatch(technology -> technology.toLowerCase().contains(skill.toLowerCase()) ||
                                skill.toLowerCase().contains(technology.toLowerCase())))
                .toList();

        return new QueryResponse.MatchedSkills(mustHave, List.of());
    }

    /**
     * Builds relevant projects list.
     */
    private List<QueryResponse.RelevantProject> buildRelevantProjects(
            List<WorkExperienceRepository.WorkExperience> workExperiences,
            QueryParser.ParsedQuery parsedQuery) {

        // Filter and sort by relevance
        return workExperiences.stream()
                .filter(workExperience -> isRelevant(workExperience, parsedQuery))
                .sorted((a, b) -> {
                    // Sort by recency (most recent first)
                    Instant aEnd = a.endDate() != null ? a.endDate() : Instant.now();
                    Instant bEnd = b.endDate() != null ? b.endDate() : Instant.now();
                    return bEnd.compareTo(aEnd);
                })
                .limit(5) // Top 5 most relevant projects
                .map(workExperience -> {
                    String duration = calculateDuration(workExperience.startDate(), workExperience.endDate());
                    return new QueryResponse.RelevantProject(
                            workExperience.projectName(),
                            workExperience.technologies(),
                            workExperience.role(),
                            duration
                    );
                })
                .toList();
    }

    /**
     * Checks if work experience is relevant to query.
     */
    private boolean isRelevant(
            WorkExperienceRepository.WorkExperience workExperience,
            QueryParser.ParsedQuery parsedQuery) {

        List<String> queryTechnologies = new ArrayList<>(parsedQuery.technologies());
        queryTechnologies.addAll(parsedQuery.skills());

        if (queryTechnologies.isEmpty()) {
            return true; // If no specific tech requirements, all are relevant
        }

        // Check if any query technology matches work experience technologies
        return queryTechnologies.stream()
                .anyMatch(queryTechnology -> workExperience.technologies().stream()
                        .anyMatch(technology -> technology.toLowerCase().contains(queryTechnology.toLowerCase()) ||
                                queryTechnology.toLowerCase().contains(technology.toLowerCase())));
    }

    /**
     * Calculates project duration string.
     */
    private String calculateDuration(Instant start, Instant end) {
        if (start == null) {
            return "Unknown";
        }

        Instant endDate = end != null ? end : Instant.now();
        Duration duration = Duration.between(start, endDate);

        long months = duration.toDays() / 30;
        if (months < 1) {
            return "< 1 month";
        } else if (months < 12) {
            return months + " months";
        } else {
            long years = months / 12;
            long remainingMonths = months % 12;
            if (remainingMonths == 0) {
                return years + (years == 1 ? " year" : " years");
            } else {
                return years + (years == 1 ? " year" : " years") + " " + remainingMonths + " months";
            }
        }
    }

    /**
     * Builds experience indicators.
     */
    private QueryResponse.Experience buildExperience(
            List<WorkExperienceRepository.WorkExperience> workExperiences) {

        boolean etlPipelines = workExperiences.stream()
                .anyMatch(workExperience -> workExperience.projectSummary() != null &&
                        workExperience.projectSummary().toLowerCase().contains("etl"));

        boolean highPerformanceServices = workExperiences.stream()
                .anyMatch(workExperience -> workExperience.projectSummary() != null &&
                        (workExperience.projectSummary().toLowerCase().contains("high performance") ||
                                workExperience.projectSummary().toLowerCase().contains("low latency")));

        boolean systemArchitecture = workExperiences.stream()
                .anyMatch(workExperience -> workExperience.role() != null &&
                        (workExperience.role().toLowerCase().contains("architect") ||
                                workExperience.role().toLowerCase().contains("lead")));

        boolean monitoring = workExperiences.stream()
                .anyMatch(workExperience -> workExperience.technologies().stream()
                        .anyMatch(technology -> technology.toLowerCase().contains("monitoring") ||
                                technology.toLowerCase().contains("prometheus") ||
                                technology.toLowerCase().contains("grafana")));

        boolean onCall = workExperiences.stream()
                .anyMatch(workExperience -> workExperience.responsibilities() != null &&
                        workExperience.responsibilities().toLowerCase().contains("on-call"));

        return new QueryResponse.Experience(
                etlPipelines,
                highPerformanceServices,
                systemArchitecture,
                monitoring,
                onCall
        );
    }
}

