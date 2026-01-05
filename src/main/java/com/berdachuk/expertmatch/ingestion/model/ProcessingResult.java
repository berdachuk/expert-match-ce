package com.berdachuk.expertmatch.ingestion.model;

import java.util.List;

/**
 * Result of processing a single employee profile.
 */
public record ProcessingResult(
        String employeeId,
        String employeeName,
        boolean success,
        String errorMessage,
        int projectsProcessed,
        int projectsSkipped,
        List<String> projectErrors
) {
    /**
     * Creates a successful result.
     */
    public static ProcessingResult success(String employeeId, String employeeName,
                                           int projectsProcessed, int projectsSkipped,
                                           List<String> projectErrors) {
        return new ProcessingResult(employeeId, employeeName, true, null,
                projectsProcessed, projectsSkipped, projectErrors);
    }

    /**
     * Creates a failed result.
     */
    public static ProcessingResult failure(String employeeId, String employeeName, String errorMessage) {
        return new ProcessingResult(employeeId, employeeName, false, errorMessage,
                0, 0, List.of());
    }
}

