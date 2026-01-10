package com.berdachuk.expertmatch.llm.sgr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SGR (Schema-Guided Reasoning) patterns.
 */
@Configuration
@ConfigurationProperties(prefix = "expertmatch.sgr")
@Getter
@Setter
public class SGRPatternConfig {

    private boolean enabled = true;
    private boolean schemaValidation = true;
    private CascadeConfig cascade = new CascadeConfig();
    private RoutingConfig routing = new RoutingConfig();
    private CycleConfig cycle = new CycleConfig();

    @Getter
    @Setter
    public static class CascadeConfig {
        private boolean enabled = true;
        private String model;
    }

    @Getter
    @Setter
    public static class RoutingConfig {
        private boolean enabled = true;
        private String model;
    }

    @Getter
    @Setter
    public static class CycleConfig {
        private boolean enabled = true;
        private String model;
    }
}

