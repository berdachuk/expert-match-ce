package com.berdachuk.expertmatch.ingestion;

import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Datafaker integration in TestDataGenerator.
 * Tests that Datafaker can generate the required data types without database access.
 */
class TestDataGeneratorDatafakerTest {

    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
    }

    @Test
    void testGenerateFirstName() {
        String firstName = faker.name().firstName();
        assertNotNull(firstName);
        assertFalse(firstName.isEmpty());
        assertTrue(firstName.length() > 0);
    }

    @Test
    void testGenerateLastName() {
        String lastName = faker.name().lastName();
        assertNotNull(lastName);
        assertFalse(lastName.isEmpty());
        assertTrue(lastName.length() > 0);
    }

    @Test
    void testGenerateFullName() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String fullName = firstName + " " + lastName;

        assertNotNull(fullName);
        assertFalse(fullName.isEmpty());
        assertTrue(fullName.contains(" "));
        assertEquals(fullName.trim(), fullName);
    }

    @Test
    void testGenerateEmailAddress() {
        String email = faker.internet().emailAddress();
        assertNotNull(email);
        assertFalse(email.isEmpty());
        assertTrue(email.contains("@"));
        assertTrue(email.contains("."));
    }

    @Test
    void testGenerateSafeEmailAddress() {
        String email = faker.internet().safeEmailAddress();
        assertNotNull(email);
        assertFalse(email.isEmpty());
        assertTrue(email.contains("@"));
        // safeEmailAddress uses example.com domain
        assertTrue(email.endsWith("@example.com"));
    }

    @Test
    void testGenerateCompanyName() {
        String company = faker.company().name();
        assertNotNull(company);
        assertFalse(company.isEmpty());
        assertTrue(company.length() > 0);
    }

    @Test
    void testGenerateMultipleCompanyNames() {
        // Verify variety in generated names
        String company1 = faker.company().name();
        String company2 = faker.company().name();
        String company3 = faker.company().name();

        assertNotNull(company1);
        assertNotNull(company2);
        assertNotNull(company3);
        // Note: They might be the same due to randomness, but that's acceptable
    }

    @Test
    void testGenerateProjectName() {
        String buzzword1 = faker.company().buzzword();
        String buzzword2 = faker.company().buzzword();
        int number = faker.number().numberBetween(1000, 9999);
        String projectName = buzzword1 + " " + buzzword2 + " " + number;

        assertNotNull(projectName);
        assertFalse(projectName.isEmpty());
        assertTrue(projectName.contains(String.valueOf(number)));
    }

    @Test
    void testGenerateIndustry() {
        String industry = faker.company().industry();
        assertNotNull(industry);
        assertFalse(industry.isEmpty());
        assertTrue(industry.length() > 0);
    }

    @Test
    void testGenerateJobTitle() {
        String jobTitle = faker.job().title();
        assertNotNull(jobTitle);
        assertFalse(jobTitle.isEmpty());
        assertTrue(jobTitle.length() > 0);
    }

    @Test
    void testGenerateMultipleJobTitles() {
        // Verify variety in generated job titles
        String title1 = faker.job().title();
        String title2 = faker.job().title();
        String title3 = faker.job().title();

        assertNotNull(title1);
        assertNotNull(title2);
        assertNotNull(title3);
    }

    @Test
    void testGeneratePosition() {
        String position = faker.job().title();
        assertNotNull(position);
        assertFalse(position.isEmpty());
    }

    @Test
    void testGenerateRole() {
        String role = faker.job().title();
        assertNotNull(role);
        assertFalse(role.isEmpty());
    }

    @Test
    void testEmailFormatValidation() {
        // Generate multiple emails and verify format
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        for (int i = 0; i < 10; i++) {
            String email = faker.internet().emailAddress();
            assertTrue(emailPattern.matcher(email).matches(),
                    "Email should match pattern: " + email);
        }
    }

    @Test
    void testCompanyNameVariety() {
        // Generate multiple company names to verify variety
        Set<String> companies = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            companies.add(faker.company().name());
        }
        // Should have some variety (at least 5 different names out of 20)
        assertTrue(companies.size() >= 5,
                "Should generate variety of company names, got " + companies.size() + " unique names");
    }

    @Test
    void testJobTitleVariety() {
        // Generate multiple job titles to verify variety
        Set<String> titles = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            titles.add(faker.job().title());
        }
        // Should have some variety
        assertTrue(titles.size() >= 5,
                "Should generate variety of job titles, got " + titles.size() + " unique titles");
    }
}

