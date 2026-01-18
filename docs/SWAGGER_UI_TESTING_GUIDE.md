# Swagger UI Testing Guide

This guide provides step-by-step instructions for testing the ExpertMatch service using Swagger UI, including sample
data generation and testing main functionality.

## Prerequisites

1. **Service Running**: ExpertMatch service must be running on port 8093 (local profile)
2. **Database**: PostgreSQL with PgVector and Apache AGE extensions running
3. **Ollama**: Local Ollama service running on port 11434 with required models
4. **Browser**: Modern web browser to access Swagger UI

## Step 1: Start the Service

### On Remote Server (192.168.0.73)

```bash
# Connect to remote server
ssh user@192.168.0.73

# Navigate to project directory
cd ~/projects-ai/expert-match-root/expert-match

# Ensure database is running
docker compose -f docker-compose.dev.yml up -d

# Verify Ollama is available
curl http://localhost:11434/api/tags

# Start the service
export SPRING_PROFILES_ACTIVE=local
export OLLAMA_BASE_URL=http://localhost:11434
nohup mvn spring-boot:run -P local > expert-match.log 2>&1 &

# Wait for service to start (about 20 seconds)
sleep 20

# Verify service is running
curl http://localhost:8093/actuator/health
```

### Verify Service is Ready

```bash
# Check if service is listening
ss -tlnp | grep 8093

# Test health endpoint
curl http://localhost:8093/actuator/health
```

**Expected Response**: `{"status":"UP",...}`

## Step 2: Access Swagger UI

### Option 1: Direct Access (if firewall allows)

Open in your browser:

```
http://192.168.0.73:8093/swagger-ui.html
```

### Option 2: SSH Port Forwarding (if direct access blocked)

**On your local machine:**

```bash
ssh -L 8093:localhost:8093 user@192.168.0.73
```

**Keep this SSH session open**, then open in your browser:

```
http://localhost:8093/swagger-ui.html
```

### Swagger UI Interface

Once loaded, you should see:

- **API Documentation**: All available endpoints organized by tags
- **Try it out** buttons for each endpoint
- **Schemas**: Request/response models
- **Authorize** button (not needed for MVP - OAuth2 is disabled)

## Step 3: Generate Test Data

Before testing query functionality, you need to generate sample data.

### 3.1 Generate Test Data (Small Dataset)

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `POST /api/v1/test-data`
3. **Click**: "Try it out"
4. **Set parameters**:

- `size`: Select `small` from dropdown (or enter one of: `tiny`, `small`, `medium`, `large`, `huge`)
5. **Click**: "Execute"
6. **Wait**: This may take 1-2 minutes for small dataset (~50 employees)

**Expected Response**:

```json
{
  "size": "small",
  "employeesGenerated": 100,
  "workExperiencesGenerated": 500,
  "message": "Test data generated successfully"
}
```

### 3.2 Generate Embeddings

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `POST /api/v1/test-data/embeddings`
3. **Click**: "Try it out"
4. **Click**: "Execute"
5. **Wait**: This may take 2-5 minutes depending on dataset size

**Expected Response**:

```json
{
  "success": true,
  "message": "Embeddings generated successfully",
  "recordsProcessed": 500
}
```

**Note**: This step generates vector embeddings for all work experience records. It requires Ollama to be running and
the embedding model (`qwen3-embedding:8b`) to be available.

### 3.3 Build Graph Relationships (Optional)

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `POST /api/v1/test-data/graph`
3. **Click**: "Try it out"
4. **Click**: "Execute"

**Expected Response**:

```json
{
  "success": true,
  "message": "Graph relationships built successfully"
}
```

**Note**: This builds graph relationships in Apache AGE. Requires AGE extension to be installed.

### 3.4 Generate Complete Dataset (Alternative - All-in-One)

Instead of steps 3.1-3.3, you can use the complete dataset generation:

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `POST /api/v1/test-data/complete`
3. **Click**: "Try it out"
4. **Set parameters**:

- `size`: Select `small` from dropdown
5. **Click**: "Execute"
6. **Wait**: This performs all three steps (data + embeddings + graph) and may take 3-7 minutes

**Expected Response**:

```json
{
  "success": true,
  "message": "Complete dataset generated successfully",
  "data": {
    "employeesGenerated": 100,
    "workExperiencesGenerated": 500,
    "embeddingsGenerated": 500,
    "graphBuilt": true
  }
}
```

## Step 4: Test Chat Management

### 4.1 List All Chats

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `GET /api/v1/chats`
3. **Click**: "Try it out"
4. **Click**: "Execute"

