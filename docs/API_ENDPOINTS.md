# ExpertMatch API Endpoints

## Overview

The ExpertMatch backend provides RESTful API endpoints for expert discovery, chat management, and data ingestion.

**Base URL**: `/api/v1`  
**Authentication**: Header-based authentication (Spring Gateway handles JWT validation and populates headers)

---

## Query Endpoints

### POST /api/v1/query

Process natural language query for expert discovery.

**Request Body**:
```json
{
  "query": "Looking for experts in Java, Spring Boot, and AWS",
  "chatId": "507f1f77bcf86cd799439011", // Optional
  "options": {
    "maxResults": 10,
    "minConfidence": 0.7,
    "includeSources": true,
    "includeEntities": true,
    "rerank": true,
    "deepResearch": false,
     "useCascadePattern": false,
     "useRoutingPattern": false,
     "useCyclePattern": false,
    "includeExecutionTrace": false
  }
}
```

**Response** (200 OK):
```json
{
  "answer": "Based on your requirements, I found several experts...",
  "experts": [...],
  "sources": [...],
  "entities": [...],
  "confidence": 0.85,
  "queryId": "507f1f77bcf86cd799439012",
  "chatId": "507f1f77bcf86cd799439011",
  "messageId": "507f1f77bcf86cd799439013",
  "processingTime": 1234,
  "summary": {
    "total": 10,
    "perfectMatches": 3,
    "goodMatches": 5,
    "partialMatches": 2
  }
}
```

**Validation**:

- `query`: Required, max 5000 characters
- `chatId`: Optional, must be 24-character hex string if provided
- `options.maxResults`: 1-100, default: 10
- `options.minConfidence`: 0.0-1.0, default: 0.7
- `options.includeSources`: boolean, default: true
- `options.includeEntities`: boolean, default: true
- `options.rerank`: boolean, default: false (enable semantic reranking)
- `options.deepResearch`: boolean, default: false (enable deep research SGR pattern)
- `options.useCascadePattern`: boolean, default: false (enable Cascade pattern for structured expert evaluation,
  requires exactly 1 expert)
- `options.useRoutingPattern`: boolean, default: false (enable Routing pattern for LLM-based query classification)
- `options.useCyclePattern`: boolean, default: false (enable Cycle pattern for multiple expert evaluations, requires
  multiple experts)
- `options.includeExecutionTrace`: boolean, default: false (includes step-by-step processing details with LLM models and
  token usage)

**Error Responses**:

- `400 Bad Request`: Invalid request format or validation errors
- `401 Unauthorized`: Missing or invalid authentication (handled by Spring Gateway)
- `403 Forbidden`: Chat access denied
- `404 Not Found`: Chat not found
- `503 Service Unavailable`: LLM or embedding service unavailable
- `504 Gateway Timeout`: Request timeout (default: 5 minutes, configurable in `application-local.yml`)

**Note on Timeouts**:

- Default server timeout: 5 minutes (300,000ms) - configured in `application-local.yml`
- Complex queries with deep research, multiple SGR patterns, or large result sets may take several minutes
- If you encounter timeout errors, consider:
- Using `includeExecutionTrace: true` to identify slow processing steps
    - Breaking complex queries into smaller sub-queries
    - Increasing timeout values in `application-local.yml` if needed

---

**Last updated**: 2025-12-21 - Added timeout configuration and error handling notes

---

### GET /api/v1/query/examples

Get a list of example queries organized by categories. Helps new users understand how to formulate queries for expert
discovery.

**Authentication**: Not required (public endpoint)

**Response** (200 OK):

```json
{
  "examples": [
    {
      "category": "Basic",
      "title": "Simple Technology Query",
      "query": "Looking for experts in Java and Spring Boot"
    },
    {
      "category": "Technology",
      "title": "Backend Technologies",
      "query": "Find Java developers with Spring Boot, Hibernate, and PostgreSQL experience"
    },
    {
      "category": "Seniority",
      "title": "Senior Level",
      "query": "Looking for senior Java developers with 10+ years of experience"
    }
  ]
}
```

