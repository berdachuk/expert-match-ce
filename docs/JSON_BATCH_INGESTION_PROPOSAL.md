# JSON Batch Ingestion - Implementation Complete ✅

**Status**: ✅ **Fully Implemented** (2026-01-04)

This document describes the implemented JSON batch ingestion functionality for ExpertMatch.

## Executive Summary

This document proposes a solution for ingesting expert profile data from JSON files containing arrays of expert
profiles. The solution extends the current ingestion architecture to support batch processing of multiple experts from
structured JSON files, with robust handling of partial/missing data.

## Current Architecture Analysis

### Current Ingestion Flow

The current ingestion system follows this flow:

```
POST /api/v1/test-data/complete?size={size}&clear={boolean}
    ↓
TestDataGenerator.generateTestData()
    ↓
┌─────────────────────────────────────────┐
│ 1. Generate synthetic employees         │
│ 2. Generate projects                    │
│ 3. Generate work experience records     │
│ 4. Generate Siarhei Berdachuk profile   │ ← Single hardcoded profile
└─────────────────────────────────────────┘
    ↓
TestDataGenerator.generateEmbeddings()
    ↓
GraphBuilderService.buildGraph()
```

### Current JSON Profile Processing

**Location**: `TestDataGenerator.generateSiarheiBerdachukData()`

**Current Structure**:

- Single JSON file: `data/siarhei-berdachuk-profile.json`
- Single expert profile per file
- Structure:
  ```json
  {
    "employee": { ... },
    "summary": "...",
    "projects": [ ... ]
  }
  ```

**Processing Steps**:

1. Load JSON file from classpath
2. Parse into `EmployeeProfile` record
3. Insert/update employee record
4. Create work experience records for each project
5. Handle duplicates with `ON CONFLICT` clauses

### Key Components

1. **TestDataGenerator**: Main service for data generation
    - `generateSiarheiBerdachukData()`: Processes single profile
    - `loadSiarheiBerdachukProfile()`: Loads JSON from classpath
    - `createWorkExperienceRecord()`: Creates work experience with metadata

2. **Data Models**:
    - `EmployeeProfile`: Root record (employee + summary + projects)
    - `EmployeeData`: Employee fields (id, name, email, seniority, etc.)
    - `ProjectData`: Project fields (code, name, dates, technologies, etc.)

3. **Database Operations**:
    - Employee: `INSERT ... ON CONFLICT DO UPDATE`
    - Work Experience: `INSERT ... ON CONFLICT DO NOTHING`
    - Projects: Created/found via project name matching

## Requirements

### Functional Requirements

1. **Support Array of Experts**: JSON files should support arrays of expert profiles
   ```json
   [
     { "employee": {...}, "summary": "...", "projects": [...] },
     { "employee": {...}, "summary": "...", "projects": [...] }
   ]
   ```

2. **Backward Compatibility**: Single profile format should still work
   ```json
   { "employee": {...}, "summary": "...", "projects": [...] }
   ```

3. **Partial Data Handling**: Gracefully handle missing fields:
    - Missing `summary` → Use empty string or null
    - Missing `projects` → Skip work experience creation
    - Missing optional employee fields → Use defaults or skip
    - Missing project fields → Use defaults or skip project

4. **Multiple File Support**: Process multiple JSON files from a directory

5. **Error Handling**: Continue processing other experts if one fails

6. **Deduplication**: Prevent duplicate employees and work experience records

### Non-Functional Requirements

1. **Performance**: Batch processing for efficiency
2. **Logging**: Detailed logs for each processed expert
3. **Validation**: Validate JSON structure before processing
4. **Idempotency**: Re-running ingestion should not create duplicates

## Proposed Solution

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│ JSON Batch Ingestion Service                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐      ┌──────────────────┐           │
│  │ JSON File Loader │  →   │ Profile Parser   │           │
│  │                  │      │                  │           │
│  │ - Classpath files│      │ - Array support  │           │
│  │ - File system    │      │ - Single profile │           │
│  │ - URL resources  │      │ - Validation     │           │
│  └──────────────────┘      └──────────────────┘           │
│         ↓                            ↓                      │
│  ┌──────────────────────────────────────────┐              │
│  │ Profile Processor                        │              │
│  │                                          │              │
│  │ - Employee ingestion                     │              │
│  │ - Project creation                       │              │
│  │ - Work experience creation               │              │
│  │ - Partial data handling                  │              │
│  │ - Error recovery                         │              │
│  └──────────────────────────────────────────┘              │
│         ↓                                                   │
│  ┌──────────────────┐                                      │
│  │ Result Reporter  │                                      │
│  │                  │                                      │
│  │ - Success count  │                                      │
│  │ - Error count    │                                      │
│  │ - Details        │                                      │
│  └──────────────────┘                                      │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Plan

