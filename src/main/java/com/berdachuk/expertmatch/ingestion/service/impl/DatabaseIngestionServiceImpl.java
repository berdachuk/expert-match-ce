package com.berdachuk.expertmatch.ingestion.service.impl;

import com.berdachuk.expertmatch.ingestion.model.*;
import com.berdachuk.expertmatch.ingestion.repository.ExternalWorkExperienceRepository;
import com.berdachuk.expertmatch.ingestion.service.DatabaseIngestionService;
import com.berdachuk.expertmatch.ingestion.service.IngestProgressCallback;
import com.berdachuk.expertmatch.ingestion.service.ProfileProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
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

    private static LocalDate firstNonNull(LocalDate... values) {
        for (LocalDate v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static boolean hasSqlException(Throwable t) {
        for (Throwable x = t; x != null; x = x.getCause()) {
            if (x instanceof SQLException) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public IngestionResult ingestAll(int batchSize, IngestProgressCallback callback) {
        log.info("Starting ingestion from external database with batch size: {}, progress callback: {}", batchSize, callback != null);
        return doIngestFromOffset(0L, batchSize, callback);
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

    @Override
    @Transactional
    public IngestionResult ingestFromOffset(long fromOffset, int batchSize) {
        return doIngestFromOffset(fromOffset, batchSize, null);
    }

    @Override
    @Transactional
    public IngestionBatchResult ingestOneBatch(long fromOffset, int batchSize, IngestProgressCallback callback) {
        List<Map<String, Object>> records = externalWorkExperienceRepository.findFromOffset(fromOffset, batchSize);
        if (records.isEmpty()) {
            return new IngestionBatchResult(0, 0, 0, fromOffset, false);
        }
        log.info("Processing batch of {} records starting from offset {}", records.size(), fromOffset);

        Map<String, String> existingProjects = new HashMap<>();
        int processedInBatch = 0;
        int successCount = 0;
        int errorCount = 0;

        Map<String, List<Map<String, Object>>> recordsByEmployee = groupByEmployee(records);
        if (recordsByEmployee.isEmpty() && !records.isEmpty()) {
            Map<String, Object> sampleRecord = records.get(0);
            String sampleKeys = sampleRecord.keySet().toString();
            log.warn("No employee IDs found in records. Sample record keys: {}", sampleKeys);
            throw new IllegalStateException(
                    "No employee IDs could be extracted from " + records.size() + " records. "
                            + "Expected column 'employee' (or employee_data, employee_json) JSONB with 'id' or 'employee_id'. "
                            + "Sample record keys: " + sampleKeys);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : recordsByEmployee.entrySet()) {
            String employeeId = entry.getKey();
            List<Map<String, Object>> employeeRecords = entry.getValue();
            try {
                EmployeeProfile profile = convertToEmployeeProfile(employeeRecords);
                ProcessingResult result = profileProcessor.processProfile(profile, existingProjects, false);
                if (result.success()) {
                    successCount++;
                } else {
                    errorCount++;
                }
                processedInBatch++;
            } catch (Exception e) {
                if (hasSqlException(e)) {
                    throw new IllegalStateException(
                            "Database error while processing employee " + employeeId + ": " + e.getMessage(), e);
                }
                log.error("Failed to process employee {}: {}", employeeId, e.getMessage(), e);
                errorCount++;
                processedInBatch++;
            }
        }

        if (callback != null) {
            callback.onBatchProgress(processedInBatch, 0, "Processed " + processedInBatch + " employees");
        }

        long nextOffset = fromOffset;
        boolean hasMore = false;
        Map<String, Object> lastRecord = records.get(records.size() - 1);
        Object lastOffset = lastRecord.get("message_offset");
        if (lastOffset instanceof Number) {
            nextOffset = ((Number) lastOffset).longValue() + 1;
            hasMore = (records.size() >= batchSize);
        }

        return new IngestionBatchResult(processedInBatch, successCount, errorCount, nextOffset, hasMore);
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

    @Transactional
    protected IngestionResult doIngestFromOffset(long fromOffset, int batchSize, IngestProgressCallback callback) {
        log.info("Starting ingestion from external database from offset: {}, batch size: {}", fromOffset, batchSize);

        List<ProcessingResult> results = new ArrayList<>();
        int totalProcessed = 0;
        int successCount = 0;
        int errorCount = 0;
        long currentOffset = fromOffset;
        int batchIndex = 0;

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
                String sampleKeys = sampleRecord.keySet().toString();
                log.warn("No employee IDs found in records. Sample record keys: {}", sampleKeys);
                throw new IllegalStateException(
                        "No employee IDs could be extracted from " + records.size() + " records. "
                                + "Expected column 'employee' (or employee_data, employee_json) JSONB with 'id' or 'employee_id'. "
                                + "Sample record keys: " + sampleKeys);
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
                    if (hasSqlException(e)) {
                        throw new IllegalStateException(
                                "Database error while processing employee " + employeeId + ": " + e.getMessage(), e);
                    }
                    log.error("Failed to process employee {}: {}", employeeId, e.getMessage(), e);
                    errorCount++;
                    totalProcessed++;
                    results.add(ProcessingResult.failure(employeeId, "unknown", e.getMessage()));
                }
            }

            if (callback != null) {
                callback.onBatchProgress(totalProcessed, batchIndex, "Processed " + totalProcessed + " employees");
            }
            batchIndex++;

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
     * Extracts employee ID from database record.
     * Handles both Map and PGobject (PostgreSQL JSONB) types.
     * Tries keys: employee, employee_data, employee_json; and id, employee_id inside the object.
     */
    @SuppressWarnings("unchecked")
    private String extractEmployeeId(Map<String, Object> record) {
        for (String employeeKey : List.of("employee", "employee_data", "employee_json")) {
            Object employeeObj = record.get(employeeKey);
            if (employeeObj == null) {
                continue;
            }

            Map<String, Object> employee = parseEmployeeObject(employeeObj);
            if (employee != null) {
                for (String idKey : List.of("id", "employee_id")) {
                    Object id = employee.get(idKey);
                    if (id != null) {
                        return id.toString();
                    }
                }
            }
        }
        return null;
    }

    private Map<String, Object> parseEmployeeObject(Object employeeObj) {
        return parseJsonObject(employeeObj);
    }

    /**
     * Extracts employee map from database record.
     * Tries keys: employee, employee_data, employee_json.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractEmployeeMap(Map<String, Object> record) {
        for (String employeeKey : List.of("employee", "employee_data", "employee_json")) {
            Object employeeObj = record.get(employeeKey);
            Map<String, Object> employee = parseEmployeeObject(employeeObj);
            if (employee != null && !employee.isEmpty()) {
                return employee;
            }
        }
        throw new IllegalArgumentException("Cannot extract employee data from record: no employee field found. Record keys: " + record.keySet());
    }

    /**
     * Converts employee map to EmployeeData.
     * Tries snake_case and camelCase keys for compatibility with different external schemas.
     */
    private EmployeeData convertToEmployeeData(Map<String, Object> employeeMap) {
        String id = extractString(employeeMap, "id") != null ? extractString(employeeMap, "id") : extractString(employeeMap, "employee_id");
        String name = extractString(employeeMap, "name");
        String email = extractString(employeeMap, "email");
        String seniority = extractString(employeeMap, "seniority");
        String languageEnglish = extractString(employeeMap, "language_english") != null ? extractString(employeeMap, "language_english") : extractString(employeeMap, "languageEnglish");
        String availabilityStatus = extractString(employeeMap, "availability_status") != null ? extractString(employeeMap, "availability_status") : extractString(employeeMap, "availabilityStatus");

        return new EmployeeData(id, name, email, seniority, languageEnglish, availabilityStatus);
    }

    /**
     * Converts database record to ProjectData.
     */
    @SuppressWarnings("unchecked")
    private ProjectData convertToProjectData(Map<String, Object> record) {
        try {
            // Extract project info (support snake_case and camelCase from external DB)
            Map<String, Object> projectMap = extractProjectMap(record);
            String projectName = firstNonBlank(
                    extractString(projectMap, "name"),
                    extractString(projectMap, "projectName"),
                    extractString(record, "project"),
                    extractString(record, "project_description"),
                    extractString(record, "project_name"));
            if (projectName == null || projectName.isBlank()) {
                log.warn("Skipping record with missing project name. Record keys: {}", record.keySet());
                return null;
            }

            // Extract dates (support start_date, startDate, etc.)
            LocalDate startDate = firstNonNull(
                    extractDate(record, "start_date"),
                    extractDate(record, "startDate"));
            LocalDate endDate = firstNonNull(
                    extractDate(record, "end_date"),
                    extractDate(record, "endDate"));
            if (startDate == null) {
                log.warn("Skipping record with missing start_date. Record keys: {}", record.keySet());
                return null;
            }

            // Extract customer from JSONB (customer, customer_data, customer_json) and top-level columns
            Map<String, Object> customerMap = extractCustomerMap(record);
            String customerId = firstNonBlank(
                    extractString(customerMap, "id"),
                    extractString(customerMap, "customer_id"));
            String customerName = firstNonBlank(
                    extractString(customerMap, "name"),
                    extractString(customerMap, "customerName"),
                    extractString(record, "customer_name"),
                    extractString(record, "customer"));
            String customerDescription = extractString(record, "customer_description");
            String industry = firstNonBlank(
                    extractString(customerMap, "industry"),
                    extractString(record, "industry"),
                    extractString(record, "customer_description"));

            // Extract other fields (try multiple column names for role/position)
            String companyName = extractString(record, "company");
            String role = firstNonBlank(
                    extractString(record, "position"),
                    extractString(record, "role"));
            String projectSummary = extractString(record, "project_description");
            String responsibilities = extractString(record, "participation");

            // Extract technologies
            List<String> technologies = extractTechnologies(record);

            String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
            return new ProjectData(
                    null, // projectCode
                    projectName,
                    customerId,
                    customerName,
                    companyName,
                    role,
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDateStr,
                    technologies,
                    responsibilities,
                    industry,
                    projectSummary,
                    customerDescription
            );
        } catch (Exception e) {
            log.warn("Failed to convert record to ProjectData: {}", e.getMessage());
            return null;
        }
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
     * Extracts project map from database record.
     * Tries keys: project, project_data, project_json. Handles Map and PGobject (PostgreSQL JSONB).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractProjectMap(Map<String, Object> record) {
        for (String projectKey : List.of("project", "project_data", "project_json")) {
            Object projectObj = record.get(projectKey);
            if (projectObj == null) {
                continue;
            }
            Map<String, Object> parsed = parseJsonObject(projectObj);
            if (parsed != null && !parsed.isEmpty()) {
                return parsed;
            }
        }
        return new HashMap<>();
    }

    /**
     * Extracts customer map from database record.
     * Tries keys: customer, customer_data, customer_json. Handles Map and PGobject (PostgreSQL JSONB).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractCustomerMap(Map<String, Object> record) {
        for (String customerKey : List.of("customer", "customer_data", "customer_json")) {
            Object customerObj = record.get(customerKey);
            if (customerObj == null) {
                continue;
            }
            Map<String, Object> parsed = parseJsonObject(customerObj);
            if (parsed != null && !parsed.isEmpty()) {
                return parsed;
            }
        }
        return new HashMap<>();
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj.getClass().getName().equals("org.postgresql.util.PGobject")) {
                String jsonValue = (String) obj.getClass().getMethod("getValue").invoke(obj);
                if (jsonValue != null && !jsonValue.isEmpty()) {
                    return objectMapper.readValue(jsonValue, Map.class);
                }
            } else if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
        } catch (Exception e) {
            log.debug("Failed to parse JSON object: {}", e.getMessage());
        }
        return null;
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
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        }
        if (value instanceof String) {
            try {
                String s = (String) value;
                if (s.length() > 10) {
                    s = s.substring(0, 10);
                }
                return LocalDate.parse(s);
            } catch (Exception e) {
                log.warn("Failed to parse date from field {}: {}", fieldName, value);
            }
        }
        return null;
    }
}
