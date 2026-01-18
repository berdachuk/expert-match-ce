package com.berdachuk.expertmatch.ingestion.service.impl;

import com.berdachuk.expertmatch.ingestion.model.*;
import com.berdachuk.expertmatch.ingestion.repository.ExternalWorkExperienceRepository;
import com.berdachuk.expertmatch.ingestion.service.DatabaseIngestionService;
import com.berdachuk.expertmatch.ingestion.service.ProfileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for ingesting work experience data from external source database.
 * <p>
 * IMPORTANT: This service ONLY reads from the external source database.
 * All write operations go to the primary application database via ProfileProcessor.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
public class DatabaseIngestionServiceImpl implements DatabaseIngestionService {

    private final ExternalWorkExperienceRepository externalWorkExperienceRepository;
    private final ProfileProcessor profileProcessor;
    private final ObjectMapper objectMapper;

    public DatabaseIngestionServiceImpl(
            @Qualifier("externalWorkExperienceRepository")
            ExternalWorkExperienceRepository externalWorkExperienceRepository,
            ProfileProcessor profileProcessor,
            ObjectMapper objectMapper) {
        this.externalWorkExperienceRepository = externalWorkExperienceRepository;
        this.profileProcessor = profileProcessor;
        this.objectMapper = objectMapper;

        log.info("DatabaseIngestionService initialized with external repository for source database ingestion");
    }

    @Override
    @Transactional
    public IngestionResult ingestAll(int batchSize) {
        log.info("Starting ingestion from external database with batch size: {}", batchSize);
        return ingestFromOffset(0L, batchSize);
    }

    @Override
    @Transactional
    public IngestionResult ingestFromOffset(long fromOffset, int batchSize) {
        log.info("Starting ingestion from external database from offset: {}, batch size: {}", fromOffset, batchSize);

        List<ProcessingResult> results = new ArrayList<>();
        int totalProcessed = 0;
        int successCount = 0;
        int errorCount = 0;
        long currentOffset = fromOffset;

        // Load existing projects for lookup optimization
        Map<String, String> existingProjects = new HashMap<>();

        while (true) {
            List<Map<String, Object>> records = externalWorkExperienceRepository.findFromOffset(currentOffset, batchSize);
            log.info("Retrieved {} records from external database starting from offset {}", records.size(), currentOffset);
            if (records.isEmpty()) {
                log.info("No more records found, stopping ingestion");
                break;
            }

            log.info("Processing batch of {} records starting from offset {}", records.size(), currentOffset);

            // Group records by employee
            Map<String, List<Map<String, Object>>> recordsByEmployee = groupByEmployee(records);
            log.info("Grouped {} records into {} employee groups", records.size(), recordsByEmployee.size());
            if (recordsByEmployee.isEmpty() && !records.isEmpty()) {
                Map<String, Object> sampleRecord = records.get(0);
                log.warn("No employee IDs found in records. Sample record keys: {}",
                        sampleRecord.keySet());
                // Log actual employee and entity structures for debugging
                Object employeeObj = sampleRecord.get("employee");
                Object entityObj = sampleRecord.get("entity");
                log.warn("Sample employee object type: {}, value: {}",
                        employeeObj != null ? employeeObj.getClass().getSimpleName() : "null",
                        employeeObj instanceof Map ? ((Map<?, ?>) employeeObj).keySet() : employeeObj);
                log.warn("Sample entity object type: {}, value: {}",
                        entityObj != null ? entityObj.getClass().getSimpleName() : "null",
                        entityObj instanceof Map ? ((Map<?, ?>) entityObj).keySet() : entityObj);
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : recordsByEmployee.entrySet()) {
                String employeeId = entry.getKey();
                List<Map<String, Object>> employeeRecords = entry.getValue();

                try {
                    EmployeeProfile profile = convertToEmployeeProfile(employeeRecords);
                    // Don't apply defaults when ingesting from external database - use only real data
                    ProcessingResult result = profileProcessor.processProfile(profile, existingProjects, false);
                    results.add(result);

                    if (result.success()) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Failed to process employee {}: {}", employeeId, e.getMessage(), e);
                    errorCount++;
                    totalProcessed++;
                    results.add(ProcessingResult.failure(employeeId, "unknown", e.getMessage()));
                }
            }

            // Update offset to the last message_offset in this batch
            if (!records.isEmpty()) {
                Map<String, Object> lastRecord = records.get(records.size() - 1);
                Object lastOffset = lastRecord.get("message_offset");
                if (lastOffset instanceof Number) {
                    currentOffset = ((Number) lastOffset).longValue() + 1;
                } else {
                    break; // Cannot determine next offset
                }
            } else {
                break;
            }
        }

        log.info("Completed ingestion: total={}, success={}, errors={}", totalProcessed, successCount, errorCount);
        return IngestionResult.of(totalProcessed, successCount, errorCount, results, "external-database");
    }

