package com.berdachuk.expertmatch.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    /**
     * Security configuration - permits all requests without authentication.
     * Authentication and authorization are handled by Spring Gateway, which populates
     * user information in HTTP headers (X-User-Id, X-User-Roles, X-User-Email).
     * This service only handles CORS configuration for Swagger UI and frontend access.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());
        return http.build();
    }

    /**
     * CORS configuration to allow Swagger UI and frontend applications to access the API.
     * Allows all origins, methods, and headers for MVP development.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all origins for MVP (can be restricted in production)
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Total-Count"));
        // Allow credentials for Swagger UI
        configuration.setAllowCredentials(false); // Set to false when using wildcard origins
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
