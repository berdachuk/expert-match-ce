t see# Coding Rules and Guidelines

## General Rules

### No System Print Statements

**Rule**: Never use `System.out.println()`, `System.err.println()`, or any other `System.out`/`System.err` print
methods.

**Rationale**:

- System print statements bypass the logging framework
- They cannot be controlled by log levels or log configuration
- They make it difficult to manage log output in different environments
- They don't integrate with log aggregation and monitoring tools
- They can cause performance issues in production
- They make it harder to filter, format, and route logs

**Examples**:

- **Don't**: `System.out.println("Debug message");`
- **Don't**: `System.err.println("Error occurred");`
- **Do**: Use SLF4J logger: `logger.debug("Debug message");`
- **Do**: Use SLF4J logger: `logger.error("Error occurred", exception);`

**Implementation**:

- Use SLF4J logger (`org.slf4j.Logger`) for all logging
- Use appropriate log levels: `trace`, `debug`, `info`, `warn`, `error`
- Remove all `System.out.println()` and `System.err.println()` calls
- Replace with appropriate logger calls with proper log levels
- In tests, use `@Slf4j` annotation or create a logger instance

**Exception**:

- No exceptions - all logging must use the logging framework

### No Fallbacks

**Rule**: Do not create fallback implementations or default return values when services are unavailable.

**Rationale**:

- Failures should be explicit and visible
- Fallbacks can hide configuration issues
- Better to fail fast and fix the root cause
- Exceptions provide better debugging information

**Examples**:

- **Don't**: Return a default/fallback answer when LLM service is unavailable
- **Do**: Throw `IllegalStateException` or let exceptions propagate

**Implementation**:

- Remove all `generateFallback*` methods
- Remove try-catch blocks that return fallback values
- Remove null checks that return default values
- Let exceptions propagate to the global exception handler
- Ensure proper error handling at the API layer

**Exception Handling**:

- Use `GlobalExceptionHandler` for consistent error responses
- Log errors with full context
- Return appropriate HTTP status codes
- Provide meaningful error messages to clients

### No Name Abbreviations

**Rule**: Do not create abbreviated variable names. Use full, descriptive names.

**Rationale**:

- Abbreviations reduce code readability
- Full names are self-documenting and clearer
- Abbreviations can be ambiguous (e.g., `emp` could mean employee, employer, or employment)
- Modern IDEs provide autocomplete, so longer names don't slow down development

**Examples**:

- **Don't**: `emp : employees` or `for (Map<String, Object> emp : employees)`
- **Do**: `employee : employees` or `for (Map<String, Object> employee : employees)`
- **Don't**: `tech : technologies`, `proj : projects`, `exp : experiences`
- **Do**: `technology : technologies`, `project : projects`, `experience : experiences`

**Implementation**:

- Use full words for variable names in loops, lambdas, and method parameters
- Avoid abbreviations even for commonly shortened words
- Prefer clarity over brevity

**Exception - SQL Aliases**:

- Short aliases are allowed in SQL queries for table and column aliases
- SQL aliases improve query readability and are a common SQL practice
- Examples: `FROM expertmatch.work_experience we`, `SELECT e.id FROM employees e`
- This exception applies only to SQL query strings, not to Java variable names

## JDBC Template Usage

**Rule**: Always use `NamedParameterJdbcTemplate` when possible. Use `JdbcTemplate` only for special cases like `ConnectionCallback`.

**Naming Convention**:

- `namedJdbcTemplate` for `NamedParameterJdbcTemplate` instances
- `jdbcTemplate` for `JdbcTemplate` instances (only when needed for special operations)

**Rationale**:

- Named parameters improve code readability and maintainability
- Named parameters prevent parameter order errors
- Named parameters make SQL queries more self-documenting
- `JdbcTemplate` should only be used for special cases like `ConnectionCallback` (e.g., Apache AGE LOAD commands)

**Examples**:

- **Do**: `private final NamedParameterJdbcTemplate namedJdbcTemplate;`
- **Do**: Use `:paramName` syntax in SQL queries
- **Do**: Use `Map<String, Object>` for parameters
- **Don't**: Use `JdbcTemplate` with positional parameters (`?`) when named parameters can be used
- **Acceptable**: Use `JdbcTemplate` for `ConnectionCallback` operations (e.g., `jdbcTemplate.execute((ConnectionCallback<...>) ...)`)

