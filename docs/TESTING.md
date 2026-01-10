# Testing Guide

## Test Structure

The ExpertMatch backend includes unit tests and integration tests.

### Unit Tests

Located in `src/test/java/com/berdachuk/expertmatch/`:

- `QueryParserTest` - Tests query parsing functionality
- `ResultFusionServiceTest` - Tests RRF fusion logic
- `IdGeneratorTest` - Tests ID generation

### Integration Tests

Located in `src/test/java/com/berdachuk/expertmatch/`:

- `EmployeeRepositoryTest` - Database integration test for employee repository
- `WorkExperienceRepositoryTest` - Database integration test for work experience repository
- `VectorSearchServiceTest` - Integration test for vector similarity search
- `KeywordSearchServiceTest` - Integration test for keyword/full-text search
- `ChatServiceTest` - Integration test for chat management operations
- `QueryServiceIntegrationTest` - Full integration test for query processing pipeline
- `GraphServiceIT` - Integration test for Apache AGE graph operations
- `GraphBuilderServiceIT` - Comprehensive integration test for graph building from database data, including Customer
  vertices and relationships

### Unit Tests (Mocked Dependencies)

- `EmbeddingServiceTest` - Unit test for embedding generation (mocks EmbeddingClient)

### Integration Tests

Located in `src/test/java/com/berdachuk/expertmatch/integration/`:

- `BaseIntegrationTest` - Base class with Testcontainers setup
- `QueryServiceIntegrationTest` - Integration test for query processing
- `ExpertMatchApplicationTest` - Spring context loading test

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=QueryParserTest
```

### Run Integration Tests Only

```bash
mvn test -Dtest="*IntegrationTest"
```

### Run with Coverage

```bash
mvn test jacoco:report
```

## Test Configuration

### Test Profile

Tests use the `test` profile with:

- Testcontainers PostgreSQL for all database tests
- Disabled OAuth2 security
- Test-specific logging levels
- **Mock AI providers** - All LLM calls use mocks, no real API calls are made
- **Datafaker** - Synthetic test data generation using Datafaker library (replaces hardcoded arrays)
- **LLM Constant Expansion** - Optional LLM-based expansion of domain-specific constants (technologies, tools, etc.) via
  `ConstantExpansionService`

### Mock AI Provider Configuration

**CRITICAL**: All integration tests use **mock AI providers** instead of real LLM services. This ensures:
- Tests run fast without network dependencies
- No API costs or rate limits
- Tests are deterministic and reproducible
- Tests can run in CI/CD without Ollama or OpenAI access

#### How It Works

1. **TestAIConfig** (`src/test/java/com/berdachuk/expertmatch/config/TestAIConfig.java`):
- Provides `@Primary` mock beans for `ChatModel`, `EmbeddingModel`, and `rerankingChatModel`
    - Mocks return valid JSON responses without making real API calls
    - Only active in `test` profile

2. **SpringAIConfig Exclusion**:
- `SpringAIConfig` is excluded from test profile via `@Profile("!test")`
    - Prevents real LLM models from being created during tests

3. **Auto-Configuration Exclusions**:
- Spring AI auto-configuration classes are excluded in `application-test.yml`:
- `OllamaChatAutoConfiguration`
        - `OllamaEmbeddingAutoConfiguration`
        - `OllamaApiAutoConfiguration`
        - `OpenAiChatAutoConfiguration`
        - `OpenAiEmbeddingAutoConfiguration`

4. **BaseIntegrationTest**:
- All integration tests extend `BaseIntegrationTest`
    - Ensures test profile is active and mocks are used

#### Verifying Mock Usage

The test suite includes comprehensive logging to verify mocks are being used:

**Expected Log Output:**

```
INFO  TestAIConfig - Creating MOCK EmbeddingModel for tests - NO real LLM calls will be made
INFO  TestAIConfig - Creating MOCK ChatModel for tests - NO real LLM calls will be made
INFO  TestAIConfig - Creating MOCK rerankingChatModel for tests - NO real LLM calls will be made
INFO  TestAIConfigListener - ✓ SpringAIConfig not found (correct - should be excluded in test profile)
INFO  TestAIConfigListener - ✓ No OllamaApi beans found (correct)
INFO  TestAIConfigListener - ✓ No OpenAiApi beans found (correct)
INFO  TestAIConfigListener -   ✓ ChatModel: ChatModel$MockitoMock$... - MOCK (correct)
INFO  TestAIConfigListener -   ✓ EmbeddingModel: EmbeddingModel$MockitoMock$... - MOCK (correct)
```

**Warning Signs (Real LLM Usage Detected):**

```
ERROR TestAIConfigListener - ✗ ChatModel: OllamaChatModel - REAL OLLAMA (should be mock!)
ERROR TestAIConfigListener - ✗ EmbeddingModel: OpenAiEmbeddingModel - REAL OPENAI (should be mock!)
ERROR SpringAIConfig - REAL LLM CREATION: Creating OllamaApi...
```

#### Checking for Real LLM Usage

To verify tests are using mocks:

```bash
# Run tests and check for mock usage
mvn clean test 2>&1 | grep -E "(MOCK|REAL||✗)"

