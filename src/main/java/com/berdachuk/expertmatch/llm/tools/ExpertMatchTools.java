package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.service.ExpertEnrichmentService;
import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import com.berdachuk.expertmatch.query.domain.QueryParser;
import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.query.service.ExpertContextHolder;
import com.berdachuk.expertmatch.query.service.QueryService;
import com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI tools for ExpertMatch functionality.
 * Provides tool methods that can be called by LLM for expert discovery.
 */
@Slf4j
@Component
public class ExpertMatchTools {

    private final QueryService queryService;
    private final HybridRetrievalService retrievalService;
    private final ExpertEnrichmentService enrichmentService;
    private final EmployeeRepository employeeRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final QueryParser queryParser;
    private final HeaderBasedUserContext userContext;

    public ExpertMatchTools(
            @Lazy QueryService queryService,
            HybridRetrievalService retrievalService,
            ExpertEnrichmentService enrichmentService,
            EmployeeRepository employeeRepository,
            WorkExperienceRepository workExperienceRepository,
            @Lazy QueryParser queryParser,
            HeaderBasedUserContext userContext
    ) {
        this.queryService = queryService;
        this.retrievalService = retrievalService;
        this.enrichmentService = enrichmentService;
        this.employeeRepository = employeeRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.queryParser = queryParser;
        this.userContext = userContext;
    }

    @Tool(description = "Get the experts that were already retrieved for the current query. Use this to access expert information that was found by the retrieval system. Call this FIRST before generating your answer.")
    public List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> getRetrievedExperts() {
        List<AnswerGenerationService.ExpertContext> contexts = ExpertContextHolder.get();
        if (contexts == null || contexts.isEmpty()) {
            log.warn("⚠️  getRetrievedExperts() called but no expert contexts found in ExpertContextHolder");
            return List.of();
        }
        log.info("✅ getRetrievedExperts() called - converting {} expert contexts to ExpertMatch format", contexts.size());
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = convertContextsToExpertMatches(contexts);
        log.info("✅ getRetrievedExperts() returning {} experts", result.size());
        return result;
    }

    @Tool(description = "Process natural language query for expert discovery. Use this for general expert search queries. WARNING: Only use this if you need to search for DIFFERENT experts than those already retrieved.")
    public QueryResponse expertQuery(
            @ToolParam(description = "Natural language query describing expert requirements") String query,
            @ToolParam(description = "Optional chat ID for conversation context (24-character hex string)") String chatId
    ) {
        log.info("✅ expertQuery() tool called with query: {}", query);
        // Prevent recursion: if we're already processing a query, don't call QueryService again
        if (ExpertContextHolder.hasContexts()) {
            log.warn("⚠️  expertQuery tool called while already processing a query - preventing recursion");
            throw new IllegalStateException("Cannot call expertQuery tool while already processing a query. Use getRetrievedExperts() to access already-retrieved experts.");
        }
        QueryRequest request = new QueryRequest(query, chatId, null);
        String userId = userContext.getUserIdOrAnonymous();
        log.info("✅ expertQuery() executing query via QueryService");
        return queryService.processQuery(request, chatId, userId);
    }

    @Tool(description = "Find experts matching specific criteria. Use this when you have explicit skill, technology, or seniority requirements.")
    public List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> findExperts(
            @ToolParam(description = "List of required skills") List<String> skills,
            @ToolParam(description = "Seniority level (e.g., A3, A4, A5)") String seniority,
            @ToolParam(description = "Required technologies") List<String> technologies,
            @ToolParam(description = "Domain or industry") String domain
    ) {
        log.info("✅ findExperts() tool called - skills: {}, seniority: {}, technologies: {}, domain: {}",
                skills, seniority, technologies, domain);
        // Build query from criteria
        StringBuilder queryBuilder = new StringBuilder();
        if (skills != null && !skills.isEmpty()) {
            queryBuilder.append("Skills: ").append(String.join(", ", skills)).append(". ");
        }
        if (technologies != null && !technologies.isEmpty()) {
            queryBuilder.append("Technologies: ").append(String.join(", ", technologies)).append(". ");
        }
        if (seniority != null && !seniority.isBlank()) {
            queryBuilder.append("Seniority: ").append(seniority).append(". ");
        }
        if (domain != null && !domain.isBlank()) {
            queryBuilder.append("Domain: ").append(domain).append(". ");
        }

        String query = queryBuilder.toString().trim();
        if (query.isEmpty()) {
            return List.of();
        }

        // Parse query
        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);

        // Create query request
        QueryRequest request = new QueryRequest(query, null, null);

