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

### Directory Layout

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

### Module Structure

ExpertMatch follows a **domain-driven module organization** with consistent, layered structure across all modules:

```
[domain-module]/
├── domain/                    # Domain entities, DTOs, enums, constants, filters, wrappers
│   ├── [Entity].java
│   ├── [RelatedEntity].java
│   ├── dto/                  # Data Transfer Objects
│   ├── filters/              # Query filters
│   └── wrappers/            # Response wrappers
├── repository/              # Data access layer interfaces
│   ├── [Entity]Repository.java        # Interface
│   ├── [RelatedEntity]Repository.java # Interface
│   └── impl/                 # Repository implementations
│       ├── [Entity]RepositoryImpl.java
│       └── [Entity]Mapper.java        # RowMapper for JDBC
├── service/                  # Business logic layer interfaces
│   ├── [Entity]Service.java           # Interface
│   ├── [RelatedEntity]Service.java    # Interface
│   └── impl/                 # Service implementations
│       └── [Entity]ServiceImpl.java
└── rest/                     # REST API controllers
    ├── [Entity]RestControllerV2.java
    └── [Entity]DataRestControllerV2.java
```

**Example: Employee Module**

```
employee/
├── domain/
│   └── Employee.java         # Employee entity record
├── repository/
│   ├── EmployeeRepository.java           # Interface
│   └── impl/
│       ├── EmployeeRepositoryImpl.java   # Implementation
│       └── EmployeeMapper.java           # RowMapper
├── service/
│   ├── EmployeeService.java              # Interface
│   ├── ExpertEnrichmentService.java      # Interface
│   └── impl/
│       ├── EmployeeServiceImpl.java      # Implementation
│       └── ExpertEnrichmentServiceImpl.java
└── rest/
    └── (REST controllers if needed)
```

**Key Principles:**

