package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP Client configuration with increased timeouts for LLM API calls.
 * Handles long-running requests to LLM endpoints that may take several minutes.
 */
@Slf4j
@Configuration
public class HttpClientConfig {

    /**
     * Creates a RestClient bean with increased timeouts for LLM API calls.
     * Timeouts are configured to handle long-running LLM inference requests:
     * - Connect timeout: 30 seconds
     * - Read timeout: 10 minutes (600 seconds) for long-running LLM requests
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        factory.setReadTimeout((int) Duration.ofMinutes(10).toMillis()); // 10 minutes for LLM requests

        log.info("Configured RestClient with timeouts - Connect: 30s, Read: 10m");

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}

