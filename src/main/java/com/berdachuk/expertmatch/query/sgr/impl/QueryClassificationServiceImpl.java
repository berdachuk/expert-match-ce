package com.berdachuk.expertmatch.query.sgr;

import com.berdachuk.expertmatch.llm.sgr.SGRPatternConfig;
import com.berdachuk.expertmatch.llm.sgr.StructuredOutputHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for query classification using Routing pattern.
 * Forces LLM to explicitly choose one reasoning path.
 */
@Slf4j
@Service
public class QueryClassificationServiceImpl implements QueryClassificationService {
    private final StructuredOutputHelper structuredOutputHelper;
    private final SGRPatternConfig config;
    private final PromptTemplate queryClassificationPromptTemplate;

    public QueryClassificationServiceImpl(
            StructuredOutputHelper structuredOutputHelper,
            SGRPatternConfig config,
            PromptTemplate queryClassificationPromptTemplate) {
        this.structuredOutputHelper = structuredOutputHelper;
        this.config = config;
        this.queryClassificationPromptTemplate = queryClassificationPromptTemplate;
    }

    /**
     * Classifies a query using Routing pattern.
     * Forces LLM to explicitly choose one intent from available options.
     *
     * @param query User query to classify
     * @return Query classification with intent and confidence
     */
    @Override
    public QueryClassification classifyWithRouting(String query) {
        if (!config.isEnabled() || !config.getRouting().isEnabled()) {
            log.warn("Routing pattern is disabled, cannot classify query");
            throw new IllegalStateException("Routing pattern is disabled");
        }

        try {
            String prompt = buildRoutingPrompt(query);
            return structuredOutputHelper.callWithStructuredOutput(prompt, QueryClassification.class);
        } catch (StructuredOutputHelper.StructuredOutputException e) {
            log.error("Failed to classify query with Routing pattern", e);
            throw new RuntimeException("Failed to classify query with Routing pattern", e);
        }
    }

    /**
     * Builds prompt for Routing pattern classification using PromptTemplate.
     */
    private String buildRoutingPrompt(String query) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        return queryClassificationPromptTemplate.render(variables);
    }
}

