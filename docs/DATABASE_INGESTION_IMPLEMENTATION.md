# Database Ingestion Implementation

This document provides a comprehensive analysis of how data ingestion from the external database is implemented in the
ExpertMatch application.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Components](#components)
5. [Data Processing Flow](#data-processing-flow)
6. [Data Models](#data-models)
7. [API Endpoints](#api-endpoints)
8. [Key Features](#key-features)
9. [Security Considerations](#security-considerations)

---

## Overview

The ExpertMatch application implements a read-only database ingestion system that imports work experience data from an
external source database. The system is designed to:

- Read work experience records from a separate PostgreSQL database (`aist-tool-networking`)
- Transform the data into the application's internal domain model
- Store the ingested data in the primary application database (`expertmatch`)
- Process records in batches with support for incremental ingestion using message offsets

### Key Principles

1. **Read-Only Access**: The external database is accessed in read-only mode only
2. **Separate DataSources**: External and internal databases use completely separate DataSources
3. **Batch Processing**: Records are processed in configurable batch sizes
4. **Incremental Ingestion**: Supports resuming from a specific message offset
5. **Fail-Fast**: No fallback mechanisms - errors are explicitly reported

---

## Architecture

### Module Structure

The ingestion functionality is organized within the `ingestion` module following Spring Modulith principles:

```
ingestion/
├── config/
│   ├── ExternalDatabaseConfig.java      # External DataSource configuration
│   └── ExternalDatabaseProperties.java  # Configuration properties
├── domain/                              # Not present (uses model package)
├── model/
│   ├── EmployeeProfile.java             # Employee profile data model
│   ├── EmployeeData.java                # Employee data
│   ├── ProjectData.java                 # Project data
│   ├── IngestionResult.java             # Ingestion result record
│   └── ProcessingResult.java            # Processing result record
├── repository/
│   ├── ExternalWorkExperienceRepository.java    # Interface
│   └── impl/
│       └── ExternalWorkExperienceRepositoryImpl.java  # Implementation
├── rest/
│   └── IngestionController.java         # REST API controller
└── service/
    ├── DatabaseIngestionService.java    # Service interface
    ├── ExternalDatabaseConnectionService.java
    ├── JsonProfileIngestionService.java # Separate JSON file ingestion
    ├── ProfileProcessor.java            # Profile processing logic
    └── impl/
        ├── DatabaseIngestionServiceImpl.java
        ├── ExternalDatabaseConnectionServiceImpl.java
        └── JsonProfileIngestionServiceImpl.java
```

### Dependency Declaration

```java
@org.springframework.modulith.ApplicationModule(
        id = "ingestion",
        displayName = "Data Ingestion",
        allowedDependencies = {"employee", "embedding", "graph", "technology", "api", "core", "workexperience", "project"}
)
package com.berdachuk.expertmatch.ingestion;
```

---

## Configuration

### External Database Properties

The external database connection is configured via [
`ExternalDatabaseProperties`](src/main/java/com/berdachuk/expertmatch/ingestion/config/ExternalDatabaseProperties.java:15):

```yaml
expertmatch:
  ingestion:
    external-database:
      enabled: false                           # Enable external database ingestion
      host: localhost                         # Database host
      port: 5432                              # Database port
      database: aist-tool-networking          # Database name
      username: ingest_user                   # Database username
      password: secure_password               # Database password
      schema: work_experience                 # Schema name
      connection-timeout: 30000               # Connection timeout (ms)
      maximum-pool-size: 5                    # HikariCP max pool size
      minimum-idle: 2                         # HikariCP min idle connections
```

### DataSource Configuration

[`ExternalDatabaseConfig`](src/main/java/com/berdachuk/expertmatch/ingestion/config/ExternalDatabaseConfig.java:42)
creates a read-only HikariDataSource:

```java

@Bean(name = "externalDataSource")
public DataSource externalDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl + "?readOnly=true");  // Read-only connections
    config.setUsername(properties.getUsername());
    config.setPassword(properties.getPassword());
    config.setDriverClassName("org.postgresql.Driver");
    config.setMaximumPoolSize(properties.getMaximumPoolSize());
    config.setMinimumIdle(properties.getMinimumIdle());
    config.setConnectionTestQuery("SELECT 1");
    config.setPoolName("ExternalDBPool-ReadOnly");
    config.setInitializationFailTimeout(-1);  // Don't fail fast on init
    return new HikariDataSource(config);
}
```

### Conditional Bean Creation

All external database-related beans are conditionally created only when ingestion is enabled:

```java
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
```

---

## Components

### 1. Repository Layer

#### [
`ExternalWorkExperienceRepository`](src/main/java/com/berdachuk/expertmatch/ingestion/repository/ExternalWorkExperienceRepository.java:13)

Interface defining read operations for the external database:

```java
public interface ExternalWorkExperienceRepository {
    long countAll();

    List<Map<String, Object>> findAll(int offset, int limit);

    List<Map<String, Object>> findFromOffset(long fromOffset, int limit);
}
```

#### [
`ExternalWorkExperienceRepositoryImpl`](src/main/java/com/berdachuk/expertmatch/ingestion/repository/impl/ExternalWorkExperienceRepositoryImpl.java:31)

Implementation using the external DataSource. Key features:

- **DataSource Validation**: Validates it's connected to the correct database (not the primary one)
- **Search Path Handling**: Sets the search path for each connection
- **Raw SQL Queries**: Executes SQL queries against the `work_experience.work_experience_json` table
- **Row Mapping**: Returns results as `Map<String, Object>` for flexibility

**Key Query - findFromOffset:**

```sql
SELECT * FROM work_experience.work_experience_json 
WHERE message_offset >= ? 
ORDER BY message_offset 
LIMIT ?
```

### 2. Service Layer

#### [
`DatabaseIngestionService`](src/main/java/com/berdachuk/expertmatch/ingestion/service/DatabaseIngestionService.java:8)

Service interface defining ingestion operations:

```java
public interface DatabaseIngestionService {
    IngestionResult ingestAll(int batchSize);

    IngestionResult ingestFromOffset(long fromOffset, int batchSize);
}
```

#### [
`DatabaseIngestionServiceImpl`](src/main/java/com/berdachuk/expertmatch/ingestion/service/impl/DatabaseIngestionServiceImpl.java:28)

Main service implementation. Responsibilities:

1. **Batch Processing**: Processes records in configurable batch sizes
2. **Record Grouping**: Groups records by employee ID
3. **Data Conversion**: Converts database records to domain models
4. **Profile Processing**: Delegates to [
   `ProfileProcessor`](src/main/java/com/berdachuk/expertmatch/ingestion/service/ProfileProcessor.java:28)
5. **Offset Tracking**: Tracks message_offset for incremental ingestion

**Key Method - ingestFromOffset:**

```java
public IngestionResult ingestFromOffset(long fromOffset, int batchSize) {
    // Load existing projects for lookup optimization
    Map<String, String> existingProjects = new HashMap<>();

    while (true) {
        // Fetch batch from external database
        List<Map<String, Object>> records =
                externalWorkExperienceRepository.findFromOffset(currentOffset, batchSize);

        if (records.isEmpty()) break;

        // Group records by employee
        Map<String, List<Map<String, Object>>> recordsByEmployee =
                groupByEmployee(records);

        // Process each employee's records
        for (Map.Entry<String, List<Map<String, Object>>> entry : recordsByEmployee.entrySet()) {
            EmployeeProfile profile = convertToEmployeeProfile(employeeRecords);
            ProcessingResult result = profileProcessor.processProfile(profile, existingProjects, false);
            results.add(result);
        }

        // Update offset to last record's message_offset + 1
        currentOffset = lastRecordOffset + 1;
    }

    return IngestionResult.of(totalProcessed, successCount, errorCount, results, "external-database");
}
```

#### [`ProfileProcessor`](src/main/java/com/berdachuk/expertmatch/ingestion/service/ProfileProcessor.java:28)

Processes individual employee profiles into the internal database:

- Creates/updates [`Employee`](src/main/java/com/berdachuk/expertmatch/employee/domain/Employee.java:1) records
- Creates/updates [`Project`](src/main/java/com/berdachuk/expertmatch/project/domain/Project.java:1) records
- Creates [`WorkExperience`](src/main/java/com/berdachuk/expertmatch/workexperience/domain/WorkExperience.java:1)
  records
- Builds metadata JSON for work experience
- Checks for existing records to avoid duplicates

### 3. Connection Service

#### [
`ExternalDatabaseConnectionService`](src/main/java/com/berdachuk/expertmatch/ingestion/service/ExternalDatabaseConnectionService.java:6)

Provides connection verification for the external database:

```java
public interface ExternalDatabaseConnectionService {
    boolean verifyConnection();

    String getConnectionInfo();
}
```

### 4. REST Controller

#### [`IngestionController`](src/main/java/com/berdachuk/expertmatch/ingestion/rest/IngestionController.java:25)

Exposes ingestion endpoints:

- **POST `/api/v1/ingestion/database`**: Ingest from external database
- **GET `/api/v1/ingestion/database/verify`**: Verify database connection

---

## Data Processing Flow

### High-Level Flow

```
┌─────────────────┐
│  REST Request   │
│  (POST /ingestion/database)
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  IngestionController.ingestFromDatabase │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  DatabaseIngestionService.ingestFromOffset
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Batch Processing Loop:                 │
│  1. Fetch records from external DB     │
│     (ExternalWorkExperienceRepository) │
│  2. Group by employee ID               │
│  3. Convert to EmployeeProfile         │
│  4. Process each profile               │
│     (ProfileProcessor)                 │
│  5. Update offset                      │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  ProfileProcessor.processProfile:       │
│  1. Create/Update Employee             │
│  2. For each project:                  │
│     - Find/Create Project              │
│     - Create WorkExperience            │
│  3. Build metadata JSON               │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  Internal Database (expertmatch)       │
│  - employee table                      │
│  - project table                       │
│  - work_experience table               │
└─────────────────────────────────────────┘
```

### Detailed Processing Steps

#### 1. Record Fetching

```java
List<Map<String, Object>> records =
        externalWorkExperienceRepository.findFromOffset(currentOffset, batchSize);
```

Query executes on external database:

```sql
SELECT * FROM work_experience.work_experience_json 
WHERE message_offset >= 12345 
ORDER BY message_offset 
LIMIT 100
```

#### 2. Employee ID Extraction

Handles both Map and PGobject (PostgreSQL JSONB) types:

```java
private String extractEmployeeId(Map<String, Object> record) {
    Object employeeObj = record.get("employee");

    // Handle PGobject (PostgreSQL JSONB type)
    if (employeeObj.getClass().getName().equals("org.postgresql.util.PGobject")) {
        String jsonValue = (String) employeeObj.getClass()
                .getMethod("getValue").invoke(employeeObj);
        employee = objectMapper.readValue(jsonValue, Map.class);
    }
    // Handle Map type
    else if (employeeObj instanceof Map) {
        employee = (Map<String, Object>) employeeObj;
    }

    return employee.get("id").toString();
}
```

#### 3. Record Grouping

```java
Map<String, List<Map<String, Object>>> recordsByEmployee =
        groupByEmployee(records);
```

Groups all work experience records by employee ID to process each employee's complete profile together.

#### 4. Profile Conversion

Converts grouped database records to [
`EmployeeProfile`](src/main/java/com/berdachuk/expertmatch/ingestion/model/EmployeeProfile.java:12):

```java
EmployeeProfile convertToEmployeeProfile(List<Map<String, Object>> records) {
    // Extract employee data from first record
    Map<String, Object> employeeMap = extractEmployeeMap(firstRecord);
    EmployeeData employee = convertToEmployeeData(employeeMap);

    // Convert all records to ProjectData
    List<ProjectData> projects = records.stream()
            .map(this::convertToProjectData)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return new EmployeeProfile(employee, null, projects);
}
```

#### 5. Profile Processing

Writes data to internal database:

```java
public ProcessingResult processProfile(EmployeeProfile profile, Map<String, String> existingProjects, boolean applyDefaults) {
    // 1. Create/Update Employee
    Employee employeeEntity = new Employee(
            employee.id(), employee.name(), employee.email(),
            employee.seniority(), employee.languageEnglish(),
            employee.availabilityStatus()
    );
    employeeRepository.createOrUpdate(employeeEntity);

    // 2. Process each project
    for (ProjectData projectData : profile.projects()) {
        processProject(employeeId, projectData, existingProjects);
        projectsProcessed++;
    }

    return ProcessingResult.success(employeeId, employeeName,
            projectsProcessed, projectsSkipped, projectErrors);
}
```

---

## Data Models

### Ingestion Models

#### [`EmployeeProfile`](src/main/java/com/berdachuk/expertmatch/ingestion/model/EmployeeProfile.java:12)

```java
public record EmployeeProfile(
        EmployeeData employee,
        String summary,          // Optional
        List<ProjectData> projects  // Optional
) {
    public boolean isValid() {
        return employee != null && employee.isValid();
    }
}
```

#### [`EmployeeData`](src/main/java/com/berdachuk/expertmatch/ingestion/model/EmployeeData.java:1)

```java
public record EmployeeData(
        String id,
        String name,
        String email,
        String seniority,
        String languageEnglish,
        String availabilityStatus
) {
    public boolean isValid() {
        return id != null && !id.isBlank() && name != null && !name.isBlank();
    }
}
```

#### [`ProjectData`](src/main/java/com/berdachuk/expertmatch/ingestion/model/ProjectData.java:1)

```java
public record ProjectData(
        String projectCode,
        String projectName,
        String customerName,
        String companyName,
        String role,
        String startDate,
        String endDate,
        List<String> technologies,
        String responsibilities,
        String industry,
        String projectSummary
) {
    public boolean isValid() {
        return projectName != null && !projectName.isBlank() &&
                startDate != null && !startDate.isBlank();
    }
}
```

#### [`IngestionResult`](src/main/java/com/berdachuk/expertmatch/ingestion/model/IngestionResult.java:8)

```java
public record IngestionResult(
        int totalProfiles,
        int successCount,
        int errorCount,
        List<ProcessingResult> results,
        String sourceName
)
```

#### [`ProcessingResult`](src/main/java/com/berdachuk/expertmatch/ingestion/model/ProcessingResult.java:8)

```java
public record ProcessingResult(
        String employeeId,
        String employeeName,
        boolean success,
        String errorMessage,
        int projectsProcessed,
        int projectsSkipped,
        List<String> projectErrors
)
```

### Domain Models (Internal Database)

#### [`Employee`](src/main/java/com/berdachuk/expertmatch/employee/domain/Employee.java:1)

Stores employee information in the internal database.

#### [`Project`](src/main/java/com/berdachuk/expertmatch/project/domain/Project.java:1)

Stores project information in the internal database.

#### [`WorkExperience`](src/main/java/com/berdachuk/expertmatch/workexperience/domain/WorkExperience.java:1)

Stores work experience records linking employees to projects with metadata.

---

## API Endpoints

### Ingest from External Database

**Endpoint**: `POST /api/v1/ingestion/database`

**Parameters**:

- `fromOffset` (optional, query): Starting message offset. Defaults to 0.
- `batchSize` (optional, query): Batch size for processing. Defaults to 100.

**Request Example**:

```bash
curl -X POST "http://localhost:8080/api/v1/ingestion/database?fromOffset=0&batchSize=100" \
  -H "X-User-Id: admin" \
  -H "X-User-Roles: ROLE_ADMIN"
```

**Response Example**:

```json
{
  "totalProfiles": 50,
  "successCount": 48,
  "errorCount": 2,
  "sourceName": "external-database",
  "results": [
    {
      "employeeId": "EMP001",
      "employeeName": "John Doe",
      "success": true,
      "errorMessage": null,
      "projectsProcessed": 3,
      "projectsSkipped": 0,
      "projectErrors": []
    },
    {
      "employeeId": "EMP002",
      "employeeName": "Jane Smith",
      "success": false,
      "errorMessage": "Profile is invalid: missing required employee fields",
      "projectsProcessed": 0,
      "projectsSkipped": 0,
      "projectErrors": []
    }
  ]
}
```

### Verify External Database Connection

**Endpoint**: `GET /api/v1/ingestion/database/verify`

**Response Example**:

```json
{
  "connected": true,
  "connectionInfo": "ingest_user@localhost:5432/aist-tool-networking (schema: work_experience)"
}
```

**Error Response** (if not enabled):

```json
{
  "connected": false,
  "error": "External database ingestion is not enabled"
}
```

---

## Key Features

### 1. Read-Only External Database Access

The external database is accessed with `readOnly=true` in the JDBC URL, preventing any accidental write operations:

```java
String jdbcUrl = properties.getJdbcUrl();  // Includes ?readOnly=true
```

### 2. DataSource Validation

The repository implementation validates it's connected to the correct database:

```java
private void validateDataSource() {
    try (Connection conn = externalDataSource.getConnection()) {
        String dbName = conn.getCatalog();
        String expectedDb = properties.getDatabase();

        if (!dbName.equals(expectedDb)) {
            throw new IllegalStateException(
                    "External repository DataSource points to wrong database");
        }

        // Verify it's not pointing to the primary database
        if (dbName.equals("expertmatch")) {
            throw new IllegalStateException(
                    "External repository DataSource is pointing to primary database");
        }
    }
}
```

### 3. Batch Processing with Offset Tracking

Records are processed in configurable batches, tracking the `message_offset` for incremental ingestion:

```java
while(true){
List<Map<String, Object>> records =
        externalWorkExperienceRepository.findFromOffset(currentOffset, batchSize);
    
    if(records.

isEmpty())break;

// Process records...

// Update offset to last record's message_offset + 1
Object lastOffset = lastRecord.get("message_offset");
currentOffset =((Number)lastOffset).

longValue() +1;
        }
```

### 4. PGobject (JSONB) Handling

The implementation handles PostgreSQL JSONB columns represented as PGobject:

```java
if(employeeObj.getClass().

getName().

equals("org.postgresql.util.PGobject")){
String jsonValue = (String) employeeObj.getClass()
        .getMethod("getValue").invoke(employeeObj);
employee =objectMapper.

readValue(jsonValue, Map .class);
}
```

### 5. Duplicate Detection

The [`ProfileProcessor`](src/main/java/com/berdachuk/expertmatch/ingestion/service/ProfileProcessor.java:28) checks for
existing work experience records:

```java
LocalDate startDate = LocalDate.parse(project.startDate());
if(workExperienceRepository.

exists(employeeId, project.projectName(),startDate)){
        log.

debug("Work experience already exists, skipping");
    return;
            }
```

### 6. Metadata Generation

Work experience metadata is generated with structured JSON:

```java
Map<String, Object> metadata = new HashMap<>();
metadata.

put("tools",toolsText);
metadata.

put("tools_ref",toolsRef);
metadata.

put("technologies_ref",technologiesRef);
metadata.

put("project_role",projectRole);
metadata.

put("primary_project_role",primaryProjectRole);
metadata.

put("customer_description",customerDescription);
// ... more fields
return objectMapper.

writeValueAsString(metadata);
```

### 7. Conditional Service Registration

All external database components are conditionally registered:

```java
@ConditionalOnProperty(name = "expertmatch.ingestion.external-database.enabled", havingValue = "true")
```

This allows the application to run without external database configuration.

### 8. Fail-Fast Error Handling

No fallback mechanisms are implemented. Errors are explicitly thrown:

```java
try{
        // Processing logic
        }catch(Exception e){
        log.

error("Failed to process employee {}: {}",employeeId, e.getMessage(),e);
errorCount++;
        results.

add(ProcessingResult.failure(employeeId, "unknown",e.getMessage()));
        }
```

---

## Security Considerations

### 1. Read-Only Access

External database connections are marked as read-only:

- JDBC URL includes `?readOnly=true`
- HikariCP pool name: `ExternalDBPool-ReadOnly`
- All operations are SELECT queries only

### 2. No Direct Write Operations

The implementation never writes to the external database:

- [
  `ExternalWorkExperienceRepository`](src/main/java/com/berdachuk/expertmatch/ingestion/repository/ExternalWorkExperienceRepository.java:13)
  only has read methods
- All write operations go to the internal database via [
  `ProfileProcessor`](src/main/java/com/berdachuk/expertmatch/ingestion/service/ProfileProcessor.java:28)

### 3. DataSource Separation

Separate DataSources prevent cross-database operations:

- `externalDataSource`: For reading from external database
- Primary datasource (unnamed): For writing to internal database

### 4. Authorization

Authorization is handled by Spring Gateway:

- Users must have appropriate roles (e.g., `ROLE_ADMIN`)
- Headers populated by Gateway: `X-User-Id`, `X-User-Roles`, `X-User-Email`

### 5. Configuration Security

Database credentials are externalized:

```yaml
username: ${INGEST_POSTGRES_USER:}
password: ${INGEST_POSTGRES_PASSWORD:}
```

---

## External Database Schema

### Expected Table Structure

The external database should contain a table like:

```sql
CREATE TABLE work_experience.work_experience_json (
    message_offset BIGINT PRIMARY KEY,
    employee JSONB,           -- Employee information
    project JSONB,            -- Project information
    technologies VARCHAR,     -- Comma-separated technologies
    technologies_ref JSONB,   -- Technology references
    start_date DATE,
    end_date DATE,
    customer_name VARCHAR,
    company VARCHAR,
    position VARCHAR,
    project_description TEXT,
    participation TEXT,
    customer_description VARCHAR
);
```

### Employee JSONB Structure

```json
{
  "id": "EMP001",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "seniority": "Senior",
  "language_english": "Fluent",
  "availability_status": "Available"
}
```

### Project JSONB Structure

```json
{
  "name": "Customer Portal",
  "code": "PRJ001"
}
```

---

## Usage Examples

### Enable External Database Ingestion

Set the configuration property:

```yaml
expertmatch:
  ingestion:
    external-database:
      enabled: true
      host: external-db.example.com
      port: 5432
      database: aist-tool-networking
      username: ingest_user
      password: secure_password
      schema: work_experience
```

### Ingest All Records

```bash
curl -X POST "http://localhost:8080/api/v1/ingestion/database?batchSize=100" \
  -H "X-User-Id: admin" \
  -H "X-User-Roles: ROLE_ADMIN"
```

### Ingest from Specific Offset

```bash
curl -X POST "http://localhost:8080/api/v1/ingestion/database?fromOffset=5000&batchSize=50" \
  -H "X-User-Id: admin" \
  -H "X-User-Roles: ROLE_ADMIN"
```

### Verify Connection

```bash
curl -X GET "http://localhost:8080/api/v1/ingestion/database/verify"
```

---

## Troubleshooting

### Service Not Available

If the ingestion endpoints return "External database ingestion is not enabled":

1. Verify the configuration property is set:
   ```yaml
   expertmatch:
     ingestion:
       external-database:
         enabled: true
   ```

2. Check application logs for configuration loading issues.

### Connection Failed

If connection verification fails:

1. Verify external database is accessible
2. Check credentials
3. Verify network connectivity (VPN if required)
4. Check schema name

### DataSource Validation Error

If you see "External repository DataSource is pointing to wrong database":

1. Verify `expertmatch.ingestion.external-database.database` configuration
2. Ensure the JDBC URL is correct
3. Check if database name matches expected value

### Records Not Processing

If records are fetched but not processed:

1. Check logs for employee ID extraction issues
2. Verify JSONB structure in external database
3. Check if employee/project data is valid (required fields present)

---

## Summary

The ExpertMatch database ingestion system provides:

- **Secure Read-Only Access**: Prevents accidental modifications to source database
- **Scalable Batch Processing**: Processes records in configurable batches
- **Incremental Ingestion**: Supports resuming from specific offsets
- **Robust Error Handling**: Fail-fast approach with detailed error reporting
- **Flexible Data Handling**: Handles various PostgreSQL data types including JSONB
- **Separation of Concerns**: Clear separation between external read and internal write operations
- **Conditional Registration**: Components only loaded when needed

The implementation follows Spring Boot best practices, uses interface-based design, and integrates seamlessly with the
Spring Modulith architecture.
