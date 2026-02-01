package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.berdachuk.expertmatch.ingestion.model.ProjectData;
import com.berdachuk.expertmatch.project.domain.Project;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for processing a single EmployeeProfile into the database.
 * Handles partial data, applies defaults, and creates employee and work experience records.
 */
@Slf4j
@Component
public class ProfileProcessor {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final ObjectMapper objectMapper;

    public ProfileProcessor(
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository,
            WorkExperienceRepository workExperienceRepository,
            ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a single employee profile into the database.
     * Applies default values for missing optional fields.
     * Creates employee record and work experience records for each project.
     *
     * @param profile          EmployeeProfile to process
     * @param existingProjects Map of existing project IDs to project names (for project lookup)
     * @return ProcessingResult with success status and details
     */
    public ProcessingResult processProfile(EmployeeProfile profile, Map<String, String> existingProjects) {
        return processProfile(profile, existingProjects, true);
    }

    /**
     * Processes a single employee profile into the database.
     * Optionally applies default values for missing optional fields.
     * Creates employee record and work experience records for each project.
     *
     * @param profile          EmployeeProfile to process
     * @param existingProjects Map of existing project IDs to project names (for project lookup)
     * @param applyDefaults    If true, applies default values for missing optional fields. If false, uses only real data.
     * @return ProcessingResult with success status and details
     */
    public ProcessingResult processProfile(EmployeeProfile profile, Map<String, String> existingProjects, boolean applyDefaults) {
        // Validate profile
        if (!profile.isValid()) {
            String errorMsg = "Profile is invalid: missing required employee fields (id or name)";
            log.warn("Failed to process profile: {}", errorMsg);
            return ProcessingResult.failure(
                    profile.employee() != null ? profile.employee().id() : "unknown",
                    profile.employee() != null ? profile.employee().name() : "unknown",
                    errorMsg
            );
        }

        // Apply defaults only if requested (for test data generation), otherwise use only real data
        var employee = applyDefaults ? profile.employee().withDefaults() : profile.employee();
        String employeeId = employee.id();
        String employeeName = employee.name();

        try {
            // Insert or update employee
            Employee employeeEntity = new Employee(
                    employeeId,
                    employeeName,
                    employee.email(),
                    employee.seniority(),
                    employee.languageEnglish(),
                    employee.availabilityStatus()
            );

            employeeRepository.createOrUpdate(employeeEntity);
            log.debug("Created/updated employee: {} ({})", employeeName, employeeId);

            // Process projects
            int projectsProcessed = 0;
            int projectsSkipped = 0;
            List<String> projectErrors = new ArrayList<>();

            if (profile.projects() != null && !profile.projects().isEmpty()) {
                for (ProjectData projectData : profile.projects()) {
                    try {
                        if (!projectData.isValid()) {
                            String error = String.format("Project '%s' is invalid: missing required fields",
                                    projectData.projectName() != null ? projectData.projectName() : "unknown");
                            log.warn("Skipping invalid project: {}", error);
                            projectsSkipped++;
                            projectErrors.add(error);
                            continue;
                        }

                        // Apply defaults to project data only if requested (for test data generation)
                        var project = applyDefaults ? projectData.withDefaults() : projectData;
                        processProject(employeeId, project, existingProjects);
                        projectsProcessed++;
                    } catch (Exception e) {
                        String error = String.format("Failed to process project '%s': %s",
                                projectData.projectName() != null ? projectData.projectName() : "unknown",
                                e.getMessage());
                        log.warn("Error processing project: {}", error, e);
                        projectsSkipped++;
                        projectErrors.add(error);
                    }
                }
            }

            return ProcessingResult.success(employeeId, employeeName,
                    projectsProcessed, projectsSkipped, projectErrors);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to process employee profile: %s", e.getMessage());
            log.error("Error processing profile for employee {}: {}", employeeName, errorMsg, e);
            return ProcessingResult.failure(employeeId, employeeName, errorMsg);
        }
    }

    /**
     * Processes a single project and creates or overwrites work experience record.
     * When ingesting from Kafka history, existing records are updated so new data overwrites old.
     */
    private void processProject(String employeeId, ProjectData project, Map<String, String> existingProjects) {
        LocalDate startDate = LocalDate.parse(project.startDate());
        String workExpId = workExperienceRepository
                .findIdByEmployeeIdAndProjectNameAndStartDate(employeeId, project.projectName(), startDate)
                .orElse(IdGenerator.generateId());

        // Find or create project
        String projectId = findOrCreateProject(project, existingProjects);

        // Build metadata
        String metadataJson = buildMetadataJson(project);

        // Convert LocalDate to Instant for WorkExperience domain entity (endDate null = ongoing, use startDate)
        LocalDate endDate = project.endDate() != null && !project.endDate().isBlank()
                ? LocalDate.parse(project.endDate())
                : startDate;
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        WorkExperience workExperience = new WorkExperience(
                workExpId,
                employeeId,
                projectId,
                project.customerId(),
                project.projectName(),
                project.customerName(),
                project.industry(),
                project.role(),
                startInstant,
                endInstant,
                project.projectSummary(),
                project.responsibilities(),
                project.technologies()
        );

        workExperienceRepository.createOrUpdate(workExperience, metadataJson);
        log.debug("Created/updated work experience for employee {} at project {}", employeeId, project.projectName());
    }

    /**
     * Finds existing project or creates a new one.
     */
    private String findOrCreateProject(ProjectData project, Map<String, String> existingProjects) {
        // Try to find existing project by name in the map first
        String projectId = existingProjects.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase().contains(
                        project.projectName().toLowerCase().substring(0, Math.min(10, project.projectName().length()))))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (projectId != null && existingProjects.containsKey(projectId)) {
            return projectId;
        }

        // Try to find in database using repository
        Optional<String> existingProjectId = projectRepository.findIdByName(project.projectName());
        if (existingProjectId.isPresent()) {
            projectId = existingProjectId.get();
            existingProjects.put(projectId, project.projectName());
            return projectId;
        }

        // Create new project
        projectId = IdGenerator.generateProjectId();
        Project projectEntity = new Project(
                projectId,
                project.projectName(),
                project.projectSummary(),
                null, // link
                null, // projectType
                null, // technologies
                IdGenerator.generateCustomerId(),
                project.customerName(),
                project.industry()
        );

        try {
            projectRepository.createOrUpdate(projectEntity);
            existingProjects.put(projectId, project.projectName());
        } catch (Exception e) {
            log.warn("Failed to create project {}: {}", project.projectName(), e.getMessage());
        }

        return projectId;
    }

