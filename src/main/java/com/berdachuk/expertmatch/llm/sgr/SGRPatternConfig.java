package com.berdachuk.expertmatch.llm.sgr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SGR (Schema-Guided Reasoning) patterns.
 */
@Configuration
@ConfigurationProperties(prefix = "expertmatch.sgr")
public class SGRPatternConfig {

    private boolean enabled = true;
    private boolean schemaValidation = true;
    private CascadeConfig cascade = new CascadeConfig();
    private RoutingConfig routing = new RoutingConfig();
    private CycleConfig cycle = new CycleConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSchemaValidation() {
        return schemaValidation;
    }

    public void setSchemaValidation(boolean schemaValidation) {
        this.schemaValidation = schemaValidation;
    }

    public CascadeConfig getCascade() {
        return cascade;
    }

    public void setCascade(CascadeConfig cascade) {
        this.cascade = cascade;
    }

    public RoutingConfig getRouting() {
        return routing;
    }

    public void setRouting(RoutingConfig routing) {
        this.routing = routing;
    }

    public CycleConfig getCycle() {
        return cycle;
    }

    public void setCycle(CycleConfig cycle) {
        this.cycle = cycle;
    }

    public static class CascadeConfig {
        private boolean enabled = true;
        private String model;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class RoutingConfig {
        private boolean enabled = true;
        private String model;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class CycleConfig {
        private boolean enabled = true;
        private String model;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}

