# ExpertMatch Development Guide

This guide provides comprehensive instructions for setting up and developing ExpertMatch backend and Thymeleaf-based frontend.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Backend Setup](#backend-setup)
4. [Frontend Setup (Thymeleaf)](#frontend-setup-thymeleaf)
5. [Development Workflow](#development-workflow)
6. [Testing](#testing)
7. [Common Tasks](#common-tasks)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

- **Java 21+** - Backend development
- **Maven 3.9+** - Build tool
- **Docker & Docker Compose** - Database and services
- **PostgreSQL 17** - Database (via Docker)
- **Git** - Version control

### Recommended Tools

- **IntelliJ IDEA** or **VS Code** - IDE
- **Postman** or **Insomnia** - API testing
- **pgAdmin** or **DBeaver** - Database management

## Project Structure

```
./
├── expert-match/              # Backend (Spring Boot) - current directory
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/         # Java source code
│   │   │   └── resources/    # Configuration files
│   │   └── test/             # Test code
│   ├── docs/                 # Documentation
│   └── pom.xml               # Maven configuration
│
├── src/main/resources/templates/  # Thymeleaf templates (MVP)
│   ├── fragments/            # Thymeleaf fragments
│   ├── index.html            # Main page template
│   └── ...                   # Other Thymeleaf templates
│
```

## Backend Setup

### 1. Clone and Navigate

```bash
cd expert-match
```

### 2. Build Test Container

The project uses a custom PostgreSQL test container with Apache AGE and PgVector:

```bash
./scripts/build-test-container.sh
```

Or manually:

```bash
docker build -f docker/Dockerfile.test -t expertmatch-postgres-test:latest .
```

### 3. Configure Environment

Copy and edit environment variables:

```bash
cp .env.example .env
# Edit .env with your configuration
```

Key environment variables:

- `SPRING_AI_OPENAI_API_KEY` - OpenAI API key for LLM (legacy, use component-specific keys below)
- `SPRING_DATASOURCE_URL` - PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

**AI Provider Configuration:**
You can configure different providers (Ollama or OpenAI) for chat, embedding, and reranking independently:

- **Chat**: `CHAT_PROVIDER`, `CHAT_BASE_URL`, `CHAT_API_KEY`, `CHAT_MODEL`, `CHAT_TEMPERATURE`
- **Embedding**: `EMBEDDING_PROVIDER`, `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`,
  `EMBEDDING_DIMENSIONS`
- **Reranking**: `RERANKING_PROVIDER`, `RERANKING_BASE_URL`, `RERANKING_API_KEY`, `RERANKING_MODEL`,
  `RERANKING_TEMPERATURE`

See [Backend Configuration](#backend-configuration) section for detailed configuration options.

### 4. Start Database Services

```bash
docker-compose up -d postgres
```

Wait for PostgreSQL to be ready (check logs):

```bash
docker-compose logs -f postgres
```

### 5. Run Database Migrations

Migrations are automatically applied on startup via Flyway. The project uses a single V1 migration for MVP.

### 6. Build and Run Backend

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Backend will be available at: `http://localhost:8093`

### 7. Verify Backend

- Health check: `http://localhost:8093/api/v1/system/health`
- Swagger UI: `http://localhost:8093/swagger-ui.html`
- API Docs: `http://localhost:8093/v3/api-docs`

## Frontend Setup (Thymeleaf)

ExpertMatch uses Thymeleaf for server-side rendering. The frontend is integrated with the Spring Boot backend and served on the same port.

### 1. Thymeleaf Templates Location

Thymeleaf templates are located in:

```
src/main/resources/templates/
├── fragments/            # Thymeleaf fragments
├── index.html            # Main page template
└── ...                   # Other Thymeleaf templates
```

### 2. Static Resources

Static resources (CSS, JavaScript, images) are located in:

```
src/main/resources/static/
├── css/                  # Stylesheets
├── js/                   # JavaScript files
└── images/               # Images
```

### 3. Development

Thymeleaf templates are automatically reloaded by Spring Boot DevTools when you make changes. No separate frontend build process is required.

### 4. Accessing the Frontend

Once the backend is running, access the frontend at:

- Main application: `http://localhost:8093`
- Swagger UI: `http://localhost:8093/swagger-ui.html`

## Development Workflow

### Backend Development

1. **Make Changes**
    - Edit Java source files in `src/main/java/`
    - Update OpenAPI spec if adding/modifying endpoints: `src/main/resources/api/openapi.yaml`
    - Follow [Coding Rules](CODING_RULES.md)

2. **Run Tests**
   ```bash
   mvn test                    # All tests
   mvn test -Dtest=ClassName   # Specific test
   ```

3. **Rebuild and Restart**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Check Logs**
    - Application logs: Console output
    - Database logs: `docker-compose logs postgres`

### Frontend Development (Thymeleaf)

1. **Make Changes**
    - Edit Thymeleaf templates in `src/main/resources/templates/`
    - Update fragments in `src/main/resources/templates/fragments/`
    - Modify static resources in `src/main/resources/static/`

2. **Hot Reload**
    - Spring Boot DevTools automatically reloads on file changes
    - Check browser console for errors
    - Restart application if needed: `mvn spring-boot:run`

3. **API Integration**
    - Use JavaScript `fetch()` API for REST API calls
    - Use Thymeleaf form submissions for form-based interactions
    - API endpoints are available at `/api/v1/*`

## Testing

### Backend Testing

See [Testing Guide](TESTING.md) for detailed information.

**Key Points:**

- Always use Testcontainers (never H2)
- Custom test container: `expertmatch-postgres-test:latest`
- Base class: `BaseIntegrationTest`
- TDD approach: Write tests first
- **Mock AI providers** - All tests use mocks, no real LLM calls

**Run Tests:**

```bash
mvn test                    # All tests
mvn test -Dtest=ClassName   # Specific test class
mvn clean test             # Fresh database container
```

**Important: Test Isolation**

- Tests automatically use mock AI providers (ChatModel, EmbeddingModel)
- No real LLM API calls are made during tests
- Verify mock usage: `mvn test 2>&1 | grep -E "(MOCK|REAL|⚠️)"`
- If you see real LLM usage, check for running application instances:
  ```bash
  ps aux | grep ExpertMatchApplication | grep -v grep
  # Stop any instances running with non-test profiles
  pkill -f "ExpertMatchApplication.*local"
  ```

See [Testing Guide - Mock AI Provider Configuration](TESTING.md#mock-ai-provider-configuration) for details.

### Frontend Testing

The Thymeleaf frontend is tested through:

- **Spring Boot** built-in validation
- **Integration tests** that verify template rendering
- **Manual testing** through browser

**Run Checks:**

```bash
mvn clean compile         # Compile and validate
mvn test                  # Run tests
```

## Configuration

#### Backend Configuration

**Timeout Settings** (`application-local.yml`):

The application includes increased timeout configurations for handling long-running queries:

```yaml
server:
  connection-timeout: 300000  # 5 minutes
  tomcat:
    connection-timeout: 300000
    keep-alive-timeout: 300000

spring:
  datasource:
    hikari:
      connection-timeout: 60000  # 60 seconds
      max-lifetime: 1800000  # 30 minutes
      idle-timeout: 600000  # 10 minutes
```

These timeouts are designed to handle:

- Complex queries with deep research
- Multiple SGR patterns (Cascade, Routing, Cycle)
- Large result sets with reranking
- Long-running LLM inference

**Note**: If you experience timeout issues, you may need to:

1. Increase timeout values in `application-local.yml`
2. Ensure Ollama server timeout is also increased (if using local Ollama)
3. Monitor query processing time using `includeExecutionTrace: true`

**AI Provider Configuration** (`application.yml` and `application-local.yml`):

The application supports configuring different AI providers (Ollama or OpenAI) for chat, embedding, and reranking
independently. This allows you to mix providers for optimal cost and performance.

**Configuration Properties:**

Each component (chat, embedding, reranking) can be configured separately via `spring.ai.custom.*` properties:

```yaml
spring:
  ai:
    custom:
      chat:
        provider: ${CHAT_PROVIDER:ollama}  # 'ollama' or 'openai' (default: ollama for local profile)
        base-url: ${CHAT_BASE_URL:${OLLAMA_BASE_URL:http://localhost:11434}}
        api-key: ${CHAT_API_KEY:${OLLAMA_API_KEY:ollama}}
        model: ${CHAT_MODEL:${OLLAMA_MODEL:qwen3:30b-a3b-instruct-2507-q4_K_M}}
        temperature: ${CHAT_TEMPERATURE:0.7}
      embedding:
        provider: ${EMBEDDING_PROVIDER:ollama}  # 'ollama' or 'openai' (default: ollama for local profile)
        base-url: ${EMBEDDING_BASE_URL:${OLLAMA_BASE_URL:http://localhost:11434}}
        api-key: ${EMBEDDING_API_KEY:${OLLAMA_API_KEY:ollama}}
        model: ${EMBEDDING_MODEL:${OLLAMA_EMBEDDING_MODEL:qwen3-embedding-8b}}
        dimensions: ${EMBEDDING_DIMENSIONS:1536}
      reranking:
        provider: ${RERANKING_PROVIDER:ollama}  # 'ollama' or 'openai'
        base-url: ${RERANKING_BASE_URL:http://localhost:11434}
        api-key: ${RERANKING_API_KEY:}
        model: ${RERANKING_MODEL:dengcao/Qwen3-Reranker-8B:Q4_K_M}
        temperature: ${RERANKING_TEMPERATURE:0.1}
```

**Environment Variables:**

You can configure providers via environment variables:

```bash
# Default (local profile): All services use local Ollama
export CHAT_PROVIDER=ollama
export CHAT_BASE_URL=http://localhost:11434
export CHAT_MODEL=qwen3:30b-a3b-instruct-2507-q4_K_M

export EMBEDDING_PROVIDER=ollama
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=qwen3-embedding-8b

export RERANKING_PROVIDER=ollama
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M

# Example: Use OpenAI for chat, Ollama for embedding and reranking
# export CHAT_PROVIDER=openai
# export CHAT_BASE_URL=https://api.openai.com
# export CHAT_API_KEY=sk-...
# export CHAT_MODEL=gpt-4
```

**Provider Selection:**

- **Ollama**: Use for local or remote Ollama instances. No API key required. Supports native Ollama API.
- **OpenAI**: Use for OpenAI or OpenAI-compatible providers (Azure OpenAI, etc.). Requires API key.

**Fallback Behavior:**

If custom configuration is not provided, the application falls back to auto-configured models:

- `spring.ai.ollama.*` for Ollama models
- `spring.ai.openai.*` for OpenAI models

The application automatically selects the appropriate model based on active Spring profiles (local prefers Ollama,
dev/staging/prod prefer OpenAI).

## Common Tasks

### Adding a New API Endpoint

1. **Backend**:

     - Add controller method in appropriate controller
    - Update OpenAPI spec: `src/main/resources/api/openapi.yaml`
    - Add tests
    - Rebuild backend

2. **Frontend**:

    - Update Thymeleaf templates to use new API endpoints
    - Update JavaScript code for API calls if needed
    - Test UI integration

### Adding a New Frontend Page

1. Create Thymeleaf template in `src/main/resources/templates/`
2. Add controller method to serve the page
3. Add route mapping in controller
4. Update navigation fragments if needed

### Database Schema Changes

1. **MVP**: Update `V1__initial_schema.sql` directly
2. **Post-MVP**: Create new migration file (V2, V3, etc.)
3. Test migrations with Testcontainers
4. Update entity classes if needed

### Debugging

**Backend:**

- Check application logs
- Use IntelliJ debugger
- Check database: `docker-compose exec postgres psql -U expertmatch`

**Frontend (Thymeleaf):**

- Browser DevTools console
- Network tab for API calls
- Thymeleaf template debugging
- Check API endpoint configuration
- Verify template syntax in IDE

## Troubleshooting

### Backend Issues

**Port Already in Use:**

```bash
# Find process using port 8093
lsof -i :8093
# Kill process or change port in application.properties
```

**Database Connection Failed:**

```bash
# Check if PostgreSQL is running
docker-compose ps postgres
# Check logs
docker-compose logs postgres
# Restart database
docker-compose restart postgres
```

**Test Container Not Found:**

```bash
# Build test container
./scripts/build-test-container.sh
```

**Maven Build Fails:**

```bash
# Clean and rebuild
mvn clean install
# Check Java version
java -version  # Should be 21+
```

### Frontend Issues (Thymeleaf)

**Styles Not Loading:**

- Check CSS file imports in templates
- Verify static resources are properly configured in `src/main/resources/static/`
- Check Thymeleaf template configuration
- Ensure Spring Boot static resource handling is enabled

**API Calls Fail:**

- Verify backend is running: `curl http://localhost:8093/api/v1/system/health`
- Check JavaScript console for errors
- Verify API endpoint URLs in JavaScript code
- Check browser console for CORS errors
- Ensure API endpoints are correctly mapped in controllers

**Template Not Rendering:**

- Check Thymeleaf template syntax
- Verify controller returns correct view name
- Check template location matches view resolver configuration
- Review application logs for template errors

### Common Solutions

**Clear All Caches:**

```bash
# Backend
mvn clean
```

**Reset Database:**

```bash
docker-compose down -v
docker-compose up -d postgres
# Migrations will run automatically
```

**Full Reset:**

```bash
# Backend
cd expert-match
mvn clean
docker-compose down -v
mvn clean install
```

## Best Practices

### Backend

- Follow TDD: Write tests first
- Use Testcontainers for all database tests
- Keep OpenAPI spec up-to-date
- Use Lombok `@Slf4j` for logging
- Never add fallback error handling
- Use PromptTemplates with `.st` files for LLM prompts

### Frontend (Thymeleaf)

- Use Thymeleaf templates for server-side rendering
- Use JavaScript fetch API or form submissions for API calls
- Follow Spring MVC patterns
- Use Bootstrap or custom CSS for styling
- Keep templates organized in fragments for reusability

## Additional Resources

- [Testing Guide](TESTING.md) - Detailed testing patterns
- [Coding Rules](CODING_RULES.md) - Development guidelines
- [API Endpoints](API_ENDPOINTS.md) - API reference
- [Authorization Guide](AUTHORIZATION_GUIDE.md) - Auth details
- [Cypher & Apache AGE Tutorial](CYPHER_APACHE_AGE_TUTORIAL.md) - Graph database guide

## Getting Help

1. Check this guide and related documentation
2. Review code examples in the codebase
3. Check test files for usage patterns
5. Contact the ExpertMatch team

---

*Last updated: 2025-12-21 - Added timeout configuration documentation*

