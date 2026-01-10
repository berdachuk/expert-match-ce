package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for employee/expert data access.
 */
@Slf4j
@Repository
public class EmployeeRepositoryImpl implements EmployeeRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmployeeMapper employeeMapper;
    private final ChatClient chatClient;
    private final PromptTemplate nameMatchingPromptTemplate;
    private final ObjectMapper objectMapper;

    @InjectSql("/sql/employee/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/employee/findByEmail.sql")
    private String findByEmailSql;

    @InjectSql("/sql/employee/findByIds.sql")
    private String findByIdsSql;

    @InjectSql("/sql/employee/findEmployeeIdsByName.sql")
    private String findEmployeeIdsByNameSql;

    @InjectSql("/sql/employee/findEmployeeIdsByNameSimilarity.sql")
    private String findEmployeeIdsByNameSimilaritySql;

    @InjectSql("/sql/employee/findEmployeeIdsByNameSimilarityCandidates.sql")
    private String findEmployeeIdsByNameSimilarityCandidatesSql;

    public EmployeeRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            EmployeeMapper employeeMapper,
            @Lazy ChatClient chatClient,
            @Qualifier("nameMatchingPromptTemplate") PromptTemplate nameMatchingPromptTemplate,
            ObjectMapper objectMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.employeeMapper = employeeMapper;
        this.chatClient = chatClient;
        this.nameMatchingPromptTemplate = nameMatchingPromptTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Finds employee by ID.
     */
    @Override
    public Optional<Employee> findById(String employeeId) {
        Map<String, Object> params = Map.of("id", employeeId);
        List<Employee> results = namedJdbcTemplate.query(findByIdSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    /**
     * Finds employee by email.
     */
    @Override
    public Optional<Employee> findByEmail(String email) {
        Map<String, Object> params = Map.of("email", email);
        List<Employee> results = namedJdbcTemplate.query(findByEmailSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    /**
     * Finds multiple employees by IDs.
     */
    @Override
    public List<Employee> findByIds(List<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("ids", employeeIds.toArray(new String[0]));
        return namedJdbcTemplate.query(findByIdsSql, params, employeeMapper);
    }

    /**
     * Finds employees by name (case-insensitive partial match).
     * Supports searching by full name or parts of the name.
     */
    @Override
    public List<String> findEmployeeIdsByName(String name, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        // Use ILIKE pattern matching - wrap in % for partial match
        String namePattern = "%" + name.trim() + "%";

        Map<String, Object> params = Map.of(
                "namePattern", namePattern,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findEmployeeIdsByNameSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    /**
     * Finds employees by name using trigram similarity (fuzzy matching).
     * Uses pg_trgm extension for handling typos and name variations.
     *
     * @param name                The name to search for
     * @param similarityThreshold Minimum similarity threshold (0.0-1.0). Recommended: 0.3-0.5
     * @param maxResults          Maximum number of results
     * @return List of employee IDs ordered by similarity (highest first)
     */
    @Override
    public List<String> findEmployeeIdsByNameSimilarity(String name, double similarityThreshold, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        // Use pg_trgm extension for trigram similarity
        // similarity() function returns a value between 0 and 1
        // word_similarity() is better for partial matches (e.g., "John" matching "John Doe")
        Map<String, Object> params = Map.of(
                "name", name.trim(),
                "threshold", similarityThreshold,
                "maxResults", maxResults
        );

        try {
            return namedJdbcTemplate.query(findEmployeeIdsByNameSimilaritySql, params, (rs, rowNum) -> rs.getString("id"));
        } catch (org.springframework.jdbc.BadSqlGrammarException |
                 org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle case where pg_trgm extension is not available
            java.sql.SQLException sqlException = null;
            if (e instanceof org.springframework.jdbc.UncategorizedSQLException) {
                sqlException = ((org.springframework.jdbc.UncategorizedSQLException) e).getSQLException();
            } else if (e.getCause() instanceof java.sql.SQLException) {
                sqlException = (java.sql.SQLException) e.getCause();
            }

            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            String message = sqlException != null ? sqlException.getMessage() : "";
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : "";

            // Check if error is related to missing pg_trgm extension
            if (sqlState != null && sqlState.equals("42883") ||
                    message.contains("function similarity") ||
                    message.contains("function word_similarity") ||
                    message.contains("pg_trgm") ||
                    exceptionMessage.contains("function similarity") ||
                    exceptionMessage.contains("function word_similarity") ||
                    exceptionMessage.contains("pg_trgm")) {
                // pg_trgm extension not available, try LLM fallback
                log.info("pg_trgm extension not available, using LLM fallback for name similarity search: '{}'", name);
                return findEmployeeIdsByNameSimilarityUsingLLM(name, maxResults);
            }
            // Re-throw other SQL errors
            throw e;
        }
    }

    /**
     * Finds employees by name using LLM as fallback when pg_trgm is not available.
     * Gets candidate names first, then uses LLM to match them.
     */
    private List<String> findEmployeeIdsByNameSimilarityUsingLLM(String name, int maxResults) {
        try {
            // First, get candidate employee names using a simple LIKE query to limit candidates
            // This prevents sending thousands of names to the LLM
            String namePattern = "%" + name.trim() + "%";
            String[] nameParts = name.trim().split("\\s+");
            String firstPattern = nameParts.length > 0 ? "%" + nameParts[0] + "%" : namePattern;
            String lastPattern = nameParts.length > 1 ? "%" + nameParts[nameParts.length - 1] + "%" : namePattern;

            Map<String, Object> candidateParams = new HashMap<>();
            candidateParams.put("namePattern", namePattern);
            candidateParams.put("firstPattern", firstPattern);
            candidateParams.put("lastPattern", lastPattern);

            // Get candidate employees with their IDs and names
            Map<String, String> candidateMap = new HashMap<>(); // name -> id
            namedJdbcTemplate.query(findEmployeeIdsByNameSimilarityCandidatesSql, candidateParams, (rs, rowNum) -> {
                String employeeId = rs.getString("id");
                String employeeName = rs.getString("name");
                candidateMap.put(employeeName, employeeId);
                return null;
            });

            if (candidateMap.isEmpty()) {
                log.debug("No candidate employees found for LLM name matching: '{}'", name);
                return List.of();
            }

            // Build prompt with candidate names
            List<String> candidateNames = candidateMap.keySet().stream().sorted().toList();
            String candidateNamesText = String.join("\n", candidateNames);

            Map<String, Object> promptVariables = new HashMap<>();
            promptVariables.put("queryName", name.trim());
            promptVariables.put("candidateNames", candidateNamesText);

            String prompt = nameMatchingPromptTemplate.render(promptVariables);

            // Call LLM to find matching names
            String responseText = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (responseText == null || responseText.isBlank()) {
                log.warn("Empty response from LLM for name matching: '{}'", name);
                return List.of();
            }

            // Parse JSON response to get matched names
            List<String> matchedNames = parseNameMatchingResponse(responseText);

            // Convert matched names to employee IDs
            List<String> matchedIds = matchedNames.stream()
                    .filter(candidateMap::containsKey)
                    .map(candidateMap::get)
                    .distinct()
                    .limit(maxResults)
                    .toList();

            log.info("LLM name matching found {} employees for '{}'", matchedIds.size(), name);
            return matchedIds;

        } catch (Exception e) {
            log.error("Error during LLM name matching for '{}'", name, e);
            // Return empty list on error to allow graceful degradation
            return List.of();
        }
    }

    /**
     * Parses LLM response containing JSON array of matched employee names.
     */
    private List<String> parseNameMatchingResponse(String responseText) {
        try {
            // Extract JSON from response (handle markdown code blocks if present)
            String jsonText = responseText.trim();
            if (jsonText.contains("```json")) {
                int startIdx = jsonText.indexOf("```json") + 7;
                int endIdx = jsonText.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonText = jsonText.substring(startIdx, endIdx).trim();
                }
            } else if (jsonText.contains("```")) {
                int startIdx = jsonText.indexOf("```") + 3;
                int endIdx = jsonText.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonText = jsonText.substring(startIdx, endIdx).trim();
                }
            }

            // Parse JSON array
            TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {
            };
            return objectMapper.readValue(jsonText, typeRef);
        } catch (Exception e) {
            log.error("Failed to parse LLM name matching response: {}", responseText, e);
            return List.of();
        }
    }

}