- **Interface-Based Design**: All services and repositories are defined as interfaces with separate implementations
- **Layer Separation**: Clear separation between domain, repository, service, and REST layers
- **Self-Contained Modules**: Each module contains its own domain, repository, service, and REST layers
- **Mappers in impl/**: RowMappers are located in the same `impl/` folder as repository implementations
- **Consistent Structure**: All modules follow the same structure for maintainability

See [Interface-Based Design](#interface-based-design) section for more details.

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

### Test-Driven Development Approach

ExpertMatch follows a **Test-Driven Development (TDD)** approach. See [CODING_RULES.md](CODING_RULES.md#test-driven-development-approach) for complete guidelines.

**Core Principles:**

1. **Integration Tests First**: Always prefer integration tests that verify full flow
2. **Full Flow Verification**: Test complete workflows from API endpoints to database
3. **Test Independence**: Each test prepares its own data and cleans up after itself
4. **Minimize Unit Tests**: Use unit tests only for pure logic, algorithms, or utilities

**Test Pattern:**

```java
@SpringBootTest
class MyServiceIT extends BaseIntegrationTest {
    
    @Autowired
    private MyService myService;  // Inject interface
    
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    
    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.related_table");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.main_table");
        
        // Prepare required test data for this test
        createTestData();
    }
    
    @Test
    @Transactional
    void testFullFlow() {
        // Test complete flow from service to database
        // Verify actual database changes, not just method calls
    }
}
```

### Backend Testing

See [Testing Guide](TESTING.md) for detailed information.

**Key Points:**

- Always use Testcontainers (never H2)
- Custom test container: `expertmatch-postgres-test:latest`
- Base class: `BaseIntegrationTest`
- TDD approach: Write integration tests first
- **Mock AI providers** - All tests use mocks, no real LLM calls
- **Test Independence** - Each test clears and prepares its own data

**Run Tests:**

```bash
mvn test                    # All tests
mvn test -Dtest=ClassName   # Specific test class
mvn clean test             # Fresh database container
mvn verify                 # Full build with tests
```

**Important: Test Isolation**

- Tests automatically use mock AI providers (ChatModel, EmbeddingModel)
- Each test must clear data in `@BeforeEach` for independence
- No real LLM API calls are made during tests
- Verify mock usage: `mvn test 2>&1 | grep -E "(MOCK|REAL|)"`
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

## Agent Skills Development

### Overview

Agent Skills provide modular knowledge management through Markdown-based skills that complement existing Java `@Tool`
methods. Skills are loaded from the classpath or filesystem and discovered on-demand during LLM interactions.

### Configuration

Agent Skills are **optional** and disabled by default. Enable them via configuration:

**application.yml**:

```yaml
expertmatch:
  skills:
    enabled: true  # Enable Agent Skills
    directory: .claude/skills  # Skills directory (classpath or filesystem)
```

**application-local.yml** (for local development):

```yaml
expertmatch:
  skills:
    enabled: true
```

### Skills Directory Structure

Skills are organized in directories under `.claude/skills/`:

```
.claude/skills/
├── expert-matching-hybrid-retrieval/
│   └── SKILL.md
├── rag-answer-generation/
│   └── SKILL.md
├── person-name-matching/
│   └── SKILL.md
├── query-classification/
│   └── SKILL.md
├── rfp-response-generation/
│   └── SKILL.md
└── team-formation/
    └── SKILL.md
```

### Implementation Details

**Spring AI 1.1.0 Compatibility**:

In Spring AI 1.1.0, `SkillsTool.Builder.build()` returns `ToolCallback`, not `SkillsTool`. The configuration handles
this:

```java
@Bean
@Qualifier("skillsTool")
public ToolCallback skillsTool() {
    SkillsTool.Builder builder = SkillsTool.builder();
    // Load from classpath
    builder.addSkillsResource(resourceLoader.getResource("classpath:.claude/skills"));
    // Optionally load from filesystem
    File skillsDir = new File(".claude/skills");
    if (skillsDir.exists() && skillsDir.isDirectory()) {
        builder.addSkillsDirectory(".claude/skills");
    }
    return builder.build();  // Returns ToolCallback
}
```

**ChatClient Integration**:

Agent Skills are registered via `defaultToolCallbacks()`, not `defaultTools()`:

```java
@Bean("chatClientWithSkills")
public ChatClient chatClientWithSkills(
        ChatClient.Builder builder,
        @Qualifier("skillsTool") ToolCallback skillsTool,
        FileSystemTools fileSystemTools,
        ExpertMatchTools expertTools,
        ChatManagementTools chatTools,
        RetrievalTools retrievalTools
) {
    return builder
            .defaultToolCallbacks(skillsTool)  // Agent Skills (ToolCallback)
            .defaultTools(fileSystemTools)  // File reading tools
            .defaultTools(expertTools, chatTools, retrievalTools)  // Java @Tool methods
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
}
```

### Creating a New Skill

1. Create a directory under `.claude/skills/`:
   ```bash
   mkdir -p src/main/resources/.claude/skills/my-new-skill
   ```

2. Create `SKILL.md` file:
   ```markdown
   # My New Skill

   ## Purpose
   Description of what this skill does.

   ## How to Use
   Instructions for the LLM on when and how to use this skill.

   ## Examples
   Example usage scenarios.
   ```

3. Skills are automatically discovered when the application starts (if enabled).

### Testing Agent Skills

Agent Skills configuration is tested in `AgentSkillsConfigurationIT`:

```java
@SpringBootTest
@TestPropertySource(properties = {"expertmatch.skills.enabled=true"})
class AgentSkillsConfigurationIT extends BaseIntegrationTest {
    @Autowired
    @Qualifier("skillsTool")
    private ToolCallback skillsTool;
    
    @Test
    void testSkillsToolBeanExists() {
        assertThat(skillsTool).isNotNull();
    }
}
```

### Dependencies

- `spring-ai-agent-utils:0.3.0` - Provides `SkillsTool` and `FileSystemTools`
- `spring-ai:1.1.0` - Spring AI framework (required for `ToolCallback` interface)

### Known Limitations

- **ToolSearchToolCallAdvisor**: Incompatible with Spring AI 1.1.0 (ToolCallAdvisor is final). The Tool Search Tool
  feature requires an updated version of `spring-ai-agent-utils` compatible with Spring AI 1.1.0.

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

## Data Access Patterns

ExpertMatch follows standardized data access patterns inspired by WCA-Backend for consistency, maintainability, and scalability.

### Overview

The project uses three key patterns for data access:

1. **External SQL Files** - SQL queries stored in separate `.sql` files
2. **Dedicated Row Mappers** - Reusable mapper classes for ResultSet mapping
3. **DataAccessUtils.uniqueResult()** - Standard Spring pattern for single-result queries

### Pattern 1: External SQL Files with @InjectSql

**Purpose**: Separate SQL logic from Java code for better maintainability and IDE support.

**Infrastructure**:

- `@InjectSql` annotation: Marks fields for SQL injection
- `SqlInjectBeanPostProcessor`: Loads SQL files at startup and injects them into annotated fields
- SQL files stored in `src/main/resources/sql/{module}/{operation}.sql`

**Usage Example**:

```java
@Repository
public class EmployeeRepository {
    @InjectSql("/sql/employee/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/employee/findByEmail.sql")
    private String findByEmailSql;

    public Optional<Employee> findById(String id) {
        Map<String, Object> params = Map.of("id", id);
        List<Employee> results = namedJdbcTemplate.query(findByIdSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }
}
```

**SQL File** (`src/main/resources/sql/employee/findById.sql`):
```sql
SELECT id, name, email, seniority, language_english, availability_status
FROM expertmatch.employee
WHERE id = :id
```

**File Organization**:
```
src/main/resources/sql/
├── employee/
│   ├── findById.sql
│   ├── findByEmail.sql
│   ├── findByIds.sql
│   └── findEmployeeIdsByName.sql
└── chat/
    ├── findById.sql
    ├── findAllByUserId.sql
    └── create.sql
```

**Benefits**:

- Full SQL syntax highlighting in IDE
- SQL changes reviewed independently
- Easier to maintain complex queries
- Separation of concerns
- No runtime performance overhead (loaded at startup)

**Error Handling**: `SqlInjectBeanPostProcessor` throws `BeanCreationException` at startup if SQL file is not found (fail-fast).

### Pattern 2: Dedicated Row Mapper Classes

**Purpose**: Centralize ResultSet mapping logic for reusability and testability.

**Location**: Mappers are located in the same `impl/` folder as repository implementations (e.g., `repository/impl/EmployeeMapper.java`).

**Implementation**:

Create mapper classes implementing Spring's `RowMapper<T>`:

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper implements RowMapper<Employee> {
    @Override
    public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Employee(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("seniority"),
                rs.getString("language_english"),
                rs.getString("availability_status")
        );
    }
}
```

**Usage in Repository Implementation**:

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepositoryImpl implements EmployeeRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmployeeMapper employeeMapper;

    public EmployeeRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            EmployeeMapper employeeMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Optional<Employee> findById(String id) {
        Map<String, Object> params = Map.of("id", id);
        List<Employee> results = namedJdbcTemplate.query(findByIdSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        Map<String, Object> params = Map.of("email", email);
        List<Employee> results = namedJdbcTemplate.query(findByEmailSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }
}
```

**Benefits**:

- Reusable across multiple repository methods
- Testable independently (unit test mapping logic)
- Centralized mapping logic (easier to maintain)
- Consistent pattern across all repositories
- Handles complex entities and nested objects well

**Null Handling Example** (ChatMapper):

```java
package com.berdachuk.expertmatch.chat.repository.impl;

import com.berdachuk.expertmatch.chat.domain.Chat;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper implements RowMapper<Chat> {
    @Override
    public Chat mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Chat(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("name"),
                rs.getBoolean("is_default"),
                mapTimestamp(rs, "created_at"),
                mapTimestamp(rs, "updated_at"),
                mapTimestamp(rs, "last_activity_at"),
                rs.getInt("message_count")
        );
    }

    private Instant mapTimestamp(ResultSet rs, String columnName) throws SQLException {
        var timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toInstant() : null;
    }
}
```

### Pattern 3: DataAccessUtils.uniqueResult()

**Purpose**: Standard Spring pattern for handling single-result queries with automatic validation.

**Usage**:

```java
import org.springframework.dao.support.DataAccessUtils;

public Optional<Employee> findById(String id) {
    Map<String, Object> params = Map.of("id", id);
    List<Employee> results = namedJdbcTemplate.query(findByIdSql, params, employeeMapper);
    return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
}
```

**Behavior**:

- **Single Result**: Returns the element if exactly one result exists
- **No Results**: Returns `null` (wrapped in `Optional.empty()`)
- **Multiple Results**: Throws `IncorrectResultSizeDataAccessException` (data integrity check)

**Benefits**:

- Concise and readable code
- Validates exactly one result (fails fast on unexpected duplicates)
- Standard Spring Framework pattern
- Better data integrity (detects constraint violations or query errors)
- Consistent with WCA-Backend approach

**Before (Manual Check)**:
```java
List<Employee> results = namedJdbcTemplate.query(sql, params, mapper);
return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
```

**After (DataAccessUtils)**:
```java
List<Employee> results = namedJdbcTemplate.query(sql, params, mapper);
return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
```

### Complete Example

**Repository Interface** (`repository/EmployeeRepository.java`):

```java
package com.berdachuk.expertmatch.employee.repository;

import com.berdachuk.expertmatch.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Optional<Employee> findById(String employeeId);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByIds(List<String> employeeIds);
}
```

**Repository Implementation** (`repository/impl/EmployeeRepositoryImpl.java`):

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.domain.Employee;
import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class EmployeeRepositoryImpl implements EmployeeRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmployeeMapper employeeMapper;

    @InjectSql("/sql/employee/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/employee/findByEmail.sql")
    private String findByEmailSql;

    public EmployeeRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            EmployeeMapper employeeMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public Optional<Employee> findById(String id) {
        Map<String, Object> params = Map.of("id", id);
        List<Employee> results = namedJdbcTemplate.query(findByIdSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        Map<String, Object> params = Map.of("email", email);
        List<Employee> results = namedJdbcTemplate.query(findByEmailSql, params, employeeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }
}
```

**Mapper** (`repository/impl/EmployeeMapper.java`):

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper implements RowMapper<Employee> {
    @Override
    public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Employee(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("seniority"),
                rs.getString("language_english"),
                rs.getString("availability_status")
        );
    }
}
```

### Migration Checklist

When creating new repositories or migrating existing ones:

- [ ] Create repository interface in `repository/` package
- [ ] Create repository implementation in `repository/impl/` package
- [ ] Create mapper class in `repository/impl/` package (same folder as implementation)
- [ ] Create SQL files in `src/main/resources/sql/{module}/`
- [ ] Add `@InjectSql` annotations to repository implementation fields
- [ ] Implement `RowMapper<T>` in mapper class
- [ ] Inject mapper via constructor in repository implementation
- [ ] Use `DataAccessUtils.uniqueResult()` for single-result queries
- [ ] Remove inline SQL strings and lambda mappers
- [ ] Update all references to use repository interface (not implementation)
- [ ] Run tests to verify functionality

### When to Use Each Pattern

**External SQL Files**: Always use for all SQL queries (separation of concerns, IDE support)

**Dedicated Mappers**: Use for all entities (reusability, testability, maintainability)

**DataAccessUtils.uniqueResult()**: Use for all single-result queries (`findById`, `findByEmail`, etc.)

### References

- **WCA-Backend Pattern**: `/home/berdachuk/projects/wca-lab/wca-backend`
- **Spring DataAccessUtils**: `org.springframework.dao.support.DataAccessUtils`
- **Spring RowMapper**: `org.springframework.jdbc.core.RowMapper`

## Interface-Based Design

ExpertMatch follows an **interface-based design pattern** for all services and repositories, ensuring loose coupling, better testability, and maintainability.

### Principles

- **Always Use Interfaces**: All services and repositories must be defined as interfaces with separate implementation classes
- **Interface Location**: Interfaces are in the main package (e.g., `service/`, `repository/`)
- **Implementation Location**: Implementations are in `impl/` subdirectories (e.g., `service/impl/`, `repository/impl/`)
- **Dependency Injection**: Always inject interfaces, never concrete implementations
- **Mapper Location**: RowMappers are located in the same `impl/` folder as repository implementations

### Service Example

**Service Interface** (`service/EmployeeService.java`):

```java
package com.berdachuk.expertmatch.employee.service;

import com.berdachuk.expertmatch.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeService {
    Optional<Employee> findById(String employeeId);
    List<Employee> findAll();
}
```

**Service Implementation** (`service/impl/EmployeeServiceImpl.java`):

```java
package com.berdachuk.expertmatch.employee.service.impl;

import com.berdachuk.expertmatch.employee.service.EmployeeService;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    
    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findById(String employeeId) {
        return employeeRepository.findById(employeeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}
```

### Repository Example

**Repository Interface** (`repository/EmployeeRepository.java`):

```java
package com.berdachuk.expertmatch.employee.repository;

import com.berdachuk.expertmatch.employee.domain.Employee;
import java.util.Optional;

public interface EmployeeRepository {
    Optional<Employee> findById(String employeeId);
}
```

**Repository Implementation** (`repository/impl/EmployeeRepositoryImpl.java`):

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepositoryImpl implements EmployeeRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final EmployeeMapper employeeMapper;
    
    public EmployeeRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            EmployeeMapper employeeMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.employeeMapper = employeeMapper;
    }
    
    @Override
    public Optional<Employee> findById(String employeeId) {
        // Implementation details
    }
}
```

**Mapper** (`repository/impl/EmployeeMapper.java`):

```java
package com.berdachuk.expertmatch.employee.repository.impl;

import com.berdachuk.expertmatch.employee.domain.Employee;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper implements RowMapper<Employee> {
    @Override
    public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
        // Mapping logic
    }
}
```

### Usage in Other Classes

Always inject interfaces, never implementations:

```java
@Service
public class QueryService {
    private final EmployeeService employeeService;  // Interface, not implementation
    private final EmployeeRepository employeeRepository;  // Interface, not implementation
    
    public QueryService(
            EmployeeService employeeService,
            EmployeeRepository employeeRepository) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
    }
}
```

### Benefits

- **Better Testability**: Easy to mock interfaces in unit tests
- **Loose Coupling**: Components depend on contracts, not implementations
- **Flexibility**: Easy to swap implementations without changing dependent code
- **Clear Separation**: Clear distinction between contract and implementation
- **Easier Maintenance**: Changes to implementation don't affect interface consumers

### Testing

**Note**: Prefer integration tests over unit tests (see Test-Driven Development Approach section). When unit tests are necessary, use implementation classes when instantiating directly:

```java
// Unit test (use sparingly - prefer integration tests)
@Test
void testPureLogic() {
    EmployeeRepository repository = new EmployeeRepositoryImpl(mockJdbcTemplate, mockMapper);
    EmployeeService service = new EmployeeServiceImpl(repository);
    // Test pure logic without database
}

// Integration test (preferred)
@SpringBootTest
class EmployeeServiceIT extends BaseIntegrationTest {
    @Autowired
    private EmployeeService employeeService; // Inject interface
    
    @Test
    void testFullFlow() {
        // Test complete flow with real database
    }
}
```

## Interface Method Documentation

ExpertMatch follows strict JavaDoc rules for interface methods. See [CODING_RULES.md](CODING_RULES.md#interface-method-documentation) for complete guidelines.

### Rule

**Always create JavaDoc comments for all methods in interfaces. Do not duplicate JavaDoc in implementation classes if the interface already has JavaDoc.**

### Interface Example

**Service Interface with JavaDoc** (`service/EmployeeService.java`):

```java
package com.berdachuk.expertmatch.employee.service;

import com.berdachuk.expertmatch.employee.domain.Employee;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for employee management operations.
 */