**Response Fields**:

- `examples`: Array of query examples
    - `category`: Category name (e.g., "Basic", "Technology", "Seniority", "Language", "Team", "Complex")
    - `title`: Title/name of the example query
    - `query`: The example query text that can be used directly in a query request

**Example Request**:

```bash
curl -X GET "http://localhost:8093/api/v1/query/examples" \
  -H "Content-Type: application/json"
```

**Use Cases**:

- Help new users understand query formats
- Provide templates for common query types
- UI integration: Display examples in a modal dialog for easy selection

**Note**:

- Examples are loaded from `query-examples.json` resource file at startup
- Examples are cached after first load for better performance
- The endpoint returns consistent results across multiple calls
- This is a public endpoint (no authentication required)
- Examples are organized by categories: Basic, Technology, Seniority, Language, Team, Complex, RFP Response, Team
  Formation, Urgent RFP, and Skill-Based

---

## Chat Management Endpoints

### GET /api/v1/chats

List all chats for the authenticated user.

**Response** (200 OK):
```json
{
  "chats": [
    {
      "id": "507f1f77bcf86cd799439011",
      "userId": "user-123",
      "name": "Default Chat",
      "isDefault": true,
      "messageCount": 5,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T12:00:00Z",
      "lastActivityAt": "2024-01-01T12:00:00Z"
    }
  ],
  "total": 1
}
```

### POST /api/v1/chats

Create a new chat.

**Request Body** (optional):
```json
{
  "name": "My Custom Chat"
}
```

**Response** (201 Created):
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": "user-123",
  "name": "My Custom Chat",
  "isDefault": false,
  "messageCount": 0,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "lastActivityAt": "2024-01-01T00:00:00Z"
}
```

### GET /api/v1/chats/{chatId}

Get chat details.

**Path Parameters**:

- `chatId`: 24-character hex string

**Response** (200 OK): Chat object

**Error Responses**:

- `400 Bad Request`: Invalid chat ID format
- `403 Forbidden`: Access denied to chat
- `404 Not Found`: Chat not found

### PATCH /api/v1/chats/{chatId}

Update chat (e.g., rename).

**Request Body**:
```json
{
  "name": "Updated Chat Name"
}
```

**Response** (200 OK): Updated chat object

**Error Responses**:

- `400 Bad Request`: Invalid chat ID format or validation errors
- `403 Forbidden`: Access denied to chat
- `404 Not Found`: Chat not found

### DELETE /api/v1/chats/{chatId}

Delete a chat.

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Chat deleted successfully"
}
```

**Error Responses**:

- `400 Bad Request`: Invalid chat ID format or trying to delete default chat
- `403 Forbidden`: Access denied to chat
- `404 Not Found`: Chat not found
- `500 Internal Server Error`: Failed to delete chat

### GET /api/v1/chats/{chatId}/history

Get conversation history for a chat with pagination.

**Query Parameters**:

- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort order (default: "sequence_number,asc")

**Response** (200 OK):
```json
{
  "chatId": "507f1f77bcf86cd799439011",
  "messages": [
    {
      "id": "507f1f77bcf86cd799439012",
      "role": "user",
      "content": "Looking for Java experts",
      "sequenceNumber": 1,
      "createdAt": "2024-01-01T00:00:00Z",
      "tokensUsed": null
    },
    {
      "id": "507f1f77bcf86cd799439013",
      "role": "assistant",
      "content": "I found several Java experts...",
      "sequenceNumber": 2,
      "createdAt": "2024-01-01T00:00:05Z",
      "tokensUsed": 150
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

**Validation**:

- `page`: Must be >= 0
- `size`: Must be between 1 and 100

**Error Responses**:

- `400 Bad Request`: Invalid pagination parameters or chat ID format
- `403 Forbidden`: Access denied to chat
- `404 Not Found`: Chat not found

---

## Test Data Generation Endpoints (Admin Only)

All test data generation endpoints require `ADMIN` role.

### POST /api/v1/test-data

Generate test data.

**Query Parameters**:

- `size`: Predefined dataset size (default: "small")
  - `tiny`: 5 employees, 5 projects, ~15 work experiences
  - `small`: 50 employees, 100 projects, ~250 work experiences
  - `medium`: 500 employees, 1,000 projects, ~4,000 work experiences
  - `large`: 2,000 employees, 4,000 projects, ~20,000 work experiences
  - `huge`: 50,000 employees, 100,000 projects, ~750,000 work experiences

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Test data generated successfully",
  "size": "small"
}
```

