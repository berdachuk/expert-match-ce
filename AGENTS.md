# ExpertMatch Development Guide for Coding Agents

This guide provides essential information for coding agents working with the ExpertMatch codebase. It covers
build/lint/test commands, code style guidelines, and development practices.

## Build Commands

### Maven Commands

Build the project:

```bash
mvn clean install
```

Build without running tests:

```bash
mvn clean install -DskipTests
```

Build with specific profile:

```bash
mvn clean install -Plocal
```

Package for deployment:

```bash
mvn clean package
```

### Run Application

Run with Maven:

```bash
mvn spring-boot:run
```

Run with Java:

```bash
java -jar target/expert-match.jar
```

Run with specific profile:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
```

## Test Commands

### Run All Tests

```bash
mvn test
```

### Run Unit Tests Only

```bash
mvn test -Dtest="!*IT"
```

### Run Integration Tests Only

```bash
mvn verify
```

Or specifically:

```bash
mvn test -Dtest="*IT"
```

### Run Specific Test Class

```bash
mvn test -Dtest=EmployeeRepositoryIT
```

### Run Single Test Method

```bash
mvn test -Dtest=EmployeeRepositoryIT#testFindById
```

### Run Tests with Coverage Report

```bash
mvn test jacoco:report
```

### Test Container Setup

Before running integration tests, build the test container:

```bash
./scripts/build-test-container.sh
```

Or manually:

```bash
docker build -f docker/Dockerfile.test -t expertmatch-postgres-test:latest .
```

## Lint/Formatting Commands

### Check Code Quality

Currently no specific linting tools configured beyond Maven compiler checks.

## Spring Modulith Architecture

1. **Always Follow Modulith Best Practices**: This project uses Spring Modulith for modular architecture - always follow
   Modulith rules and best practices when writing code
2. **Module Boundaries**: Each module is defined by a `package-info.java` file with
   `@org.springframework.modulith.ApplicationModule` annotation
3. **Core Module Intentional Sharing**: The core module contains shared infrastructure intentionally used across all
   modules - this is an intentional design choice, not a violation
4. **Declare Module Dependencies**: Always explicitly declare module dependencies in `package-info.java` using
   `@ApplicationModule(allowedDependencies = {...})` annotation
5. **Dependency Declaration Syntax**: Declare dependencies using module names only (e.g., "core", "employee") without
   sub-package qualifiers like "::"
6. **Cross-module Dependencies**: Minimize cross-module dependencies and avoid circular dependencies between modules
7. **Service Orchestration**: Orchestration services like `QueryService` and `RetrievalService` may
   legitimately depend on multiple domain modules to coordinate complex workflows
8. **Module Access Verification**: The ModulithVerificationTest should pass and verify module structure compliance

## Code Style Guidelines

### General Principles

1. **Test-Driven Development**: Always follow the TDD approach:
    - Think about testing first
    - Create test before implementing the feature
    - Run the test to verify it fails (red phase)
    - Implement the feature to make the test pass (green phase)
    - Refactor while keeping tests green

2. **Interface-Based Design**: All services and repositories must be defined as interfaces with separate implementation
   classes

3. **SOLID Principles**: Follow Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and
   Dependency Inversion principles

4. **Domain-Driven Design**: Use ubiquitous language consistently, define bounded contexts, distinguish between entities
   and value objects

### Imports and Packages

1. **Package Structure**: Follow domain-driven module organization:
   ```
   [domain-module]/
   ├── domain/                    # Domain entities, DTOs, enums, constants, filters, wrappers
   │   ├── [Entity].java
   │   ├── dto/                  # Data Transfer Objects
   │   ├── filters/              # Query filters
   │   └── wrappers/            # Response wrappers
   ├── repository/              # Data access layer interfaces
   │   ├── [Entity]Repository.java        # Interface
   │   └── impl/                 # Repository implementations
   │       ├── [Entity]RepositoryImpl.java
   │       └── [Entity]Mapper.java        # RowMapper for JDBC
   ├── service/                  # Business logic layer interfaces
   │   ├── [Entity]Service.java           # Interface
   │   └── impl/                 # Service implementations
   │       └── [Entity]ServiceImpl.java
   └── rest/                     # REST API controllers
       ├── [Entity]RestControllerV2.java
       └── [Entity]DataRestControllerV2.java
   ```

2. **Import Statements**: Always use imports instead of fully qualified names
   ```java
   // Good
   import java.util.List;
   
   // Bad
   java.util.List
   ```

3. **Static Imports**: Use sparingly and only for well-known constants or utility methods

### Formatting

1. **Indentation**: Use 4 spaces for indentation (no tabs)
2. **Line Length**: Maximum 120 characters per line
3. **Brace Style**: Use Java standard (opening brace on same line)
4. **Blank Lines**: Use blank lines to separate logical sections of code

### Naming Conventions

1. **Classes**: PascalCase (e.g., `EmployeeService`)
2. **Methods**: camelCase (e.g., `findById`)
3. **Variables**: camelCase (e.g., `employeeId`)
4. **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
5. **Packages**: lowercase with dots (e.g., `com.berdachuk.expertmatch.employee`)
6. **Database Columns**: snake_case (e.g., `employee_id`, `created_at`)
7. **JSON/API Fields**: camelCase (e.g., `employeeId`, `messageType`)

### Types

1. **Records vs Classes**:
    - Use records for simple data holders (immutable)
    - Use classes when behavior or mutability is needed

2. **Collections**: Use appropriate collection types:
    - `List` for ordered collections
    - `Set` for unique elements
    - `Map` for key-value pairs

3. **Optionals**: Use `Optional<T>` for methods that may not return a value

4. **Primitive Wrappers**: Prefer primitives over wrappers when nullability is not needed

### Error Handling

1. **Never Add Fallbacks**: Do not implement fallback mechanisms that silently handle errors
2. **Fail Fast**: When operations fail, throw exceptions instead of falling back to alternative behavior
3. **Explicit Error Handling**: All errors should be explicitly handled and reported, not hidden by fallback logic
4. **Checked vs Unchecked Exceptions**:
    - Use unchecked exceptions for programming errors
    - Use checked exceptions for recoverable conditions

### Documentation

1. **JavaDoc**:
    - Always create JavaDoc comments for all methods in interfaces
    - Do not duplicate JavaDoc in implementation classes if the interface already has JavaDoc
    - Document parameters, return values, and exceptions

2. **Inline Comments**: Use sparingly and only for complex logic

3. **Language**: All code, comments, and documentation must be in English

### Lombok Usage

1. **Use Lombok to Simplify Code**: Always use Lombok annotations to reduce boilerplate code where possible
2. **Common Annotations**:
    - `@Slf4j` - For logging
    - `@Getter` / `@Setter` - For getters and setters
    - `@Data` - For simple data classes
    - `@Builder` - For builder pattern
    - `@AllArgsConstructor` / `@NoArgsConstructor` / `@RequiredArgsConstructor` - For constructors
    - `@ToString` - For toString methods
    - `@EqualsAndHashCode` - For equals and hashCode methods
    - `@Value` - For immutable value objects

### Logging

1. **Use Lombok Logging**: Always use Lombok's `@Slf4j` annotation for logging
2. **Log Levels**:
    - TRACE: Very detailed tracing information
    - DEBUG: Diagnostic information for developers
    - INFO: General operational information
    - WARN: Warning conditions that don't prevent functionality
    - ERROR: Error events that might still allow the application to continue running

### Transaction Management

1. **Service-Level Transactions**: Always manage transactions at the service layer, not at the repository or controller
   layer
2. **Use @Transactional**: Use Spring's `@Transactional` annotation on service methods that perform database operations
3. **Read-Only Transactions**: Use `readOnly = true` for read-only operations to optimize performance

## Repository Design

### Single Entity Principle

1. **Single Entity Focus**: Repositories should always work with one entity type
2. **Related Data Loading**: If related entity data needs to be loaded, prefer doing it in a single SQL query with JOINs
   and proper mapping
3. **Service-Level Aggregation**: If related data cannot be loaded in a single SQL query, the aggregation should happen
   at the service layer, not in the repository
4. **Batch Loading**: When loading related data for a collection of entities, always use batch loading methods that
   accept a collection of IDs and return a Map

### Data Access Patterns

1. **External SQL Files**: Store SQL queries in separate `.sql` files
2. **Dedicated Row Mappers**: Use reusable mapper classes for ResultSet mapping
3. **DataAccessUtils.uniqueResult()**: Use for single-result queries with automatic validation

## Testing Guidelines

### Test Structure

1. **Integration Tests First**: Prefer integration tests over unit tests
2. **Full Flow Verification**: Test complete workflows from API endpoints to database
3. **Test Independence**: Each test prepares its own data and cleans up after itself
4. **Minimize Unit Tests**: Use unit tests only for pure logic, algorithms, or when integration tests are impractical

### Test Implementation

1. **Extend BaseIntegrationTest**: All integration tests should extend `BaseIntegrationTest`
2. **Inject Interfaces**: Always inject service/repository interfaces, never concrete implementations
3. **Clear Data in @BeforeEach**: Always clear relevant tables before creating test data
4. **Mock AI Providers**: Tests automatically use mock AI providers instead of real LLM services

### Naming Conventions

1. **Integration Tests**: Use `*IT` suffix (e.g., `EmployeeRepositoryIT`)
2. **Unit Tests**: Use `*Test` or `*Tests` suffix (e.g., `QueryParserTest`)

## AI Provider Configuration

### OpenAI-Compatible Providers Only

**CRITICAL**: The application uses **OpenAI-compatible providers only**. Ollama is excluded from the project.

### Component-Specific Configuration

The application supports configuring different OpenAI-compatible providers for chat, embedding, and reranking
independently:

- **Chat**: `CHAT_PROVIDER` (must be `openai`), `CHAT_BASE_URL`, `CHAT_API_KEY`, `CHAT_MODEL`, `CHAT_TEMPERATURE`
- **Embedding**: `EMBEDDING_PROVIDER` (must be `openai`), `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`,
  `EMBEDDING_DIMENSIONS`
- **Reranking**: `RERANKING_PROVIDER` (must be `openai`), `RERANKING_BASE_URL`, `RERANKING_API_KEY`, `RERANKING_MODEL`,
  `RERANKING_TEMPERATURE`

### Default Configuration

- **Chat**: `openai` provider, `https://api.openai.com`, `gpt-4`
- **Embedding**: `openai` provider, `https://api.openai.com`, `text-embedding-3-large` (1536 dimensions)
- **Reranking**: `openai` provider, `https://api.openai.com`, `gpt-4` (uses chat models for semantic reranking)

