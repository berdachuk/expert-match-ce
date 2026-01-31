package com.berdachuk.expertmatch.core.domain;

import java.util.List;

/**
 * Parsed query result with extracted requirements.
 */
public record ParsedQuery(
        String originalQuery,
        List<String> skills,
        List<String> seniorityLevels,
        String language,
        String intent,
        List<String> technologies
) {
}