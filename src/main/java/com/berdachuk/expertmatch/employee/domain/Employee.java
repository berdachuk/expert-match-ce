package com.berdachuk.expertmatch.employee.domain;

/**
 * Employee/Expert domain entity.
 * Represents an employee/expert in the system.
 * <p>
 * Seniority levels:
 * - A levels: A1 (Junior), A2 (Middle), A3 (Senior), A4 (Lead), A5 (Principal)
 * - B levels: B1 (Junior Manager), B2 (Middle Manager), B3 (Senior Manager) - Higher than A
 * - C levels: C1 (Director), C2 (VP/Executive) - Higher than B
 */
public record Employee(
        String id,
        String name,
        String email,
        String seniority,
        String languageEnglish,
        String availabilityStatus
) {
}