### Base URL Format

**IMPORTANT**: For OpenAI-compatible APIs, do **NOT** include `/v1` in the base URL. Spring AI's `OpenAiApi`
automatically adds `/v1/chat/completions` or `/v1/embeddings`.

**Valid Examples**:

- `https://api.openai.com` (OpenAI)
- `https://YOUR_RESOURCE.openai.azure.com` (Azure OpenAI)
- `https://api.provider.com` (Other OpenAI-compatible service)

**Invalid Examples**:

- `https://api.openai.com/v1` (includes /v1)
- `http://localhost:11434` (Ollama endpoint)

### Supported Providers

- **OpenAI**: `https://api.openai.com`
- **Azure OpenAI**: `https://YOUR_RESOURCE.openai.azure.com`
- **Other OpenAI-compatible services**: Any service that implements OpenAI API format

## Prompt Management

1. **Always Use PromptTemplates**: All LLM prompts must use Spring AI `PromptTemplate` with external `.st` (
   StringTemplate) files
2. **Template Location**: Store all prompt templates in `src/main/resources/prompts/` directory
3. **Template Format**: Use `.st` file extension for StringTemplate files

## Agent Skills Development

### Configuration

Agent Skills are optional and disabled by default. Enable them via configuration:

```yaml
expertmatch:
  skills:
    enabled: true
    directory: .claude/skills
```