#### Phase 1: Core JSON Batch Ingestion Service

**New Service**: `JsonProfileIngestionService`

**Responsibilities**:

- Load JSON files (classpath, filesystem, URL)
- Parse JSON (array or single object)
- Process each expert profile
- Handle partial data
- Report results

**Key Methods**:

```java
public class JsonProfileIngestionService {
    
    /**
     * Ingests expert profiles from a JSON file.
     * Supports both array format and single object format.
     * 
     * @param resourcePath Path to JSON resource (classpath or file system)
     * @return IngestionResult with success/error counts and details
     */
    public IngestionResult ingestFromFile(String resourcePath);
    
    /**
     * Ingests expert profiles from multiple JSON files in a directory.
     * 
     * @param directoryPath Path to directory containing JSON files
     * @return IngestionResult aggregated from all files
     */
    public IngestionResult ingestFromDirectory(String directoryPath);
    
    /**
     * Ingests expert profiles from JSON content (string or input stream).
     * 
     * @param jsonContent JSON content as string
     * @param sourceName Name of the source for logging
     * @return IngestionResult
     */
    public IngestionResult ingestFromContent(String jsonContent, String sourceName);
    
    /**
     * Processes a single expert profile.
     * Handles partial data and errors gracefully.
     * 
     * @param profile EmployeeProfile to process
     * @return ProcessingResult with success status and details
     */
    private ProcessingResult processProfile(EmployeeProfile profile);
}
```

#### Phase 2: Enhanced Data Models

**Extend Current Records** to support optional fields:

```java
/**
 * Employee profile data model with optional fields.
 */
public record EmployeeProfile(
    EmployeeData employee,
    @JsonInclude(JsonInclude.Include.NON_NULL) String summary,  // Optional
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ProjectData> projects  // Optional
) {
    /**
     * Returns true if profile has minimum required data.
     */
    public boolean isValid() {
        return employee != null 
            && employee.id() != null && !employee.id().isBlank()
            && employee.name() != null && !employee.name().isBlank();
    }
}

/**
 * Employee data model with optional fields.
 */
public record EmployeeData(
    String id,
    String name,
    @JsonInclude(JsonInclude.Include.NON_NULL) String email,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String seniority,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String languageEnglish,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String availabilityStatus  // Optional
) {
    /**
     * Returns default values for missing fields.
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

/**
 * Project data model with optional fields.
 */
public record ProjectData(
    @JsonInclude(JsonInclude.Include.NON_NULL) String projectCode,  // Optional
    String projectName,
    @JsonInclude(JsonInclude.Include.NON_NULL) String customerName,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String companyName,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String role,  // Optional
    String startDate,
    @JsonInclude(JsonInclude.Include.NON_NULL) String endDate,  // Optional (current date if null)
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> technologies,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String responsibilities,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String industry,  // Optional
    @JsonInclude(JsonInclude.Include.NON_NULL) String projectSummary  // Optional
) {
    /**
     * Returns true if project has minimum required data.
     */
    public boolean isValid() {
        return projectName != null && !projectName.isBlank()
            && startDate != null && !startDate.isBlank();
    }
    
    /**
     * Returns default values for missing fields.
     */
    public ProjectData withDefaults() {
        return new ProjectData(
            projectCode != null ? projectCode : generateProjectCode(projectName),
            projectName,
            customerName != null ? customerName : "Unknown Customer",
            companyName != null ? companyName : customerName,
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
```

#### Phase 3: JSON Parser with Array Support

**New Component**: `JsonProfileParser`

```java
public class JsonProfileParser {
    private final ObjectMapper objectMapper;
    
    /**
     * Parses JSON content into list of EmployeeProfile objects.
     * Supports both array format and single object format.
     * 
     * @param jsonContent JSON content as string
     * @return List of EmployeeProfile objects
     * @throws JsonProcessingException if JSON is invalid
     */
    public List<EmployeeProfile> parseProfiles(String jsonContent) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        
        if (rootNode.isArray()) {
            // Array format: [ {employee: {...}, ...}, ... ]
            return objectMapper.readValue(
                jsonContent,
                objectMapper.getTypeFactory().constructCollectionType(
                    List.class, EmployeeProfile.class
                )
            );
        } else if (rootNode.isObject()) {
            // Single object format: {employee: {...}, ...}
            EmployeeProfile profile = objectMapper.treeToValue(rootNode, EmployeeProfile.class);
            return List.of(profile);
        } else {
            throw new IllegalArgumentException("JSON must be an object or array");
        }
    }
}
```