    /**
     * Groups database records by employee ID.
     */
    private Map<String, List<Map<String, Object>>> groupByEmployee(List<Map<String, Object>> records) {
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();

        for (Map<String, Object> record : records) {
            String employeeId = extractEmployeeId(record);
            if (employeeId != null) {
                grouped.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(record);
            } else {
                log.debug("Could not extract employee ID from record. Record keys: {}", record.keySet());
            }
        }

        return grouped;
    }

    /**
     * Extracts employee ID from database record.
     * Handles both Map and PGobject (PostgreSQL JSONB) types.
     */
    @SuppressWarnings("unchecked")
    private String extractEmployeeId(Map<String, Object> record) {
        try {
            Object employeeObj = record.get("employee");
            if (employeeObj == null) {
                return null;
            }

            Map<String, Object> employee = null;

            // Handle PGobject (PostgreSQL JSONB type)
            if (employeeObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
                try {
                    String jsonValue = (String) employeeObj.getClass().getMethod("getValue").invoke(employeeObj);
                    if (jsonValue != null && !jsonValue.isEmpty()) {
                        employee = objectMapper.readValue(jsonValue, Map.class);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse PGobject employee: {}", e.getMessage());
                }
            }
            // Handle Map type (already parsed JSON)
            else if (employeeObj instanceof Map) {
                employee = (Map<String, Object>) employeeObj;
            }

            if (employee != null) {
                Object id = employee.get("id");
                if (id != null) {
                    return id.toString();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract employee ID from record: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Converts database records to EmployeeProfile.
     */
    @SuppressWarnings("unchecked")
    private EmployeeProfile convertToEmployeeProfile(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            throw new IllegalArgumentException("Cannot convert empty records list to EmployeeProfile");
        }

        // Use first record to extract employee info
        Map<String, Object> firstRecord = records.get(0);
        Map<String, Object> employeeMap = extractEmployeeMap(firstRecord);
        EmployeeData employee = convertToEmployeeData(employeeMap);

        // Convert all records to ProjectData
        List<ProjectData> projects = records.stream()
                .map(this::convertToProjectData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new EmployeeProfile(employee, null, projects);
    }

    /**
     * Extracts employee map from database record.
     * Handles both Map and PGobject (PostgreSQL JSONB) types.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEmployeeMap(Map<String, Object> record) {
        Object employeeObj = record.get("employee");
        if (employeeObj == null) {
            throw new IllegalArgumentException("Cannot extract employee data from record: employee field is null");
        }

        Map<String, Object> employee = null;

        // Handle PGobject (PostgreSQL JSONB type)
        if (employeeObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
            try {
                String jsonValue = (String) employeeObj.getClass().getMethod("getValue").invoke(employeeObj);
                if (jsonValue != null && !jsonValue.isEmpty()) {
                    employee = objectMapper.readValue(jsonValue, Map.class);
                }
            } catch (Exception e) {
                log.warn("Failed to parse PGobject employee: {}", e.getMessage());
            }
        }
        // Handle Map type (already parsed JSON)
        else if (employeeObj instanceof Map) {
            employee = (Map<String, Object>) employeeObj;
        }

        if (employee != null && !employee.isEmpty()) {
            return employee;
        }

        // No fallback - employee field is required and always present in the database
        throw new IllegalArgumentException("Cannot extract employee data from record: employee field is missing or empty");
    }

    /**
     * Converts employee map to EmployeeData.
     */
    private EmployeeData convertToEmployeeData(Map<String, Object> employeeMap) {
        String id = extractString(employeeMap, "id");
        String name = extractString(employeeMap, "name");
        String email = extractString(employeeMap, "email");
        String seniority = extractString(employeeMap, "seniority");
        String languageEnglish = extractString(employeeMap, "language_english");
        String availabilityStatus = extractString(employeeMap, "availability_status");

        return new EmployeeData(id, name, email, seniority, languageEnglish, availabilityStatus);
    }

    /**
     * Converts database record to ProjectData.
     */
    @SuppressWarnings("unchecked")
    private ProjectData convertToProjectData(Map<String, Object> record) {
        try {
            // Extract project info
            Map<String, Object> projectMap = extractProjectMap(record);
            String projectName = extractString(projectMap, "name");
            if (projectName == null || projectName.isBlank()) {
                projectName = extractString(record, "project_description");
            }
            if (projectName == null || projectName.isBlank()) {
                log.warn("Skipping record with missing project name");
                return null;
            }

            // Extract dates
            LocalDate startDate = extractDate(record, "start_date");
            LocalDate endDate = extractDate(record, "end_date");
            if (startDate == null) {
                log.warn("Skipping record with missing start_date");
                return null;
            }

            // Extract other fields
            String customerName = extractString(record, "customer_name");
            String companyName = extractString(record, "company");
            String role = extractString(record, "position");
            String projectSummary = extractString(record, "project_description");
            String responsibilities = extractString(record, "participation");
            String industry = extractString(record, "customer_description");

            // Extract technologies
            List<String> technologies = extractTechnologies(record);

            return new ProjectData(
                    null, // projectCode
                    projectName,
                    customerName,
                    companyName,
                    role,
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                    technologies,
                    responsibilities,
                    industry,
                    projectSummary
            );
        } catch (Exception e) {
            log.warn("Failed to convert record to ProjectData: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts project map from database record.
     * Handles both Map and PGobject (PostgreSQL JSONB) types.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractProjectMap(Map<String, Object> record) {
        Object projectObj = record.get("project");
        if (projectObj == null) {
            return new HashMap<>();
        }

        // Handle PGobject (PostgreSQL JSONB type)
        if (projectObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
            try {
                String jsonValue = (String) projectObj.getClass().getMethod("getValue").invoke(projectObj);
                if (jsonValue != null && !jsonValue.isEmpty()) {
                    return objectMapper.readValue(jsonValue, Map.class);
                }
            } catch (Exception e) {
                log.debug("Failed to parse PGobject project: {}", e.getMessage());
            }
        }
        // Handle Map type (already parsed JSON)
        else if (projectObj instanceof Map) {
            return (Map<String, Object>) projectObj;
        }

        return new HashMap<>();
    }

    /**
     * Extracts technologies list from database record.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractTechnologies(Map<String, Object> record) {
        // Try technologies field first
        Object technologiesObj = record.get("technologies");
        if (technologiesObj instanceof String) {
            String techs = (String) technologiesObj;
            if (!techs.isBlank()) {
                return Arrays.stream(techs.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList());
            }
        }

        // Try technologies_ref JSONB field
        Object technologiesRefObj = record.get("technologies_ref");
        if (technologiesRefObj instanceof List) {
            List<Map<String, Object>> technologiesRef = (List<Map<String, Object>>) technologiesRefObj;
            return technologiesRef.stream()
                    .map(ref -> extractString(ref, "name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Extracts date from record field.
     */
    private LocalDate extractDate(Map<String, Object> record, String fieldName) {
        Object value = record.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof String) {
            try {
                return LocalDate.parse((String) value);
            } catch (Exception e) {
                log.warn("Failed to parse date from field {}: {}", fieldName, value);
            }
        }
        return null;
    }

    /**
     * Extracts string value from map.
     */
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