public interface EmployeeService {
    
    /**
     * Finds an employee by their unique identifier.
     *
     * @param employeeId The unique employee identifier (19-digit numeric string)
     * @return Optional containing the employee if found, empty otherwise
     */
    Optional<Employee> findById(String employeeId);
    
    /**
     * Retrieves all employees from the database.
     *
     * @return List of all employees, empty list if none found
     */
    List<Employee> findAll();
}
```

### Implementation Example

**Service Implementation without Duplicate JavaDoc** (`service/impl/EmployeeServiceImpl.java`):

```java
package com.berdachuk.expertmatch.employee.service.impl;

import com.berdachuk.expertmatch.employee.service.EmployeeService;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    
    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findById(String employeeId) {
        return employeeRepository.findById(employeeId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}
```

**Note**: Implementation methods use `@Override` annotation but do NOT duplicate JavaDoc from the interface.

### Benefits

- **Single Source of Truth**: Documentation lives in the interface, not duplicated
- **Easier Maintenance**: Update documentation in one place
- **Clear Contracts**: Interfaces clearly document the contract
- **Reduced Duplication**: No need to maintain duplicate documentation
- **SOLID Principles**: Supports Dependency Inversion Principle

## Technology Normalization and Skill Matching

ExpertMatch uses **Technology normalization** to improve skill matching accuracy. The system leverages the `technology`
table to normalize skill names and handle synonyms.

### Overview

When matching skills from queries against expert technologies, the system:

1. **Normalizes skill names** using the Technology table
2. **Handles synonyms** (e.g., "React" matches "ReactJS", "React.js", "react")
3. **Uses normalized matching** for improved accuracy
4. **Falls back** to simple case-insensitive matching if Technology table is empty

### Technology Table Structure

The `technology` table contains:

- `name`: Original technology name (e.g., "React")
- `normalized_name`: Normalized version (e.g., "react")
- `category`: Technology category (e.g., "Frontend Framework")
- `synonyms`: JSON array of synonyms (e.g., `["ReactJS", "React.js", "reactjs"]`)

### Usage in ExpertEnrichmentService

The `ExpertEnrichmentService` uses `TechnologyRepository` to normalize skills:

```java
@Service
public class ExpertEnrichmentServiceImpl implements ExpertEnrichmentService {
    private final TechnologyRepository technologyRepository;
    
    // Technology cache loaded once per service instance
    private Map<String, String> technologyCache;
    
    private String normalizeTechnology(String technology) {
        // 1. Check exact name match
        // 2. Check normalized name match
        // 3. Check synonym match
        // 4. Return normalized name or lowercase fallback
    }
    
    private boolean matchesSkill(String skill, String technology) {
        String normalizedSkill = normalizeTechnology(skill);
        String normalizedTech = normalizeTechnology(technology);
        
        // Match using normalized names
        return normalizedSkill.equals(normalizedTech) 
            || normalizedSkill.contains(normalizedTech)
            || normalizedTech.contains(normalizedSkill);
    }
}
```

### TechnologyRepository Methods

The `TechnologyRepository` provides methods for normalization:

- `findByName(String name)`: Find technology by exact name
- `findByNormalizedName(String normalizedName)`: Find by normalized name
- `findBySynonym(String synonym)`: Find by synonym
- `findAll()`: Get all technologies (for cache loading)

### Benefits

- **Improved Accuracy**: Handles technology name variations
- **Synonym Support**: Matches "React" with "ReactJS", "React.js", etc.
- **Performance**: Technology cache loaded once per service instance
- **Fallback**: Works even if Technology table is empty

### Adding New Technologies

To add new technologies with synonyms:

1. Insert into `technology` table:
   ```sql
   INSERT INTO technology (id, name, normalized_name, category, synonyms)
   VALUES ('...', 'React', 'react', 'Frontend Framework', 
           '["ReactJS", "React.js", "reactjs"]'::jsonb);
   ```

2. The cache will be reloaded on next service initialization or cache refresh

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

*Last updated: 2026-01-10 - Updated module structure and interface-based design documentation*

