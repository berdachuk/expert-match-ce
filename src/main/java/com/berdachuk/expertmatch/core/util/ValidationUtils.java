package com.berdachuk.expertmatch.core.util;

import java.util.regex.Pattern;

/**
 * Utility class for validation operations.
 */
public class ValidationUtils {

    // MongoDB-compatible ID pattern (24 hex characters)
    private static final Pattern ID_PATTERN = Pattern.compile("^[0-9a-f]{24}$", Pattern.CASE_INSENSITIVE);

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Validates MongoDB-compatible ID format.
     */
    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    /**
     * Validates email format.
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates seniority level format (A1-A5, B1-B3, C1-C2).
     * <p>
     * Hierarchy: B levels are higher than A levels, C levels are higher than B levels.
     * - A levels: A1 (Junior), A2 (Middle), A3 (Senior), A4 (Lead), A5 (Principal)
     * - B levels: B1 (Junior Manager), B2 (Middle Manager), B3 (Senior Manager) - Higher than A
     * - C levels: C1 (Director), C2 (VP/Executive) - Higher than B
     */
    public static boolean isValidSeniority(String seniority) {
        if (seniority == null) {
            return false;
        }
        return seniority.matches("^[ABC][1-5]$");
    }

    /**
     * Validates English level format (A1-C2).
     */
    public static boolean isValidEnglishLevel(String level) {
        if (level == null) {
            return false;
        }
        return level.matches("^[ABC][12]$");
    }
}

