package com.berdachuk.expertmatch.employee.service.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.service.ExpertEnrichmentService;
import com.berdachuk.expertmatch.query.domain.QueryParser;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService;
import com.berdachuk.expertmatch.technology.domain.Technology;
import com.berdachuk.expertmatch.technology.repository.TechnologyRepository;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for enriching expert recommendations with detailed data.
 */
@Slf4j
@Service
public class ExpertEnrichmentServiceImpl implements ExpertEnrichmentService {

    private final EmployeeRepository employeeRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final TechnologyRepository technologyRepository;

    // Cache for technology normalization (loaded once per service instance)
    private volatile Map<String, Technology> technologyCache;
    private volatile Map<String, Set<String>> synonymToTechnologyMap;

    public ExpertEnrichmentServiceImpl(
            EmployeeRepository employeeRepository,
            WorkExperienceRepository workExperienceRepository,
            TechnologyRepository technologyRepository) {
        this.employeeRepository = employeeRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.technologyRepository = technologyRepository;
    }

    /**
     * Enriches retrieval results with expert details.
     */
    @Override
    public List<QueryResponse.ExpertMatch> enrichExperts(
            HybridRetrievalService.RetrievalResult retrievalResult,
            QueryParser.ParsedQuery parsedQuery) {

        List<String> expertIds = retrievalResult.expertIds();
        Map<String, Double> relevanceScores = retrievalResult.relevanceScores();

        // Fetch employee data
        List<Employee> employees = employeeRepository.findByIds(expertIds);
        Map<String, Employee> employeeMap = employees.stream()
                .collect(Collectors.toMap(Employee::id, e -> e));

        // Fetch work experience
        Map<String, List<WorkExperience>> workExperienceMap =
                workExperienceRepository.findByEmployeeIds(expertIds);

        // Build expert matches
        List<QueryResponse.ExpertMatch> expertMatches = new ArrayList<>();

        for (String expertId : expertIds) {
            com.berdachuk.expertmatch.employee.domain.Employee employee = employeeMap.get(expertId);
            if (employee == null) {
                continue; // Skip if employee not found
            }

            List<WorkExperience> workExperiences =
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
     * Calculates skill match score using Technology normalization and synonyms.
     */
    private QueryResponse.SkillMatch calculateSkillMatch(
            QueryParser.ParsedQuery parsedQuery,
            List<WorkExperience> workExperiences) {

        List<String> requiredSkills = new ArrayList<>(parsedQuery.skills());
        requiredSkills.addAll(parsedQuery.technologies());

        // Extract all technologies from work experience and normalize them
        Set<String> expertTechnologies = workExperiences.stream()
                .flatMap(workExperience -> workExperience.technologies().stream())
                .map(this::normalizeTechnologyName)
                .collect(Collectors.toSet());

        // Normalize required skills and match against expert technologies
        long mustHaveMatched = requiredSkills.stream()
                .map(this::normalizeSkillName)
                .filter(normalizedSkill -> expertTechnologies.stream()
                        .anyMatch(expertTech -> matchesTechnology(normalizedSkill, expertTech)))
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
     * Builds matched skills breakdown using Technology normalization and synonyms.
     */
    private QueryResponse.MatchedSkills buildMatchedSkills(
            QueryParser.ParsedQuery parsedQuery,
            List<WorkExperience> workExperiences) {

        List<String> requiredSkills = new ArrayList<>(parsedQuery.skills());
        requiredSkills.addAll(parsedQuery.technologies());

        // Extract and normalize expert technologies
        Set<String> expertTechnologies = workExperiences.stream()
                .flatMap(workExperience -> workExperience.technologies().stream())
                .map(this::normalizeTechnologyName)
                .collect(Collectors.toSet());

        // Match required skills against expert technologies using normalization
        List<String> mustHave = requiredSkills.stream()
                .filter(skill -> {
                    String normalizedSkill = normalizeSkillName(skill);
                    return expertTechnologies.stream()
                            .anyMatch(expertTech -> matchesTechnology(normalizedSkill, expertTech));
                })
                .collect(Collectors.toList());

        return new QueryResponse.MatchedSkills(mustHave, List.of());
    }

    /**
     * Builds relevant projects list.
     */
    private List<QueryResponse.RelevantProject> buildRelevantProjects(
            List<WorkExperience> workExperiences,
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
     * Checks if work experience is relevant to query using Technology normalization.
     */
    private boolean isRelevant(
            WorkExperience workExperience,
            QueryParser.ParsedQuery parsedQuery) {

        List<String> queryTechnologies = new ArrayList<>(parsedQuery.technologies());
        queryTechnologies.addAll(parsedQuery.skills());

        if (queryTechnologies.isEmpty()) {
            return true; // If no specific tech requirements, all are relevant
        }

        // Normalize work experience technologies
        Set<String> normalizedWorkTechs = workExperience.technologies().stream()
                .map(this::normalizeTechnologyName)
                .collect(Collectors.toSet());

        // Check if any query technology matches work experience technologies using normalization
        return queryTechnologies.stream()
                .anyMatch(queryTechnology -> {
                    String normalizedQuery = normalizeSkillName(queryTechnology);
                    return normalizedWorkTechs.stream()
                            .anyMatch(workTech -> matchesTechnology(normalizedQuery, workTech));
                });
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
            List<WorkExperience> workExperiences) {

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

    /**
     * Normalizes a skill name using Technology table normalization and synonyms.
     * Returns the normalized name if found in Technology table, otherwise returns lowercase version.
     */
    private String normalizeSkillName(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            return "";
        }

        // Load technology cache if not already loaded
        ensureTechnologyCacheLoaded();

        String lowerSkill = skillName.toLowerCase().trim();

        // Try to find by exact name match
        Technology technology = technologyCache.get(lowerSkill);
        if (technology != null) {
            return technology.normalizedName().toLowerCase();
        }

        // Try to find by normalized name match
        for (Technology tech : technologyCache.values()) {
            if (tech.normalizedName().toLowerCase().equals(lowerSkill)) {
                return tech.normalizedName().toLowerCase();
            }
        }

        // Try to find by synonym match
        Set<String> matchingTechs = synonymToTechnologyMap.get(lowerSkill);
        if (matchingTechs != null && !matchingTechs.isEmpty()) {
            // Return the normalized name of the first matching technology
            String firstMatch = matchingTechs.iterator().next();
            Technology matchedTech = technologyCache.get(firstMatch);
            if (matchedTech != null) {
                return matchedTech.normalizedName().toLowerCase();
            }
        }

        // Fallback: return lowercase version if no match found
        return lowerSkill;
    }

    /**
     * Normalizes a technology name using Technology table.
     * Returns the normalized name if found, otherwise returns lowercase version.
     */
    private String normalizeTechnologyName(String technologyName) {
        if (technologyName == null || technologyName.isBlank()) {
            return "";
        }

        // Load technology cache if not already loaded
        ensureTechnologyCacheLoaded();

        String lowerTech = technologyName.toLowerCase().trim();

        // Try to find by exact name match
        Technology technology = technologyCache.get(lowerTech);
        if (technology != null) {
            return technology.normalizedName().toLowerCase();
        }

        // Try to find by normalized name match
        for (Technology tech : technologyCache.values()) {
            if (tech.normalizedName().toLowerCase().equals(lowerTech)) {
                return tech.normalizedName().toLowerCase();
            }
        }

        // Try to find by synonym match
        Set<String> matchingTechs = synonymToTechnologyMap.get(lowerTech);
        if (matchingTechs != null && !matchingTechs.isEmpty()) {
            // Return the normalized name of the first matching technology
            String firstMatch = matchingTechs.iterator().next();
            Technology matchedTech = technologyCache.get(firstMatch);
            if (matchedTech != null) {
                return matchedTech.normalizedName().toLowerCase();
            }
        }

        // Fallback: return lowercase version if no match found
        return lowerTech;
    }

    /**
     * Checks if a normalized skill name matches a normalized technology name.
     * Uses exact match on normalized names, or checks if skill is a synonym of technology.
     */
    private boolean matchesTechnology(String normalizedSkill, String normalizedTechnology) {
        if (normalizedSkill == null || normalizedTechnology == null) {
            return false;
        }

        // Exact match on normalized names
        if (normalizedSkill.equals(normalizedTechnology)) {
            return true;
        }

        // Check if skill contains technology or vice versa (for partial matches)
        if (normalizedSkill.contains(normalizedTechnology) || normalizedTechnology.contains(normalizedSkill)) {
            return true;
        }

        // Check if skill matches any synonym of the technology
        ensureTechnologyCacheLoaded();
        for (Technology tech : technologyCache.values()) {
            if (tech.normalizedName().toLowerCase().equals(normalizedTechnology)) {
                // Check if normalized skill matches any synonym
                if (tech.synonyms() != null) {
                    for (String synonym : tech.synonyms()) {
                        if (synonym.toLowerCase().equals(normalizedSkill)) {
                            return true;
                        }
                    }
                }
                break;
            }
        }

        return false;
    }

    /**
     * Ensures technology cache is loaded (lazy initialization).
     */
    private void ensureTechnologyCacheLoaded() {
        if (technologyCache == null) {
            synchronized (this) {
                if (technologyCache == null) {
                    loadTechnologyCache();
                }
            }
        }
    }

    /**
     * Loads all technologies into cache for efficient lookup.
     */
    private void loadTechnologyCache() {
        try {
            List<Technology> technologies = technologyRepository.findAll();
            Map<String, Technology> cache = new HashMap<>();
            Map<String, Set<String>> synonymMap = new HashMap<>();

            for (Technology tech : technologies) {
                // Index by lowercase name
                cache.put(tech.name().toLowerCase(), tech);

                // Index by normalized name
                cache.put(tech.normalizedName().toLowerCase(), tech);

                // Index synonyms
                if (tech.synonyms() != null) {
                    for (String synonym : tech.synonyms()) {
                        String lowerSynonym = synonym.toLowerCase();
                        synonymMap.computeIfAbsent(lowerSynonym, k -> new HashSet<>()).add(tech.name().toLowerCase());
                    }
                }
            }

            this.technologyCache = cache;
            this.synonymToTechnologyMap = synonymMap;

            log.debug("Loaded {} technologies into cache with {} synonym mappings",
                    technologies.size(), synonymMap.size());
        } catch (Exception e) {
            log.warn("Failed to load technology cache, falling back to simple matching: {}", e.getMessage());
            // Initialize empty caches to prevent repeated failures
            this.technologyCache = new HashMap<>();
            this.synonymToTechnologyMap = new HashMap<>();
        }
    }
}

