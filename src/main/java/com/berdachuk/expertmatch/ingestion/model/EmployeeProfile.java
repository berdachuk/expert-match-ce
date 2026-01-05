package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Employee profile data model with optional fields.
 * Required: employee (with valid id and name)
 * Optional: summary, projects
 */
public record EmployeeProfile(
        EmployeeData employee,
        @JsonInclude(JsonInclude.Include.NON_NULL) String summary,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ProjectData> projects
) {
    /**
     * Returns true if profile has minimum required data.
     * Required: employee must be present and valid.
     */
    public boolean isValid() {
        return employee != null && employee.isValid();
    }
}

