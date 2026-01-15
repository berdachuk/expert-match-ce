package com.berdachuk.expertmatch.technology.domain;

import java.util.List;

/**
 * Technology domain entity.
 * Represents a technology in the system.
 */
public record Technology(
        String id,
        String name,
        String normalizedName,
        String category,
        List<String> synonyms
) {
}
