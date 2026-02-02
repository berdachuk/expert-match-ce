package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.embedding.service.EmbeddingService;
import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.ingestion.model.EmployeeProfile;
import com.berdachuk.expertmatch.ingestion.model.ProjectData;
import com.berdachuk.expertmatch.project.domain.Project;
import com.berdachuk.expertmatch.project.repository.ProjectRepository;
import com.berdachuk.expertmatch.technology.domain.Technology;
import com.berdachuk.expertmatch.technology.repository.TechnologyRepository;
import com.berdachuk.expertmatch.workexperience.domain.WorkExperience;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for generating synthetic test data for MVP development.
 * Uses Datafaker library for realistic synthetic data generation.
 *
 * <p>Datafaker is used for:
 * <ul>
 *   <li>Name generation (first and last names)</li>
 *   <li>Email addresses</li>
 *   <li>Company and customer names</li>
 *   <li>Project names</li>
 *   <li>Industries</li>
 *   <li>Job titles and positions</li>
 * </ul>
 *
 * <p>Domain-specific constants (technologies, tools, seniority levels, etc.) are kept as static arrays
 * as they are specific to the software development domain.
 */
@Slf4j
@Service
public class TestDataGenerator {

    private static final String SIARHEI_PROFILE_RESOURCE = "data/siarhei-berdachuk-profile.json";
    // Technology stacks
    private static final String[] TECHNOLOGIES = {
            "Java", "Spring Boot", "Python", "Node.js", "React", "Angular", "Vue.js",
            "PostgreSQL", "MongoDB", "Redis", "Kafka", "Docker", "Kubernetes",
            "AWS", "Azure", "GCP", "TypeScript", "JavaScript", "Go", "Rust"
    };
    // Technology categories and metadata
    private static final Map<String, String> TECHNOLOGY_CATEGORIES = createTechnologyCategoriesMap();
    // Technology synonyms
    private static final Map<String, String[]> TECHNOLOGY_SYNONYMS = createTechnologySynonymsMap();
    // Seniority levels
    private static final String[] SENIORITY_LEVELS = {"A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "C1", "C2"};
    // English levels
    private static final String[] ENGLISH_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"};
    // Project types
    private static final String[] PROJECT_TYPES = {
            "Web Application", "Microservices", "ETL Pipeline", "Data Analytics",
            "Mobile App", "API Development", "System Integration", "Cloud Migration"
    };
    // Tools
    private static final String[] TOOLS = {
            "IntelliJ IDEA", "VS Code", "Eclipse", "Git", "Jira", "Confluence",
            "Jenkins", "GitLab CI", "Docker Desktop", "Postman", "Swagger",
            "Kubernetes Dashboard", "AWS Console", "Azure Portal", "GCP Console"
    };
    // Team names
    private static final String[] TEAM_NAMES = {
            "Backend Team", "Frontend Team", "Full Stack Team", "DevOps Team",
            "Data Engineering Team", "Platform Team", "Integration Team", "QA Team"
    };
    // Banking domain knowledge
    private static final String[] BANKING_TECHNOLOGIES = {
            "Java", "Spring Boot", "Oracle Database", "IBM Mainframe", "COBOL",
            "PL/SQL", "MQ Series", "WebSphere", "TIBCO", "SWIFT", "ISO 20022",
            "Payment Gateway", "Core Banking System", "AML", "KYC", "Fraud Detection",
            "Risk Management", "Basel III", "RegTech", "FinTech", "Blockchain",
            "Cryptocurrency", "Digital Wallet", "Open Banking API", "P2P Lending"
    };
    private static final String[] BANKING_PROJECT_TYPES = {
            "Core Banking System", "Payment Processing", "Risk Management",
            "Compliance System", "Fraud Detection", "Digital Banking",
            "Mobile Banking App", "Open Banking API", "AML/KYC System",
            "Treasury Management", "Loan Management", "Credit Scoring"
    };
    private static final String[] BANKING_CUSTOMER_NAMES = {
            "Global Bank", "First National Bank", "Metropolitan Bank", "Union Bank",
            "Commercial Bank", "Investment Bank", "Retail Bank", "Digital Bank",
            "Regional Bank", "Community Bank", "Credit Union", "Savings Bank"
    };
    private static final String BANKING_INDUSTRY = "Banking and Financial Services";
    // Healthcare domain knowledge
    private static final String[] HEALTHCARE_TECHNOLOGIES = {
            "Java", "Spring Boot", "HL7", "FHIR", "DICOM", "HIPAA",
            "Epic", "Cerner", "Allscripts", "Electronic Health Records (EHR)",
            "Electronic Medical Records (EMR)", "Health Information Exchange (HIE)",
            "Clinical Decision Support System (CDSS)", "Telemedicine", "mHealth",
            "Medical Imaging", "PACS", "Radiology Information System (RIS)",
            "Laboratory Information System (LIS)", "Pharmacy Information System",
            "Patient Portal", "Healthcare Analytics", "Population Health Management",
            "Interoperability", "Medical Device Integration", "Wearable Health Devices"
    };
    private static final String[] HEALTHCARE_PROJECT_TYPES = {
            "Electronic Health Records (EHR) System", "Telemedicine Platform",
            "Medical Imaging System", "Clinical Decision Support System",
            "Patient Portal", "Healthcare Analytics Platform", "Population Health Management",
            "Health Information Exchange (HIE)", "Laboratory Information System",
            "Pharmacy Management System", "Medical Device Integration", "mHealth Application"
    };
    private static final String[] HEALTHCARE_CUSTOMER_NAMES = {
            "General Hospital", "Medical Center", "Community Health System",
            "Regional Healthcare Network", "University Medical Center", "Children's Hospital",
            "Memorial Hospital", "City Hospital", "County Medical Center",
            "Healthcare Group", "Health System", "Medical Group"
    };
    private static final String HEALTHCARE_INDUSTRY = "Healthcare and Medical Services";
    private final EmbeddingService embeddingService;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final TechnologyRepository technologyRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final Random random = new Random();
    /**
     * Datafaker instance for generating realistic synthetic test data.
     */
    private final Faker faker = new Faker();
    private final ObjectMapper objectMapper;
    /**
     * Optional service for LLM-based constant expansion.
     * May be null if not available in the environment.
     */
    @Autowired(required = false)
    private ConstantExpansionService constantExpansionService;
    /**
     * Expanded constants (lazily initialized when first needed).
     * If null, base constants are used.
     * Expansion happens only once when TestDataGenerator starts generating data.
     */
    private volatile List<String> expandedTechnologies;
    private volatile List<String> expandedTools;
    private volatile List<String> expandedProjectTypes;
    private volatile List<String> expandedTeamNames;
    private volatile Map<String, String> expandedTechnologyCategories;
    private volatile Map<String, String[]> expandedTechnologySynonyms;
    /**
     * Flag to ensure expansion happens only once.
     */
    private volatile boolean expansionInitialized = false;

    public TestDataGenerator(
            EmbeddingService embeddingService,
            EmployeeRepository employeeRepository,
            ProjectRepository projectRepository,
            TechnologyRepository technologyRepository,
            WorkExperienceRepository workExperienceRepository,
            ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.technologyRepository = technologyRepository;
        this.workExperienceRepository = workExperienceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates technology categories map.
     */
    private static Map<String, String> createTechnologyCategoriesMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Java", "Programming Language");
        map.put("Spring Boot", "Framework");
        map.put("Python", "Programming Language");
        map.put("Node.js", "Runtime");
        map.put("React", "Frontend Framework");
        map.put("Angular", "Frontend Framework");
        map.put("Vue.js", "Frontend Framework");
        map.put("PostgreSQL", "Database");
        map.put("MongoDB", "Database");
        map.put("Redis", "Database");
        map.put("Kafka", "Messaging");
        map.put("Docker", "Containerization");
        map.put("Kubernetes", "Orchestration");
        map.put("AWS", "Cloud Platform");
        map.put("Azure", "Cloud Platform");
        map.put("GCP", "Cloud Platform");
        map.put("TypeScript", "Programming Language");
        map.put("JavaScript", "Programming Language");
        map.put("Go", "Programming Language");
        map.put("Rust", "Programming Language");
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates technology synonyms map.
     */
    private static Map<String, String[]> createTechnologySynonymsMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put("Java", new String[]{"Java Programming", "Java Language"});
        map.put("Spring Boot", new String[]{"Spring", "Spring Framework"});
        map.put("Python", new String[]{"Python Programming", "Python Language"});
        map.put("Node.js", new String[]{"Node", "NodeJS"});
        map.put("React", new String[]{"React.js", "ReactJS"});
        map.put("Angular", new String[]{"AngularJS"});
        map.put("Vue.js", new String[]{"Vue", "VueJS"});
        map.put("PostgreSQL", new String[]{"Postgres", "Postgre"});
        map.put("MongoDB", new String[]{"Mongo"});
        map.put("Redis", new String[]{"Reddis"});
        map.put("Kafka", new String[]{"Apache Kafka"});
        map.put("Docker", new String[]{"Docker Engine"});
        map.put("Kubernetes", new String[]{"K8s", "Kubernetes Cluster"});
        map.put("AWS", new String[]{"Amazon Web Services"});
        map.put("Azure", new String[]{"Microsoft Azure"});
        map.put("GCP", new String[]{"Google Cloud Platform"});
        map.put("TypeScript", new String[]{"TS"});
        map.put("JavaScript", new String[]{"JS"});
        map.put("Go", new String[]{"Golang"});
        map.put("Rust", new String[]{"Rust Programming"});
        return Collections.unmodifiableMap(map);
    }

    /**
     * Lazily initializes expanded constants using LLM if expansion service is available.
     * Called only when TestDataGenerator starts generating data (first call to any getter method).
     * Expansion happens only once and results are cached.
     */
    private synchronized void ensureExpansionInitialized() {
        if (expansionInitialized) {
            return; // Already initialized
        }

        if (constantExpansionService == null) {
            log.debug("ConstantExpansionService not available, using base constants");
            expansionInitialized = true; // Mark as initialized to avoid repeated checks
            return;
        }

        try {
            log.info("Initializing expanded constants using LLM (lazy initialization on first use)...");

            // Expand technologies
            List<String> baseTechnologies = Arrays.asList(TECHNOLOGIES);
            expandedTechnologies = constantExpansionService.expandTechnologies(baseTechnologies);
            log.info("Technologies expanded: {} -> {}", baseTechnologies.size(), expandedTechnologies.size());

            // Expand tools
            List<String> baseTools = Arrays.asList(TOOLS);
            expandedTools = constantExpansionService.expandTools(baseTools);
            log.info("Tools expanded: {} -> {}", baseTools.size(), expandedTools.size());

            // Expand project types
            List<String> baseProjectTypes = Arrays.asList(PROJECT_TYPES);
            expandedProjectTypes = constantExpansionService.expandProjectTypes(baseProjectTypes);
            log.info("Project types expanded: {} -> {}", baseProjectTypes.size(), expandedProjectTypes.size());

            // Expand team names
            List<String> baseTeamNames = Arrays.asList(TEAM_NAMES);
            expandedTeamNames = constantExpansionService.expandTeamNames(baseTeamNames);
            log.info("Team names expanded: {} -> {}", baseTeamNames.size(), expandedTeamNames.size());

            // Expand technology categories (use expanded technologies if available)
            List<String> technologies = expandedTechnologies != null ? expandedTechnologies : Arrays.asList(TECHNOLOGIES);
            expandedTechnologyCategories = constantExpansionService.expandTechnologyCategories(
                    technologies, TECHNOLOGY_CATEGORIES);
            log.info("Technology categories expanded: {} -> {}",
                    TECHNOLOGY_CATEGORIES.size(), expandedTechnologyCategories.size());

            // Expand technology synonyms (use expanded technologies if available)
            expandedTechnologySynonyms = constantExpansionService.expandTechnologySynonyms(
                    technologies, TECHNOLOGY_SYNONYMS);
            log.info("Technology synonyms expanded: {} -> {}",
                    TECHNOLOGY_SYNONYMS.size(), expandedTechnologySynonyms.size());

            log.info("Constant expansion completed successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize expanded constants, using base constants: {}", e.getMessage());
            // Expanded constants remain null, will use base constants
        } finally {
            expansionInitialized = true; // Mark as initialized even on failure
        }
    }

    /**
     * Gets technologies list, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private List<String> getTechnologies() {
        ensureExpansionInitialized();
        return expandedTechnologies != null ? expandedTechnologies : Arrays.asList(TECHNOLOGIES);
    }

    /**
     * Gets tools list, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private List<String> getTools() {
        ensureExpansionInitialized();
        return expandedTools != null ? expandedTools : Arrays.asList(TOOLS);
    }

    /**
     * Gets project types list, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private List<String> getProjectTypes() {
        ensureExpansionInitialized();
        return expandedProjectTypes != null ? expandedProjectTypes : Arrays.asList(PROJECT_TYPES);
    }

    /**
     * Gets team names list, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private List<String> getTeamNames() {
        ensureExpansionInitialized();
        return expandedTeamNames != null ? expandedTeamNames : Arrays.asList(TEAM_NAMES);
    }

    /**
     * Gets technology categories map, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private Map<String, String> getTechnologyCategories() {
        ensureExpansionInitialized();
        return expandedTechnologyCategories != null ? expandedTechnologyCategories : TECHNOLOGY_CATEGORIES;
    }

    /**
     * Gets technology synonyms map, using expanded version if available.
     * Triggers lazy initialization on first call.
     */
    private Map<String, String[]> getTechnologySynonyms() {
        ensureExpansionInitialized();
        return expandedTechnologySynonyms != null ? expandedTechnologySynonyms : TECHNOLOGY_SYNONYMS;
    }

    /**
     * Generates employee records.
     */
    private void generateEmployees(int count) {
        Set<String> usedEmails = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateEmployeeId();
            String name = generateName();
            String email = generateUniqueEmail(name, usedEmails);
            String seniority = SENIORITY_LEVELS[random.nextInt(SENIORITY_LEVELS.length)];
            String englishLevel = ENGLISH_LEVELS[random.nextInt(ENGLISH_LEVELS.length)];

            Employee employee = new Employee(
                    id,
                    name,
                    email,
                    seniority,
                    englishLevel,
                    null // availabilityStatus
            );

            try {
                employeeRepository.createOrUpdate(employee);
                usedEmails.add(email);
            } catch (Exception e) {
                // If id conflict or other error, skip this employee
                log.warn("Failed to insert employee with email {}: {}", email, e.getMessage());
            }
        }
    }