**Implementation**:

- Always prefer `NamedParameterJdbcTemplate` for all database operations
- Use named parameters (`:paramName`) instead of positional parameters (`?`)
- Use `Map<String, Object>` to pass parameters
- Use `Collections.emptyMap()` for queries without parameters
- Only use `JdbcTemplate` when `ConnectionCallback` is required (e.g., Apache AGE extension loading)

## Apache AGE Cypher Execution

**Rule**: Do not handle Cypher execution failures gracefully. Let exceptions propagate.

**Rationale**:

- Cypher query failures indicate configuration or data issues that need to be fixed
- Graceful handling can hide problems with graph initialization or query syntax
- Failures should be explicit and visible for debugging
- Better to fail fast and fix the root cause

**Examples**:

- **Don't**: Catch Cypher exceptions and return empty lists
- **Don't**: Suppress Cypher errors with warnings
- **Do**: Let `RetrievalException` propagate from `GraphService.executeCypher()`
- **Do**: Let exceptions propagate from `GraphSearchService` methods

**Implementation**:

- `GraphService.executeCypher()` should throw `RetrievalException` on any failure
- `GraphSearchService` methods should throw `RetrievalException` on Cypher failures
- Do not catch and suppress Cypher execution exceptions
- Ensure proper error handling at the API layer via `GlobalExceptionHandler`

## Database Table Naming Convention

**Rule**: Use singular (not plural) names for database tables representing entities. Use plural names only for
many-to-many relationship tables.

**Rationale**:

- Singular names are more natural for entity tables (one row = one entity)
- Aligns with common database naming conventions
- Reduces confusion between table names and entity names
- Plural names are reserved for relationship tables that represent collections

**Examples**:

- **Do**: `expertmatch.chat` (table for chat entities)
- **Do**: `expertmatch.employee` (table for employee entities)
- **Do**: `expertmatch.work_experience` (table for work experience entities)
- **Do**: `expertmatch.project` (table for project entities)
- **Don't**: `expertmatch.chats`, `expertmatch.employees`, `expertmatch.work_experiences`
- **Do**: `expertmatch.employee_projects` (many-to-many relationship table)
- **Do**: `expertmatch.project_technologies` (many-to-many relationship table)

**Implementation**:

- Use singular names for all entity tables
- Use plural names only for many-to-many relationship/junction tables
- Apply this convention consistently across all database schemas
- Update existing table names if they violate this rule

**Exception**:

- Many-to-many relationship tables may use plural names (e.g., `employee_projects`, `project_technologies`)

## Integration Test Independence

**Rule**: All integration tests must be independent. Each test should create its own test data in a transaction and
restore the database state to what it was before the test.

**Rationale**:

- Tests should not depend on execution order
- Tests should not interfere with each other
- Tests should be able to run in parallel
- Tests should be repeatable and deterministic
- Database state pollution can cause flaky tests

**Examples**:

- **Don't**: Rely on data created by other tests
- **Don't**: Leave test data in the database after test completion
- **Don't**: Assume a clean database state
- **Do**: Create all required test data in `@BeforeEach` or test method
- **Do**: Clean up test data in `@AfterEach` or use transactions
- **Do**: Use `@Transactional` with rollback for test methods when appropriate

**Implementation**:

- Each test should create its own test data in `@BeforeEach` or at the start of the test method
- Use `@Transactional` annotation on test methods to automatically rollback changes
- Alternatively, explicitly delete test data in `@AfterEach` methods
- Clear relevant tables at the start of `@BeforeEach` to ensure clean state
- Use unique identifiers (timestamps, UUIDs) to avoid conflicts between parallel test runs
- For Testcontainers, each test class gets a fresh database, but tests within a class should still be independent

**Transaction Management**:

- Use `@Transactional` with `@Rollback` for test methods that modify data
- Use `@DirtiesContext` sparingly - prefer transaction rollback
- For tests that need to verify committed data, use separate transactions or explicit cleanup

**Example Pattern**:

```java
@BeforeEach
void setUp() {
    // Clear existing data to ensure clean state
    namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
    namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employee");

    // Create test data specific to this test
    createTestData();
}

@AfterEach
void tearDown() {
    // Clean up test data (if not using @Transactional)
    // namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.work_experience");
    // namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM expertmatch.employees");
}
```

## Lombok Usage

