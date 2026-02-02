package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.query.service.QueryExamplesService.QueryExample;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for QueryExamplesService.
 * Tests the service that provides example queries for the ExpertMatch system.
 * <p>
 * Note: This is a legitimate unit test because it tests a service that reads static JSON data
 * without database dependencies. According to TDD rules, unit tests are acceptable for services
 * that don't interact with databases or external systems.
 */
class QueryExamplesServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QueryExamplesService service = new com.berdachuk.expertmatch.query.service.impl.QueryExamplesServiceImpl(objectMapper);

    @Test
    void testGetExamples_ReturnsNonEmptyList() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        assertNotNull(examples);
        assertFalse(examples.isEmpty());
    }

    @Test
    void testGetExamples_ContainsExpectedCategories() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        List<String> categories = examples.stream()
                .map(QueryExample::category)
                .distinct()
                .toList();

        assertTrue(categories.contains("Basic"), "Should contain Basic category");
        assertTrue(categories.contains("Technology"), "Should contain Technology category");
        assertTrue(categories.contains("Seniority"), "Should contain Seniority category");
        assertTrue(categories.contains("Language"), "Should contain Language category");
        assertTrue(categories.contains("Team"), "Should contain Team category");
        assertTrue(categories.contains("Complex"), "Should contain Complex category");
    }

    @Test
    void testGetExamples_AllExamplesHaveRequiredFields() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        for (QueryExample example : examples) {
            assertNotNull(example.category(), "Category should not be null");
            assertFalse(example.category().isBlank(), "Category should not be blank");
            assertNotNull(example.title(), "Title should not be null");
            assertFalse(example.title().isBlank(), "Title should not be blank");
            assertNotNull(example.query(), "Query should not be null");
            assertFalse(example.query().isBlank(), "Query should not be blank");
        }
    }

    @Test
    void testGetExamples_ContainsExpectedBasicExamples() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        List<QueryExample> basicExamples = examples.stream()
                .filter(e -> "Basic".equals(e.category()))
                .toList();

        assertFalse(basicExamples.isEmpty(), "Should contain Basic examples");
        assertTrue(basicExamples.size() >= 4, "Should contain at least 4 Basic examples");

        // Check for specific examples
        boolean hasSimpleTechnology = basicExamples.stream()
                .anyMatch(e -> e.title().contains("Simple Technology") || e.query().contains("Java and Spring Boot"));
        assertTrue(hasSimpleTechnology, "Should contain Simple Technology Query example");
    }

    @Test
    void testGetExamples_ContainsExpectedTechnologyExamples() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        List<QueryExample> technologyExamples = examples.stream()
                .filter(e -> "Technology".equals(e.category()))
                .toList();

        assertFalse(technologyExamples.isEmpty(), "Should contain Technology examples");
        assertTrue(technologyExamples.size() >= 5, "Should contain at least 5 Technology examples");
    }

    @Test
    void testGetExamples_QueryExamplesAreUnique() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        long uniqueQueries = examples.stream()
                .map(QueryExample::query)
                .distinct()
                .count();

        assertEquals(examples.size(), uniqueQueries, "All query examples should be unique");
    }

    @Test
    void testGetExamples_EachCategoryHasMultipleExamples() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        Map<String, Long> categoryCounts = examples.stream()
                .collect(Collectors.groupingBy(QueryExample::category, Collectors.counting()));

        categoryCounts.forEach((category, count) -> {
            assertTrue(count >= 2, "Category " + category + " should have at least 2 examples, but has " + count);
        });
    }

    @Test
    void testGetExamples_QueriesAreReasonableLength() {
        // Act
        List<QueryExample> examples = service.getExamples();

        // Assert
        for (QueryExample example : examples) {
            assertTrue(example.query().length() >= 10,
                    "Query should be at least 10 characters: " + example.query());
            assertTrue(example.query().length() <= 500,
                    "Query should not exceed 500 characters: " + example.query());
        }
    }
}