**Expected Response**:

```json
{
  "chats": [
    {
      "id": "507f1f77bcf86cd799439011",
      "name": "Default Chat",
      "isDefault": true,
      "messageCount": 0,
      "createdAt": "2025-12-07T...",
      "updatedAt": "2025-12-07T..."
    }
  ]
}
```

**Note**: If this is the first time, you should see a default chat created automatically.

### 4.2 Create a New Chat

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `POST /api/v1/chats`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "name": "RFP Project - Banking App"
   }
   ```
5. **Click**: "Execute"

**Expected Response**:

```json
{
  "id": "507f1f77bcf86cd799439012",
  "name": "RFP Project - Banking App",
  "isDefault": false,
  "messageCount": 0,
  "createdAt": "2025-12-07T...",
  "updatedAt": "2025-12-07T..."
}
```

**Save the `id`** - you'll need it for query operations.

### 4.3 Get Chat Details

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `GET /api/v1/chats/{chatId}`
3. **Click**: "Try it out"
4. **Set path parameter**:

- `chatId`: Paste the chat ID from step 4.2
5. **Click**: "Execute"

**Expected Response**: Chat details with message count and timestamps.

## Step 5: Test Query Functionality

### 5.1 Basic Query (Without Chat Context)

1. **Navigate to**: `Query Endpoints` section
2. **Find endpoint**: `POST /api/v1/query`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "query": "Looking for experts in Java, Spring Boot, and AWS"
   }
   ```
5. **Click**: "Execute"
6. **Wait**: This may take 10-30 seconds as it:
- Generates embeddings for the query
    - Searches vector database
    - Reranks results (if enabled)
    - Generates answer using LLM

**Expected Response**:

```json
{
  "answer": "Based on your requirements, I found several experts...",
  "experts": [
    {
      "id": "8760000000000420950",
      "name": "John Doe",
      "email": "john.doe@example.com",
      "seniority": "A4",
      "relevanceScore": 0.92,
      "skillMatch": {
        "mustHaveMatched": 3,
        "mustHaveTotal": 3,
        "niceToHaveMatched": 2,
        "niceToHaveTotal": 4,
        "matchScore": 0.85
      }
    }
  ],
  "sources": [...],
  "entities": [...],
  "confidence": 0.85,
  "queryId": "...",
  "chatId": "...",
  "messageId": "...",
  "processingTimeMs": 1234
}
```

### 5.2 Query with Chat Context

1. **Navigate to**: `Query Endpoints` section
2. **Find endpoint**: `POST /api/v1/query`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "query": "Need experts for a banking project. Requirements: Java 21+, Spring Boot, AWS, MongoDB",
     "chatId": "507f1f77bcf86cd799439012",
     "options": {
       "maxResults": 10,
       "minConfidence": 0.7,
       "includeSources": true,
       "includeEntities": true,
       "rerank": true,
       "deepResearch": false
     }
   }
   ```
5. **Click**: "Execute"

**Note**: Using a `chatId` maintains conversation context. The system will remember previous queries in this chat.

### 5.3 Query with Advanced Options

1. **Navigate to**: `Query Endpoints` section
2. **Find endpoint**: `POST /api/v1/query`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "query": "Find team members for a microservices project. Need: Tech Lead (A4/A5), Java developers, Spring Boot, MongoDB, Kafka",
     "options": {
       "maxResults": 20,
       "minConfidence": 0.8,
       "includeSources": true,
       "includeEntities": true,
       "rerank": true,
       "deepResearch": true
     }
   }
   ```
5. **Click**: "Execute"

**Options Explained**:

- `maxResults`: Maximum number of experts to return (default: 10)
- `minConfidence`: Minimum confidence threshold (0.0-1.0, default: 0.7)
- `includeSources`: Include source work experiences in response
- `includeEntities`: Include extracted entities (skills, technologies, etc.)
- `rerank`: Enable semantic reranking (improves relevance)
- `deepResearch`: Enable deep research mode (more thorough analysis)

### 5.4 RFP Scenario Query

