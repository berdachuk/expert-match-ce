# JSON Profile Ingestion API

## Endpoint

**POST** `/api/v1/ingestion/json-profiles`

Ingests expert profiles from JSON files. Supports both array format (multiple profiles) and single object format (
backward compatible). Handles partial data gracefully with defaults.

**Tag**: `Ingest`  
**Requires**: ADMIN role

---

## Query Parameters

| Parameter   | Type   | Required | Description                                                                                                                                                         | Example                                           |
|-------------|--------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `directory` | string | No       | Directory path containing JSON profile files. Supports classpath (e.g., `"classpath:data"`) or file system paths. If not specified, defaults to `"classpath:data"`. | `"classpath:data"`                                |
| `file`      | string | No       | Single JSON file path to ingest. Supports classpath (e.g., `"classpath:data/profile.json"`) or file system paths. If specified, `directory` parameter is ignored.   | `"classpath:data/siarhei-berdachuk-profile.json"` |

**Note**: If neither `directory` nor `file` is specified, defaults to `"classpath:data"`.

---

## JSON Schema

### Format Support

The endpoint supports two JSON formats:

1. **Array Format** (multiple profiles):

```json
[
  { "employee": {...}, "summary": "...", "projects": [...] },
  { "employee": {...}, "summary": "...", "projects": [...] }
]
```

2. **Single Object Format** (backward compatible):

```json
{
  "employee": {...},
  "summary": "...",
  "projects": [...]
}
```

---