        // Perform retrieval
        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Enrich experts
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = enrichmentService.enrichExperts(retrievalResult, parsedQuery);
        log.info("✅ findExperts() returning {} experts", result.size());
        return result;
    }

    @Tool(description = "Get detailed profile for a specific expert by ID or name.")
    public com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch getExpertProfile(
            @ToolParam(description = "Expert ID (VARCHAR(74))") String expertId,
            @ToolParam(description = "Expert name (alternative to expertId)") String expertName
    ) {
        log.info("✅ getExpertProfile() tool called - expertId: {}, expertName: {}", expertId, expertName);
        com.berdachuk.expertmatch.employee.domain.Employee employee = null;

        if (expertId != null && !expertId.isBlank()) {
            employee = employeeRepository.findById(expertId).orElse(null);
        } else if (expertName != null && !expertName.isBlank()) {
            // Try to find by name (simplified - would need name search in real implementation)
            // For now, return null if name provided but no ID
            return null;
        }

        if (employee == null) {
            return null;
        }

        // Get work experience
        List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience> workExperiences =
                workExperienceRepository.findByEmployeeId(employee.id());

        // Build expert match (simplified - would use enrichment service in full implementation)
        com.berdachuk.expertmatch.query.domain.QueryResponse.LanguageProficiency language = new com.berdachuk.expertmatch.query.domain.QueryResponse.LanguageProficiency(
                employee.languageEnglish()
        );

        // Build relevant projects
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject> relevantProjects = workExperiences.stream()
                .limit(5)
                .map(we -> new com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject(
                        we.projectName(),
                        we.technologies(),
                        we.role(),
                        calculateDuration(we.startDate(), we.endDate())
                ))
                .toList();

        // Build experience
        com.berdachuk.expertmatch.query.domain.QueryResponse.Experience experience = buildExperience(workExperiences);

        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch result = new com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch(
                employee.id(),
                employee.name(),
                employee.email(),
                employee.seniority(),
                language,
                null, // skillMatch
                null, // matchedSkills
                relevantProjects,
                experience,
                1.0, // relevanceScore
                employee.availabilityStatus()
        );
        log.info("✅ getExpertProfile() returning profile for: {}", result.name());
        return result;
    }

    @Tool(description = "Match project requirements with experts. Use this for RFP responses or team formation.")
    public List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> matchProjectRequirements(
            @ToolParam(description = "Project requirements object") Map<String, Object> requirements
    ) {
        log.info("✅ matchProjectRequirements() tool called with requirements: {}", requirements);
        // Extract requirements
        @SuppressWarnings("unchecked")
        List<String> skills = (List<String>) requirements.getOrDefault("skills", List.of());
        @SuppressWarnings("unchecked")
        List<String> technicalStack = (List<String>) requirements.getOrDefault("technical_stack", List.of());
        String seniority = (String) requirements.getOrDefault("seniority", null);
        @SuppressWarnings("unchecked")
        List<String> responsibilities = (List<String>) requirements.getOrDefault("responsibilities", List.of());

        // Combine skills and technical stack
        List<String> allTechnologies = new ArrayList<>();
        if (skills != null) allTechnologies.addAll(skills);
        if (technicalStack != null) allTechnologies.addAll(technicalStack);

        // Build query
        StringBuilder queryBuilder = new StringBuilder();
        if (!allTechnologies.isEmpty()) {
            queryBuilder.append("Technologies: ").append(String.join(", ", allTechnologies)).append(". ");
        }
        if (seniority != null && !seniority.isBlank()) {
            queryBuilder.append("Seniority: ").append(seniority).append(". ");
        }
        if (responsibilities != null && !responsibilities.isEmpty()) {
            queryBuilder.append("Responsibilities: ").append(String.join(", ", responsibilities)).append(". ");
        }

        String query = queryBuilder.toString().trim();
        if (query.isEmpty()) {
            return List.of();
        }

        // Parse and retrieve
        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);
        QueryRequest request = new QueryRequest(query, null, null);
        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Enrich experts
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = enrichmentService.enrichExperts(retrievalResult, parsedQuery);
        log.info("✅ matchProjectRequirements() returning {} experts", result.size());
        return result;
    }

    @Tool(description = "Get experts who worked on a specific project by project ID or name.")
    public List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> getProjectExperts(
            @ToolParam(description = "Project ID (VARCHAR(74))") String projectId,
            @ToolParam(description = "Project name (alternative to projectId)") String projectName
    ) {
        log.info("✅ getProjectExperts() tool called - projectId: {}, projectName: {}", projectId, projectName);
        // For now, use a simplified approach - query all work experiences and filter
        // In a production system, we'd add a repository method to query by project_id or project_name
        // This is a placeholder implementation
        if ((projectId == null || projectId.isBlank()) && (projectName == null || projectName.isBlank())) {
            return List.of();
        }

        // Build a query to find experts
        String query;
        if (projectId != null && !projectId.isBlank()) {
            query = "Experts who worked on project with ID: " + projectId;
        } else {
            query = "Experts who worked on project: " + projectName;
        }

        // Use the general query service to find experts
        QueryRequest request = new QueryRequest(query, null, null);
        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);
        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Filter results to only include experts who actually worked on the specified project
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> allExperts = enrichmentService.enrichExperts(retrievalResult, parsedQuery);

        // Get work experiences for filtering
        List<String> expertIds = allExperts.stream()
                .map(com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch::id)
                .toList();

        Map<String, List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience>> workExperienceMap =
                workExperienceRepository.findByEmployeeIds(expertIds);

        // Filter to only experts who worked on the specified project
        return allExperts.stream()
                .filter(expert -> {
                    List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience> workExperiences =
                            workExperienceMap.getOrDefault(expert.id(), List.of());

                    if (projectId != null && !projectId.isBlank()) {
                        return workExperiences.stream()
                                .anyMatch(we -> projectId.equals(we.projectId()));
                    } else {
                        return workExperiences.stream()
                                .anyMatch(we -> projectName != null && projectName.equalsIgnoreCase(we.projectName()));
                    }
                })
                .toList();
    }

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

    private com.berdachuk.expertmatch.query.domain.QueryResponse.Experience buildExperience(
            List<com.berdachuk.expertmatch.workexperience.domain.WorkExperience> workExperiences) {

        boolean etlPipelines = workExperiences.stream()
                .anyMatch(we -> we.projectSummary() != null &&
                        we.projectSummary().toLowerCase().contains("etl"));

        boolean highPerformanceServices = workExperiences.stream()
                .anyMatch(we -> we.projectSummary() != null &&
                        (we.projectSummary().toLowerCase().contains("high performance") ||
                                we.projectSummary().toLowerCase().contains("low latency")));

        boolean systemArchitecture = workExperiences.stream()
                .anyMatch(we -> we.role() != null &&
                        (we.role().toLowerCase().contains("architect") ||
                                we.role().toLowerCase().contains("lead")));

        boolean monitoring = workExperiences.stream()
                .anyMatch(we -> we.technologies().stream()
                        .anyMatch(tech -> tech.toLowerCase().contains("monitoring") ||
                                tech.toLowerCase().contains("prometheus") ||
                                tech.toLowerCase().contains("grafana")));

        boolean onCall = workExperiences.stream()
                .anyMatch(we -> we.responsibilities() != null &&
                        we.responsibilities().toLowerCase().contains("on-call"));

        return new com.berdachuk.expertmatch.query.domain.QueryResponse.Experience(
                etlPipelines,
                highPerformanceServices,
                systemArchitecture,
                monitoring,
                onCall
        );
    }

    /**
     * Converts ExpertContext objects to ExpertMatch format.
     * This is used by getRetrievedExperts() tool to return expert data.
     */
    private List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> convertContextsToExpertMatches(
            List<AnswerGenerationService.ExpertContext> contexts) {
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> expertMatches = new ArrayList<>();

        for (AnswerGenerationService.ExpertContext context : contexts) {
            // Extract metadata
            Map<String, Object> metadata = context.metadata() != null ? context.metadata() : new HashMap<>();

            // Build matched skills from context skills
            List<String> skills = context.skills() != null ? context.skills() : List.of();
            com.berdachuk.expertmatch.query.domain.QueryResponse.MatchedSkills matchedSkills =
                    new com.berdachuk.expertmatch.query.domain.QueryResponse.MatchedSkills(
                            skills, // All skills as must-have for simplicity
                            List.of() // No nice-to-have distinction in ExpertContext
                    );

            // Build skill match score from metadata
            Double matchScore = metadata.containsKey("matchScore")
                    ? ((Number) metadata.get("matchScore")).doubleValue()
                    : null;
            com.berdachuk.expertmatch.query.domain.QueryResponse.SkillMatch skillMatch = null;
            if (matchScore != null) {
                skillMatch = new com.berdachuk.expertmatch.query.domain.QueryResponse.SkillMatch(
                        skills.size(), // mustHaveMatched
                        skills.size(), // mustHaveTotal
                        0, // niceToHaveMatched
                        0, // niceToHaveTotal
                        matchScore
                );
            }

            // Build relevant projects from context projects
            List<com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject> relevantProjects =
                    context.projects() != null
                            ? context.projects().stream()
                            .map(projectName -> new com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject(
                                    projectName,
                                    List.of(), // Technologies not available in ExpertContext
                                    null, // Role not available
                                    null // Duration not available
                            ))
                            .toList()
                            : List.of();

            // Get relevance score from metadata
            Double relevanceScore = metadata.containsKey("relevanceScore")
                    ? ((Number) metadata.get("relevanceScore")).doubleValue()
                    : (matchScore != null ? matchScore : 0.5);

            // Build language proficiency (default to English if not available)
            com.berdachuk.expertmatch.query.domain.QueryResponse.LanguageProficiency language =
                    new com.berdachuk.expertmatch.query.domain.QueryResponse.LanguageProficiency("English");

            // Build experience (minimal - not available in ExpertContext)
            com.berdachuk.expertmatch.query.domain.QueryResponse.Experience experience =
                    new com.berdachuk.expertmatch.query.domain.QueryResponse.Experience(
                            false, false, false, false, false
                    );

            // Create ExpertMatch
            com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expertMatch =
                    new com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch(
                            context.expertId(),
                            context.name(),
                            context.email(),
                            context.seniority(),
                            language,
                            skillMatch,
                            matchedSkills,
                            relevantProjects,
                            experience,
                            relevanceScore,
                            null // Availability not available in ExpertContext
                    );

            expertMatches.add(expertMatch);
        }

        return expertMatches;
    }
}

