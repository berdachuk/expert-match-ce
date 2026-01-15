# ExpertMatch Backend - Quick Start Guide

## Overview

ExpertMatch is an enterprise-grade expert discovery and team formation system that uses hybrid GraphRAG retrieval to match project requirements with qualified experts.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL 17 with PgVector and Apache AGE extensions
- Ollama (for local LLM) - optional, can use remote

## Setup

### 1. Start Database Services

```bash
cd expert-match
# Create data directory if it doesn't exist
mkdir -p ~/data
docker compose up -d
```

This starts:

- PostgreSQL 17 with PgVector and Apache AGE
- Database migrations run automatically
- Database data is stored in `~/data/expertmatch-postgres` on your host machine

### 2. Configure Environment

Create `.env` file or set environment variables:

```bash
# Database
DB_USERNAME=expertmatch
DB_PASSWORD=expertmatch

# AI Provider Configuration
# You can configure different providers (Ollama or OpenAI) for chat, embedding, and reranking independently

# Option 1: Ollama (local or remote) - Default for all components
OLLAMA_BASE_URL=http://localhost:11435  # Use port 11435 for Docker Ollama, or 11434 for local Ollama
OLLAMA_MODEL=qwen3:4b-instruct-2507-q4_K_M  # Default local LLM model
EMBEDDING_MODEL=qwen3-embedding-8b

# Option 2: OpenAI or OpenAI-compatible provider
# OPENAI_API_KEY=your-api-key-here
# OPENAI_BASE_URL=https://api.openai.com  # Optional: Leave empty for OpenAI, or set for Azure OpenAI, etc.
# OPENAI_CHAT_MODEL=gpt-4-turbo-preview  # or provider-specific model name
# OPENAI_EMBEDDING_MODEL=text-embedding-3-large
# OPENAI_EMBEDDING_DIMENSIONS=1536
# Note: Ollama Cloud uses native API format, not OpenAI-compatible. Use OpenAI, Azure OpenAI, or other compatible providers.

# Option 3: Separate providers for each component (Advanced)
# Configure different providers and base URLs for chat, embedding, and reranking
# Example 1: Use OpenAI for chat, Ollama for embedding and reranking
# CHAT_PROVIDER=openai
# CHAT_BASE_URL=https://api.openai.com
# CHAT_API_KEY=sk-...
# CHAT_MODEL=gpt-4
# EMBEDDING_PROVIDER=ollama
# EMBEDDING_BASE_URL=http://localhost:11434
# EMBEDDING_MODEL=qwen3-embedding:8b
# RERANKING_PROVIDER=ollama
# RERANKING_BASE_URL=http://localhost:11434
# RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M

# Example 2: Use OpenAI-compatible API with local Ollama (as in application-local.yml.sample)
# This configuration uses OpenAI-compatible API format but connects to local Ollama
# Chat configuration
# CHAT_PROVIDER=openai
# CHAT_BASE_URL=http://localhost:11434
# # Chat model options:
# # - devstral-small-2:24b-cloud (24B, current default)
# # - qwen3:30b-a3b-instruct-2507-q4_K_M (30.5B, alternative)
# # - qwen3:4b-instruct-2507-q4_K_M (4B, faster)
# CHAT_MODEL=devstral-small-2:24b-cloud

# Embedding configuration
# EMBEDDING_PROVIDER=openai
# EMBEDDING_BASE_URL=http://localhost:11434
# EMBEDDING_MODEL=qwen3-embedding:0.6b

# Reranking configuration
# RERANKING_PROVIDER=openai
# RERANKING_BASE_URL=http://localhost:11434
# # Reranking can use a chat model - using the same model as chat for consistency
# # Reranking model options:
# # - devstral-small-2:24b-cloud (24B, current default)
# # - qwen3:30b-a3b-instruct-2507-q4_K_M (30.5B, alternative)
# # - qwen3:4b-instruct-2507-q4_K_M (4B, faster)
# RERANKING_MODEL=devstral-small-2:24b-cloud
# RERANKING_TEMPERATURE=0.1

# OAuth2 (for production)
OAUTH2_ISSUER_URI=https://oauth.example.com
OAUTH2_JWK_SET_URI=https://oauth.example.com/.well-known/jwks.json
```

**AI Provider Options:**

- **Ollama** (default): Local or remote Ollama instance, no API key needed
- **OpenAI**: Standard OpenAI API, requires API key
- **OpenAI-Compatible**: Any provider with OpenAI-compatible API (e.g., Azure OpenAI, Anthropic Claude via wrapper), requires API key and optional custom base URL
- **Note**: Ollama Cloud (`https://ollama.com/api`) uses Ollama's native API format, not OpenAI-compatible format. For OpenAI-compatible providers, use OpenAI, Azure OpenAI, or other compatible services.