### Complete JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Employee Profile",
  "description": "Expert profile with employee information and project history",
  "type": "object",
  "required": ["employee"],
  "properties": {
    "employee": {
      "type": "object",
      "description": "Employee information",
      "required": ["id", "name"],
      "properties": {
        "id": {
          "type": "string",
          "description": "Employee ID (external system format, typically 19-digit numeric string)",
          "pattern": "^[0-9]+$",
          "example": "4000741400013306668"
        },
        "name": {
          "type": "string",
          "description": "Employee full name",
          "minLength": 1,
          "example": "Siarhei Berdachuk"
        },
        "email": {
          "type": "string",
          "description": "Employee email address (optional, defaults to generated email from name)",
          "format": "email",
          "example": "siarhei.berdachuk@example.com"
        },
        "seniority": {
          "type": "string",
          "description": "Seniority level (optional, defaults to 'B1')",
          "enum": ["A1", "A2", "A3", "A4", "A5", "B1", "B2", "B3", "B4", "B5", "C1", "C2", "C3", "C4", "C5"],
          "example": "B1"
        },
        "languageEnglish": {
          "type": "string",
          "description": "English language proficiency level (optional, defaults to 'B2')",
          "enum": ["A1", "A2", "B1", "B2", "C1", "C2"],
          "example": "B2"
        },
        "availabilityStatus": {
          "type": "string",
          "description": "Availability status (optional, defaults to 'available')",
          "enum": ["available", "unavailable", "busy"],
          "example": "available"
        }
      }
    },
    "summary": {
      "type": "string",
      "description": "Professional summary or bio (optional)",
      "example": "With over 30 years of extensive experience in the field of IT..."
    },
    "projects": {
      "type": "array",
      "description": "List of projects/work experience (optional)",
      "items": {
        "type": "object",
        "required": ["projectName", "startDate"],
        "properties": {
          "projectCode": {
            "type": "string",
            "description": "Project code/identifier (optional, auto-generated from projectName if missing)",
            "example": "CSC-CRS"
          },
          "projectName": {
            "type": "string",
            "description": "Project name (required)",
            "minLength": 1,
            "example": "Content Runtime Services"
          },
          "customerName": {
            "type": "string",
            "description": "Customer/client name (optional, defaults to 'Unknown Customer')",
            "example": "Content Services Corporation"
          },
          "companyName": {
            "type": "string",
            "description": "Company name (optional, defaults to customerName if provided, otherwise 'Unknown Customer')",
            "example": "Content Services Corporation"
          },
          "role": {
            "type": "string",
            "description": "Role in the project (optional, defaults to 'Developer')",
            "example": "Team Lead, Architect"
          },
          "startDate": {
            "type": "string",
            "description": "Project start date (required, ISO 8601 format: YYYY-MM-DD)",
            "format": "date",
            "pattern": "^\\d{4}-\\d{2}-\\d{2}$",
            "example": "2024-11-01"
          },
          "endDate": {
            "type": "string",
            "description": "Project end date (optional, defaults to current date if null, ISO 8601 format: YYYY-MM-DD)",
            "format": "date",
            "pattern": "^\\d{4}-\\d{2}-\\d{2}$",
            "example": "2026-01-31"
          },
          "technologies": {
            "type": "array",
            "description": "List of technologies used in the project (optional, defaults to empty array)",
            "items": {
              "type": "string"
            },
            "example": ["Java", "Spring Boot", "Microservices", "PostgreSQL", "AWS"]
          },
          "responsibilities": {
            "type": "string",
            "description": "Job responsibilities and duties (optional, defaults to empty string)",
            "example": "Team leading and coordination. Analyze business requirements..."
          },
          "industry": {
            "type": "string",
            "description": "Industry sector (optional, defaults to 'Technology')",
            "example": "Technology"
          },
          "projectSummary": {
            "type": "string",
            "description": "Project summary/description (optional, defaults to empty string)",
            "example": "Software Architecture and Engineering Services for Content Runtime Services..."
          }
        }
      }
    }
  }
}
```

---

## Default Values

The endpoint handles missing optional fields by applying default values:

### Employee Defaults

| Field                | Default Value       | Notes                                    |
|----------------------|---------------------|------------------------------------------|
| `email`              | Generated from name | Format: `firstname.lastname@example.com` |
| `seniority`          | `"B1"`              | -                                        |
| `languageEnglish`    | `"B2"`              | -                                        |
| `availabilityStatus` | `"available"`       | -                                        |

### Project Defaults

| Field              | Default Value                          | Notes                                                              |
|--------------------|----------------------------------------|--------------------------------------------------------------------|
| `projectCode`      | Generated from projectName             | Format: First 3 words, uppercase, hyphenated (e.g., "PRJ-ABC-DEF") |
| `customerName`     | `"Unknown Customer"`                   | -                                                                  |
| `companyName`      | `customerName` or `"Unknown Customer"` | Uses customerName if provided                                      |
| `role`             | `"Developer"`                          | -                                                                  |
| `endDate`          | Current date                           | If null, uses `LocalDate.now()`                                    |
| `technologies`     | `[]`                                   | Empty array                                                        |
| `responsibilities` | `""`                                   | Empty string                                                       |
| `industry`         | `"Technology"`                         | -                                                                  |
| `projectSummary`   | `""`                                   | Empty string                                                       |

---

## Request Examples

### Example 1: Ingest from Directory

```bash
POST /api/v1/ingestion/json-profiles?directory=classpath:data
```

### Example 2: Ingest Single File

```bash
POST /api/v1/ingestion/json-profiles?file=classpath:data/siarhei-berdachuk-profile.json
```

### Example 3: Default (classpath:data)

```bash
POST /api/v1/ingestion/json-profiles
```

---

## Response Schema

### Success Response (200 OK)

```json
{
  "totalProfiles": 1,
  "successCount": 1,
  "errorCount": 0,
  "sourceName": "classpath:data/siarhei-berdachuk-profile.json",
  "results": [
    {
      "employeeId": "4000741400013306668",
      "employeeName": "Siarhei Berdachuk",
      "success": true,
      "errorMessage": null,
      "projectsProcessed": 11,
      "projectsSkipped": 0,
      "projectErrors": []
    }
  ]
}
```

### Error Response (400 Bad Request)

```json
{
  "error": "ValidationException",
  "message": "Failed to ingest JSON profiles: Invalid JSON format",
  "status": 400,
  "timestamp": "2026-01-04T22:00:00Z",
  "path": "/api/v1/ingestion/json-profiles"
}
```

### Error Response (403 Forbidden)

```json
{
  "error": "Forbidden",
  "message": "Insufficient permissions (requires ADMIN role)",
  "status": 403,
  "timestamp": "2026-01-04T22:00:00Z",
  "path": "/api/v1/ingestion/json-profiles"
}
```

---

## JSON Profile Examples

### Example 1: Complete Profile (Array Format)

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
    "summary": "With over 30 years of extensive experience in the field of IT...",
    "projects": [
      {
        "projectCode": "CSC-CRS",
        "projectName": "Content Runtime Services",
        "customerName": "Content Services Corporation",
        "companyName": "Content Services Corporation",
        "role": "Team Lead, Architect",
        "startDate": "2024-11-01",
        "endDate": "2026-01-31",
        "technologies": ["Java", "Spring Boot", "Microservices", "PostgreSQL"],
        "responsibilities": "Team leading and coordination...",
        "industry": "Technology",
        "projectSummary": "Software Architecture and Engineering Services..."
      }
    ]
  }
]
```

