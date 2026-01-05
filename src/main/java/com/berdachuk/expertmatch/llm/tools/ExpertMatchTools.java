package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.data.EmployeeRepository;
import com.berdachuk.expertmatch.data.ExpertEnrichmentService;
import com.berdachuk.expertmatch.data.WorkExperienceRepository;
import com.berdachuk.expertmatch.query.QueryParser;
import com.berdachuk.expertmatch.query.QueryRequest;
import com.berdachuk.expertmatch.query.QueryResponse;
import com.berdachuk.expertmatch.query.QueryService;
import com.berdachuk.expertmatch.retrieval.HybridRetrievalService;
import com.berdachuk.expertmatch.security.HeaderBasedUserContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring AI tools for ExpertMatch functionality.
 * Provides tool methods that can be called by LLM for expert discovery.
 */
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

    @Tool(description = "Process natural language query for expert discovery. Use this for general expert search queries.")
    public QueryResponse expertQuery(
            @ToolParam(description = "Natural language query describing expert requirements") String query,
            @ToolParam(description = "Optional chat ID for conversation context (24-character hex string)") String chatId
    ) {
        QueryRequest request = new QueryRequest(query, chatId, null);
        String userId = userContext.getUserIdOrAnonymous();
        return queryService.processQuery(request, chatId, userId);
    }

    @Tool(description = "Find experts matching specific criteria. Use this when you have explicit skill, technology, or seniority requirements.")
    public List<QueryResponse.ExpertMatch> findExperts(
            @ToolParam(description = "List of required skills") List<String> skills,
            @ToolParam(description = "Seniority level (e.g., A3, A4, A5)") String seniority,
            @ToolParam(description = "Required technologies") List<String> technologies,
            @ToolParam(description = "Domain or industry") String domain
    ) {
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
        QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);

        // Create query request
        QueryRequest request = new QueryRequest(query, null, null);

        // Perform retrieval
        HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Enrich experts
        return enrichmentService.enrichExperts(retrievalResult, parsedQuery);
    }

    @Tool(description = "Get detailed profile for a specific expert by ID or name.")
    public QueryResponse.ExpertMatch getExpertProfile(
            @ToolParam(description = "Expert ID (VARCHAR(74))") String expertId,
            @ToolParam(description = "Expert name (alternative to expertId)") String expertName
    ) {
        EmployeeRepository.Employee employee = null;

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
        List<WorkExperienceRepository.WorkExperience> workExperiences =
                workExperienceRepository.findByEmployeeId(employee.id());

        // Build expert match (simplified - would use enrichment service in full implementation)
        QueryResponse.LanguageProficiency language = new QueryResponse.LanguageProficiency(
                employee.languageEnglish()
        );

        // Build relevant projects
        List<QueryResponse.RelevantProject> relevantProjects = workExperiences.stream()
                .limit(5)
                .map(we -> new QueryResponse.RelevantProject(
                        we.projectName(),
                        we.technologies(),
                        we.role(),
                        calculateDuration(we.startDate(), we.endDate())
                ))
                .toList();

        // Build experience
        QueryResponse.Experience experience = buildExperience(workExperiences);

        return new QueryResponse.ExpertMatch(
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
    }

    @Tool(description = "Match project requirements with experts. Use this for RFP responses or team formation.")
    public List<QueryResponse.ExpertMatch> matchProjectRequirements(
            @ToolParam(description = "Project requirements object") Map<String, Object> requirements
    ) {
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
        QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);
        QueryRequest request = new QueryRequest(query, null, null);
        HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Enrich experts
        return enrichmentService.enrichExperts(retrievalResult, parsedQuery);
    }

    @Tool(description = "Get experts who worked on a specific project by project ID or name.")
    public List<QueryResponse.ExpertMatch> getProjectExperts(
            @ToolParam(description = "Project ID (VARCHAR(74))") String projectId,
            @ToolParam(description = "Project name (alternative to projectId)") String projectName
    ) {
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
        QueryParser.ParsedQuery parsedQuery = queryParser.parse(query);
        HybridRetrievalService.RetrievalResult retrievalResult = retrievalService.retrieve(request, parsedQuery);

        // Filter results to only include experts who actually worked on the specified project
        List<QueryResponse.ExpertMatch> allExperts = enrichmentService.enrichExperts(retrievalResult, parsedQuery);

        // Get work experiences for filtering
        List<String> expertIds = allExperts.stream()
                .map(QueryResponse.ExpertMatch::id)
                .toList();

        Map<String, List<WorkExperienceRepository.WorkExperience>> workExperienceMap =
                workExperienceRepository.findByEmployeeIds(expertIds);

        // Filter to only experts who worked on the specified project
        return allExperts.stream()
                .filter(expert -> {
                    List<WorkExperienceRepository.WorkExperience> workExperiences =
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

    private QueryResponse.Experience buildExperience(
            List<WorkExperienceRepository.WorkExperience> workExperiences) {

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

        return new QueryResponse.Experience(
                etlPipelines,
                highPerformanceServices,
                systemArchitecture,
                monitoring,
                onCall
        );
    }
}