#### Phase 4: Partial Data Handling Strategy

**Default Values**:

- Missing `email` → Generate from name: `{name.toLowerCase().replace(" ", ".")}@example.com`
- Missing `seniority` → Default: `"B1"`
- Missing `languageEnglish` → Default: `"B2"`
- Missing `availabilityStatus` → Default: `"available"`
- Missing `summary` → Use empty string
- Missing `projects` → Skip work experience creation (employee only)
- Missing `projectCode` → Generate from project name
- Missing `endDate` → Use current date
- Missing `technologies` → Empty list
- Missing `responsibilities` → Empty string
- Missing `industry` → Default: `"Technology"`

**Validation Rules**:

- Employee: `id` and `name` are required
- Project: `projectName` and `startDate` are required
- Skip invalid records with warning logs

#### Phase 5: Integration with Existing System

**Update `TestDataGenerator`**:

```java
@Autowired(required = false)
private JsonProfileIngestionService jsonProfileIngestionService;

/**
 * Ingests expert profiles from JSON files.
 * Can be called independently or as part of test data generation.
 */
public void ingestJsonProfiles(String directoryPath) {
    if (jsonProfileIngestionService != null) {
        IngestionResult result = jsonProfileIngestionService.ingestFromDirectory(directoryPath);
        log.info("JSON profile ingestion completed: {} successful, {} failed", 
            result.getSuccessCount(), result.getErrorCount());
    } else {
        log.warn("JsonProfileIngestionService not available, skipping JSON profile ingestion");
    }
}
```

**New Endpoint** (Optional):

```java
@PostMapping("/api/v1/ingestion/json-profiles")
public ResponseEntity<IngestionResult> ingestJsonProfiles(
    @RequestParam(required = false) String directory,
    @RequestParam(required = false) String file
) {
    if (file != null) {
        return ResponseEntity.ok(jsonProfileIngestionService.ingestFromFile(file));
    } else if (directory != null) {
        return ResponseEntity.ok(jsonProfileIngestionService.ingestFromDirectory(directory));
    } else {
        // Default: process classpath:data/*.json
        return ResponseEntity.ok(
            jsonProfileIngestionService.ingestFromDirectory("classpath:data")
        );
    }
}
```

### File Structure Examples

#### Example 1: Array Format (Multiple Experts)

```json
[
  {
    "employee": {
      "id": "4000741400013306668",
      "name": "Siarhei Berdachuk",
      "email": "siarhei.berdachuk@example.com",
      "seniority": "B1",
      "languageEnglish": "B2",
      "availabilityStatus": "available"
    },
    "summary": "With over 30 years of extensive experience...",
    "projects": [
      {
        "projectCode": "CSC-CRS",
        "projectName": "Content Runtime Services",
        "customerName": "Content Services Corporation",
        "companyName": "Content Services Corporation",
        "role": "Team Lead, Architect",
        "startDate": "2024-11-01",
        "endDate": "2026-01-31",
        "technologies": ["Java", "Spring Boot", "Microservices"],
        "responsibilities": "Team leading and coordination...",
        "industry": "Technology",
        "projectSummary": "Software Architecture and Engineering Services..."
      }
    ]
  },
  {
    "employee": {
      "id": "5000741400013306669",
      "name": "John Doe"
    },
    "projects": [
      {
        "projectName": "Project Alpha",
        "startDate": "2023-01-01",
        "technologies": ["Python", "Django"]
      }
    ]
  }
]
```

#### Example 2: Single Object Format (Backward Compatible)

```json
{
  "employee": {
    "id": "4000741400013306668",
    "name": "Siarhei Berdachuk"
  },
  "projects": []
}
```

#### Example 3: Minimal Profile (Only Required Fields)

```json
{
  "employee": {
    "id": "6000741400013306670",
    "name": "Jane Smith"
  }
}
```

### Error Handling Strategy

**Per-Profile Errors**:

- Invalid JSON structure → Log error, skip profile, continue
- Missing required fields → Log warning, skip profile, continue
- Database constraint violation → Log error, skip profile, continue
- Invalid date format → Log warning, use default, continue

**Per-Project Errors**:

- Invalid project data → Log warning, skip project, continue with employee
- Missing required project fields → Log warning, skip project, continue