    /**
     * Generates test data based on size parameter.
     *
     * @param size          Predefined size: "tiny", "small", "medium", "large", or "huge"
     * @param clearExisting If true, clears all existing test data before generation
     */
    public void generateTestData(String size, boolean clearExisting) {
        if (clearExisting) {
            log.info("Clearing existing test data before generation...");
            clearTestData();
        }

        generateTestData(size);
    }

    /**
     * Generates test data based on size parameter.
     * Does not clear existing data - new data will be appended.
     *
     * @param size Predefined size: "tiny", "small", "medium", "large", or "huge"
     */
    public void generateTestData(String size) {
        int employeeCount;
        int workExpPerEmployee;
        int projectCount;

        switch (size.toLowerCase()) {
            case "tiny" -> {
                employeeCount = 5;
                workExpPerEmployee = 3;
                projectCount = 5;
            }
            case "small" -> {
                employeeCount = 50;
                workExpPerEmployee = 5;
                projectCount = 100;
            }
            case "medium" -> {
                employeeCount = 500;
                workExpPerEmployee = 8;
                projectCount = 1000;
            }
            case "large" -> {
                employeeCount = 2000;
                workExpPerEmployee = 10;
                projectCount = 4000;
            }
            case "huge" -> {
                employeeCount = 50000;
                workExpPerEmployee = 15;
                projectCount = 100000;
            }
            default -> {
                // Default to small if invalid size provided
                log.warn("Invalid size '{}', defaulting to 'small'", size);
                employeeCount = 50;
                workExpPerEmployee = 5;
                projectCount = 100;
            }
        }

        // Generate technology catalog first
        generateTechnologies();

        // Generate projects first
        Map<String, String> projects = generateProjects(projectCount);

        // Generate employees
        generateEmployees(employeeCount);

        // Generate work experience referencing projects
        generateWorkExperience(employeeCount, workExpPerEmployee, projects);

        // Generate specific employee: Siarhei Berdachuk
        generateSiarheiBerdachukData(projects);
    }

