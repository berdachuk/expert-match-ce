package com.berdachuk.expertmatch.ingestion.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Project data model with optional fields.
 * Required fields: projectName, startDate
 * Optional fields: projectCode, customerName, companyName, role, endDate,
 * technologies, responsibilities, industry, projectSummary
 */
public record ProjectData(
        @JsonInclude(JsonInclude.Include.NON_NULL) String projectCode,
        String projectName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String customerName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String companyName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String role,
        String startDate,
        @JsonInclude(JsonInclude.Include.NON_NULL) String endDate,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> technologies,
        @JsonInclude(JsonInclude.Include.NON_NULL) String responsibilities,
        @JsonInclude(JsonInclude.Include.NON_NULL) String industry,
        @JsonInclude(JsonInclude.Include.NON_NULL) String projectSummary
) {
    /**
     * Generates project code from project name.
     * Takes first 3 words, converts to uppercase, and creates code like "PRJ-ABC-DEF".
     */
    private static String generateProjectCode(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return "PRJ-UNKNOWN";
        }
        String[] words = projectName.trim().split("\\s+");
        String code = Stream.of(words)
                .limit(3)
                .map(word -> word.replaceAll("[^A-Za-z0-9]", "").toUpperCase())
                .filter(word -> !word.isEmpty())
                .collect(Collectors.joining("-"));
        return code.isEmpty() ? "PRJ-UNKNOWN" : code;
    }

    /**
     * Returns true if project has minimum required data.
     * Required: projectName and startDate must be non-null and non-blank.
     */
    public boolean isValid() {
        return projectName != null && !projectName.isBlank()
                && startDate != null && !startDate.isBlank();
    }

    /**
     * Returns project data with default values for missing optional fields.
     *
     * @return ProjectData with defaults applied
     */
    public ProjectData withDefaults() {
        return new ProjectData(
                projectCode != null ? projectCode : generateProjectCode(projectName),
                projectName,
                customerName != null ? customerName : "Unknown Customer",
                companyName != null ? companyName : (customerName != null ? customerName : "Unknown Customer"),
                role != null ? role : "Developer",
                startDate,
                endDate != null ? endDate : LocalDate.now().toString(),
                technologies != null ? technologies : List.of(),
                responsibilities != null ? responsibilities : "",
                industry != null ? industry : "Technology",
                projectSummary != null ? projectSummary : ""
        );
    }
}

