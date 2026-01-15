package com.berdachuk.expertmatch;

import com.berdachuk.expertmatch.core.config.TestAIConfig;
import com.berdachuk.expertmatch.core.config.TestSecurityConfig;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Integration test to verify Spring Boot application context loads.
 * Uses Testcontainers with PostgreSQL as per project rules (no H2 database).
 * Extends BaseIntegrationTest to get proper database setup.
 * <p>
 * IMPORTANT: All LLM calls MUST be mocked.
 * - Uses TestAIConfig which provides @Primary mocks for ChatModel and EmbeddingModel
 * - All LLM API calls use mocked services to avoid external service dependencies
 */
@Import({TestSecurityConfig.class, TestAIConfig.class})
class ExpertMatchApplicationIT extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // Test passes if Spring context loads successfully
        // Uses Testcontainers PostgreSQL database as per project rules
    }
}