    /**
     * Clears all test data from the database in a single transaction.
     * Order: work_experience (FK to employee), employees, projects, technologies.
     * On failure all deletes roll back.
     */
    @Transactional
    public void clearTestData() {
        log.info("Clearing all test data...");

        try {
            // Delete work experience first (due to foreign key constraints)
            int workExpDeleted = workExperienceRepository.deleteAll();
            log.info("Deleted {} work experience records", workExpDeleted);

            // Delete employees
            int employeesDeleted = employeeRepository.deleteAll();
            log.info("Deleted {} employee records", employeesDeleted);

            // Delete projects
            int projectsDeleted = projectRepository.deleteAll();
            log.info("Deleted {} project records", projectsDeleted);

            // Delete technologies
            int techDeleted = technologyRepository.deleteAll();
            log.info("Deleted {} technology records", techDeleted);

            log.info("Test data cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear test data", e);
            throw new RuntimeException("Failed to clear test data", e);
        }
    }

    /**
     * Generates technology catalog in the technology table.
     */
    private void generateTechnologies() {
        List<String> techList = getTechnologies();
        Map<String, String> categories = getTechnologyCategories();
        Map<String, String[]> synonymsMap = getTechnologySynonyms();

        for (String technology : techList) {
            String id = IdGenerator.generateId();
            String normalizedName = normalizeTechnologyName(technology);
            String category = categories.getOrDefault(technology, "Other");
            List<String> synonyms = List.of(synonymsMap.getOrDefault(technology, new String[0]));

            Technology tech = new Technology(
                    id,
                    technology,
                    normalizedName,
                    category,
                    synonyms
            );

            try {
                technologyRepository.createOrUpdate(tech);
            } catch (Exception e) {
                log.warn("Failed to insert technology {}: {}", technology, e.getMessage());
            }
        }
    }

    /**
     * Normalizes technology name for consistent matching.
     */
    private String normalizeTechnologyName(String technology) {
        return technology.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }

    /**
     * Generates project records.
     */
    private Map<String, String> generateProjects(int count) {
        Map<String, String> projectMap = new HashMap<>(); // project_id -> project_name

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateProjectId();
            String name = generateProjectName();
            String summary = generateProjectSummary();
            List<String> projectTypes = getProjectTypes();
            String projectType = projectTypes.get(random.nextInt(projectTypes.size()));

            // Generate technologies (2-5 random technologies)
            int techCount = 2 + random.nextInt(4);
            List<String> technologies = new ArrayList<>();
            Set<String> selectedTechs = new HashSet<>();
            List<String> techList = getTechnologies();
            while (selectedTechs.size() < techCount) {
                selectedTechs.add(techList.get(random.nextInt(techList.size())));
            }
            technologies.addAll(selectedTechs);

            Project project = new Project(
                    id,
                    name,
                    summary,
                    null, // link
                    projectType,
                    technologies,
                    null, // customerId
                    null, // customerName
                    null  // industry
            );

            try {
                projectRepository.createOrUpdate(project);
                projectMap.put(id, name);
            } catch (Exception e) {
                log.warn("Failed to insert project with name {}: {}", name, e.getMessage());
            }
        }

