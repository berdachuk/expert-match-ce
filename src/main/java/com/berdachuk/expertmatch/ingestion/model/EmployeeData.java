package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Employee data model with optional fields.
 * Required fields: id, name
 * Optional fields: email, seniority, languageEnglish, availabilityStatus
 */
public record EmployeeData(
        String id,
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL) String email,
        @JsonInclude(JsonInclude.Include.NON_NULL) String seniority,
        @JsonInclude(JsonInclude.Include.NON_NULL) String languageEnglish,
        @JsonInclude(JsonInclude.Include.NON_NULL) String availabilityStatus
) {
    /**
     * Generates default email from employee name.
     * Format: firstname.lastname@example.com
     */
    private static String generateDefaultEmail(String name) {
        if (name == null || name.isBlank()) {
            return "unknown@example.com";
        }
        return name.toLowerCase()
                .replaceAll("\\s+", ".")
                .replaceAll("[^a-z0-9.]", "") + "@example.com";
    }

    /**
     * Returns true if employee has minimum required data.
     * Required: id and name must be non-null and non-blank.
     */
    public boolean isValid() {
        return id != null && !id.isBlank()
                && name != null && !name.isBlank();
    }

    /**
     * Returns employee data with default values for missing optional fields.
     *
     * @return EmployeeData with defaults applied
     */
    public EmployeeData withDefaults() {
        return new EmployeeData(
                id,
                name,
                email != null ? email : generateDefaultEmail(name),
                seniority != null ? seniority : "B1",
                languageEnglish != null ? languageEnglish : "B2",
                availabilityStatus != null ? availabilityStatus : "available"
        );
    }
}