# Check for SpringAIConfig instantiation (should NOT happen in tests)
mvn clean test 2>&1 | grep "SpringAIConfig"

# Check for API bean creation (should NOT happen in tests)
mvn clean test 2>&1 | grep -E "(OllamaApi|OpenAiApi)"
```

#### Troubleshooting Real LLM Usage

If you see real LLM usage during tests:

1. **Check for Running Application Instances**:
   ```bash
   # Check for application running with non-test profile
   ps aux | grep ExpertMatchApplication | grep -v grep
   
   # If found, stop it:
   pkill -f "ExpertMatchApplication.*local"
   ```

2. **Verify Test Profile is Active**:
   ```bash
   # Check logs for profile activation
   mvn clean test 2>&1 | grep "The following profiles are active"
   # Should show: "test"
   ```

3. **Check Auto-Configuration Exclusions**:
- Verify `application-test.yml` has exclusions configured
    - Verify `BaseIntegrationTest` has exclusions in properties

4. **Verify TestAIConfig is Loaded**:
   ```bash
   # Should see "Creating MOCK" messages
   mvn clean test 2>&1 | grep "Creating MOCK"
   ```

5. **Check for SpringAIConfig Bean**:
- If `SpringAIConfig` is instantiated in test profile, it will throw an exception
    - Check logs for "SpringAIConfig should NOT be active in test profile"

#### Mock Behavior

**ChatModel Mock**:

- Returns empty arrays `[]` for list-based extractions (skills, seniority, technologies, entities)
- Returns JSON object `{"language": null, "proficiency": null}` for language extraction
- All responses are valid JSON without making real API calls

**EmbeddingModel Mock**:

- Returns 1024-dimensional embedding vectors (Ollama format)
- Vectors are padded to 1536 dimensions when stored in database
- No real embedding API calls are made

**RerankingChatModel Mock**:

- Returns empty array `[]` for reranking results
- No real reranking API calls are made

### Testcontainers

All database tests use Testcontainers to spin up:

- PostgreSQL 17 with PgVector extension
- Apache AGE extension

#### Building the Test Container Image

Before running integration tests, you must build the test container image:

```bash
# Build the test container image
./scripts/build-test-container.sh

# Or manually:
docker build -f docker/Dockerfile.test -t expertmatch-postgres-test:latest .
```

**Note**: The build may take 5-10 minutes on first build as it compiles the extensions.

**Troubleshooting**:

- If tests fail with "unhandled cypher(cstring) function call", rebuild the container image
- Ensure Docker is running and has sufficient resources
- The image must be tagged as `expertmatch-postgres-test:latest`
- Isolated test database
- Automatic schema initialization
- Extension setup (vector, age)

## Writing Tests

### Database Test Example

```java
class EmployeeRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private EmployeeRepository repository;
    
    @BeforeEach
    void setUp() {
        // Set up test data in PostgreSQL
    }
    
    @Test
    void testFindById() {
        // Test implementation
    }
}
```

### Integration Test Example

```java
class QueryServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private QueryService queryService;
    
    @Test
    void testProcessQuery() {
        // Test implementation
    }
}
```

## Test Data

### Using Test Data Generator

For integration tests that need data:

```java
@Autowired
private TestDataGenerator testDataGenerator;

@BeforeEach
void setUp() {
    testDataGenerator.generateTestData("small");
}
```

### Creating Test Data in Tests

For graph-related tests, use helper methods to create test data:

```java
class GraphBuilderServiceIT extends BaseIntegrationTest {

   // Define constants for test data
   private static final String CUSTOMER_MICROSOFT = "Microsoft";
   private static final String PROJECT_JAVA_BANKING = "Java Banking App";

   // Use helper methods to create test data
   private String createEmployee(String name, String emailPrefix, String seniority) {
      // Creates employee with unique email
   }

   private void createWorkExperience(String employeeId, String project,
                                     String role, String[] technologies,
                                     String industry, String customerId,
                                     String customerName) {
      // Creates work experience with optional customer data
   }

