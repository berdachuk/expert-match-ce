package com.berdachuk.expertmatch;

import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that the debug profile can be activated.
 * This test doesn't verify the actual logging output, but ensures
 * that the application can start with the debug profile.
 */
@org.springframework.test.context.ActiveProfiles({"debug", "test"})
public class DebugProfileIT extends BaseIntegrationTest {

    @Test
    public void testDebugProfileLoads() {
        // This test will pass if the application context loads successfully
        // with the debug profile, which means the configuration is valid
        assertTrue(true, "Debug profile should load without errors");
    }
}
