package com.berdachuk.expertmatch.mcp.service;

import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.mcp.model.McpResource;
import com.berdachuk.expertmatch.project.domain.Project;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for MCP resource operations.
 */
@Slf4j
@Component
public class McpResourceHandler {

    private final ObjectMapper objectMapper;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public McpResourceHandler(
            ObjectMapper objectMapper,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository,
            WorkExperienceRepository workExperienceRepository,
            NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.objectMapper = objectMapper;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /**
     * List all available MCP resources.
     */
    public List<McpResource> listResources() {
        return List.of(
                new McpResource(
                        "expertmatch://experts/{expert_id}",
                        "Expert Profile",
                        "Expert profile resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://projects/{project_id}",
                        "Project Information",
                        "Project information resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://technologies/{technology_name}",
                        "Technology Usage",
                        "Technology usage resource",
                        "application/json"
                ),
                new McpResource(
                        "expertmatch://domains/{domain_name}",
                        "Domain Expertise",
                        "Domain expertise resource",
                        "application/json"
                )
        );
    }

    /**
     * Read a resource by URI.
     */
    public String readResource(String uri) {
        try {
            if (uri.startsWith("expertmatch://experts/")) {
                String expertId = uri.substring("expertmatch://experts/".length());
                return readExpertResource(expertId);
            } else if (uri.startsWith("expertmatch://projects/")) {
                String projectId = uri.substring("expertmatch://projects/".length());
                return readProjectResource(projectId);
            } else if (uri.startsWith("expertmatch://technologies/")) {
                String technologyName = uri.substring("expertmatch://technologies/".length());
                return readTechnologyResource(technologyName);
            } else if (uri.startsWith("expertmatch://domains/")) {
                String domainName = uri.substring("expertmatch://domains/".length());
                return readDomainResource(domainName);
            } else {
                throw new IllegalArgumentException("Unknown resource URI: " + uri);
            }
        } catch (Exception e) {
            try {
                return objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
                return "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            }
        }
    }

    private String readExpertResource(String expertId) throws Exception {
        log.debug("Reading expert resource for ID: {}", expertId);
        
        // Find employee
        Employee employee = employeeRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found: " + expertId));
        
        // Find work experience for this employee
        List<WorkExperience> workExperiences = workExperienceRepository.findByEmployeeId(expertId);
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("id", employee.id());
        response.put("name", employee.name());
        response.put("email", employee.email());
        response.put("seniority", employee.seniority());
        response.put("languageEnglish", employee.languageEnglish());
        response.put("availabilityStatus", employee.availabilityStatus());
        
        // Add work experiences
        List<Map<String, Object>> experiences = workExperiences.stream()
                .map(we -> {
                    Map<String, Object> exp = new HashMap<>();
                    exp.put("id", we.id());
                    exp.put("projectId", we.projectId());
                    exp.put("projectName", we.projectName());
                    exp.put("customerName", we.customerName());
                    exp.put("industry", we.industry());
                    exp.put("role", we.role());
                    exp.put("startDate", we.startDate() != null ? we.startDate().toString() : null);
                    exp.put("endDate", we.endDate() != null ? we.endDate().toString() : null);
                    exp.put("projectSummary", we.projectSummary());
                    exp.put("responsibilities", we.responsibilities());
                    exp.put("technologies", we.technologies());
                    return exp;
                })
                .collect(Collectors.toList());
        response.put("workExperiences", experiences);
        
        return objectMapper.writeValueAsString(response);
    }

    private String readProjectResource(String projectId) throws Exception {
        log.debug("Reading project resource for ID: {}", projectId);
        
        // Find project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("id", project.id());
        response.put("name", project.name());
        response.put("summary", project.summary());
        response.put("link", project.link());
        response.put("projectType", project.projectType());
        response.put("technologies", project.technologies());
        response.put("customerId", project.customerId());
        response.put("customerName", project.customerName());
        response.put("industry", project.industry());
        
        return objectMapper.writeValueAsString(response);
    }

    private String readTechnologyResource(String technologyName) throws Exception {
        log.debug("Reading technology resource for: {}", technologyName);
        
        // Find employees who have experience with this technology
        List<String> employeeIds = workExperienceRepository.findEmployeeIdsByTechnologies(List.of(technologyName));
        
        // Load employee details
        List<Map<String, Object>> employees = employeeRepository.findByIds(employeeIds).stream()
                .map(emp -> {
                    Map<String, Object> empMap = new HashMap<>();
                    empMap.put("id", emp.id());
                    empMap.put("name", emp.name());
                    empMap.put("email", emp.email());
                    empMap.put("seniority", emp.seniority());
                    return empMap;
                })
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("name", technologyName);
        response.put("employeeCount", employees.size());
        response.put("employees", employees);
        
        return objectMapper.writeValueAsString(response);
    }

    private String readDomainResource(String domainName) throws Exception {
        log.debug("Reading domain resource for: {}", domainName);
        
        // Query work experience by industry (domain maps to industry)
        String sql = """
                SELECT DISTINCT employee_id
                FROM expertmatch.work_experience
                WHERE LOWER(industry) = LOWER(:domainName)
                """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("domainName", domainName);
        
        List<String> employeeIds = namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("employee_id"));
        
        // Load employee details
        List<Map<String, Object>> employees = employeeRepository.findByIds(employeeIds).stream()
                .map(emp -> {
                    Map<String, Object> empMap = new HashMap<>();
                    empMap.put("id", emp.id());
                    empMap.put("name", emp.name());
                    empMap.put("email", emp.email());
                    empMap.put("seniority", emp.seniority());
                    return empMap;
                })
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("name", domainName);
        response.put("employeeCount", employees.size());
        response.put("employees", employees);
        
        return objectMapper.writeValueAsString(response);
    }
}