   @BeforeEach
   void setUp() {
      clearDatabaseTables();
      clearGraph();
   }
}
```

**Best Practices for Test Data Creation:**

- Use constants for test data values (customer names, projects, roles, etc.)
- Create helper methods for common operations (createEmployee, createWorkExperience)
- Extract cleanup logic into separate methods (clearDatabaseTables, clearGraph)
- Use unique identifiers (email prefixes with IDs) to avoid conflicts
- Keep setUp() methods simple and focused

## Best Practices

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Test Data**:
- Use constants for test data values instead of hardcoded strings
   - Create helper methods for common test data creation patterns
   - Use unique identifiers to avoid conflicts between tests
   - Extract cleanup logic into separate methods
4. **Cleanup**:
- Use `@Transactional` or manual cleanup in `@BeforeEach`/`@AfterEach`
   - Clear database tables and graph state before each test
   - Extract cleanup logic into helper methods (e.g., `clearDatabaseTables()`, `clearGraph()`)
5. **Assertions**: Use descriptive assertion messages
6. **Code Organization**:
- Keep `setUp()` methods simple and focused
   - Extract complex logic into helper methods
   - Use constants for magic strings and repeated values
   - Group related test data constants together

## Test Coverage Goals

- Unit tests: >80% coverage for business logic
- Integration tests: Cover main workflows
- API tests: Test all endpoints

## Troubleshooting

### Testcontainers Issues

If Testcontainers fails to start:
```bash
# Ensure Docker is running
docker ps

# Pull required images
docker pull pgvector/pgvector:pg17
```

### Testcontainers Issues

If Testcontainers fails:

- Ensure Docker is running: `docker ps`
- Pull required image: `docker pull pgvector/pgvector:pg17`
- Check Docker daemon is accessible
- Verify Testcontainers configuration in `BaseIntegrationTest`

### Security Configuration

Tests use `TestSecurityConfig` to disable OAuth2.
If security tests fail, check the test configuration.

### AI Provider Configuration

**IMPORTANT**: Tests automatically use mock AI providers. Do not configure real LLM services for tests.

**Test Configuration Files**:

- `src/test/resources/application-test.yml` - Test profile configuration
- `src/test/java/com/berdachuk/expertmatch/config/TestAIConfig.java` - Mock bean definitions
- `src/test/java/com/berdachuk/expertmatch/config/TestAIConfigListener.java` - Verification listener

**Key Points**:

- Never set `spring.ai.ollama.enabled=true` or `spring.ai.openai.enabled=true` in test profile
- Never configure real API keys or base URLs in test configuration
- All AI-related beans should be mocks in test profile
- If you see real LLM usage, check for running application instances with non-test profiles

## Graph Testing Patterns

### GraphBuilderServiceIT

The `GraphBuilderServiceIT` test class demonstrates best practices for graph-related integration tests:

**Key Patterns:**

- **Constants for Test Data**: All test data values (customers, projects, roles, technologies) are defined as constants
- **Helper Methods**: `createEmployee()` and `createWorkExperience()` methods reduce code duplication
- **Cleanup Methods**: `clearDatabaseTables()` and `clearGraph()` methods handle test isolation
- **Flexible Test Data**: `createTestData(boolean includeCustomers)` method supports different test scenarios

**Example Structure:**

```java
class GraphBuilderServiceIT extends BaseIntegrationTest {
   // Test data constants
   private static final String CUSTOMER_MICROSOFT = "Microsoft";
   private static final String[] TECH_JAVA_SPRING = {"Java", "Spring Boot"};

   @BeforeEach
   void setUp() {
      clearDatabaseTables();
      clearGraph();
   }

   private String createEmployee(String name, String emailPrefix, String seniority) {
      // Implementation with unique email generation
   }

   private void createWorkExperience(...) {
      // Implementation with optional customer data
   }
}
```

**Test Coverage:**

- Basic graph building (experts, projects, technologies, domains)
- Customer vertex creation
- Expert-Customer relationships (WORKED_FOR)
- Project-Customer relationships (FOR_CUSTOMER)
- Batch relationship creation
- Idempotency verification
- Edge cases (null customer IDs, empty data)

## WebController Integration Tests

The `WebControllerIT` class tests the web UI endpoints that make HTTP calls to the REST API.

### Key Features

- **Random Port Testing**: Uses `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` to start
  an embedded server
- **Dynamic API Base URL**: Uses `@DynamicPropertySource` to configure the API base URL dynamically based on the random
  port
- **Header-Based Authentication**: Tests verify that user headers (`X-User-Id`) are correctly passed to API endpoints

### Important: Avoiding Duplicate Headers

**CRITICAL**: When making API calls from `WebController`, the `X-User-Id` header must be set **only once**:
- The generated API client automatically sets the `X-User-Id` header from the `userId` parameter
- **Do NOT** call `setApiHeaders(userId)` before API methods that accept `userId` as a parameter
- Calling `setApiHeaders()` creates duplicate headers (e.g., `test-user-web-123,test-user-web-123`), which causes chat
  access validation to fail

**Correct Pattern**:

```java
//  CORRECT: Pass userId as parameter, generated client sets header
QueryResponse response = queryApi.processQuery(
                queryRequest,
                userId,  // Generated client sets X-User-Id header from this parameter
                null,    // X-User-Roles
                null     // X-User-Email
        );
