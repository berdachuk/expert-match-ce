package com.berdachuk.expertmatch.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for ExpertMatch API.
 * Provides complete OpenAPI 3.0 specification for automatic client generation.
 * <p>
 * Note: Authentication and authorization are handled by Spring Gateway.
 * Spring Gateway validates JWT tokens and populates user information in HTTP headers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expertMatchOpenAPI() {
        Components components = new Components();

        // Add header parameters for user information populated by Spring Gateway
        io.swagger.v3.oas.models.media.Schema<String> userIdSchema = new io.swagger.v3.oas.models.media.Schema<>();
        userIdSchema.setType("string");
        userIdSchema.setDefault("anonymous-user");

        components.addParameters("X-User-Id", new Parameter()
                .name("X-User-Id")
                .in("header")
                .required(false)
                .description("User identifier populated by Spring Gateway after authentication. If not provided, anonymous-user will be used.")
                .schema(userIdSchema)
                .example("anonymous-user"));

        components.addParameters("X-User-Roles", new Parameter()
                .name("X-User-Roles")
                .in("header")
                .required(false)
                .description("Comma-separated list of user roles populated by Spring Gateway (e.g., 'ROLE_USER,ROLE_ADMIN')")
                .schema(new io.swagger.v3.oas.models.media.Schema<>().type("string")));

        components.addParameters("X-User-Email", new Parameter()
                .name("X-User-Email")
                .in("header")
                .required(false)
                .description("User email address populated by Spring Gateway")
                .schema(new io.swagger.v3.oas.models.media.Schema<>().type("string")));

        return new OpenAPI()
                .info(new Info()
                        .title("ExpertMatch API")
                        .version("1.0.0")
                        .description("GraphRAG-powered expert discovery and matching service for RFPs and team formation. " +
                                "This API enables automatic client generation for frontend applications using OpenAPI generators. " +
                                "Authentication and authorization are handled by Spring Gateway, which populates user information " +
                                "in HTTP headers (X-User-Id, X-User-Roles, X-User-Email).")
                        .contact(new Contact()
                                .name("API Support")
                                .email("api-support@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com/license")))
                .servers(List.of(
                        // Use relative URL to avoid CORS issues - Swagger UI will use current origin
                        new Server()
                                .url("")
                                .description("Current Server (auto-detected)"),
                        new Server()
                                .url("http://localhost:8093")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://192.168.0.73:8093")
                                .description("Remote Development Server"),
                        new Server()
                                .url("https://api.expertmatch.com")
                                .description("Production Server")))
                .components(components);
    }
}

