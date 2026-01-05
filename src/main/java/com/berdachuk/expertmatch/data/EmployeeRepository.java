package com.berdachuk.expertmatch.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
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
public class EmployeeRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ChatClient chatClient;
    private final PromptTemplate nameMatchingPromptTemplate;
    private final ObjectMapper objectMapper;

    public EmployeeRepository(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            @Lazy ChatClient chatClient,
            @Qualifier("nameMatchingPromptTemplate") PromptTemplate nameMatchingPromptTemplate,
            ObjectMapper objectMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.chatClient = chatClient;
        this.nameMatchingPromptTemplate = nameMatchingPromptTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Finds employee by ID.
     */
    public Optional<Employee> findById(String employeeId) {
        String sql = """
                SELECT id, name, email, seniority, language_english, availability_status
                    FROM expertmatch.employee
                WHERE id = :id
                """;

        Map<String, Object> params = Map.of("id", employeeId);

        List<Employee> results = namedJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new Employee(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("seniority"),
                        rs.getString("language_english"),
                        rs.getString("availability_status")
                )
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds employee by email.
     */
    public Optional<Employee> findByEmail(String email) {
        String sql = """
                SELECT id, name, email, seniority, language_english, availability_status
                    FROM expertmatch.employee
                WHERE email = :email
                """;

        Map<String, Object> params = Map.of("email", email);

        List<Employee> results = namedJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new Employee(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("seniority"),
                        rs.getString("language_english"),
                        rs.getString("availability_status")
                )
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds multiple employees by IDs.
     */
    public List<Employee> findByIds(List<String> employeeIds) {
        if (employeeIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT id, name, email, seniority, language_english, availability_status
                    FROM expertmatch.employee
                WHERE id = ANY(:ids)
                """;

        Map<String, Object> params = Map.of("ids", employeeIds.toArray(new String[0]));

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) ->
                new Employee(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("seniority"),
                        rs.getString("language_english"),
                        rs.getString("availability_status")
                )
        );
    }

    /**
     * Finds employees by name (case-insensitive partial match).
     * Supports searching by full name or parts of the name.
     */
    public List<String> findEmployeeIdsByName(String name, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT id
                FROM expertmatch.employee
                WHERE LOWER(name) LIKE LOWER(:namePattern)
                LIMIT :maxResults
                """;

        // Use ILIKE pattern matching - wrap in % for partial match
        String namePattern = "%" + name.trim() + "%";

        Map<String, Object> params = Map.of(
                "namePattern", namePattern,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("id"));
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
    public List<String> findEmployeeIdsByNameSimilarity(String name, double similarityThreshold, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        // Use pg_trgm extension for trigram similarity
        // similarity() function returns a value between 0 and 1
        // word_similarity() is better for partial matches (e.g., "John" matching "John Doe")
        String sql = """
                SELECT id
                FROM expertmatch.employee
                WHERE similarity(LOWER(name), LOWER(:name)) >= :threshold
                   OR word_similarity(LOWER(:name), LOWER(name)) >= :threshold
                ORDER BY GREATEST(
                    similarity(LOWER(name), LOWER(:name)),
                    word_similarity(LOWER(:name), LOWER(name))
                ) DESC
                LIMIT :maxResults
                """;

        Map<String, Object> params = Map.of(
                "name", name.trim(),
                "threshold", similarityThreshold,
                "maxResults", maxResults
        );

        try {
            return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("id"));
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
            String candidateSql = """
                    SELECT id, name
                    FROM expertmatch.employee
                    WHERE LOWER(name) LIKE LOWER(:namePattern)
                       OR LOWER(name) LIKE LOWER(:firstPattern)
                       OR LOWER(name) LIKE LOWER(:lastPattern)
                    LIMIT 100
                    """;

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
            namedJdbcTemplate.query(candidateSql, candidateParams, (rs, rowNum) -> {
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

    /**
     * Employee entity.
     * <p>
     * Seniority hierarchy: B levels are higher than A levels, C levels are higher than B levels.
     * - A levels: A1 (Junior), A2 (Middle), A3 (Senior), A4 (Lead), A5 (Principal)
     * - B levels: B1 (Junior Manager), B2 (Middle Manager), B3 (Senior Manager) - Higher than A
     * - C levels: C1 (Director), C2 (VP/Executive) - Higher than B
     */
    public record Employee(
            String id,
            String name,
            String email,
            String seniority,
            String languageEnglish,
            String availabilityStatus
    ) {
    }
}

