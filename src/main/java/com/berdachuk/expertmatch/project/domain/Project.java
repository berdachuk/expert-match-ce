package com.berdachuk.expertmatch.project.domain;

import java.util.List;

/**
 * Project domain entity.
 * Represents a project in the system.
 */
public record Project(
        String id,
        String name,
        String summary,
        String link,
        String projectType,
        List<String> technologies,
        String customerId,
        String customerName,
        String industry
) {
}