**Result Reporting**:

```java
public record IngestionResult(
    int totalProfiles,
    int successCount,
    int errorCount,
    List<ProfileProcessingResult> results
) {
    public record ProfileProcessingResult(
        String employeeId,
        String employeeName,
        boolean success,
        String errorMessage,
        int projectsProcessed,
        int projectsSkipped
    ) {}
}
```

### Testing Strategy (TDD Approach)

#### Test Cases

1. **Array Format Parsing**:
    - Test parsing array of profiles
    - Test parsing single object (backward compatibility)
    - Test invalid JSON structure

2. **Partial Data Handling**:
    - Test missing optional employee fields (email, seniority, etc.)
    - Test missing summary
    - Test missing projects array
    - Test missing optional project fields
    - Test missing required fields (should fail gracefully)

3. **Database Operations**:
    - Test employee insertion with defaults
    - Test employee update on conflict
    - Test work experience creation
    - Test duplicate prevention
    - Test project creation/finding

4. **Error Recovery**:
    - Test processing continues after one profile fails
    - Test processing continues after one project fails
    - Test invalid date formats
    - Test database constraint violations

5. **Integration Tests**:
    - Test full ingestion flow
    - Test with real database (Testcontainers)
    - Test idempotency (re-running ingestion)

### Implementation Steps

1. **Step 1: Create Test Cases** (TDD)
    - Write tests for JSON parsing (array and single object)
    - Write tests for partial data handling
    - Write tests for error recovery

2. **Step 2: Implement JSON Parser**
    - Create `JsonProfileParser` class
    - Implement array/single object detection
    - Implement parsing logic
    - Make tests pass

3. **Step 3: Enhance Data Models**
    - Update `EmployeeProfile`, `EmployeeData`, `ProjectData` records
    - Add optional field annotations
    - Add validation methods
    - Add default value methods

4. **Step 4: Implement Ingestion Service**
    - Create `JsonProfileIngestionService`
    - Implement file loading (classpath, filesystem)
    - Implement profile processing
    - Implement partial data handling
    - Implement error recovery

5. **Step 5: Integration**
    - Update `TestDataGenerator` to use new service
    - Add new endpoint (optional)
    - Update documentation

6. **Step 6: Testing and Validation**
    - Run all tests
    - Test with sample JSON files
    - Verify backward compatibility
    - Performance testing

### Configuration

**Application Properties**:

```yaml
expertmatch:
  ingestion:
    json-profiles:
      # Default directory for JSON profile files
      default-directory: "classpath:data"
      # File pattern for JSON files
      file-pattern: "**/*-profile.json"
      # Enable/disable JSON profile ingestion
      enabled: true
      # Default values for missing fields
      defaults:
        seniority: "B1"
        language-english: "B2"
        availability-status: "available"
        industry: "Technology"
```

### Migration Path

1. **Phase 1**: Implement new service alongside existing code
2. **Phase 2**: Update `TestDataGenerator` to use new service for Siarhei Berdachuk profile
3. **Phase 3**: Add support for multiple JSON files
4. **Phase 4**: Add REST endpoint for manual ingestion
5. **Phase 5**: Deprecate old single-profile method (optional)

### Benefits

1. **Scalability**: Process multiple experts from single file
2. **Flexibility**: Support partial data, multiple files
3. **Maintainability**: Centralized JSON processing logic
4. **Robustness**: Error recovery, validation, logging
5. **Backward Compatibility**: Existing single-profile files still work
6. **Testability**: Clear separation of concerns, TDD approach

### Risks and Mitigation

1. **Risk**: Breaking existing functionality
    - **Mitigation**: Maintain backward compatibility, comprehensive tests

2. **Risk**: Performance issues with large files
    - **Mitigation**: Batch processing, streaming for very large files

3. **Risk**: Data quality issues from partial data
    - **Mitigation**: Validation, default values, detailed logging

### Future Enhancements

1. **Streaming Processing**: For very large JSON files
2. **Schema Validation**: JSON Schema validation before processing
3. **Incremental Updates**: Track last processed timestamp
4. **Data Transformation**: Support for different JSON formats
5. **Async Processing**: Background job processing for large batches

## Conclusion

This proposal provides a comprehensive solution for batch JSON ingestion that:

- Supports arrays of expert profiles
- Handles partial/missing data gracefully
- Maintains backward compatibility
- Follows TDD principles
- Integrates seamlessly with existing architecture
- Provides robust error handling and logging

The implementation can be done incrementally, with each step being testable and verifiable before moving to the next.

