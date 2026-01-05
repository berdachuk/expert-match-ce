package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SGRSchemaGeneratorTest {

    private SGRSchemaGenerator generator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        generator = new SGRSchemaGenerator(objectMapper);
    }

    @Test
    void testGenerateSchemaDescription_ForExpertEvaluation() {
        // Act
        String schema = generator.generateSchemaDescription(ExpertEvaluation.class);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.contains("object"));
    }

    @Test
    void testGenerateSchemaDescription_Caching() {
        // Act
        String schema1 = generator.generateSchemaDescription(ExpertEvaluation.class);
        String schema2 = generator.generateSchemaDescription(ExpertEvaluation.class);

        // Assert - Should return same instance due to caching
        assertEquals(schema1, schema2);
    }

    @Test
    void testGenerateSchemaDescription_ForSkillMatchAnalysis() {
        // Act
        String schema = generator.generateSchemaDescription(SkillMatchAnalysis.class);

        // Assert
        assertNotNull(schema);
        assertTrue(schema.contains("object"));
    }

    @Test
    void testClearCache() {
        // Arrange
        generator.generateSchemaDescription(ExpertEvaluation.class);

        // Act
        generator.clearCache();

        // Assert - Cache should be cleared, but method should still work
        String schema = generator.generateSchemaDescription(ExpertEvaluation.class);
        assertNotNull(schema);
    }
}