1. **Navigate to**: `Query Endpoints` section
2. **Find endpoint**: `POST /api/v1/query`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "query": "RFP Response: Need experts with Seniority A3-A4, English B2+, Must-have: Java 21+, Spring Boot, AWS, MongoDB, Gradle. Nice-to-have: Kubernetes, Terraform, Datadog. Responsibilities: ETL pipelines, HTTP services, system architecture, monitoring, on-call rotation",
     "options": {
       "maxResults": 15,
       "minConfidence": 0.75,
       "includeSources": true,
       "includeEntities": true,
       "rerank": true,
       "deepResearch": false
     }
   }
   ```
5. **Click**: "Execute"

This tests a complete RFP response scenario with detailed requirements.

## Step 6: View Conversation History

### 6.1 Get Conversation History

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `GET /api/v1/chats/{chatId}/history`
3. **Click**: "Try it out"
4. **Set path parameter**:

- `chatId`: Your chat ID from step 4.2
5. **Set query parameters** (optional):
- `page`: `0` (first page)
    - `size`: `20` (items per page)
    - `sort`: `sequence_number,asc` (sort order)
6. **Click**: "Execute"

**Expected Response**:

```json
{
  "messages": [
    {
      "id": "...",
      "role": "user",
      "content": "Looking for experts in Java...",
      "sequenceNumber": 1,
      "timestamp": "2025-12-07T..."
    },
    {
      "id": "...",
      "role": "assistant",
      "content": "Based on your requirements...",
      "sequenceNumber": 2,
      "timestamp": "2025-12-07T..."
    }
  ],
  "pageInfo": {
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

## Step 7: Test Streaming Query (SSE)

**Note**: Swagger UI has limited support for Server-Sent Events (SSE). For full SSE testing, use curl or an API client that supports SSE.

### Using curl (from terminal)

```bash
curl -N -X POST http://localhost:8093/api/v1/query-stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "query": "Find experts in Java and Spring Boot",
    "options": {
      "maxResults": 10
    }
  }'
```

**Expected Output**: Stream of events:

```
event: parsing
data: {"status":"parsing","message":"Parsing query..."}

event: retrieving
data: {"status":"retrieving","message":"Retrieving relevant experts..."}

event: reranking
data: {"status":"reranking","message":"Reranking results..."}

event: generating
data: {"status":"generating","message":"Generating answer..."}

event: complete
data: {"status":"complete","response":{...}}
```

## Step 8: Test Ingestion Status

### 8.1 Get All Ingestion Statuses

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `GET /api/v1/ingestion/status`
3. **Click**: "Try it out"
4. **Click**: "Execute"

**Expected Response**:

```json
{
  "statuses": [
    {
      "sourceName": "test-data",
      "state": "IDLE",
      "lastRunAt": "2025-12-07T...",
      "nextRunAt": null,
      "recordsProcessed": 500,
      "errors": []
    }
  ]
}
```

### 8.2 Get Specific Source Status

1. **Navigate to**: `Ingestion API` section
2. **Find endpoint**: `GET /api/v1/ingestion/status/{sourceName}`
3. **Click**: "Try it out"
4. **Set path parameter**:

- `sourceName`: `test-data`
5. **Click**: "Execute"

## Step 9: Test Error Scenarios

### 9.1 Invalid Query (Empty Query)

1. **Navigate to**: `Query Endpoints` section
2. **Find endpoint**: `POST /api/v1/query`
3. **Click**: "Try it out"
4. **Set request body**:
   ```json
   {
     "query": ""
   }
   ```
5. **Click**: "Execute"

**Expected Response**: `400 Bad Request` with validation error

### 9.2 Invalid Chat ID

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `GET /api/v1/chats/{chatId}`
3. **Click**: "Try it out"
4. **Set path parameter**:

- `chatId`: `invalid-id-123`
5. **Click**: "Execute"

**Expected Response**: `400 Bad Request` or `404 Not Found`

### 9.3 Invalid Pagination

1. **Navigate to**: `Chat Management` section
2. **Find endpoint**: `GET /api/v1/chats/{chatId}/history`
3. **Click**: "Try it out"
4. **Set query parameters**:

- `page`: `-1` (invalid)
    - `size`: `200` (exceeds max of 100)
5. **Click**: "Execute"

**Expected Response**: `400 Bad Request` with validation error

## Step 10: Complete Testing Workflow

### Recommended Testing Sequence

1. **Generate Test Data** (Step 3)
    - Generate small dataset
    - Generate embeddings
    - Build graph (optional)

2. **Create Chat** (Step 4.2)
    - Create a new chat for your testing session

3. **Run Queries** (Step 5)
    - Start with basic query
    - Try query with chat context
    - Test advanced options
    - Test RFP scenario

4. **Review Results** (Step 6)
    - Check conversation history
    - Verify context is maintained

5. **Test Edge Cases** (Step 9)
    - Invalid inputs
    - Error scenarios

## Troubleshooting

### Swagger UI Not Loading

**Issue**: Cannot access Swagger UI

**Solutions**:

1. Verify service is running: `curl http://localhost:8093/actuator/health`
2. Check firewall: Ensure port 8093 is accessible
3. Use SSH port forwarding if direct access is blocked
4. Try alternative URL: `http://192.168.0.73:8093/swagger-ui/index.html`

### Endpoints Return 500 Errors

**Issue**: All endpoints return `INTERNAL_ERROR`

**Solutions**:

1. Check service logs: `tail -f expert-match.log`
2. Verify database is running: `docker ps | grep postgres`
3. Verify Ollama is running: `curl http://localhost:11434/api/tags`
4. Check for validation errors in logs

### Query Returns No Results

**Issue**: Query returns empty experts array

**Solutions**:

1. Verify test data was generated: Check ingestion status
2. Verify embeddings were generated: Check ingestion status
3. Try a simpler query: "Java" instead of complex requirements
4. Check database: Verify data exists in `work_experience` table

### Embeddings Generation Fails

**Issue**: Embeddings endpoint returns error

**Solutions**:

1. Verify Ollama is running: `curl http://localhost:11434/api/tags`
2. Verify embedding model is available: `ollama list | grep embedding`
3. Check Ollama logs for errors
4. Verify `OLLAMA_BASE_URL` is set correctly

### Slow Query Responses

**Issue**: Queries take too long (>30 seconds)

**Solutions**:

1. Check Ollama performance: Verify models are loaded
2. Reduce `maxResults` in query options
3. Disable `deepResearch` if enabled
4. Check database performance: Verify indexes exist

## Tips for Effective Testing

1. **Start Small**: Use `small` dataset size for initial testing
2. **Save Chat IDs**: Keep track of chat IDs for context testing
3. **Monitor Logs**: Keep `tail -f expert-match.log` running in another terminal
4. **Test Incrementally**: Test one feature at a time
5. **Use Realistic Queries**: Test with actual RFP-like requirements
6. **Check Response Quality**: Review expert matches and relevance scores
7. **Test Conversation Context**: Make multiple queries in the same chat

## Additional Resources

- **OpenAPI Spec**: `http://192.168.0.73:8093/api/v1/openapi.json`
- **Actuator Health**: `http://192.168.0.73:8093/actuator/health`
- **Service Logs**: `tail -f expert-match.log` (on remote server)
- **Remote Setup Guide**: `docs/REMOTE_SSH_SETUP.md`

## Quick Reference

### Service URLs

- **Swagger UI**: `http://192.168.0.73:8093/swagger-ui.html`
- **API Base**: `http://192.168.0.73:8093/api/v1`
- **Health**: `http://192.168.0.73:8093/actuator/health`
- **OpenAPI Spec**: `http://192.168.0.73:8093/api/v1/openapi.json`

### Common Endpoints

| Endpoint                       | Method | Purpose                   |
|--------------------------------|--------|---------------------------|
| `/api/v1/test-data`            | POST   | Generate test data        |
| `/api/v1/test-data/embeddings` | POST   | Generate embeddings       |
| `/api/v1/test-data/complete`   | POST   | Generate complete dataset |
| `/api/v1/chats`                | GET    | List all chats            |
| `/api/v1/chats`                | POST   | Create new chat           |
| `/api/v1/query`                | POST   | Process query             |
| `/api/v1/query-stream`         | POST   | Stream query (SSE)        |
| `/api/v1/chats/{id}/history`   | GET    | Get conversation history  |

### Sample Request Bodies

**Create Chat**:

```json
{
  "name": "My Test Chat"
}
```

**Process Query**:

```json
{
  "query": "Find experts in Java and Spring Boot",
  "chatId": "507f1f77bcf86cd799439011",
  "options": {
    "maxResults": 10,
    "minConfidence": 0.7,
    "includeSources": true,
    "includeEntities": true,
    "rerank": true,
    "deepResearch": false
  }
}
```

## Next Steps

After completing this guide:

1. **Generate Larger Dataset**: Try `medium` or `large` size for more realistic testing
2. **Test Advanced Features**: Deep research, entity extraction, graph traversal
3. **Performance Testing**: Test with various query complexities
4. **Integration Testing**: Test with API clients for automated testing
5. **Review Results**: Analyze expert matching quality and relevance scores

---

**Note**: This guide assumes OAuth2 is disabled (MVP mode). If OAuth2 is enabled, you'll need to configure JWT tokens in
Swagger UI using the "Authorize" button.

