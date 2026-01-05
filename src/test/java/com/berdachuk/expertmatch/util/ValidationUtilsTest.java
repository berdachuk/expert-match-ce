package com.berdachuk.expertmatch.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ValidationUtils.
 */
class ValidationUtilsTest {

    @Test
    void testIsValidSeniority_ValidALevels() {
        assertTrue(ValidationUtils.isValidSeniority("A1"));
        assertTrue(ValidationUtils.isValidSeniority("A2"));
        assertTrue(ValidationUtils.isValidSeniority("A3"));
        assertTrue(ValidationUtils.isValidSeniority("A4"));
        assertTrue(ValidationUtils.isValidSeniority("A5"));
    }

    @Test
    void testIsValidSeniority_ValidBLevels() {
        assertTrue(ValidationUtils.isValidSeniority("B1"));
        assertTrue(ValidationUtils.isValidSeniority("B2"));
        assertTrue(ValidationUtils.isValidSeniority("B3"));
    }

    @Test
    void testIsValidSeniority_ValidCLevels() {
        assertTrue(ValidationUtils.isValidSeniority("C1"));
        assertTrue(ValidationUtils.isValidSeniority("C2"));
    }

    @Test
    void testIsValidSeniority_InvalidFormats() {
        assertFalse(ValidationUtils.isValidSeniority(null));
        assertFalse(ValidationUtils.isValidSeniority(""));
        assertFalse(ValidationUtils.isValidSeniority("A"));
        assertFalse(ValidationUtils.isValidSeniority("A6"));
        assertFalse(ValidationUtils.isValidSeniority("D1"));
        assertFalse(ValidationUtils.isValidSeniority("A4A"));
        assertFalse(ValidationUtils.isValidSeniority("4A"));

        // Note: The current validation regex ^[ABC][1-5]$ is permissive and allows:
        // - B4, B5 (though semantically B should only be B1-B3)
        // - C3, C4, C5 (though semantically C should only be C1-C2)
        // These are currently validated as true by the regex pattern, but are semantically invalid
        // per the seniority hierarchy (B > A, C > B with specific level ranges).
        // The test reflects the current implementation behavior.
        // TODO: Consider making validation more strict to match actual seniority levels:
        // - A: A1-A5 (all valid)
        // - B: B1-B3 only (B4, B5 should be invalid)
        // - C: C1-C2 only (C3, C4, C5 should be invalid)
    }

    @Test
    void testIsValidSeniority_HierarchyUnderstanding() {
        // Test that all valid levels are recognized
        // Hierarchy: B > A, C > B
        // A levels: A1 (Junior) to A5 (Principal)
        // B levels: B1 (Junior Manager) to B3 (Senior Manager) - Higher than A
        // C levels: C1 (Director) to C2 (VP/Executive) - Higher than B

        // All A levels should be valid
        for (int i = 1; i <= 5; i++) {
            assertTrue(ValidationUtils.isValidSeniority("A" + i),
                    "A" + i + " should be valid");
        }

        // All B levels should be valid
        for (int i = 1; i <= 3; i++) {
            assertTrue(ValidationUtils.isValidSeniority("B" + i),
                    "B" + i + " should be valid");
        }

        // All C levels should be valid
        for (int i = 1; i <= 2; i++) {
            assertTrue(ValidationUtils.isValidSeniority("C" + i),
                    "C" + i + " should be valid");
        }
    }

    @Test
    void testIsValidId_ValidFormat() {
        assertTrue(ValidationUtils.isValidId("507f1f77bcf86cd799439011"));
        assertTrue(ValidationUtils.isValidId("507F1F77BCF86CD799439011")); // Case insensitive
    }

    @Test
    void testIsValidId_InvalidFormat() {
        assertFalse(ValidationUtils.isValidId(null));
        assertFalse(ValidationUtils.isValidId(""));
        assertFalse(ValidationUtils.isValidId("507f1f77bcf86cd79943901")); // Too short
        assertFalse(ValidationUtils.isValidId("507f1f77bcf86cd799439011a")); // Too long
        assertFalse(ValidationUtils.isValidId("507f1f77bcf86cd79943901g")); // Invalid character
    }

    @Test
    void testIsValidEmail_ValidFormat() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user+tag@example.co.uk"));
    }

    @Test
    void testIsValidEmail_InvalidFormat() {
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example"));
        assertFalse(ValidationUtils.isValidEmail("user example.com"));
    }

    @Test
    void testIsValidEnglishLevel_ValidFormat() {
        assertTrue(ValidationUtils.isValidEnglishLevel("A1"));
        assertTrue(ValidationUtils.isValidEnglishLevel("A2"));
        assertTrue(ValidationUtils.isValidEnglishLevel("B1"));
        assertTrue(ValidationUtils.isValidEnglishLevel("B2"));
        assertTrue(ValidationUtils.isValidEnglishLevel("C1"));
        assertTrue(ValidationUtils.isValidEnglishLevel("C2"));
    }

    @Test
    void testIsValidEnglishLevel_InvalidFormat() {
        assertFalse(ValidationUtils.isValidEnglishLevel(null));
        assertFalse(ValidationUtils.isValidEnglishLevel(""));
        assertFalse(ValidationUtils.isValidEnglishLevel("A3")); // A only has 1-2
        assertFalse(ValidationUtils.isValidEnglishLevel("B3")); // B only has 1-2
        assertFalse(ValidationUtils.isValidEnglishLevel("C3")); // C only has 1-2
        assertFalse(ValidationUtils.isValidEnglishLevel("D1"));
    }
}

