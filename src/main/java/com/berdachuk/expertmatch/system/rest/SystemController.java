package com.berdachuk.expertmatch.system.rest;

import com.berdachuk.expertmatch.api.HealthApi;
import com.berdachuk.expertmatch.api.MetricsApi;
import com.berdachuk.expertmatch.api.model.Health200Response;
import com.berdachuk.expertmatch.api.model.Metrics200Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.micrometer.metrics.actuate.endpoint.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

/**
 * REST controller for system endpoints (health, metrics).
 * Implements generated API interfaces from OpenAPI specification.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SystemController implements HealthApi, MetricsApi {

    private final HealthIndicator healthIndicator;
    private final MetricsEndpoint metricsEndpoint;

    public SystemController(@Qualifier("comprehensiveHealthIndicator")
                            HealthIndicator healthIndicator, MetricsEndpoint metricsEndpoint) {
        this.healthIndicator = healthIndicator;
        this.metricsEndpoint = metricsEndpoint;
    }

    /**
     * Override default getRequest() method to resolve conflict from implementing multiple interfaces.
     */
    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * Health check endpoint that uses comprehensive health indicator.
     * Verifies: database, vector store, LLM models, embedding models.
     */
    @Override
    public ResponseEntity<Health200Response> health() {
        log.debug("Health check endpoint called");
        long startTime = System.currentTimeMillis();

        Health health = healthIndicator.health();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Health check completed in {}ms - Status: {}", duration, health.getStatus().getCode());

        Health200Response response = new Health200Response()
                .status(health.getStatus().getCode())
                .details(health.getDetails())
                .checkDuration(duration + "ms");

        return ResponseEntity.ok(response);
    }

    /**
     * Service metrics endpoint.
     */
    @Override
    public ResponseEntity<Metrics200Response> metrics() {
        Metrics200Response response = new Metrics200Response()
                .available(true)
                .message("Metrics available at /actuator/metrics/{metricName}")
                .note("Use Actuator endpoints for detailed metrics");

        return ResponseEntity.ok(response);
    }
}

