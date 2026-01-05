package com.berdachuk.expertmatch.ingestion;

import com.berdachuk.expertmatch.data.IdGenerator;
import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.ProcessingResult;
import com.berdachuk.expertmatch.ingestion.model.ProjectData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Service for processing a single EmployeeProfile into the database.
 * Handles partial data, applies defaults, and creates employee and work experience records.
 */
@Slf4j
@Component
public class ProfileProcessor {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ObjectMapper objectMapper;

    public ProfileProcessor(NamedParameterJdbcTemplate namedJdbcTemplate, ObjectMapper objectMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
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

        // Apply defaults to employee data
        var employee = profile.employee().withDefaults();
        String employeeId = employee.id();
        String employeeName = employee.name();

        try {
            // Insert or update employee
            String employeeSql = """
                    INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status)
                    VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)
                    ON CONFLICT (id) DO UPDATE SET
                        name = EXCLUDED.name,
                        email = EXCLUDED.email,
                        seniority = EXCLUDED.seniority,
                        language_english = EXCLUDED.language_english,
                        availability_status = EXCLUDED.availability_status
                    """;

            Map<String, Object> employeeParams = new HashMap<>();
            employeeParams.put("id", employeeId);
            employeeParams.put("name", employeeName);
            employeeParams.put("email", employee.email());
            employeeParams.put("seniority", employee.seniority());
            employeeParams.put("languageEnglish", employee.languageEnglish());
            employeeParams.put("availabilityStatus", employee.availabilityStatus());

            namedJdbcTemplate.update(employeeSql, employeeParams);
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

                        // Apply defaults to project data
                        var project = projectData.withDefaults();
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
     * Processes a single project and creates work experience record.
     */
    private void processProject(String employeeId, ProjectData project, Map<String, String> existingProjects) {
        // Check if work experience already exists
        String checkSql = """
                SELECT id FROM expertmatch.work_experience
                WHERE employee_id = :employeeId 
                  AND project_name = :projectName 
                  AND start_date = :startDate
                LIMIT 1
                """;

        LocalDate startDate = LocalDate.parse(project.startDate());
        Map<String, Object> checkParams = Map.of(
                "employeeId", employeeId,
                "projectName", project.projectName(),
                "startDate", startDate
        );

        List<String> existingIds = namedJdbcTemplate.query(checkSql, checkParams,
                (rs, rowNum) -> rs.getString("id"));
        if (!existingIds.isEmpty()) {
            log.debug("Work experience already exists for {} at {} starting {}, skipping",
                    employeeId, project.projectName(), startDate);
            return;
        }

        String workExpId = IdGenerator.generateId();

        // Find or create project
        String projectId = findOrCreateProject(project, existingProjects);

        // Build metadata
        String metadataJson = buildMetadataJson(project);

        // Insert work experience
        LocalDate endDate = LocalDate.parse(project.endDate());
        String[] technologies = project.technologies().toArray(new String[0]);

        String sql = """
                INSERT INTO expertmatch.work_experience 
                    (id, employee_id, project_id, project_name, project_summary, role, start_date, end_date,
                     technologies, responsibilities, customer_name, industry, metadata)
                    VALUES (:id, :employeeId, :projectId, :projectName, :projectSummary, :role, :startDate, :endDate,
                            :technologies, :responsibilities, :customerName, :industry, :metadata::jsonb)
                ON CONFLICT (id) DO NOTHING
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", workExpId);
        params.put("employeeId", employeeId);
        params.put("projectId", projectId);
        params.put("projectName", project.projectName());
        params.put("projectSummary", project.projectSummary());
        params.put("role", project.role());
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("technologies", technologies);
        params.put("responsibilities", project.responsibilities());
        params.put("customerName", project.customerName());
        params.put("industry", project.industry());
        params.put("metadata", metadataJson);

        namedJdbcTemplate.update(sql, params);
        log.debug("Created work experience for employee {} at project {}", employeeId, project.projectName());
    }

    /**
     * Finds existing project or creates a new one.
     */
    private String findOrCreateProject(ProjectData project, Map<String, String> existingProjects) {
        // Try to find existing project by name
        String projectId = existingProjects.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase().contains(
                        project.projectName().toLowerCase().substring(0, Math.min(10, project.projectName().length()))))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (projectId != null && existingProjects.containsKey(projectId)) {
            return projectId;
        }

        // Create new project
        projectId = IdGenerator.generateProjectId();
        String insertProjectSql = """
                INSERT INTO expertmatch.project (id, name, customer_id, customer_name, industry)
                VALUES (:id, :name, :customerId, :customerName, :industry)
                ON CONFLICT (id) DO NOTHING
                """;

        Map<String, Object> projectParams = new HashMap<>();
        projectParams.put("id", projectId);
        projectParams.put("name", project.projectName());
        projectParams.put("customerId", IdGenerator.generateCustomerId());
        projectParams.put("customerName", project.customerName());
        projectParams.put("industry", project.industry());

        try {
            namedJdbcTemplate.update(insertProjectSql, projectParams);
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

            // Build tools and technologies references
            String toolsText = String.join(", ", project.technologies());
            List<Map<String, Object>> toolsRef = generateToolsRef(toolsText);
            List<Map<String, Object>> technologiesRef = generateTechnologiesRef(project.technologies());

            // Parse role (may contain multiple roles separated by comma)
            String[] roleParts = project.role().split(",\\s*");
            String primaryRole = roleParts[0].trim();
            List<String> extraRoles = roleParts.length > 1
                    ? Arrays.asList(Arrays.copyOfRange(roleParts, 1, roleParts.length))
                    : new ArrayList<>();

            String projectRole = generateProjectRole(primaryRole, extraRoles);
            Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
            List<Map<String, Object>> extraProjectRoles = generateExtraProjectRoles(extraRoles);
            List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, extraRoles);
            String customerDescription = generateCustomerDescription(project.customerName());
            String participation = generateParticipation(primaryRole, project.technologies());

            metadata.put("company", project.companyName());
            metadata.put("company_url", "");
            metadata.put("is_company_berdachuk", false);
            metadata.put("team", "Development Team");
            metadata.put("tools", toolsText);
            metadata.put("tools_ref", toolsRef);
            metadata.put("technologies_ref", technologiesRef);
            metadata.put("customer_description", customerDescription);
            metadata.put("position", project.role());
            metadata.put("project_role", projectRole);
            metadata.put("primary_project_role", primaryProjectRole);
            metadata.put("extra_project_roles", extraProjectRoles);
            metadata.put("all_project_roles", allProjectRoles);
            metadata.put("participation", participation);
            metadata.put("project_description", project.projectSummary());

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