**Rule**: Use Lombok annotations to reduce boilerplate code when possible.

**Rationale**:

- Reduces repetitive code (getters, setters, constructors, builders)
- Improves code readability and maintainability
- Follows modern Java development practices
- Reduces potential for human error in boilerplate code
- Makes code more concise and focused on business logic

**Examples**:

- **Do**: Use `@Data` for simple data classes with getters, setters, toString, equals, and hashCode
- **Do**: Use `@Getter` and `@Setter` for controlled access to fields
- **Do**: Use `@AllArgsConstructor` and `@NoArgsConstructor` for constructor generation
- **Do**: Use `@Builder` for complex object creation
- **Do**: Use `@Slf4j` for logger injection
- **Do**: Use `@Value` for immutable classes
- **Don't**: Write manual getters, setters, constructors, or builders when Lombok can generate them

**Implementation**:

```java
// Simple data class
@Data
public class EmployeeDto {
    private String id;
    private String name;
    private String email;
    private String seniority;
}

// Service class with logger
@Slf4j
@Service
public class EmployeeService {
    // Logger automatically available as 'log'
    public void processEmployee(EmployeeDto employee) {
        log.info("Processing employee: {}", employee.getName());
        // business logic
    }
}

// Builder pattern
@Builder
@Getter
public class ComplexQuery {
    private String queryText;
    private int maxResults;
    private double minConfidence;
    private boolean includeSources;
    private boolean includeEntities;
}

// Immutable class
@Value
public class QueryResult {
    String queryId;
    List<EmployeeDto> employees;
    double confidenceScore;
}
```

**Best Practices**:

- Use `@Data` for simple DTOs and data transfer objects
- Use `@Getter` and `@Setter` for more control over field access
- Use `@Builder` for classes with many optional parameters
- Use `@Slf4j` for logger injection instead of manual logger creation
- Use `@Value` for immutable classes (automatically generates constructor, getters, equals, hashCode, toString)
- Combine annotations when appropriate: `@Data @Builder` or `@Getter @Setter @Builder`
- Avoid Lombok for entity classes that require custom JPA behavior

**Exceptions**:

- Entity classes with custom JPA behavior may need manual implementations
- Classes requiring custom serialization/deserialization logic
- Performance-critical code where method call overhead matters
- Classes that need custom validation in getters/setters

**Configuration**:

Ensure Lombok is properly configured in the project:

- Lombok dependency in `pom.xml`
- Lombok plugin installed in IDE
- Annotation processing enabled
- Lombok version compatible with Java version

**Benefits**:

- **Reduced Boilerplate**: Eliminates hundreds of lines of repetitive code
- **Maintainability**: Easier to modify and extend classes
- **Consistency**: Uniform code generation across the project
- **Productivity**: Faster development with less manual coding
- **Readability**: Focus on business logic rather than infrastructure code

## Spring AI PromptTemplate Usage

**Rule**: Use Spring AI's `PromptTemplate` with resource-based prompts for all LLM interactions.

**Rationale**:

- Separates prompt logic from business logic
- Enables easier prompt management and versioning
- Supports internationalization and localization
- Improves testability and maintainability
- Follows Spring's resource management patterns
- Enables prompt optimization without code changes

**Examples**:

- **Do**: Use `@Value` to inject prompt resources
- **Do**: Use `PromptTemplate` for structured prompt creation
- **Do**: Store prompts in resource files (`.st` or `.txt`)
- **Don't**: Hardcode prompts in Java code
- **Don't**: Use string concatenation for prompt building

**Implementation**:

```java
// Resource-based prompt injection
@Value("classpath:/prompts/skill-extraction.st")
private Resource skillExtractionPromptResource;

// PromptTemplate usage
public List<String> extractSkillsWithLLM(String query) {
    try {
        // Load prompt from resource
        PromptTemplate promptTemplate = new PromptTemplate(skillExtractionPromptResource);
        promptTemplate.setParameters(Map.of("query", query));

        // Create prompt
        Prompt prompt = promptTemplate.create();

        // Call LLM
        ChatResponse response = chatClient.call(prompt);

        // Parse response
        return parseSkillsFromResponse(response.getResult().getOutput().getContent());
    } catch (Exception e) {
        log.warn("LLM skill extraction failed, falling back to rule-based: {}", e.getMessage());
        return extractSkillsWithRules(query);
    }
}
```

