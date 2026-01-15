package com.berdachuk.expertmatch.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for server configuration.
 */
@Component
@ConfigurationProperties(prefix = "server")
@Getter
@Setter
public class ApplicationProperties {

    private int port = 8080;
    private String contextPath = "";

    public int getServerPort() {
        return port;
    }

    public String getServerContextPath() {
        return contextPath;
    }
}
