package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Helper class for Spring AI structured output operations.
 * Provides utilities for calling LLM with structured output constraints.
 */
@Slf4j
@Component
public class StructuredOutputHelper {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public StructuredOutputHelper(@Lazy ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls LLM with a prompt and attempts to parse the response as the specified type.
     * Falls back to manual JSON parsing if structured output API is not available.
     *
     * @param prompt        The prompt to send to the LLM
     * @param responseClass The expected response type
     * @param <T>           The type of the response
     * @return Parsed response object
     * @throws StructuredOutputException if parsing fails
     */
    public <T> T callWithStructuredOutput(String prompt, Class<T> responseClass) {
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                throw new StructuredOutputException("Empty response from LLM");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText == null || responseText.isBlank()) {
                throw new StructuredOutputException("Blank response text from LLM");
            }
            return parseStructuredResponse(responseText, responseClass);
        } catch (Exception e) {
            log.error("Error calling LLM with structured output", e);
            throw new StructuredOutputException("Failed to get structured output from LLM", e);
        }
    }

    /**
     * Parses a structured response from LLM text output.
     * Handles JSON wrapped in markdown code blocks.
     *
     * @param responseText  The raw response text from LLM
     * @param responseClass The expected response type
     * @param <T>           The type of the response
     * @return Parsed response object
     */
    private <T> T parseStructuredResponse(String responseText, Class<T> responseClass) {
        try {
            // Try to extract JSON from response (may contain markdown code blocks)
            String jsonText = responseText.trim();

            // Remove markdown code blocks if present
            if (jsonText.contains("```json")) {
                int startIdx = jsonText.indexOf("```json") + 7;
                int endIdx = jsonText.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonText = jsonText.substring(startIdx, endIdx).trim();
                }
            } else if (jsonText.contains("```")) {
                int startIdx = jsonText.indexOf("```") + 3;
                int endIdx = jsonText.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonText = jsonText.substring(startIdx, endIdx).trim();
                }
            }

            // Parse JSON
            return objectMapper.readValue(jsonText, responseClass);
        } catch (Exception e) {
            log.error("Failed to parse structured response: {}", responseText, e);
            throw new StructuredOutputException("Failed to parse structured response", e);
        }
    }

    /**
     * Exception thrown when structured output operations fail.
     */
    public static class StructuredOutputException extends RuntimeException {
        public StructuredOutputException(String message) {
            super(message);
        }

        public StructuredOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