**Best Practices**:

- Store prompts in `src/main/resources/prompts/` directory
- Use `.st` extension for StringTemplate prompts
- Use descriptive filenames (e.g., `skill-extraction.st`, `gap-analysis.st`)
- Include prompt metadata in comments at the top of each file
- Use parameterized prompts with clear variable names
- Document expected input/output format in prompt comments
- Version prompts when making significant changes

**Prompt File Structure**:

```text
# Skill Extraction Prompt v1.2
# Purpose: Extract technical skills from natural language queries
# Input: query (string) - user's natural language query
# Output: JSON array of extracted skills
# Example: ["Java", "Spring Boot", "AWS", "Microservices"]

Extract the technical skills mentioned in the following query. Return only the skill names as a JSON array:

Query: {query}

Skills:
```

**Benefits**:

- **Maintainability**: Prompts can be updated without code changes
- **Testability**: Easier to test with different prompt versions
- **Collaboration**: Non-developers can contribute to prompt optimization
- **Versioning**: Clear version history for prompt improvements
- **Localization**: Support for multiple languages and regional variations
- **Performance**: Prompt optimization without recompilation

**Configuration**:

Ensure proper Spring AI configuration:

- Spring AI dependency in `pom.xml`
- Resource loading properly configured
- PromptTemplate support enabled
- Resource directory included in classpath

**Exception Handling**:

- Gracefully handle missing prompt resources
- Provide meaningful error messages for prompt loading failures
- Fallback to default prompts when resource-based prompts are unavailable
- Log prompt-related errors for debugging and monitoring

## Fallback Method Creation

**Rule**: Do not create fallback methods unless explicitly specified in user requirements.

**Rationale**:

- Fallback methods can complicate code and create maintenance burden
- They may hide configuration issues or service failures
- Explicit requirements lead to better error handling and debugging
- Unnecessary fallbacks can mask real problems that need to be fixed

**Examples**:

- **Don't**: Create `extractSkillsWithHardcodedPrompt()` as a fallback unless explicitly requested
- **Do**: Let exceptions propagate and handle them at the appropriate level
- **Do**: Create fallback methods only when user requirements explicitly specify graceful degradation

**Implementation**:

- Remove unnecessary fallback methods that were not explicitly requested
- Use exception handling to provide clear error messages
- Let service failures be visible rather than silently falling back
- Document when fallbacks are intentionally implemented vs. when they should fail fast

**Exception**:

- Fallback methods may be created when user requirements explicitly specify graceful degradation
- Fallback methods may be created for critical system components where availability is paramount

## Markdown List Formatting

**Rule**: Always include an empty line between a list header and the list items.

**Rationale**:

- Ensures proper Markdown rendering
- Improves readability
- Prevents formatting issues in documentation
- Consistent formatting across all documents

**Examples**:

- **Don't**:
  ```markdown
  **Функциональность**:
  - Item 1
  - Item 2
  ```

- **Do**:
  ```markdown
  **Функциональность**:

  - Item 1
  - Item 2
  ```

**Implementation**:

- Always add an empty line after headers that introduce lists (e.g., `**Функциональность**:`, `**Технологии**:`, `**Предварительные требования**:`)
- Apply this rule to all Markdown documents
- Check all documentation files for compliance

**Exception**:

- No exceptions - all list headers must have an empty line before the list

## Code Samples Language

**Rule**: Always use English in code samples, comments, variable names, method names, and all code-related content.

**Rationale**:

- Code is international and should be readable by developers worldwide
- English is the standard language for programming
- Consistency across the codebase
- Better integration with libraries, frameworks, and tools
- Easier code reviews and collaboration

**Examples**:

- **Don't**:
  ```java
  // Поиск экспертов
  List<Сотрудник> сотрудники = сервис.найтиЭкспертов("Java");
  ```
  
- **Do**:
  ```java
  // Search for experts
  List<Employee> employees = service.findExperts("Java");
  ```

**Implementation**:

- All code samples in documentation must use English
- Variable names, method names, class names in English
- Comments in code samples in English
- Error messages in code samples in English
- Only documentation text (outside code blocks) can be in other languages

**Exception**:

- Documentation text outside code blocks can be in any language
- User-facing messages in the application can be localized
- Only code samples, code comments, and code-related content must be in English

## Imports and Class Names