    /**
     * Builds metadata JSON for work experience record.
     */
    private String buildMetadataJson(ProjectData project) {
        try {
            Map<String, Object> metadata = new HashMap<>();

            // Build tools and technologies references (null-safe for external ingest)
            List<String> technologies = project.technologies() != null ? project.technologies() : List.of();
            String toolsText = String.join(", ", technologies);
            List<Map<String, Object>> toolsRef = generateToolsRef(toolsText);
            List<Map<String, Object>> technologiesRef = generateTechnologiesRef(technologies);

            // Parse role (may contain multiple roles separated by comma); null-safe for external ingest
            String roleStr = project.role() != null ? project.role() : "";
            String[] roleParts = roleStr.split(",\\s*");
            String primaryRole = roleParts.length > 0 && !roleParts[0].trim().isEmpty() ? roleParts[0].trim() : "Developer";
            List<String> extraRoles = roleParts.length > 1
                    ? Arrays.asList(Arrays.copyOfRange(roleParts, 1, roleParts.length))
                    : new ArrayList<>();

            String projectRole = generateProjectRole(primaryRole, extraRoles);
            Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
            List<Map<String, Object>> extraProjectRoles = generateExtraProjectRoles(extraRoles);
            List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, extraRoles);
            String customerDescription = project.customerDescription() != null && !project.customerDescription().isBlank()
                    ? project.customerDescription()
                    : generateCustomerDescription(project.customerName() != null ? project.customerName() : "");
            String participation = generateParticipation(primaryRole, technologies);

            metadata.put("company", project.companyName() != null ? project.companyName() : "");
            metadata.put("company_url", "");
            metadata.put("is_company_berdachuk", false);
            metadata.put("team", "Development Team");
            metadata.put("tools", toolsText);
            metadata.put("tools_ref", toolsRef);
            metadata.put("technologies_ref", technologiesRef);
            metadata.put("customer_description", customerDescription);
            metadata.put("position", project.role() != null ? project.role() : "");
            metadata.put("project_role", projectRole);
            metadata.put("primary_project_role", primaryProjectRole);
            metadata.put("extra_project_roles", extraProjectRoles);
            metadata.put("all_project_roles", allProjectRoles);
            metadata.put("participation", participation);
            metadata.put("project_description", project.projectSummary() != null ? project.projectSummary() : "");

            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    // Helper methods for metadata generation (similar to TestDataGenerator)

    private List<Map<String, Object>> generateToolsRef(String toolsText) {
        List<Map<String, Object>> toolsRef = new ArrayList<>();
        if (toolsText == null || toolsText.isBlank()) {
            return toolsRef;
        }
        String[] tools = toolsText.split(", ");

        for (String tool : tools) {
            Map<String, Object> toolRef = new HashMap<>();
            toolRef.put("id", "tool." + tool.toLowerCase().replace(" ", ".").replace(".", ""));
            toolRef.put("name", tool);
            toolRef.put("type", "Tool");
            toolsRef.add(toolRef);
        }

        return toolsRef;
    }

    private List<Map<String, Object>> generateTechnologiesRef(List<String> technologies) {
        List<Map<String, Object>> technologiesRef = new ArrayList<>();

        for (String tech : technologies) {
            Map<String, Object> techRef = new HashMap<>();
            techRef.put("id", "tech." + normalizeTechnologyName(tech));
            techRef.put("name", tech);
            techRef.put("type", "Technology");
            technologiesRef.add(techRef);
        }

        return technologiesRef;
    }

    private String normalizeTechnologyName(String tech) {
        return tech.toLowerCase()
                .replace(" ", ".")
                .replace("-", ".")
                .replace("_", ".")
                .replaceAll("[^a-z0-9.]", "");
    }

    private String generateProjectRole(String primaryRole, List<String> extraRoles) {
        List<String> allRoles = new ArrayList<>();
        allRoles.add(primaryRole);
        allRoles.addAll(extraRoles);
        return String.join(", ", allRoles);
    }

    private Map<String, Object> generatePrimaryProjectRole(String role) {
        Map<String, Object> roleRef = new HashMap<>();
        roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
        roleRef.put("name", role);
        roleRef.put("type", "Role");
        return roleRef;
    }

    private List<Map<String, Object>> generateExtraProjectRoles(List<String> extraRoles) {
        List<Map<String, Object>> extraRolesRef = new ArrayList<>();

        for (String role : extraRoles) {
            Map<String, Object> roleRef = new HashMap<>();
            roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
            roleRef.put("name", role);
            roleRef.put("type", "Role");
            extraRolesRef.add(roleRef);
        }

        return extraRolesRef;
    }

    private List<Map<String, Object>> generateAllProjectRoles(String primaryRole, List<String> extraRoles) {
        List<Map<String, Object>> allRoles = new ArrayList<>();

        Map<String, Object> primaryRoleRef = new HashMap<>();
        primaryRoleRef.put("id", "custom." + primaryRole.toLowerCase().replace(" ", "."));
        primaryRoleRef.put("name", primaryRole);
        primaryRoleRef.put("type", "Role");
        primaryRoleRef.put("is_custom", true);
        allRoles.add(primaryRoleRef);

        for (String role : extraRoles) {
            Map<String, Object> roleRef = new HashMap<>();
            roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
            roleRef.put("name", role);
            roleRef.put("type", "Role");
            roleRef.put("is_custom", true);
            allRoles.add(roleRef);
        }

        return allRoles;
    }

    private String generateCustomerDescription(String customerName) {
        String[] templates = {
                "%s is a leading company in the industry.",
                "%s is a global provider of innovative solutions.",
                "%s is a well-established company with a strong market presence.",
                "%s is a technology-driven organization focused on excellence."
        };
        String template = templates[new Random().nextInt(templates.length)];
        return String.format(template, customerName);
    }

    private String generateParticipation(String role, List<String> technologies) {
        String[] templates = {
                "• Development using %s.",
                "• %s development and implementation.",
                "• Responsible for %s development with %s.",
                "• %s development, testing, and deployment using %s."
        };
        String template = templates[new Random().nextInt(templates.length)];
        return String.format(template, role, String.join(", ", technologies));
    }
}