### Skills Directory Structure

Skills are organized in directories under `.claude/skills/`:

```
.claude/skills/
├── expert-matching-hybrid-retrieval/
│   └── SKILL.md
├── rag-answer-generation/
│   └── SKILL.md
└── query-classification/
    └── SKILL.md
```

## Technology Stack

- **Backend**: Spring Boot 3.5.9, Java 21
- **Database**: PostgreSQL 17, PgVector, Apache AGE
- **AI Framework**: Spring AI
- **Testing**: JUnit 5, Testcontainers, Mockito
- **Build Tool**: Maven 3.9+

## Useful Scripts

### Build Test Container

```bash
./scripts/build-test-container.sh
```

### Restart Service

```bash
./scripts/restart-service-local.sh
```

## Development Environment

### Profiles

The application supports multiple profiles:

- `local` - Local development with OpenAI-compatible providers
- `dev` - Development with remote AI providers (OpenAI-compatible)
- `test` - Testing environment
- `staging` - Staging environment
- `prod` - Production environment

**Note**: All profiles use OpenAI-compatible providers only. Ollama is excluded from the project.

### Database Migrations

Use Flyway for database migrations:

- Migration files should be named descriptively: `V{N}__{description}.sql`
- Use only V1 migration script with all required changes consolidated for production/development
- During learning path, use incremental migration versions (V1, V2, V3, etc.)

## Common Tasks

### Adding a New Module

1. Create a new directory under the main source root following the domain module structure
2. Implement domain entities, repositories, services, and REST controllers
3. Add corresponding tests (prefer integration tests)
4. Update OpenAPI specification if needed
5. Add database migrations if necessary

### Database Schema Changes

1. **MVP**: Update `V1__initial_schema.sql` directly
2. **Post-MVP**: Create new migration file (V2, V3, etc.)
3. Test migrations with Testcontainers

### Adding New API Endpoints

1. Add controller method in appropriate controller
2. Update OpenAPI spec: `src/main/resources/api/openapi.yaml`
3. Add tests (integration tests preferred)
4. Rebuild backend

## Troubleshooting

### Test Issues

If tests fail unexpectedly:

1. Ensure test container is built: `./scripts/build-test-container.sh`
2. Check for running application instances with non-test profiles
3. Verify that mocks are being used instead of real LLM APIs

### Common Solutions

```bash
# Clear all caches and rebuild
mvn clean install

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

This guide provides essential information for coding agents working with ExpertMatch. Always refer to the .cursorrules
file for detailed coding rules and development practices.