**Separate Provider Configuration:**

You can configure different providers for chat, embedding, and reranking independently:

- **Chat Provider**: Set `CHAT_PROVIDER=ollama|openai` and configure `CHAT_BASE_URL`, `CHAT_API_KEY` (if needed), `CHAT_MODEL`
- **Embedding Provider**: Set `EMBEDDING_PROVIDER=ollama|openai` and configure `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY` (if needed), `EMBEDDING_MODEL`
- **Reranking Provider**: Set `RERANKING_PROVIDER=ollama|openai` and configure `RERANKING_BASE_URL`, `RERANKING_API_KEY` (if needed), `RERANKING_MODEL`, `RERANKING_TEMPERATURE` (optional, default: 0.1)

**Using OpenAI-Compatible API with Local Ollama:**

You can use the OpenAI-compatible API format with local Ollama by setting `PROVIDER=openai` and pointing `BASE_URL` to your local Ollama instance (e.g., `http://localhost:11434`). This is useful when:
- You want to use OpenAI-compatible API format but run models locally
- You're testing with local models before switching to cloud providers
- You want consistent API format across different environments

**Example Configuration** (as shown in `application-local.yml`):
- Chat: `CHAT_PROVIDER=openai`, `CHAT_BASE_URL=http://localhost:11434`, `CHAT_MODEL=devstral-small-2:24b-cloud`
- Embedding: `EMBEDDING_PROVIDER=openai`, `EMBEDDING_BASE_URL=http://localhost:11434`, `EMBEDDING_MODEL=qwen3-embedding:0.6b`
- Reranking: `RERANKING_PROVIDER=openai`, `RERANKING_BASE_URL=http://localhost:11434`, `RERANKING_MODEL=devstral-small-2:24b-cloud`, `RERANKING_TEMPERATURE=0.1`

**Model Options:**

- **Chat Models**: `devstral-small-2:24b-cloud` (24B, recommended), `qwen3:30b-a3b-instruct-2507-q4_K_M` (30.5B, alternative), `qwen3:4b-instruct-2507-q4_K_M` (4B, faster)
- **Embedding Models**: `qwen3-embedding:0.6b` (lightweight), `qwen3-embedding:8b` (higher quality)
- **Reranking Models**: Can use chat models (e.g., `devstral-small-2:24b-cloud`) or dedicated reranking models

This allows you to mix providers (e.g., OpenAI for chat, Ollama for embedding and reranking) for optimal cost and
performance.

### 3. Build and Run

```bash
mvn clean package
mvn spring-boot:run
```

**For verbose logging during development, enable the debug profile:**

```bash
# Enable debug profile for verbose logging
mvn spring-boot:run -Dspring-boot.run.profiles=local,debug
```

Application starts at: `http://localhost:8093`

**Note**: The ExpertMatch MVP uses Thymeleaf for server-side rendering. For development setup details, see the [Development Guide](DEVELOPMENT_GUIDE.md).

## Generate Test Data

### Authentication Note

**For local development without Spring Gateway:**
The backend service permits all requests without authentication (authentication is handled by Spring Gateway in
production). You can omit the `Authorization` header when testing locally.

**For production with Spring Gateway:**
Include the `Authorization` header with a valid JWT token.

### Option 1: Complete Pipeline (Recommended)

```bash
# Without authentication (local development)
curl -X POST "http://localhost:8093/api/v1/test-data/complete?size=small"

# With authentication (production with Spring Gateway)
curl -X POST "http://localhost:8093/api/v1/test-data/complete?size=small" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

This generates:

- ~100 employees
- ~500 work experience records
- Vector embeddings
- Graph relationships

### Option 2: Step by Step

```bash
# 1. Generate test data
# Without auth (local dev):
curl -X POST "http://localhost:8093/api/v1/test-data?size=small"
# With auth (production):
curl -X POST "http://localhost:8093/api/v1/test-data?size=small" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. Generate embeddings
# Without auth (local dev):
curl -X POST "http://localhost:8093/api/v1/test-data/embeddings"
# With auth (production):
curl -X POST "http://localhost:8093/api/v1/test-data/embeddings" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. Build graph
# Without auth (local dev):
curl -X POST "http://localhost:8093/api/v1/test-data/graph"
# With auth (production):
curl -X POST "http://localhost:8093/api/v1/test-data/graph" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Complete Test Data Generation Process with Embeddings and Graph