### Example 2: Minimal Profile (Single Object Format)

```json
{
  "employee": {
    "id": "5000741400013306669",
    "name": "John Doe"
  }
}
```

### Example 3: Profile with Partial Data

```json
{
  "employee": {
    "id": "6000741400013306670",
    "name": "Jane Smith",
    "seniority": "A3"
  },
  "projects": [
    {
      "projectName": "Project Alpha",
      "startDate": "2023-01-01",
      "technologies": ["Python", "Django"]
    }
  ]
}
```

---

## Validation Rules

### Required Fields

- **Employee**: `id` and `name` are required
- **Project**: `projectName` and `startDate` are required

### Field Constraints

- **Employee ID**: Must be non-empty string (typically numeric, 19 digits)
- **Employee Name**: Must be non-empty string
- **Email**: Must be valid email format (if provided)
- **Seniority**: Must be one of: A1-A5, B1-B5, C1-C5 (if provided)
- **Language English**: Must be one of: A1, A2, B1, B2, C1, C2 (if provided)
- **Availability Status**: Must be one of: available, unavailable, busy (if provided)
- **Start Date**: Must be in ISO 8601 format (YYYY-MM-DD)
- **End Date**: Must be in ISO 8601 format (YYYY-MM-DD), must be after startDate (if provided)

---

## Error Handling

The endpoint implements graceful error handling:

- **Per-Profile Errors**: Invalid profiles are logged, included in result, processing continues
- **Per-Project Errors**: Invalid projects are logged, included in result, processing continues
- **Invalid JSON**: File is skipped, error logged, other files continue
- **Database Errors**: Record is skipped, error logged, processing continues

All errors are included in the `IngestionResult` response for reporting.

---

## Processing Flow

1. **Parse JSON**: Detects array vs single object format
2. **Validate**: Checks required fields (employee.id, employee.name, project.projectName, project.startDate)
3. **Apply Defaults**: Fills missing optional fields with default values
4. **Process Employee**: Creates or updates employee record
5. **Process Projects**: For each project:
- Creates/updates project record
    - Creates work experience record
    - Links technologies
6. **Return Result**: Returns `IngestionResult` with success/error counts

---

## Notes

- **External System IDs**: Employee IDs use external system format (typically 19-digit numeric strings)
- **Date Format**: All dates must be in ISO 8601 format (YYYY-MM-DD)
- **Technologies**: Stored as array of strings, automatically linked to technology entities
- **Projects**: Automatically linked to employee via work experience records
- **Graph Relationships**: After ingestion, run `POST /api/v1/test-data/graph` to build graph relationships
- **Embeddings**: After ingestion, run `POST /api/v1/test-data/embeddings` to generate vector embeddings

---

**Last Updated**: 2026-01-04