**Rule**: Always use imports and simple class names for domain/data classes. Use fully qualified names for API/web model classes when there's a naming conflict.

**Rationale**:

- Domain/data classes are used more frequently in business logic, so shorter names improve readability
- API/web model classes are typically used less frequently and at boundaries, so fully qualified names provide clarity
- This pattern clearly distinguishes between domain models and API models
- Consistent approach across the codebase

**Examples**:

- **Do**: `import com.berdachuk.expertmatch.data.Employee;` then `List<Employee> employees = ...`
- **Do**: `List<com.berdachuk.expertmatch.api.model.Employee> apiEmployees = ...` (fully qualified for API models)
- **Don't**: `List<com.berdachuk.expertmatch.data.Employee> employees = ...` (use import instead)
- **Do**: `import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;` then `NamedParameterJdbcTemplate template;`

**Implementation**:

- Always import domain/data classes (e.g., `com.berdachuk.expertmatch.data.*`)
- Use fully qualified names for API/web model classes when there's a naming conflict (e.g., `com.berdachuk.expertmatch.api.model.Employee`)
- Import all third-party and standard library classes
- Organize imports: project domain imports first, then third-party, then standard library
- Let IDE handle import organization and cleanup

**Naming Conflict Resolution**:

- When both domain and API classes have the same simple name (e.g., `Employee`):
  - **Import the domain class**: `import com.berdachuk.expertmatch.data.Employee;`
  - **Use fully qualified name for API class**: `com.berdachuk.expertmatch.api.model.Employee`
- This pattern applies to all domain/API conflicts (Employee, Chat, Project, etc.)
- In interface implementations, use fully qualified names for API types that conflict with imported domain types

**Best Practices**:

- Import domain/data classes at the top of the file
- Use fully qualified names for API/web model classes in method signatures and variable declarations
- Use static imports for constants and utility methods when appropriate
- Keep imports organized and clean (IDEs can do this automatically)
- Remove unused imports regularly

## Application Configuration Files

**Rule**: Always use YAML (`.yml` or `.yaml`) files for Spring Boot application properties instead of `.properties` files.

**Rationale**:

- YAML provides better readability with hierarchical structure
- Easier to manage complex nested configurations
- More concise syntax for lists and maps
- Better support for multi-line values
- Industry standard for modern Spring Boot applications
- Easier to maintain and review in version control

**Examples**:

- **Don't**: Use `application.properties`:
  ```properties
  spring.datasource.url=jdbc:postgresql://localhost:5433/expertmatch
  spring.datasource.username=postgres
  spring.datasource.password=password
  expertmatch.query.enabled=true
  expertmatch.query.max-results=10
  ```

- **Do**: Use `application.yml`:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5433/expertmatch
      username: postgres
      password: password
  
  expertmatch:
    query:
      enabled: true
      max-results: 10
  ```

**Implementation**:

- Use `application.yml` or `application.yaml` as the main configuration file
- Place configuration files in `src/main/resources/`
- Use profile-specific files: `application-{profile}.yml` (e.g., `application-dev.yml`, `application-prod.yml`)
- Use proper YAML indentation (2 spaces, not tabs)
- Group related properties under common prefixes
- Use lists and maps when appropriate for better organization

**File Naming**:

- Main configuration: `application.yml` or `application.yaml`
- Profile-specific: `application-{profile}.yml` (e.g., `application-dev.yml`)
- Test configuration: `application-test.yml` (if needed)
- Both `.yml` and `.yaml` extensions are acceptable, but prefer `.yml` for consistency

**Best Practices**:

- Use hierarchical structure to group related properties
- Use comments (`#`) to document configuration sections
- Keep sensitive values in environment variables or external configuration
- Use Spring profiles for environment-specific configurations
- Validate YAML syntax before committing (most IDEs provide validation)
- Use consistent indentation (2 spaces recommended)
- **PostgreSQL default port**: Use port `5433` by default for PostgreSQL connections (instead of standard `5432`) to avoid conflicts with system PostgreSQL installations

**PostgreSQL Port Convention**:

- Default PostgreSQL port in application configuration: `5433`
- This avoids conflicts with system PostgreSQL installations that typically use port `5432`
- Example: `jdbc:postgresql://localhost:5433/expertmatch`
- Production environments may override this via environment variables or profile-specific configurations

**Exception**:

- No exceptions - all application configuration must use YAML format