The ExpertMatch system provides a comprehensive workflow for generating complete test datasets including data,
embeddings, and graph relationships.

### Data Generation Workflow

1. **Technology Table Population** (NEW)
   - Creates a normalized technology catalog with metadata
   - Includes 20 technologies with categories, normalized names, and synonyms
   - Database table: `expertmatch.technology`

2. **Core Data Generation**
   - Employees: 100-10,000 records with skills and experience
   - Projects: 200-20,000 records with technology stacks
   - Work Experience: 500-300,000 records linking employees to projects
       - Includes CSV-aligned metadata JSONB with company, tools, roles, team information
       - Metadata structure matches CSV work experience format for maximum compatibility
   - Database tables: `expertmatch.employee`, `expertmatch.project`, `expertmatch.work_experience`

3. **Embedding Generation**
   - Creates vector embeddings for semantic search
   - Extracts text from work experience records
   - Generates embeddings using configured embedding service
   - Normalizes to 1536 dimensions
   - Database fields: `embedding` (vector), `embedding_dimension` (int)

4. **Graph Construction**
   - Creates Apache AGE graph relationships
   - Vertices: Expert, Project, Technology, Domain
   - Relationships: PARTICIPATED_IN, USES, IN_DOMAIN

### Complete Workflow Options

#### Option A: Step-by-Step Generation

```bash
# 1. Generate core data
POST /api/v1/test-data?size=small

# 2. Generate embeddings
POST /api/v1/test-data/embeddings

# 3. Build graph
POST /api/v1/test-data/graph
```

#### Option B: Complete Dataset Generation (Data + Embeddings + Graph)

```bash
POST /api/v1/test-data/complete?size=large
```

### Size Parameters

- **tiny**: 5 employees, 5 projects, ~15 work experiences (fastest, for quick testing)
- **small**: 50 employees, 100 projects, ~250 work experiences (default)
- **medium**: 500 employees, 1,000 projects, ~4,000 work experiences
- **large**: 2,000 employees, 4,000 projects, ~20,000 work experiences
- **huge**: 50,000 employees, 100,000 projects, ~750,000 work experiences (for performance testing)

### Data Flow

```
generateCompleteDataset("large")
    ↓
generateTestData("large")
    ↓
generateTechnologies()          ← NEW: Technology catalog
    ↓
generateEmployees(10000)        ← Employees with metadata
    ↓
generateProjects(20000)         ← Projects with technologies
    ↓
generateWorkExperience(10000,15) ← Work experiences linking employees to projects
    ↓
generateEmbeddings()             ← Vector embeddings for semantic search
    ↓
buildGraph()                     ← Apache AGE graph relationships
```

### Verification

After complete generation, the system contains:

- Normalized technology catalog with metadata
- Employee records with skills and experience
- Project records with technology stacks
- Work experience records with detailed project information
- CSV-aligned metadata (company, tools, roles, team, etc.) stored in JSONB
- Vector embeddings for all work experiences
- Graph relationships for expert discovery and analysis

### Usage in Development

The complete dataset enables:

- **Semantic Search**: Find experts using natural language queries
- **Graph Traversal**: Discover expert networks and technology usage patterns
- **Hybrid Retrieval**: Combine keyword, semantic, and graph-based search
- **Technology Analytics**: Analyze technology adoption and expertise distribution

## Test Expert Discovery

### Basic Query

```bash
# Without auth (local dev):
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Looking for experts in Java, Spring Boot, and AWS",
    "options": {
      "maxResults": 10,
      "rerank": true
    }
  }'

# With auth (production):
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "query": "Looking for experts in Java, Spring Boot, and AWS",
    "options": {
      "maxResults": 10,
      "rerank": true
    }
  }'
```

### Team Formation Query

```bash
# Without auth (local dev):
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Need a team for a banking app. Requirements: Java 21+, Spring Boot, AWS, MongoDB",
    "options": {
      "maxResults": 20,
      "rerank": true
    }
  }'

# With auth (production):
curl -X POST "http://localhost:8093/api/v1/query" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "query": "Need a team for a banking app. Requirements: Java 21+, Spring Boot, AWS, MongoDB",
    "options": {
      "maxResults": 20,
      "rerank": true
    }
  }'
```

## API Endpoints

### Query Endpoints
- `POST /api/v1/query` - Process expert discovery query
- `GET /api/v1/query/examples` - Get list of example queries (public endpoint, no authentication required)