        return projectMap;
    }

    /**
     * Generates embeddings for all work experience records.
     */
    public void generateEmbeddings() {
        List<WorkExperience> records = workExperienceRepository.findWithoutEmbeddings();

        int totalRecords = records.size();
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;

        log.info("Starting embedding generation for {} work experience records", totalRecords);

        // Generate embeddings in batches
        for (WorkExperience workExperience : records) {
            processedCount++;
            try {
                String text = buildEmbeddingText(workExperience);
                long embeddingStartTime = System.currentTimeMillis();
                List<Double> embedding = embeddingService.generateEmbedding(text);
                long embeddingEndTime = System.currentTimeMillis();

                if (!embedding.isEmpty()) {
                    int originalDimension = embedding.size();
                    workExperienceRepository.updateEmbedding(workExperience.id(), embedding, originalDimension);
                    successCount++;
                }

                // Log progress every 100 items
                if (processedCount % 100 == 0) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime;
                    double itemsPerSecond = (processedCount * 1000.0) / elapsedTime;
                    double avgEmbeddingTime = (embeddingEndTime - embeddingStartTime) / 1000.0;

                    log.info(String.format("Progress: %d/%d records processed (%d%% complete), Success: %d, Failed: %d, " +
                                    "Items/sec: %.2f, Avg embedding time: %.3fs",
                            processedCount, totalRecords,
                            (processedCount * 100 / totalRecords),
                            successCount, failedCount,
                            itemsPerSecond, avgEmbeddingTime));
                }
            } catch (Exception e) {
                failedCount++;
                log.warn("Failed to generate embedding for work experience record: {}",
                        workExperience.id(), e);
            }
        }

        // Final summary
        long endTime = System.currentTimeMillis();
        long totalElapsedTime = endTime - startTime;
        double totalItemsPerSecond = (processedCount * 1000.0) / totalElapsedTime;

        log.info(String.format("Embedding generation completed. Total: %d, Success: %d, Failed: %d, " +
                        "Total time: %.3fs, Overall rate: %.2f items/sec",
                processedCount, successCount, failedCount,
                totalElapsedTime / 1000.0, totalItemsPerSecond));
    }

    /**
     * Builds text for embedding generation.
     */
    private String buildEmbeddingText(WorkExperience workExperience) {
        StringBuilder text = new StringBuilder();

        if (workExperience.projectSummary() != null) {
            text.append(workExperience.projectSummary()).append(" ");
        }

        if (workExperience.responsibilities() != null) {
            text.append(workExperience.responsibilities()).append(" ");
        }

        if (workExperience.technologies() != null && !workExperience.technologies().isEmpty()) {
            text.append("Technologies: ").append(String.join(", ", workExperience.technologies()));
        }

        return text.toString().trim();
    }


    // Helper methods for generating random data

    /**
     * Generates a random name using Datafaker.
     */
    private String generateName() {
        return faker.name().firstName() + " " + faker.name().lastName();
    }

    /**
     * Generates an email address using Datafaker.
     * Note: The name parameter is kept for backward compatibility but not used.
     */
    private String generateEmail(String name) {
        // Use safeEmailAddress for better uniqueness and example.com domain
        return faker.internet().safeEmailAddress();
    }

    /**
     * Generates a unique email address using Datafaker.
     */
    private String generateUniqueEmail(String name, Set<String> usedEmails) {
        String email = faker.internet().safeEmailAddress();
        int counter = 1;

        // Ensure email is unique by appending a counter if needed
        while (usedEmails.contains(email)) {
            // Generate new email with counter suffix
            String base = email.substring(0, email.indexOf("@"));
            email = base + counter + "@example.com";
            counter++;
        }

        return email;
    }

    /**
     * Generates project name using Datafaker.
     */
    private String generateProjectName() {
        return faker.company().buzzword() + " " +
                faker.company().buzzword() + " " +
                faker.number().numberBetween(1000, 9999);
    }

    private String generateProjectSummary() {
        String[] templates = {
                "Developed and maintained %s application using %s",
                "Built scalable %s system with %s",
                "Implemented %s solution leveraging %s",
                "Designed and developed %s platform using %s"
        };
        String template = templates[random.nextInt(templates.length)];
        List<String> projectTypes = getProjectTypes();
        List<String> techList = getTechnologies();
        String projectType = projectTypes.get(random.nextInt(projectTypes.size()));
        String technology = techList.get(random.nextInt(techList.size()));
        return String.format(template, projectType.toLowerCase(), technology);
    }

    /**
     * Generates customer name using Datafaker.
     */
    private String generateCustomerName() {
        return faker.company().name();
    }

    private String generateResponsibilities(String role, List<String> technologies) {
        return String.format(
                "Responsible for %s development using %s. " +
                        "Collaborated with cross-functional teams to deliver high-quality software solutions.",
                role.toLowerCase(),
                String.join(", ", technologies)
        );
    }

    /**
     * Generates company name using Datafaker.
     */
    private String generateCompany() {
        return faker.company().name();
    }

    /**
     * Generates company URL.
     */
    private String generateCompanyUrl(String company) {
        String base = company.toLowerCase().replace(" ", "").replace(".", "").replace("llc", "").replace("inc", "").replace("ltd", "");
        return "https://www." + base + ".com";
    }

    /**
     * Generates is_company_berdachuk boolean.
     */
    private boolean generateIsCompanyBerdachuk(String company) {
        return false;
    }

    /**
     * Generates team name/description.
     */
    private String generateTeam() {
        List<String> teamNames = getTeamNames();
        return teamNames.get(random.nextInt(teamNames.size()));
    }

    /**
     * Generates tools as comma-separated text.
     */
    private String generateTools(List<String> technologies) {
        int toolCount = 2 + random.nextInt(3); // 2-4 tools
        List<String> selectedTools = new ArrayList<>();
        Set<String> usedTools = new HashSet<>();

        List<String> tools = getTools();
        while (selectedTools.size() < toolCount) {
            String tool = tools.get(random.nextInt(tools.size()));
            if (!usedTools.contains(tool)) {
                selectedTools.add(tool);
                usedTools.add(tool);
            }
        }

        return String.join(", ", selectedTools);
    }

    /**
     * Generates tools_ref as JSONB array.
     */
    private List<Map<String, Object>> generateToolsRef(String toolsText) {
        List<Map<String, Object>> toolsRef = new ArrayList<>();
        String[] tools = toolsText.split(", ");

        for (String tool : tools) {
            Map<String, Object> toolRef = new HashMap<>();
            toolRef.put("id", "tool." + tool.toLowerCase().replace(" ", ".").replace(".", ""));
            toolRef.put("name", tool);
            toolRef.put("type", "Tool");
            toolsRef.add(toolRef);
        }

        return toolsRef;
    }

    /**
     * Generates technologies_ref as JSONB array.
     */
    private List<Map<String, Object>> generateTechnologiesRef(List<String> technologies) {
        List<Map<String, Object>> technologiesRef = new ArrayList<>();

        for (String tech : technologies) {
            Map<String, Object> techRef = new HashMap<>();
            techRef.put("id", "tech." + normalizeTechnologyName(tech));
            techRef.put("name", tech);
            techRef.put("type", "Technology");
            technologiesRef.add(techRef);
        }

        return technologiesRef;
    }

    /**
     * Generates project_role as text (comma-separated roles).
     */
    private String generateProjectRole(String primaryRole, List<String> extraRoles) {
        List<String> allRoles = new ArrayList<>();
        allRoles.add(primaryRole);
        allRoles.addAll(extraRoles);
        return String.join(", ", allRoles);
    }

    /**
     * Generates primary_project_role as JSONB object.
     */
    private Map<String, Object> generatePrimaryProjectRole(String role) {
        Map<String, Object> roleRef = new HashMap<>();
        roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
        roleRef.put("name", role);
        roleRef.put("type", "Role");
        return roleRef;
    }

    /**
     * Generates extra_project_roles as JSONB array.
     */
    private List<Map<String, Object>> generateExtraProjectRoles(List<String> extraRoles) {
        List<Map<String, Object>> extraRolesRef = new ArrayList<>();

        for (String role : extraRoles) {
            Map<String, Object> roleRef = new HashMap<>();
            roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
            roleRef.put("name", role);
            roleRef.put("type", "Role");
            extraRolesRef.add(roleRef);
        }

        return extraRolesRef;
    }

    /**
     * Generates all_project_roles as JSONB array (includes primary + extra).
     */
    private List<Map<String, Object>> generateAllProjectRoles(String primaryRole, List<String> extraRoles) {
        List<Map<String, Object>> allRoles = new ArrayList<>();

        // Add primary role with is_custom flag
        Map<String, Object> primaryRoleRef = new HashMap<>();
        primaryRoleRef.put("id", "custom." + primaryRole.toLowerCase().replace(" ", "."));
        primaryRoleRef.put("name", primaryRole);
        primaryRoleRef.put("type", "Role");
        primaryRoleRef.put("is_custom", true);
        allRoles.add(primaryRoleRef);

        // Add extra roles with is_custom flag
        for (String role : extraRoles) {
            Map<String, Object> roleRef = new HashMap<>();
            roleRef.put("id", "custom." + role.toLowerCase().replace(" ", "."));
            roleRef.put("name", role);
            roleRef.put("type", "Role");
            roleRef.put("is_custom", true);
            allRoles.add(roleRef);
        }

        return allRoles;
    }

    /**
     * Generates customer description.
     */
    private String generateCustomerDescription(String customerName) {
        String[] templates = {
                "%s is a leading company in the industry.",
                "%s is a global provider of innovative solutions.",
                "%s is a well-established company with a strong market presence.",
                "%s is a technology-driven organization focused on excellence."
        };
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, customerName);
    }

    /**
     * Generates position text using Datafaker.
     */
    private String generatePosition() {
        return faker.job().title();
    }

    /**
     * Generates participation text (maps to responsibilities).
     */
    private String generateParticipation(String role, List<String> technologies) {
        String[] templates = {
                "• Development using %s.",
                "• %s development and implementation.",
                "• Responsible for %s development with %s.",
                "• %s development, testing, and deployment using %s."
        };
        String template = templates[random.nextInt(templates.length)];
        return String.format(template, role, String.join(", ", technologies));
    }

    /**
     * Builds metadata JSONB with all CSV-aligned fields.
     */
    private Map<String, Object> buildMetadataJson(
            String company, String companyUrl, boolean isCompanyBerdachuk,
            String team, String tools, List<Map<String, Object>> toolsRef,
            List<Map<String, Object>> technologiesRef,
            String customerDescription, String position,
            String projectRole, Map<String, Object> primaryProjectRole,
            List<Map<String, Object>> extraProjectRoles,
            List<Map<String, Object>> allProjectRoles,
            String participation, String projectDescription) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("company", company);
        metadata.put("company_url", companyUrl);
        metadata.put("is_company_berdachuk", isCompanyBerdachuk);
        metadata.put("team", team);
        metadata.put("tools", tools);
        metadata.put("tools_ref", toolsRef);
        metadata.put("technologies_ref", technologiesRef);
        metadata.put("customer_description", customerDescription);
        metadata.put("position", position);
        metadata.put("project_role", projectRole);
        metadata.put("primary_project_role", primaryProjectRole);
        metadata.put("extra_project_roles", extraProjectRoles);
        metadata.put("all_project_roles", allProjectRoles);
        metadata.put("participation", participation);
        metadata.put("project_description", projectDescription);

        return metadata;
    }

    /**
     * Generates work experience records.
     */
    private void generateWorkExperience(int employeeCount, int workExpPerEmployee,
                                        Map<String, String> projects) {
        // Get all employee IDs
        List<String> employeeIds = employeeRepository.findAllIds(employeeCount);

        // Get all project IDs
        List<String> projectIds = new ArrayList<>(projects.keySet());

        for (String employeeId : employeeIds) {
            for (int i = 0; i < workExpPerEmployee; i++) {
                generateWorkExperienceRecord(employeeId, projectIds, projects);
            }
        }
    }

    /**
     * Generates a single work experience record.
     */
    private void generateWorkExperienceRecord(String employeeId, List<String> projectIds,
                                              Map<String, String> projects) {
        String id = IdGenerator.generateId();

        // Select random project
        String projectId = projectIds.get(random.nextInt(projectIds.size()));
        String projectName = projects.get(projectId);

        String projectSummary = generateProjectSummary();
        String projectDescription = projectSummary; // CSV field: project_description maps to project_summary

        // Generate primary role and extra roles using Datafaker
        String primaryRole = faker.job().title();
        int extraRoleCount = random.nextInt(2); // 0-1 extra roles
        List<String> extraRoles = new ArrayList<>();
        Set<String> usedRoles = new HashSet<>();
        usedRoles.add(primaryRole);

        while (extraRoles.size() < extraRoleCount) {
            String role = faker.job().title();
            if (!usedRoles.contains(role)) {
                extraRoles.add(role);
                usedRoles.add(role);
            }
        }

        String role = primaryRole; // Use primary role for direct column
        String projectRole = generateProjectRole(primaryRole, extraRoles);

        String industry = faker.company().industry();
        String customerName = generateCustomerName();
        String customerDescription = generateCustomerDescription(customerName);

        // Generate dates (random project in last 5 years)
        LocalDate startDate = LocalDate.now().minusYears(5)
                .plusDays(random.nextInt(1825)); // 5 years in days
        LocalDate endDate = startDate.plusMonths(6 + random.nextInt(24)); // 6-30 months duration

        // Generate technologies (2-5 random technologies)
        int techCount = 2 + random.nextInt(4);
        List<String> technologies = new ArrayList<>();
        Set<String> selectedTechs = new HashSet<>();
        while (selectedTechs.size() < techCount) {
            selectedTechs.add(TECHNOLOGIES[random.nextInt(TECHNOLOGIES.length)]);
        }
        technologies.addAll(selectedTechs);

        String responsibilities = generateResponsibilities(role, technologies);
        String participation = generateParticipation(role, technologies); // CSV field: participation

        // Generate CSV-aligned fields
        String company = generateCompany();
        String companyUrl = generateCompanyUrl(company);
        boolean isCompanyBerdachuk = generateIsCompanyBerdachuk(company);
        String team = generateTeam();
        String tools = generateTools(technologies);
        List<Map<String, Object>> toolsRef = generateToolsRef(tools);
        List<Map<String, Object>> technologiesRef = generateTechnologiesRef(technologies);
        String position = generatePosition();
        Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
        List<Map<String, Object>> extraProjectRoles = generateExtraProjectRoles(extraRoles);
        List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, extraRoles);

        // Build metadata JSONB with all CSV-aligned fields
        Map<String, Object> metadata = buildMetadataJson(
                company, companyUrl, isCompanyBerdachuk,
                team, tools, toolsRef,
                technologiesRef,
                customerDescription, position,
                projectRole, primaryProjectRole,
                extraProjectRoles, allProjectRoles,
                participation, projectDescription
        );

        // Convert metadata to JSON string for PostgreSQL JSONB
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON, using empty object: {}", e.getMessage());
            metadataJson = "{}";
        }

        // Convert LocalDate to Instant for WorkExperience domain entity
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        WorkExperience workExperience = new WorkExperience(
                id,
                employeeId,
                projectId,
                null, // customerId
                projectName,
                customerName,
                industry,
                role,
                startInstant,
                endInstant,
                projectSummary,
                responsibilities,
                technologies
        );

        try {
            workExperienceRepository.createOrUpdate(workExperience, metadataJson);
        } catch (Exception e) {
            log.warn("Failed to insert work experience for {}: {}", projectName, e.getMessage());
        }
    }

    /**
     * Generates specific employee data for Siarhei Berdachuk with ID 4000741400013306668.
     * This is a real-world example profile with anonymized company names.
     * Profile data is loaded from JSON file: data/siarhei-berdachuk-profile.json
     */
    private void generateSiarheiBerdachukData(Map<String, String> projects) {
        try {
            // Load profile data from JSON file
            EmployeeProfile profile = loadSiarheiBerdachukProfile();

            // Apply defaults to handle optional fields
            var employeeProfile = profile.employee().withDefaults();
            String employeeId = employeeProfile.id();
            String name = employeeProfile.name();
            String email = employeeProfile.email();
            String seniority = employeeProfile.seniority();
            String englishLevel = employeeProfile.languageEnglish();
            String availabilityStatus = employeeProfile.availabilityStatus();

            // Insert or update employee
            Employee employee = new Employee(
                    employeeId,
                    name,
                    email,
                    seniority,
                    englishLevel,
                    availabilityStatus
            );

            try {
                employeeRepository.createOrUpdate(employee);
                log.info("Created/updated employee: {} ({})", name, employeeId);
            } catch (Exception e) {
                log.warn("Failed to insert employee {}: {}", name, e.getMessage());
                return; // Don't continue if employee creation failed
            }

            // Generate work experience records from JSON profile
            if (profile.projects() != null && !profile.projects().isEmpty()) {
                for (ProjectData projectData : profile.projects()) {
                    // Apply defaults to handle optional fields
                    var project = projectData.withDefaults();
                    createWorkExperienceRecord(
                            employeeId, projects,
                            project.projectCode(),
                            project.projectName(),
                            project.customerName(),
                            project.companyName(),
                            project.role(),
                            LocalDate.parse(project.startDate()),
                            LocalDate.parse(project.endDate()),
                            project.technologies().toArray(new String[0]),
                            project.responsibilities(),
                            project.industry(),
                            project.projectSummary()
                    );
                }
            }

            log.info("Created work experience records for Siarhei Berdachuk from JSON profile");
        } catch (Exception e) {
            log.error("Failed to load or process Siarhei Berdachuk profile from JSON: {}", e.getMessage(), e);
            // Don't throw - allow test data generation to continue without this specific profile
        }
    }

    /**
     * Loads Siarhei Berdachuk profile data from JSON resource file.
     *
     * @return EmployeeProfile with employee data and projects
     * @throws IOException if the resource file cannot be read or parsed
     */
    private EmployeeProfile loadSiarheiBerdachukProfile() throws IOException {
        ClassPathResource resource = new ClassPathResource(SIARHEI_PROFILE_RESOURCE);

        if (!resource.exists()) {
            throw new IOException("Profile resource file not found: " + SIARHEI_PROFILE_RESOURCE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, EmployeeProfile.class);
        }
    }

    /**
     * Helper method to create a work experience record for Siarhei Berdachuk.
     * Checks for existing records to avoid duplicates.
     */
    private void createWorkExperienceRecord(String employeeId, Map<String, String> projects,
                                            String projectCode, String projectName,
                                            String customerName, String companyName,
                                            String role, LocalDate startDate, LocalDate endDate,
                                            String[] technologies, String responsibilities, String industry, String projectSummary) {
        // Check if work experience already exists for this employee, project, and start date
        if (workExperienceRepository.exists(employeeId, projectName, startDate)) {
            log.debug("Work experience already exists for {} at {} starting {}, skipping", employeeId, projectName, startDate);
            return; // Skip if already exists
        }

        String id = IdGenerator.generateId();

        // Use existing project if available, otherwise create a new one
        String projectId = projects.entrySet().stream()
                .filter(e -> e.getValue().toLowerCase().contains(projectName.toLowerCase().substring(0, Math.min(10, projectName.length()))))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(IdGenerator.generateProjectId());

        // If project doesn't exist, create it
        if (!projects.containsKey(projectId)) {
            Project project = new Project(
                    projectId,
                    projectName,
                    null, // summary
                    null, // link
                    null, // projectType
                    null, // technologies
                    IdGenerator.generateCustomerId(),
                    customerName,
                    industry
            );
            try {
                projectRepository.createOrUpdate(project);
                projects.put(projectId, projectName);
            } catch (Exception e) {
                log.warn("Failed to create project {}: {}", projectName, e.getMessage());
            }
        }

        // Build metadata with all required fields
        String toolsText = String.join(", ", technologies);
        List<Map<String, Object>> toolsRef = generateToolsRef(toolsText);
        List<String> technologiesList = Arrays.asList(technologies);
        List<Map<String, Object>> technologiesRef = generateTechnologiesRef(technologiesList);

        // Parse role (may contain multiple roles separated by comma)
        String[] roleParts = role.split(",\\s*");
        String primaryRole = roleParts[0].trim();
        List<String> extraRoles = roleParts.length > 1
                ? Arrays.asList(Arrays.copyOfRange(roleParts, 1, roleParts.length))
                : new ArrayList<>();

        String projectRole = generateProjectRole(primaryRole, extraRoles);
        Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
        List<Map<String, Object>> extraProjectRoles = generateExtraProjectRoles(extraRoles);
        List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, extraRoles);
        String customerDescription = generateCustomerDescription(customerName);
        String participation = generateParticipation(primaryRole, technologiesList);

        Map<String, Object> metadata = buildMetadataJson(
                companyName, "", false,
                "Development Team", toolsText, toolsRef, technologiesRef,
                customerDescription, role, // Use role as position
                projectRole, primaryProjectRole, extraProjectRoles, allProjectRoles,
                participation, projectSummary
        );

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            metadataJson = "{}";
        }

        // Convert LocalDate to Instant for WorkExperience domain entity
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        WorkExperience workExperience = new WorkExperience(
                id,
                employeeId,
                projectId,
                null, // customerId
                projectName,
                customerName,
                industry,
                role,
                startInstant,
                endInstant,
                projectSummary,
                responsibilities,
                Arrays.asList(technologies)
        );

        try {
            workExperienceRepository.createOrUpdate(workExperience, metadataJson);
        } catch (Exception e) {
            log.warn("Failed to insert work experience for {}: {}", projectName, e.getMessage());
        }
    }

    /**
     * Generates a small subset of test data with banking domain knowledge.
     * Creates employees, projects, and work experiences focused on banking and financial services.
     *
     * @param employeeCount      Number of employees to generate (default: 10)
     * @param workExpPerEmployee Number of work experiences per employee (default: 2-3)
     * @param projectCount       Number of banking projects to generate (default: 15)
     */
    public void generateBankingDomainSubset(int employeeCount, int workExpPerEmployee, int projectCount) {
        log.info("Generating banking domain subset: {} employees, {} projects, ~{} work experiences",
                employeeCount, projectCount, employeeCount * workExpPerEmployee);

        // Generate banking-specific employees
        List<String> employeeIds = generateBankingEmployees(employeeCount);

        // Generate banking-specific projects
        Map<String, String> projects = generateBankingProjects(projectCount);
        List<String> projectIds = new ArrayList<>(projects.keySet());

        int totalWorkExp = 0;
        for (String employeeId : employeeIds) {
            int expCount = workExpPerEmployee + random.nextInt(2); // workExpPerEmployee to workExpPerEmployee+1
            for (int i = 0; i < expCount; i++) {
                generateBankingWorkExperience(employeeId, projectIds, projects);
                totalWorkExp++;
            }
        }

        log.info("Banking domain subset generation completed: {} employees, {} projects, {} work experiences",
                employeeIds.size(), projects.size(), totalWorkExp);
    }

    /**
     * Generates employees for banking domain subset.
     *
     * @return List of generated employee IDs
     */
    private List<String> generateBankingEmployees(int count) {
        List<Map<String, Object>> employees = new ArrayList<>();
        List<String> employeeIds = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateEmployeeId();
            String name = generateName();
            String email = generateUniqueEmail(name, usedEmails);
            String seniority = SENIORITY_LEVELS[random.nextInt(SENIORITY_LEVELS.length)];
            String englishLevel = ENGLISH_LEVELS[random.nextInt(ENGLISH_LEVELS.length)];

            Map<String, Object> employee = new HashMap<>();
            employee.put("id", id);
            employee.put("name", name);
            employee.put("email", email);
            employee.put("seniority", seniority);
            employee.put("language_english", englishLevel);

            employees.add(employee);
            employeeIds.add(id);
        }

        // Use repository to create employees
        for (Map<String, Object> employeeMap : employees) {
            Employee employeeEntity = new Employee(
                    (String) employeeMap.get("id"),
                    (String) employeeMap.get("name"),
                    (String) employeeMap.get("email"),
                    (String) employeeMap.get("seniority"),
                    (String) employeeMap.get("language_english"),
                    null // availabilityStatus
            );
            try {
                employeeRepository.createOrUpdate(employeeEntity);
            } catch (Exception e) {
                log.warn("Failed to insert employee {}: {}", employeeMap.get("name"), e.getMessage());
            }
        }

        return employeeIds;
    }

    /**
     * Generates banking-specific projects.
     */
    private Map<String, String> generateBankingProjects(int count) {
        Map<String, String> projectMap = new HashMap<>();
        List<Map<String, Object>> projects = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateProjectId();
            String projectType = BANKING_PROJECT_TYPES[random.nextInt(BANKING_PROJECT_TYPES.length)];
            String name = generateBankingProjectName(projectType);
            String summary = generateBankingProjectSummary(projectType);

            // Generate banking technologies for the project
            int techCount = 2 + random.nextInt(4);
            List<String> technologies = new ArrayList<>();
            Set<String> selectedTechs = new HashSet<>();
            while (selectedTechs.size() < techCount) {
                selectedTechs.add(BANKING_TECHNOLOGIES[random.nextInt(BANKING_TECHNOLOGIES.length)]);
            }
            technologies.addAll(selectedTechs);

            Map<String, Object> project = new HashMap<>();
            project.put("id", id);
            project.put("name", name);
            project.put("summary", summary);
            project.put("projectType", projectType);
            project.put("technologies", technologies.toArray(new String[0]));

            projects.add(project);
            projectMap.put(id, name);
        }

        // Use repository to create projects
        for (Map<String, Object> projectMapItem : projects) {
            Project projectEntity = new Project(
                    (String) projectMapItem.get("id"),
                    (String) projectMapItem.get("name"),
                    (String) projectMapItem.get("summary"),
                    null, // link
                    (String) projectMapItem.get("projectType"),
                    Arrays.asList((String[]) projectMapItem.get("technologies")),
                    null, // customerId
                    null, // customerName
                    null  // industry
            );
            try {
                projectRepository.createOrUpdate(projectEntity);
            } catch (Exception e) {
                log.warn("Failed to insert project {}: {}", projectMapItem.get("name"), e.getMessage());
            }
        }

        return projectMap;
    }

    /**
     * Generates a banking-specific work experience record.
     */
    private void generateBankingWorkExperience(String employeeId, List<String> projectIds, Map<String, String> projects) {
        String id = IdGenerator.generateId();

        // Select random project
        String projectId = projectIds.get(random.nextInt(projectIds.size()));
        String projectName = projects.get(projectId);

        String projectType = BANKING_PROJECT_TYPES[random.nextInt(BANKING_PROJECT_TYPES.length)];
        String projectSummary = generateBankingProjectSummary(projectType);
        String customerName = BANKING_CUSTOMER_NAMES[random.nextInt(BANKING_CUSTOMER_NAMES.length)];

        // Generate banking-specific role
        String primaryRole = generateBankingRole();
        String role = primaryRole;
        String projectRole = generateProjectRole(primaryRole, new ArrayList<>());

        // Generate banking-specific technologies (2-5 technologies from banking tech stack)
        int techCount = 2 + random.nextInt(4);
        List<String> technologies = new ArrayList<>();
        Set<String> selectedTechs = new HashSet<>();
        while (selectedTechs.size() < techCount) {
            selectedTechs.add(BANKING_TECHNOLOGIES[random.nextInt(BANKING_TECHNOLOGIES.length)]);
        }
        technologies.addAll(selectedTechs);

        String responsibilities = generateBankingResponsibilities(primaryRole, technologies, projectType);

        // Generate dates (random project in last 5 years)
        LocalDate startDate = LocalDate.now().minusYears(5)
                .plusDays(random.nextInt(1825)); // 5 years in days
        LocalDate endDate = startDate.plusMonths(6 + random.nextInt(24)); // 6-30 months duration

        // Build metadata
        String company = generateCompany();
        String companyUrl = generateCompanyUrl(company);
        boolean isCompanyBerdachuk = generateIsCompanyBerdachuk(company);
        String team = generateTeam();
        String tools = generateTools(technologies);
        List<Map<String, Object>> toolsRef = generateToolsRef(tools);
        List<Map<String, Object>> technologiesRef = generateTechnologiesRef(technologies);
        String position = generatePosition();
        Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
        List<Map<String, Object>> extraProjectRoles = new ArrayList<>();
        List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, new ArrayList<>());

        Map<String, Object> metadata = buildMetadataJson(
                company, companyUrl, isCompanyBerdachuk,
                team, tools, toolsRef,
                technologiesRef,
                generateCustomerDescription(customerName), position,
                projectRole, primaryProjectRole,
                extraProjectRoles, allProjectRoles,
                generateParticipation(role, technologies), projectSummary
        );

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            metadataJson = "{}";
        }

        // Convert LocalDate to Instant for WorkExperience domain entity
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        WorkExperience workExperience = new WorkExperience(
                id,
                employeeId,
                projectId,
                null, // customerId
                projectName,
                customerName,
                BANKING_INDUSTRY,
                role,
                startInstant,
                endInstant,
                projectSummary,
                responsibilities,
                technologies
        );

        try {
            workExperienceRepository.createOrUpdate(workExperience, metadataJson);
        } catch (Exception e) {
            log.warn("Failed to insert banking work experience for {}: {}", projectName, e.getMessage());
        }
    }

    /**
     * Generates a banking-specific project name.
     */
    private String generateBankingProjectName(String projectType) {
        return faker.company().name() + " " + projectType;
    }

    /**
     * Generates a banking-specific project summary.
     */
    private String generateBankingProjectSummary(String projectType) {
        return String.format(
                "Developed and maintained %s for a major financial institution. " +
                        "Implemented secure payment processing, compliance features, and integration with core banking systems. " +
                        "Ensured regulatory compliance with banking standards and security requirements.",
                projectType.toLowerCase()
        );
    }

    /**
     * Generates a banking-specific role.
     */
    private String generateBankingRole() {
        String[] bankingRoles = {
                "Banking Software Developer", "Core Banking System Developer", "Payment Systems Engineer",
                "Risk Management Developer", "Compliance System Developer", "Fraud Detection Engineer",
                "Financial Systems Architect", "Banking Integration Specialist", "Treasury Systems Developer",
                "Credit Risk Analyst", "AML/KYC System Developer", "Digital Banking Developer"
        };
        return bankingRoles[random.nextInt(bankingRoles.length)];
    }

    /**
     * Generates banking-specific responsibilities.
     */
    private String generateBankingResponsibilities(String role, List<String> technologies, String projectType) {
        String sb = "Developed and maintained " + projectType.toLowerCase() + " using " +
                String.join(", ", technologies.subList(0, Math.min(3, technologies.size()))) +
                ". " +
                "Ensured compliance with banking regulations and security standards. " +
                "Implemented secure transaction processing and data protection mechanisms. " +
                "Collaborated with compliance and risk management teams.";
        return sb;
    }

    /**
     * Generates a small subset of test data with healthcare domain knowledge.
     * Creates employees, projects, and work experiences focused on healthcare and medical services.
     *
     * @param employeeCount      Number of employees to generate (default: 10)
     * @param workExpPerEmployee Number of work experiences per employee (default: 2-3)
     * @param projectCount       Number of healthcare projects to generate (default: 15)
     */
    public void generateHealthcareDomainSubset(int employeeCount, int workExpPerEmployee, int projectCount) {
        log.info("Generating healthcare domain subset: {} employees, {} projects, ~{} work experiences",
                employeeCount, projectCount, employeeCount * workExpPerEmployee);

        // Generate healthcare-specific employees
        List<String> employeeIds = generateHealthcareEmployees(employeeCount);

        // Generate healthcare-specific projects
        Map<String, String> projects = generateHealthcareProjects(projectCount);
        List<String> projectIds = new ArrayList<>(projects.keySet());

        int totalWorkExp = 0;
        for (String employeeId : employeeIds) {
            int expCount = workExpPerEmployee + random.nextInt(2); // workExpPerEmployee to workExpPerEmployee+1
            for (int i = 0; i < expCount; i++) {
                generateHealthcareWorkExperience(employeeId, projectIds, projects);
                totalWorkExp++;
            }
        }

        log.info("Healthcare domain subset generation completed: {} employees, {} projects, {} work experiences",
                employeeIds.size(), projects.size(), totalWorkExp);
    }

    /**
     * Generates employees for healthcare domain subset.
     *
     * @return List of generated employee IDs
     */
    private List<String> generateHealthcareEmployees(int count) {
        List<Map<String, Object>> employees = new ArrayList<>();
        List<String> employeeIds = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateEmployeeId();
            String name = generateName();
            String email = generateUniqueEmail(name, usedEmails);
            String seniority = SENIORITY_LEVELS[random.nextInt(SENIORITY_LEVELS.length)];
            String englishLevel = ENGLISH_LEVELS[random.nextInt(ENGLISH_LEVELS.length)];

            Map<String, Object> employee = new HashMap<>();
            employee.put("id", id);
            employee.put("name", name);
            employee.put("email", email);
            employee.put("seniority", seniority);
            employee.put("language_english", englishLevel);

            employees.add(employee);
            employeeIds.add(id);
        }

        // Use repository to create employees
        for (Map<String, Object> employeeMap : employees) {
            Employee employeeEntity = new Employee(
                    (String) employeeMap.get("id"),
                    (String) employeeMap.get("name"),
                    (String) employeeMap.get("email"),
                    (String) employeeMap.get("seniority"),
                    (String) employeeMap.get("language_english"),
                    null // availabilityStatus
            );
            try {
                employeeRepository.createOrUpdate(employeeEntity);
            } catch (Exception e) {
                log.warn("Failed to insert employee {}: {}", employeeMap.get("name"), e.getMessage());
            }
        }

        return employeeIds;
    }

    /**
     * Generates healthcare-specific projects.
     */
    private Map<String, String> generateHealthcareProjects(int count) {
        Map<String, String> projectMap = new HashMap<>();
        List<Map<String, Object>> projects = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String id = IdGenerator.generateProjectId();
            String projectType = HEALTHCARE_PROJECT_TYPES[random.nextInt(HEALTHCARE_PROJECT_TYPES.length)];
            String name = generateHealthcareProjectName(projectType);
            String summary = generateHealthcareProjectSummary(projectType);

            // Generate healthcare technologies for the project
            int techCount = 2 + random.nextInt(4);
            List<String> technologies = new ArrayList<>();
            Set<String> selectedTechs = new HashSet<>();
            while (selectedTechs.size() < techCount) {
                selectedTechs.add(HEALTHCARE_TECHNOLOGIES[random.nextInt(HEALTHCARE_TECHNOLOGIES.length)]);
            }
            technologies.addAll(selectedTechs);

            Map<String, Object> project = new HashMap<>();
            project.put("id", id);
            project.put("name", name);
            project.put("summary", summary);
            project.put("projectType", projectType);
            project.put("technologies", technologies.toArray(new String[0]));

            projects.add(project);
            projectMap.put(id, name);
        }

        // Use repository to create projects
        for (Map<String, Object> projectMapItem : projects) {
            Project projectEntity = new Project(
                    (String) projectMapItem.get("id"),
                    (String) projectMapItem.get("name"),
                    (String) projectMapItem.get("summary"),
                    null, // link
                    (String) projectMapItem.get("projectType"),
                    Arrays.asList((String[]) projectMapItem.get("technologies")),
                    null, // customerId
                    null, // customerName
                    null  // industry
            );
            try {
                projectRepository.createOrUpdate(projectEntity);
            } catch (Exception e) {
                log.warn("Failed to insert project {}: {}", projectMapItem.get("name"), e.getMessage());
            }
        }

        return projectMap;
    }

    /**
     * Generates a healthcare-specific work experience record.
     */
    private void generateHealthcareWorkExperience(String employeeId, List<String> projectIds, Map<String, String> projects) {
        String id = IdGenerator.generateId();

        // Select random project
        String projectId = projectIds.get(random.nextInt(projectIds.size()));
        String projectName = projects.get(projectId);

        String projectType = HEALTHCARE_PROJECT_TYPES[random.nextInt(HEALTHCARE_PROJECT_TYPES.length)];
        String projectSummary = generateHealthcareProjectSummary(projectType);
        String customerName = HEALTHCARE_CUSTOMER_NAMES[random.nextInt(HEALTHCARE_CUSTOMER_NAMES.length)];

        // Generate healthcare-specific role
        String primaryRole = generateHealthcareRole();
        String role = primaryRole;
        String projectRole = generateProjectRole(primaryRole, new ArrayList<>());

        // Generate healthcare-specific technologies (2-5 technologies from healthcare tech stack)
        int techCount = 2 + random.nextInt(4);
        List<String> technologies = new ArrayList<>();
        Set<String> selectedTechs = new HashSet<>();
        while (selectedTechs.size() < techCount) {
            selectedTechs.add(HEALTHCARE_TECHNOLOGIES[random.nextInt(HEALTHCARE_TECHNOLOGIES.length)]);
        }
        technologies.addAll(selectedTechs);

        String responsibilities = generateHealthcareResponsibilities(primaryRole, technologies, projectType);

        // Generate dates (random project in last 5 years)
        LocalDate startDate = LocalDate.now().minusYears(5)
                .plusDays(random.nextInt(1825)); // 5 years in days
        LocalDate endDate = startDate.plusMonths(6 + random.nextInt(24)); // 6-30 months duration

        // Build metadata
        String company = generateCompany();
        String companyUrl = generateCompanyUrl(company);
        boolean isCompanyBerdachuk = generateIsCompanyBerdachuk(company);
        String team = generateTeam();
        String tools = generateTools(technologies);
        List<Map<String, Object>> toolsRef = generateToolsRef(tools);
        List<Map<String, Object>> technologiesRef = generateTechnologiesRef(technologies);
        String position = generatePosition();
        Map<String, Object> primaryProjectRole = generatePrimaryProjectRole(primaryRole);
        List<Map<String, Object>> extraProjectRoles = new ArrayList<>();
        List<Map<String, Object>> allProjectRoles = generateAllProjectRoles(primaryRole, new ArrayList<>());

        Map<String, Object> metadata = buildMetadataJson(
                company, companyUrl, isCompanyBerdachuk,
                team, tools, toolsRef,
                technologiesRef,
                generateCustomerDescription(customerName), position,
                projectRole, primaryProjectRole,
                extraProjectRoles, allProjectRoles,
                generateParticipation(role, technologies), projectSummary
        );

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata to JSON: {}", e.getMessage());
            metadataJson = "{}";
        }

        // Convert LocalDate to Instant for WorkExperience domain entity
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        WorkExperience workExperience = new WorkExperience(
                id,
                employeeId,
                projectId,
                null, // customerId
                projectName,
                customerName,
                HEALTHCARE_INDUSTRY,
                role,
                startInstant,
                endInstant,
                projectSummary,
                responsibilities,
                technologies
        );

        try {
            workExperienceRepository.createOrUpdate(workExperience, metadataJson);
        } catch (Exception e) {
            log.warn("Failed to insert healthcare work experience for {}: {}", projectName, e.getMessage());
        }
    }

    /**
     * Generates a healthcare-specific project name.
     */
    private String generateHealthcareProjectName(String projectType) {
        return faker.company().name() + " " + projectType;
    }

    /**
     * Generates a healthcare-specific project summary.
     */
    private String generateHealthcareProjectSummary(String projectType) {
        return String.format(
                "Developed and maintained %s for a major healthcare organization. " +
                        "Implemented HIPAA-compliant solutions, integrated with EHR systems, and ensured " +
                        "patient data security and privacy. Worked with clinical workflows and healthcare interoperability standards.",
                projectType.toLowerCase()
        );
    }

    /**
     * Generates a healthcare-specific role.
     */
    private String generateHealthcareRole() {
        String[] healthcareRoles = {
                "Healthcare Software Developer", "EHR System Developer", "HL7/FHIR Integration Specialist",
                "Medical Imaging System Developer", "Telemedicine Platform Developer", "Clinical Systems Engineer",
                "Healthcare Data Analyst", "Health Information Systems Developer", "Patient Portal Developer",
                "Healthcare Interoperability Specialist", "Medical Device Integration Engineer", "mHealth Developer"
        };
        return healthcareRoles[random.nextInt(healthcareRoles.length)];
    }

    /**
     * Generates healthcare-specific responsibilities.
     */
    private String generateHealthcareResponsibilities(String role, List<String> technologies, String projectType) {
        String sb = "Developed and maintained " + projectType.toLowerCase() + " using " +
                String.join(", ", technologies.subList(0, Math.min(3, technologies.size()))) +
                ". " +
                "Ensured HIPAA compliance and patient data security. " +
                "Integrated with EHR systems and healthcare interoperability standards (HL7, FHIR). " +
                "Collaborated with clinical staff and healthcare IT teams.";
        return sb;
    }

    // Data models moved to com.berdachuk.expertmatch.ingestion.model package
    // Using imported classes: EmployeeProfile, EmployeeData, ProjectData
}
