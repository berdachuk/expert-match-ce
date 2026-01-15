package com.berdachuk.expertmatch.workexperience.domain;

import java.time.Instant;
import java.util.List;

/**
 * Work experience entity.
 */
public record WorkExperience(
        String id,
        String employeeId,
        String projectId,        // External system project_id (nullable)
        String customerId,       // External system customer_id (nullable)
        String projectName,
        String customerName,
        String industry,
        String role,
        Instant startDate,
        Instant endDate,
        String projectSummary,
        String responsibilities,
        List<String> technologies
) {
}