```

**Incorrect Pattern**:

```java
//  WRONG: This creates duplicate headers
setApiHeaders(userId);  // Sets default header

QueryResponse response = queryApi.processQuery(
        queryRequest,
        userId,  // Generated client ALSO sets X-User-Id header
        null,
        null
);
// Result: X-User-Id header becomes "test-user-web-123,test-user-web-123"
```

### Chat Access Validation Tests

The following tests verify chat access validation:

- `testSendMessage_WithValidMessage_RedirectsToChats` - Verifies that sending a message to a chat validates ownership
- `testDeleteChat_WithValidChatId_RedirectsToIndex` - Verifies that deleting a chat validates ownership

These tests ensure that:

1. Users can only access chats they own
2. The `X-User-Id` header is correctly passed from the web controller to the API
3. Chat ownership validation works correctly

### Troubleshooting Chat Access Validation Failures

If you see "Access denied to chat" errors in tests:

1. **Check for Duplicate Headers**:
   ```bash
   # Look for duplicate user IDs in logs
   mvn test 2>&1 | grep "request.userId" | grep ","
   # Should NOT show: "request.userId=test-user-web-123,test-user-web-123"
   ```

2. **Verify Header is Set Once**:
- Check that `setApiHeaders()` is NOT called before API methods that accept `userId` parameter
   - The generated client automatically sets the header from the parameter

3. **Check User ID Consistency**:
- Ensure the test creates the chat with the same user ID used in the request
   - Verify the `X-User-Id` header matches the chat owner's user ID

---

## LLM Constant Expansion

The `ConstantExpansionService` provides optional LLM-based expansion of domain-specific constants in
`TestDataGenerator`. This feature allows the system to generate more varied test data by expanding base constants (
technologies, tools, project types, etc.) using LLM.

### How It Works

1. **Lazy Initialization**: `TestDataGenerator` uses lazy initialization - expansion happens only when test data
   generation starts (first call to any constant getter method), not during application startup
2. **One-Time Execution**: Expansion happens only once per application instance, using a synchronized method with a flag
   to ensure thread safety
3. **LLM Calls**: Service uses prompt templates to call LLM and request additional values for each constant type
4. **Caching**: Results are cached in two layers:
- `ConstantExpansionService` caches results by input hash to avoid repeated LLM calls
    - `TestDataGenerator` caches expanded constants in instance fields after first expansion
5. **Fallback**: If LLM is unavailable or fails, the service falls back to base constants
6. **Usage**: `TestDataGenerator` uses expanded constants when available, otherwise uses base constants

### Constant Types Expanded

- **Technologies**: Expands from ~20 to 50+ technology names
- **Tools**: Expands from ~14 to 30+ development tools
- **Project Types**: Expands from ~8 to 20+ project types
- **Team Names**: Expands from ~8 to 20+ team name patterns
- **Technology Categories**: Expands category mappings for new technologies
- **Technology Synonyms**: Expands synonym mappings for new technologies

### Test Environment

In tests, `ConstantExpansionService` uses mocked LLM (via `TestAIConfig`). The mock returns empty arrays `[]` by
default, which causes the service to fall back to base constants. This ensures:

- Tests run fast without real LLM calls
- Tests are deterministic and reproducible
- No API costs during testing

### Enabling Expansion

Expansion is **optional** and automatically enabled if:

- `expertmatch.ingestion.constant-expansion.enabled=true` is set in configuration (e.g., `application-local.yml`)
- `ConstantExpansionService` is available in the Spring context
- `ChatClient` is configured and available
- LLM call succeeds

If any of these conditions are not met, the system gracefully falls back to base constants.

**Important**: Expansion happens lazily when `TestDataGenerator` starts generating data, not during application
initialization. This ensures:

- No LLM calls during application startup
- Faster application startup time
- Expansion only occurs when actually needed

---

**Note**: For fullstack development including frontend testing, see the [Development Guide](DEVELOPMENT_GUIDE.md).
