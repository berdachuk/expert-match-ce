package com.berdachuk.expertmatch.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * Registers JsonNullableModule to handle JsonNullable types from OpenAPI-generated models.
 */
@Slf4j
@Configuration
public class JacksonConfig {

    /**
     * Configures ObjectMapper with JsonNullableModule support.
     * This allows proper serialization/deserialization of JsonNullable fields
     * used in OpenAPI-generated API models.
     * <p>
     * The JsonNullableModule handles:
     * - Serialization of JsonNullable fields (null, undefined, or value)
     * - Deserialization of JSON values into JsonNullable types
     * - Proper handling of String values deserialized into JsonNullable<String>
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Register JsonNullableModule for OpenAPI-generated models
        // This MUST be registered to handle JsonNullable deserialization
        objectMapper.registerModule(new JsonNullableModule());

        // Configure deserialization features to be more lenient
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

        // Configure serialization features
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        log.info("Configured ObjectMapper with JsonNullableModule support");

        return objectMapper;
    }

}