### Chat Management
- `GET /api/v1/chats` - List all chats
- `POST /api/v1/chats` - Create new chat
- `GET /api/v1/chats/{chatId}` - Get chat details
- `PATCH /api/v1/chats/{chatId}` - Update chat
- `DELETE /api/v1/chats/{chatId}` - Delete chat
- `GET /api/v1/chats/default` - Get default chat
- `GET /api/v1/chats/{chatId}/history` - Get conversation history

### Test Data Generation (Admin Only)

- `POST /api/v1/test-data?size=small` - Generate test data
- `POST /api/v1/test-data/embeddings` - Generate embeddings
- `POST /api/v1/test-data/graph` - Build graph relationships
- `POST /api/v1/test-data/complete?size=small` - Complete pipeline (recommended)

## API Documentation

- Swagger UI: http://localhost:8093/swagger-ui.html
- OpenAPI Spec: http://localhost:8093/api/v1/openapi.json
- Health Check: http://localhost:8093/actuator/health

## Architecture

### Request Flow

```
1. User Query → QueryController
2. QueryParser → Extract requirements
3. HybridRetrievalService → 
   - VectorSearchService (semantic similarity)
   - GraphSearchService (relationship traversal)
   - KeywordSearchService (exact matches)
4. ResultFusionService → RRF fusion
5. SemanticReranker → Rerank results
6. ExpertEnrichmentService → Enrich with details
7. AnswerGenerationService → Generate answer
8. QueryResponse → Return structured data
```

### Modules

- **query**: Query processing and requirements analysis
- **retrieval**: Hybrid GraphRAG retrieval engine
- **llm**: LLM orchestration and answer generation
- **chat**: Chat management and conversation history
- **data**: Data access layer
- **embedding**: Vector embedding generation
- **graph**: Graph relationship management
- **ingestion**: Data ingestion and test data generation
- **exception**: Global exception handling

## Configuration

### Timeout Settings

For long-running queries (especially with LLM processing), the application includes increased timeout configurations in
`application-local.yml`:

- **Server HTTP Timeout**: 5 minutes (300,000ms) - for handling long-running query requests
- **Database Connection Timeout**: 60 seconds - for database operations
- **Tomcat Keep-Alive Timeout**: 5 minutes - for maintaining HTTP connections

These timeouts are configured to handle complex queries that may take several minutes to process, especially when using:

- Deep research mode
- Multiple SGR patterns (Cascade, Routing, Cycle)
- Large result sets with reranking
- Complex graph traversals

**Note**: If you experience timeout issues with very long-running queries, you may need to:

1. Increase Ollama server timeout (if using local Ollama)
2. Adjust timeout values in `application-local.yml`
3. Consider breaking complex queries into smaller sub-queries

## Troubleshooting

### Database Connection Issues

```bash
# Check if PostgreSQL is running
docker ps

# Check database logs
docker compose logs postgres

# Verify extensions
psql -U expertmatch -d expertmatch -c "SELECT * FROM pg_extension WHERE extname IN ('vector', 'age');"
```

### Ollama Connection Issues

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Pull required models
ollama pull qwen2.5:14b
ollama pull qwen3-embedding-8b
```

### Graph Building Issues

```bash
# Verify graph exists
psql -U expertmatch -d expertmatch -c "SELECT * FROM ag_catalog.ag_graph WHERE name = 'expertmatch_graph';"

# Check graph vertices
# (Use Apache AGE Cypher queries)
```

### Timeout Issues

If you encounter timeout errors during query processing:

1. **Check Server Timeout Configuration**:
   ```yaml
   # In application-local.yml.sample
   server:
     connection-timeout: 300000  # 5 minutes
   ```

2. **Verify Ollama Server Timeout** (if using local Ollama):
- Ensure Ollama server has sufficient timeout for long-running inference
    - Check Ollama server logs for timeout errors

3. **Monitor Query Processing Time**:
- Use execution trace (`includeExecutionTrace: true`) to identify slow steps
    - Check application logs for processing time information

4. **Adjust Timeout Values**:
- Increase `server.connection-timeout` in `application-local.yml` if needed
    - Restart application after configuration changes

## Next Steps

1. **Generate Test Data**: Use ingestion endpoints to populate database
2. **Test Queries**: Try different query types (expert search, team formation)
3. **Monitor Performance**: Check actuator endpoints for metrics
4. **Review Logs**: Check application logs for any issues
5. **Customize**: Adjust retrieval weights, LLM prompts, etc.

## Support

For issues or questions, refer to:

- Implementation Status: `IMPLEMENTATION_STATUS.md`
- API Specifications: `../openspec/specs/backend/`
- Database Schema: `src/main/resources/db/migration/`

---

*Last updated: 2026-01-04 - Updated frontend reference and verified endpoint paths*

