package com.berdachuk.expertmatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for server configuration.
 */
@Component
@ConfigurationProperties(prefix = "server")
public class ApplicationProperties {

    private int port = 8080;
    private String contextPath = "";

    public int getServerPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServerContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
