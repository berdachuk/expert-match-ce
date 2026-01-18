# External Database Ingestion

This document describes how to configure and use the external database ingestion feature.

## Overview

The external database ingestion feature allows importing work experience data from an external PostgreSQL database (
`aist-tool-networking`) into ExpertMatch. The data is read from the `work_experience.work_experience_json` table.

## Configuration

### Enable External Database Ingestion

Set the following environment variables or update `application.yml`:

```bash
export EXPERTMATCH_INGESTION_EXTERNAL_DB_ENABLED=true
export INGEST_POSTGRES_HOST=192.168.0.73
export INGEST_POSTGRES_PORT=5432
export INGEST_POSTGRES_DB=aist-tool-networking
export INGEST_POSTGRES_USER=Auto_EPM-ESP_srv-aist@epam.com
export INGEST_POSTGRES_PASSWORD=Frovcab4omEs_
export INGEST_POSTGRES_SCHEMA=work_experience
```

Or configure in `application.yml`:

```yaml
expertmatch:
  ingestion:
    external-database:
      enabled: true
      host: ${INGEST_POSTGRES_HOST:192.168.0.73}
      port: ${INGEST_POSTGRES_PORT:5432}
      database: ${INGEST_POSTGRES_DB:aist-tool-networking}
      username: ${INGEST_POSTGRES_USER:Auto_EPM-ESP_srv-aist@epam.com}
      password: ${INGEST_POSTGRES_PASSWORD:}
      schema: ${INGEST_POSTGRES_SCHEMA:work_experience}
```

**Note**: The external database is accessible via VPN only.

## Verification

### Verify Connection via REST API

```bash
curl http://localhost:8080/api/v1/ingestion/database/verify
```

Expected response:

```json
{
  "connected": true,
  "connectionInfo": "Auto_EPM-ESP_srv-aist@epam.com@192.168.0.73:5432/aist-tool-networking (schema: work_experience)"
}
```

### Verify Connection via Test

Run the integration test:

```bash
mvn test -Dtest=ExternalDatabaseConnectionServiceIT \
  -Dexpertmatch.ingestion.external-database.enabled=true \
  -DINGEST_POSTGRES_HOST=192.168.0.73 \
  -DINGEST_POSTGRES_PORT=5432 \
  -DINGEST_POSTGRES_DB=aist-tool-networking \
  -DINGEST_POSTGRES_USER=Auto_EPM-ESP_srv-aist@epam.com \
  -DINGEST_POSTGRES_PASSWORD=Frovcab4omEs_
```

## Ingestion

### Ingest All Records

```bash
curl -X POST "http://localhost:8080/api/v1/ingestion/database?batchSize=100"
```

### Ingest from Specific Offset

```bash
curl -X POST "http://localhost:8080/api/v1/ingestion/database?fromOffset=1000&batchSize=100"
```

### Response Format

```json
{
  "totalProfiles": 150,
  "successCount": 145,
  "errorCount": 5,
  "sourceName": "external-database",
  "results": [
    {
      "employeeId": "8760000000000420950",
      "employeeName": "John Doe",
      "success": true,
      "errorMessage": null,
      "projectsProcessed": 3,
      "projectsSkipped": 0,
      "projectErrors": []
    }
  ]
}
```

## Database Schema

The ingestion reads from `work_experience.work_experience_json` table with the following key columns:

- `message_offset` (int8) - Primary key, used for pagination
- `entity` (jsonb) - Entity data
- `start_date` (date) - Project start date
- `end_date` (date) - Project end date
- `project` (jsonb) - Project information
- `employee` (jsonb) - Employee information
- `customer` (jsonb) - Customer information
- `technologies` (text) - Comma-separated technologies
- `technologies_ref` (jsonb) - Technologies as JSON array
- `raw_message` (jsonb) - Decoded message

## Data Processing

The ingestion process:

1. Reads records from `work_experience.work_experience_json` table
2. Groups records by employee ID
3. Converts database records to `EmployeeProfile` objects
4. Processes each profile using `ProfileProcessor`:
    - Creates/updates employee records
    - Creates/updates project records
    - Creates work experience records
    - Generates embeddings and graph relationships

## Batch Processing

Records are processed in batches to optimize memory usage and transaction management. The default batch size is 100
records, but can be configured via the `batchSize` parameter.

## Error Handling

- Invalid records are skipped with error messages logged
- Employee records with missing required fields are skipped
- Project records with missing required fields are skipped
- Connection errors are logged and returned in the response

## Troubleshooting

### Connection Failed

1. Verify VPN connection is active
2. Check database credentials
3. Verify database host and port are accessible
4. Check firewall rules

### No Records Processed

1. Verify the `work_experience` schema exists
2. Check if `work_experience_json` table has data
3. Verify the `message_offset` range is correct

### Processing Errors

1. Check application logs for detailed error messages
2. Verify data format matches expected structure
3. Check for missing required fields in database records
