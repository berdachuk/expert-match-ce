package com.berdachuk.expertmatch.graph.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.graph.domain.*;
import com.berdachuk.expertmatch.graph.repository.GraphBuilderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Repository implementation for graph builder data access operations.
 */
@Slf4j
@Repository
public class GraphBuilderRepositoryImpl implements GraphBuilderRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ExpertMapper expertMapper;
    private final ProjectMapper projectMapper;
    private final CustomerMapper customerMapper;
    private final ParticipationRelationshipMapper participationRelationshipMapper;
    private final ExpertCustomerRelationshipMapper expertCustomerRelationshipMapper;
    private final ProjectTechnologyRelationshipMapper projectTechnologyRelationshipMapper;
    private final ProjectCustomerRelationshipMapper projectCustomerRelationshipMapper;

    @InjectSql("/sql/graph/findAllExperts.sql")
    private String findAllExpertsSql;

    @InjectSql("/sql/graph/findAllProjects.sql")
    private String findAllProjectsSql;

    @InjectSql("/sql/graph/findAllTechnologies.sql")
    private String findAllTechnologiesSql;

    @InjectSql("/sql/graph/findAllDomains.sql")
    private String findAllDomainsSql;

    @InjectSql("/sql/graph/findAllCustomers.sql")
    private String findAllCustomersSql;

    @InjectSql("/sql/graph/findAllExpertProjectRelationships.sql")
    private String findAllExpertProjectRelationshipsSql;

    @InjectSql("/sql/graph/findAllExpertCustomerRelationships.sql")
    private String findAllExpertCustomerRelationshipsSql;

    @InjectSql("/sql/graph/findAllProjectTechnologyRelationships.sql")
    private String findAllProjectTechnologyRelationshipsSql;

    @InjectSql("/sql/graph/findAllProjectDomainRelationships.sql")
    private String findAllProjectDomainRelationshipsSql;

    @InjectSql("/sql/graph/findAllProjectCustomerRelationships.sql")
    private String findAllProjectCustomerRelationshipsSql;

    public GraphBuilderRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.expertMapper = new ExpertMapper();
        this.projectMapper = new ProjectMapper();
        this.customerMapper = new CustomerMapper();
        this.participationRelationshipMapper = new ParticipationRelationshipMapper();
        this.expertCustomerRelationshipMapper = new ExpertCustomerRelationshipMapper();
        this.projectTechnologyRelationshipMapper = new ProjectTechnologyRelationshipMapper();
        this.projectCustomerRelationshipMapper = new ProjectCustomerRelationshipMapper();
    }

    @Override
    public List<ExpertData> findAllExperts() {
        return namedJdbcTemplate.query(findAllExpertsSql, expertMapper);
    }

    @Override
    public List<ProjectData> findAllProjects() {
        return namedJdbcTemplate.query(findAllProjectsSql, projectMapper);
    }

    @Override
    public List<String> findAllTechnologies() {
        List<String> technologies = namedJdbcTemplate.query(findAllTechnologiesSql, (rs, rowNum) -> {
            String technology = rs.getString("technology");
            if (technology != null && !technology.isEmpty()) {
                return technology;
            }
            return null;
        });
        return technologies.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public List<String> findAllDomains() {
        List<String> domains = namedJdbcTemplate.query(findAllDomainsSql, (rs, rowNum) -> {
            String domain = rs.getString("domain");
            if (domain != null && !domain.isEmpty()) {
                return domain;
            }
            return null;
        });
        return domains.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public List<CustomerData> findAllCustomers() {
        // Use LinkedHashSet for deduplication
        Set<CustomerData> customerSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(findAllCustomersSql, (rs, rowNum) -> {
            String customerId = rs.getString("customer_id");
            String customerName = rs.getString("customer_name");
            if (customerName != null && !customerName.isEmpty()) {
                customerSet.add(new CustomerData(customerId, customerName));
            }
            return null;
        });
        return new ArrayList<>(customerSet);
    }

    @Override
    public List<ParticipationRelationship> findAllExpertProjectRelationships() {
        return namedJdbcTemplate.query(findAllExpertProjectRelationshipsSql, participationRelationshipMapper)
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<ExpertCustomerRelationship> findAllExpertCustomerRelationships() {
        // Use LinkedHashSet for deduplication
        Set<ExpertCustomerRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(findAllExpertCustomerRelationshipsSql, (rs, rowNum) -> {
            String employeeId = rs.getString("employee_id");
            String customerId = rs.getString("customer_id");
            if (employeeId != null && customerId != null) {
                relationshipSet.add(new ExpertCustomerRelationship(employeeId, customerId));
            }
            return null;
        });
        return new ArrayList<>(relationshipSet);
    }

    @Override
    public List<ProjectTechnologyRelationship> findAllProjectTechnologyRelationships() {
        // Use LinkedHashSet for deduplication
        Set<ProjectTechnologyRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(findAllProjectTechnologyRelationshipsSql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String technology = rs.getString("technology");
            if (technology != null && !technology.isEmpty() && projectId != null) {
                relationshipSet.add(new ProjectTechnologyRelationship(projectId, technology));
            }
            return null;
        });
        return new ArrayList<>(relationshipSet);
    }

    @Override
    public List<String> findAllProjectDomainRelationships() {
        List<String> relationships = namedJdbcTemplate.query(findAllProjectDomainRelationshipsSql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String domain = rs.getString("domain");
            if (domain != null && !domain.isEmpty() && projectId != null) {
                return projectId + "-" + domain;
            }
            return null;
        });
        return relationships.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public List<ProjectCustomerRelationship> findAllProjectCustomerRelationships() {
        // Use LinkedHashSet for deduplication
        Set<ProjectCustomerRelationship> relationshipSet = new LinkedHashSet<>();
        namedJdbcTemplate.query(findAllProjectCustomerRelationshipsSql, (rs, rowNum) -> {
            String projectId = rs.getString("project_id");
            String customerId = rs.getString("customer_id");
            if (projectId != null && customerId != null) {
                relationshipSet.add(new ProjectCustomerRelationship(projectId, customerId));
            }
            return null;
        });
        return new ArrayList<>(relationshipSet);
    }

    /**
     * RowMapper for ExpertData.
     */
    private static class ExpertMapper implements RowMapper<ExpertData> {
        @Override
        public ExpertData mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("id");
            String name = rs.getString("name");
            String email = rs.getString("email");
            String seniority = rs.getString("seniority");
            return new ExpertData(id, name, email, seniority);
        }
    }

    /**
     * RowMapper for ProjectData.
     */
    private static class ProjectMapper implements RowMapper<ProjectData> {
        @Override
        public ProjectData mapRow(ResultSet rs, int rowNum) throws SQLException {
            String projectId = rs.getString("project_id");
            String projectName = rs.getString("project_name");
            String projectType = rs.getString("industry");
            return new ProjectData(projectId, projectName, projectType);
        }
    }

    /**
     * RowMapper for CustomerData.
     */
    private static class CustomerMapper implements RowMapper<CustomerData> {
        @Override
        public CustomerData mapRow(ResultSet rs, int rowNum) throws SQLException {
            String customerId = rs.getString("customer_id");
            String customerName = rs.getString("customer_name");
            return new CustomerData(customerId, customerName);
        }
    }

    /**
     * RowMapper for ParticipationRelationship.
     */
    private static class ParticipationRelationshipMapper implements RowMapper<ParticipationRelationship> {
        @Override
        public ParticipationRelationship mapRow(ResultSet rs, int rowNum) throws SQLException {
            String employeeId = rs.getString("employee_id");
            String projectId = rs.getString("project_id");
            String role = rs.getString("role");
            if (projectId != null) {
                return new ParticipationRelationship(employeeId, projectId, role);
            }
            return null;
        }
    }

    /**
     * RowMapper for ExpertCustomerRelationship.
     */
    private static class ExpertCustomerRelationshipMapper implements RowMapper<ExpertCustomerRelationship> {
        @Override
        public ExpertCustomerRelationship mapRow(ResultSet rs, int rowNum) throws SQLException {
            String employeeId = rs.getString("employee_id");
            String customerId = rs.getString("customer_id");
            return new ExpertCustomerRelationship(employeeId, customerId);
        }
    }

    /**
     * RowMapper for ProjectTechnologyRelationship.
     */
    private static class ProjectTechnologyRelationshipMapper implements RowMapper<ProjectTechnologyRelationship> {
        @Override
        public ProjectTechnologyRelationship mapRow(ResultSet rs, int rowNum) throws SQLException {
            String projectId = rs.getString("project_id");
            String technology = rs.getString("technology");
            return new ProjectTechnologyRelationship(projectId, technology);
        }
    }

    /**
     * RowMapper for ProjectCustomerRelationship.
     */
    private static class ProjectCustomerRelationshipMapper implements RowMapper<ProjectCustomerRelationship> {
        @Override
        public ProjectCustomerRelationship mapRow(ResultSet rs, int rowNum) throws SQLException {
            String projectId = rs.getString("project_id");
            String customerId = rs.getString("customer_id");
            return new ProjectCustomerRelationship(projectId, customerId);
        }
    }
}