**Error Responses**:

- `400 Bad Request`: Invalid size parameter
- `401 Unauthorized`: Missing or invalid authentication (handled by Spring Gateway)
- `403 Forbidden`: Insufficient permissions (not admin)

### POST /api/v1/test-data/embeddings

Generate embeddings for work experience records.

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Embeddings generated successfully"
}
```

### POST /api/v1/test-data/graph

Build graph relationships from database data.

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Graph relationships built successfully"
}
```

### POST /api/v1/test-data/complete

Generate complete dataset: data + embeddings + graph.

**Query Parameters**:

- `size`: "small", "medium", or "large" (default: "small")
- `clear`: If true, clears existing data before generating (default: false)

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Complete dataset generated successfully",
  "size": "small"
}
```

---

## Error Response Format

All error responses follow this structure:

```json
{
  "errorCode": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2024-01-01T00:00:00Z",
  "details": ["Optional", "Additional", "Details"]
}
```

**Common Error Codes**:

- `VALIDATION_ERROR`: Request validation failed
- `RESOURCE_NOT_FOUND`: Resource not found
- `RETRIEVAL_ERROR`: Retrieval operation failed
- `SERVICE_UNAVAILABLE`: Service not configured or unavailable
- `INTERNAL_ERROR`: Unexpected internal error

---

## Authentication

ExpertMatch Service uses **header-based authentication** where Spring Gateway handles JWT validation and populates user
information in HTTP headers.

### Required Headers

All requests should include the following headers (populated by Spring Gateway):

```
X-User-Id: <USER_ID>
X-User-Roles: <ROLES>  (comma-separated, e.g., "ROLE_USER,ROLE_ADMIN")
X-User-Email: <EMAIL>  (optional)
```

**Note**: These headers are now documented in the OpenAPI specification and will appear in Swagger UI for all endpoints.
See the [OpenAPI Documentation](#openapi-documentation) section for details.

### Spring Gateway Integration

Spring Gateway:

1. Validates JWT tokens from clients
2. Extracts user information from JWT claims:
- User ID from `sub` claim → `X-User-Id` header
    - Roles from `authorities` claim → `X-User-Roles` header (comma-separated)
    - Email from `email` claim → `X-User-Email` header (optional)
3. Forwards requests to ExpertMatch Service with populated headers

### Local Development

For local development without Spring Gateway, headers can be set manually. If headers are missing, the service will use
`"anonymous-user"` as the default user ID.

### Example Request

```bash
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "X-User-Id: user-123" \
  -H "X-User-Roles: ROLE_USER" \
  -H "Content-Type: application/json" \
  -d '{"query": "Looking for experts in Java"}'
```

---

## Rate Limiting

Currently not implemented. Consider adding rate limiting for production.

---

## API Versioning

API versioning is done via URL path: `/api/v1/`

Future versions will use `/api/v2/`, etc.

---

## OpenAPI Documentation

- **Swagger UI**: http://localhost:8093/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8093/api/v1/openapi.json

The OpenAPI specification includes:

- All request/response schemas
- **User authentication headers** (`X-User-Id`, `X-User-Roles`, `X-User-Email`) documented as parameters for all
  endpoints
- Request/response examples
- Validation rules
- Error response schemas

All endpoints that require user identification now have the user headers documented in the Parameters section of Swagger
UI, making it easy to see which headers are required or optional.

---

## Health Check

- **Health Endpoint**: http://localhost:8093/actuator/health

