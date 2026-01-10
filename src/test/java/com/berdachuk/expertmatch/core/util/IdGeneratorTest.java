package com.berdachuk.expertmatch.core.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IdGenerator.
 */
class IdGeneratorTest {

    @Test
    void testGenerateId() {
        String id = IdGenerator.generateId();

        assertNotNull(id);
        assertEquals(24, id.length(), "ID should be 24 characters");
        assertTrue(id.matches("[0-9a-f]{24}"), "ID should be hexadecimal");
    }

    @Test
    void testGenerateUniqueIds() {
        String id1 = IdGenerator.generateId();
        String id2 = IdGenerator.generateId();

        assertNotEquals(id1, id2, "Generated IDs should be unique");
    }

    @Test
    void testGenerateMultipleIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(IdGenerator.generateId());
        }

        assertEquals(100, ids.size(), "All IDs should be unique");
    }
}

