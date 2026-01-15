# Ingestion Endpoints Analysis

## Overview

This document analyzes the current ingestion endpoints to identify what's fully implemented, what's partially
implemented, and what can be removed.

## Fully Implemented Endpoints 

These endpoints are fully functional and should be kept:

### 1. `POST /api/v1/test-data`

- **Status**:  Fully implemented
- **Implementation**: `IngestionController.generateTestData()`
- **Service**: `TestDataGenerator.generateTestData()`
- **Purpose**: Generate synthetic test data (employees, work experience, technologies, projects)
- **Keep**: Yes

### 2. `POST /api/v1/test-data/embeddings`

- **Status**:  Fully implemented
- **Implementation**: `IngestionController.generateEmbeddings()`
- **Service**: `TestDataGenerator.generateEmbeddings()`
- **Purpose**: Generate embeddings for work experience records
- **Keep**: Yes

### 3. `POST /api/v1/test-data/graph`

- **Status**:  Fully implemented
- **Implementation**: `IngestionController.buildGraph()`
- **Service**: `GraphBuilderService.buildGraph()`
- **Purpose**: Build graph relationships from database data
- **Keep**: Yes

### 4. `POST /api/v1/test-data/complete`

- **Status**:  Fully implemented
- **Implementation**: `IngestionController.generateCompleteDataset()`
- **Service**: `TestDataGenerator.generateTestData()` + `generateEmbeddings()` + `GraphBuilderService.buildGraph()`
- **Purpose**: Complete pipeline: test data + embeddings + graph
- **Keep**: Yes

### 5. `POST /api/v1/ingestion/json-profiles`

- **Status**:  Fully implemented
- **Implementation**: `IngestionController.ingestJsonProfiles()`
- **Service**: `JsonProfileIngestionService`
- **Purpose**: Ingest expert profiles from JSON files (array or single object format)
- **Features**:
- Supports array format: `[{profile1}, {profile2}]`
    - Supports single object format: `{profile1}` (backward compatible)
    - Handles partial data with default values
    - Processes multiple files from directory
    - Error recovery (continues on failure)
- **Keep**: Yes

## Partially Implemented / Placeholder Endpoints 

These endpoints have API contracts but only implement status management, not actual ingestion logic:

### 6. `POST /api/v1/ingestion/trigger/{sourceName}`

- **Status**:  Placeholder only
- **Implementation**: `IngestionController.triggerIngestion()`
- **Service**: `IngestionService.triggerIngestion()`
- **Current Behavior**: Only creates/updates ingestion status in database, no actual ingestion
- **TODO Comment**: "Implement actual ingestion logic based on source type"
- **Recommendation**: **Remove** - No Kafka consumer, no actual ingestion logic implemented

### 7. `GET /api/v1/ingestion/status`

- **Status**:  Partially implemented
- **Implementation**: `IngestionController.getAllStatuses()`
- **Service**: `IngestionService.getAllStatuses()`
- **Current Behavior**: Returns status from database (but statuses are only created by placeholders)
- **Recommendation**: **Remove** - Only useful if actual ingestion sources exist

### 8. `GET /api/v1/ingestion/status/{sourceName}`

- **Status**:  Partially implemented
- **Implementation**: `IngestionController.getStatus()`
- **Service**: `IngestionService.getStatus()`
- **Current Behavior**: Returns status from database (but statuses are only created by placeholders)
- **Recommendation**: **Remove** - Only useful if actual ingestion sources exist

### 9. `POST /api/v1/ingestion/pause/{sourceName}`

- **Status**:  Placeholder only
- **Implementation**: `IngestionController.pauseIngestion()`
- **Service**: `IngestionService.pauseIngestion()`
- **Current Behavior**: Only updates status to "PAUSED", no actual pause logic
- **Recommendation**: **Remove** - No actual ingestion to pause

### 10. `POST /api/v1/ingestion/resume/{sourceName}`

- **Status**:  Placeholder only
- **Implementation**: `IngestionController.resumeIngestion()`
- **Service**: `IngestionService.resumeIngestion()`
- **Current Behavior**: Only updates status to "RUNNING", no actual resume logic
- **Recommendation**: **Remove** - No actual ingestion to resume

### 11. `POST /api/v1/ingestion/file`

- **Status**:  Placeholder only
- **Implementation**: `IngestionController.uploadFile()`
- **Service**: `IngestionService.processFileUpload()`
- **Current Behavior**: Only creates status and marks as "COMPLETED", no actual file parsing/ingestion
- **TODO Comment**: "Implement actual file processing logic"
- **Recommendation**: **Remove** - No file parsing, no data extraction, no actual ingestion

## Summary

### Keep (5 endpoints):

1. `POST /api/v1/test-data` - Generate test data
2. `POST /api/v1/test-data/embeddings` - Generate embeddings
3. `POST /api/v1/test-data/graph` - Build graph relationships
4. `POST /api/v1/test-data/complete` - Complete pipeline (recommended)
5. `POST /api/v1/ingestion/json-profiles` - Ingest expert profiles from JSON files

### Remove (6 endpoints):

1. `POST /api/v1/ingestion/trigger/{sourceName}` - No actual ingestion logic
2. `GET /api/v1/ingestion/status` - Only tracks placeholder statuses
3. `GET /api/v1/ingestion/status/{sourceName}` - Only tracks placeholder statuses
4. `POST /api/v1/ingestion/pause/{sourceName}` - No actual ingestion to pause
5. `POST /api/v1/ingestion/resume/{sourceName}` - No actual ingestion to resume
6. `POST /api/v1/ingestion/file` - No actual file processing

## Related Code to Remove

### Services:
- `IngestionService` - Can be removed entirely (only contains placeholder logic)
- `IngestionStatusRepository` - Can be removed (only used by placeholders)

### Database Tables:
- `ingestion_status` table (if exists) - Can be removed

### OpenAPI Spec:
- Remove the 6 placeholder endpoints from `openapi.yaml`
- Remove related schemas: `TriggerIngestionResponse`, `IngestionStatusResponse`, `IngestionStatusListResponse`,
  `PauseResumeIngestionResponse`, `FileUploadResponse`

## Rationale

The current implementation follows a "Unified Processing Pipeline" architecture described in the documentation, but the
actual ingestion logic from external sources (Kafka, file uploads, REST APIs) is not implemented. The MVP focuses on:

1. **Test data generation** - Fully implemented and working
2. **Embedding generation** - Fully implemented and working
3. **Graph building** - Fully implemented and working

The placeholder endpoints for external source ingestion (Kafka, file uploads) were planned but never implemented.
Removing them will:

- Simplify the API surface
- Reduce maintenance burden
- Remove confusing endpoints that don't actually work
- Focus on what's actually implemented and working

If external source ingestion is needed in the future, it can be re-implemented with proper Kafka consumers, file
parsers, and actual ingestion logic.

