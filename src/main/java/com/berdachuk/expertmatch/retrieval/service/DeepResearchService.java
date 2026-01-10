package com.berdachuk.expertmatch.retrieval.service;

import com.berdachuk.expertmatch.employee.service.ExpertEnrichmentService;
import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;
import com.berdachuk.expertmatch.query.service.ModelInfoExtractor;
import com.berdachuk.expertmatch.query.service.TokenUsageExtractor;
import com.berdachuk.expertmatch.retrieval.domain.GapAnalysis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for performing deep research using SGR pattern.
 * Implements multi-step iterative retrieval: initial retrieval → gap analysis → query refinement → expanded retrieval → synthesis.
 */
@Slf4j
@Service
public class DeepResearchService {
    // Deep research configuration constants
    // Note: deepResearch is controlled via API parameter (QueryOptions.deepResearch)
    private static final int MAX_REFINED_QUERIES = 3;  // Maximum number of refined queries to generate
    private static final double SYNTHESIS_WEIGHT_INITIAL = 0.6;  // Weight for initial results in synthesis
    private static final double SYNTHESIS_WEIGHT_EXPANDED = 0.4;  // Weight for expanded results in synthesis
    private final HybridRetrievalService retrievalService;
    private final ExpertEnrichmentService enrichmentService;
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final PromptTemplate queryRefinementPromptTemplate;
    private final PromptTemplate gapAnalysisPromptTemplate;

    public DeepResearchService(
            HybridRetrievalService retrievalService,
            ExpertEnrichmentService enrichmentService,
            @Lazy ChatClient chatClient,
            ChatModel chatModel,
            ObjectMapper objectMapper,
            Environment environment,
            PromptTemplate queryRefinementPromptTemplate,
            @org.springframework.beans.factory.annotation.Qualifier("gapAnalysisPromptTemplate") PromptTemplate gapAnalysisPromptTemplate) {
        this.retrievalService = retrievalService;
        this.enrichmentService = enrichmentService;
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.queryRefinementPromptTemplate = queryRefinementPromptTemplate;
        this.gapAnalysisPromptTemplate = gapAnalysisPromptTemplate;
    }

    /**
     * Performs deep research: initial retrieval → gap analysis → expansion → synthesis.
     */
    public com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult performDeepResearch(
            QueryRequest request,
            com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery) {
        return performDeepResearch(request, parsedQuery, null);
    }

    /**
     * Performs deep research: initial retrieval → gap analysis → expansion → synthesis with optional execution tracing.
     */
    public com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult performDeepResearch(
            QueryRequest request,
            com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery,
            ExecutionTracer tracer) {

        log.info("🔬 Starting deep research for query: '{}'", request.query());

        // 1. Initial retrieval
        log.info("Deep Research Step 1/6: Performing initial retrieval...");
        if (tracer != null) {
            tracer.startStep("Initial Retrieval", "HybridRetrievalService", "retrieve");
        }
        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult initialResult = retrievalService.retrieve(request, parsedQuery, tracer);
        log.info("Initial retrieval completed: {} experts found", initialResult.expertIds().size());
        if (tracer != null) {
            tracer.endStep("Query: " + request.query(), "Experts: " + initialResult.expertIds().size());
        }

        if (initialResult.expertIds().isEmpty()) {
            log.warn("Initial retrieval returned no results, skipping deep research");
            return initialResult;
        }

        // 2. Enrich experts to build contexts for gap analysis
        log.info("Deep Research Step 2/6: Enriching experts for gap analysis...");
        if (tracer != null) {
            tracer.startStep("Enrich Experts for Gap Analysis", "ExpertEnrichmentService", "enrichExperts");
        }
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> initialExperts = enrichmentService.enrichExperts(initialResult, parsedQuery);
        List<AnswerGenerationService.ExpertContext> expertContexts = buildExpertContexts(initialExperts, initialResult);
        log.info("Expert enrichment completed: {} experts enriched", initialExperts.size());
        if (tracer != null) {
            tracer.endStep("Expert IDs: " + initialResult.expertIds().size(), "Enriched: " + initialExperts.size());
        }

        // 3. Gap analysis
        log.info("Deep Research Step 3/6: Analyzing gaps in initial results...");
        GapAnalysis gapAnalysis = analyzeGaps(request.query(), initialResult, expertContexts, tracer);
        log.info("Gap analysis completed: needsExpansion={}, gaps identified={}",
                gapAnalysis.needsExpansion(), gapAnalysis.identifiedGaps().size());

        if (!gapAnalysis.hasSignificantGaps()) {
            log.info("No significant gaps identified, returning initial results");
            return initialResult;
        }

        // 4. Query refinement
        log.info("Deep Research Step 4/6: Generating refined queries based on gaps...");
        List<String> refinedQueries = generateRefinedQueries(request.query(), gapAnalysis, tracer);
        log.info("Query refinement completed: {} refined queries generated", refinedQueries.size());

        if (refinedQueries.isEmpty()) {
            log.warn("No refined queries generated, returning initial results");
            return initialResult;
        }

        // 5. Expanded retrieval for each refined query
        log.info("Deep Research Step 5/6: Performing expanded retrieval for {} refined queries...", refinedQueries.size());
        List<com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult> expandedResults = new ArrayList<>();
        for (int i = 0; i < refinedQueries.size(); i++) {
            String refinedQuery = refinedQueries.get(i);
            log.info("  Expanded retrieval {}/{}: '{}'", i + 1, refinedQueries.size(), refinedQuery);
            if (tracer != null) {
                tracer.startStep("Expanded Retrieval " + (i + 1), "HybridRetrievalService", "retrieve");
            }
            try {
                QueryRequest expandedRequest = new QueryRequest(
                        refinedQuery,
                        request.chatId(),
                        new com.berdachuk.expertmatch.query.domain.QueryRequest.QueryOptions(
                                request.options().maxResults(),
                                request.options().minConfidence(),
                                request.options().includeSources(),
                                request.options().includeEntities(),
                                request.options().rerank(),
                                false, // Don't recurse deep research
                                request.options().useCascadePattern(),
                                request.options().useRoutingPattern(),
                                request.options().useCyclePattern(),
                                request.options().includeExecutionTrace()
                        )
                );
                com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery expandedParsedQuery = new com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery(refinedQuery, List.of(), List.of(), null, "expert_search", List.of());
                com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult expandedResult =
                        retrievalService.retrieve(expandedRequest, expandedParsedQuery, tracer);
                expandedResults.add(expandedResult);
                log.info("  Expanded retrieval {}/{} completed: {} experts found",
                        i + 1, refinedQueries.size(), expandedResult.expertIds().size());
                if (tracer != null) {
                    tracer.endStep("Refined query: " + refinedQuery, "Experts: " + expandedResult.expertIds().size());
                }
            } catch (Exception e) {
                log.error("Error during expanded retrieval for query: {}", refinedQuery, e);
                if (tracer != null) {
                    tracer.failStep("Expanded Retrieval " + (i + 1), "HybridRetrievalService", "retrieve", "Error: " + e.getMessage());
                }
                // Continue with other queries
            }
        }

        if (expandedResults.isEmpty()) {
            log.warn("No expanded results retrieved, returning initial results");
            return initialResult;
        }

        // 6. Synthesize results
        log.info("Deep Research Step 6/6: Synthesizing results (initial: {}, expanded: {})...",
                initialResult.expertIds().size(), expandedResults.size());
        if (tracer != null) {
            tracer.startStep("Synthesize Results", "DeepResearchService", "synthesizeResults");
        }
        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult synthesizedResult =
                synthesizeResults(initialResult, expandedResults, request.options().maxResults());
        log.info("🔬 Deep research completed: {} experts in final synthesized result", synthesizedResult.expertIds().size());
        if (tracer != null) {
            tracer.endStep("Initial: " + initialResult.expertIds().size() + ", Expanded: " + expandedResults.size(),
                    "Synthesized: " + synthesizedResult.expertIds().size());
        }

        return synthesizedResult;
    }

    /**
     * Analyzes gaps in initial retrieval results using LLM.
     */
    private GapAnalysis analyzeGaps(
            String query,
            com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult initialResult,
            List<AnswerGenerationService.ExpertContext> expertContexts,
            ExecutionTracer tracer) {

        try {
            if (tracer != null) {
                tracer.startStep("Gap Analysis", "DeepResearchService", "analyzeGaps");
            }

            String prompt = buildGapAnalysisPrompt(query, initialResult, expertContexts);

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during gap analysis");
                if (tracer != null) {
                    tracer.failStep("Gap Analysis", "DeepResearchService", "analyzeGaps", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during gap analysis");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText == null || responseText.isBlank()) {
                log.warn("Blank response text from LLM during gap analysis, returning default gap analysis");
                return new GapAnalysis(List.of(), List.of(), List.of(), false);
            }
            GapAnalysis gapAnalysis = parseGapAnalysis(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + query + ", Experts: " + expertContexts.size(),
                        "Needs expansion: " + gapAnalysis.needsExpansion() + ", Gaps: " + gapAnalysis.identifiedGaps().size(),
                        modelInfo, tokenUsage);
            }

            return gapAnalysis;
        } catch (Exception e) {
            log.error("Error during gap analysis", e);
            if (tracer != null) {
                tracer.failStep("Gap Analysis", "DeepResearchService", "analyzeGaps", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to analyze gaps for query: " + query, e);
        }
    }

    /**
     * Builds prompt for gap analysis using PromptTemplate.
     */
    private String buildGapAnalysisPrompt(
            String query,
            com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult initialResult,
            List<AnswerGenerationService.ExpertContext> expertContexts) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("expertCount", initialResult.expertIds().size());

        // Build expert summaries section
        if (!expertContexts.isEmpty()) {
            StringBuilder expertSummaries = new StringBuilder();
            for (int i = 0; i < Math.min(expertContexts.size(), 5); i++) {
                AnswerGenerationService.ExpertContext expert = expertContexts.get(i);
                expertSummaries.append("- ").append(expert.name());
                if (expert.skills() != null && !expert.skills().isEmpty()) {
                    expertSummaries.append(" (Skills: ").append(String.join(", ", expert.skills())).append(")");
                }
                if (expert.projects() != null && !expert.projects().isEmpty()) {
                    expertSummaries.append(" (Projects: ").append(String.join(", ", expert.projects())).append(")");
                }
                expertSummaries.append("\n");
            }
            variables.put("expertSummaries", expertSummaries.toString());
        } else {
            variables.put("expertSummaries", null);
        }

        return gapAnalysisPromptTemplate.render(variables);
    }

    /**
     * Parses gap analysis from LLM response.
     */
    private GapAnalysis parseGapAnalysis(String responseText) {
        try {
            // Try to extract JSON from response (may contain markdown code blocks)
            String jsonText = responseText.trim();
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

            Map<String, Object> parsed = objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {
            });

            List<String> gaps = extractList(parsed, "identifiedGaps");
            List<String> ambiguities = extractList(parsed, "ambiguities");
            List<String> missing = extractList(parsed, "missingInformation");
            boolean needsExpansion = parsed.get("needsExpansion") instanceof Boolean
                    ? (Boolean) parsed.get("needsExpansion")
                    : Boolean.parseBoolean(String.valueOf(parsed.getOrDefault("needsExpansion", false)));

            return new GapAnalysis(gaps, ambiguities, missing, needsExpansion);
        } catch (Exception e) {
            log.error("Failed to parse gap analysis JSON", e);
            throw new RuntimeException("Failed to parse gap analysis response: " + responseText, e);
        }
    }

    /**
     * Extracts list from parsed JSON map.
     */
    private List<String> extractList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Generates refined queries based on gap analysis.
     */
    private List<String> generateRefinedQueries(String originalQuery, GapAnalysis gapAnalysis, ExecutionTracer tracer) {
        try {
            if (tracer != null) {
                tracer.startStep("Query Refinement", "DeepResearchService", "generateRefinedQueries");
            }

            String prompt = buildQueryRefinementPrompt(originalQuery, gapAnalysis);

            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null ||
                    response.getResult().getOutput() == null ||
                    response.getResult().getOutput().getText() == null) {
                log.error("Empty response from LLM during query refinement");
                if (tracer != null) {
                    tracer.failStep("Query Refinement", "DeepResearchService", "generateRefinedQueries", "Empty response from LLM");
                }
                throw new RuntimeException("Empty response from LLM during query refinement");
            }

            String responseText = response.getResult().getOutput().getText();
            if (responseText == null || responseText.isBlank()) {
                log.warn("Blank response text from LLM during query refinement, returning empty list");
                return List.of();
            }
            List<String> refinedQueries = parseRefinedQueries(responseText);

            if (tracer != null) {
                String modelInfo = ModelInfoExtractor.extractModelInfo(chatModel, environment);
                com.berdachuk.expertmatch.query.domain.ExecutionTrace.TokenUsage tokenUsage = TokenUsageExtractor.extractTokenUsage(response);
                tracer.endStepWithLLM("Query: " + originalQuery + ", Gaps: " + gapAnalysis.identifiedGaps().size(),
                        "Refined queries: " + refinedQueries.size(),
                        modelInfo, tokenUsage);
            }

            return refinedQueries;
        } catch (Exception e) {
            log.error("Error during query refinement", e);
            if (tracer != null) {
                tracer.failStep("Query Refinement", "DeepResearchService", "generateRefinedQueries", "Error: " + e.getMessage());
            }
            throw new RuntimeException("Failed to generate refined queries for query: " + originalQuery, e);
        }
    }

    /**
     * Builds prompt for query refinement using PromptTemplate.
     */
    private String buildQueryRefinementPrompt(String originalQuery, GapAnalysis gapAnalysis) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("originalQuery", originalQuery);

        // Build gaps section
        boolean hasGaps = !gapAnalysis.identifiedGaps().isEmpty();
        variables.put("hasGaps", hasGaps);
        if (hasGaps) {
            StringBuilder gapsSection = new StringBuilder();
            gapAnalysis.identifiedGaps().forEach(gap -> gapsSection.append("- ").append(gap).append("\n"));
            variables.put("identifiedGaps", gapsSection.toString());
        }

        // Build ambiguities section
        boolean hasAmbiguities = !gapAnalysis.ambiguities().isEmpty();
        variables.put("hasAmbiguities", hasAmbiguities);
        if (hasAmbiguities) {
            StringBuilder ambiguitiesSection = new StringBuilder();
            gapAnalysis.ambiguities().forEach(amb -> ambiguitiesSection.append("- ").append(amb).append("\n"));
            variables.put("ambiguities", ambiguitiesSection.toString());
        }

        // Build missing information section
        boolean hasMissingInfo = !gapAnalysis.missingInformation().isEmpty();
        variables.put("hasMissingInfo", hasMissingInfo);
        if (hasMissingInfo) {
            StringBuilder missingInfoSection = new StringBuilder();
            gapAnalysis.missingInformation().forEach(miss -> missingInfoSection.append("- ").append(miss).append("\n"));
            variables.put("missingInformation", missingInfoSection.toString());
        }

        return queryRefinementPromptTemplate.render(variables);
    }

    /**
     * Parses refined queries from LLM response.
     */
    private List<String> parseRefinedQueries(String responseText) {
        try {
            // Try to extract JSON array from response
            String jsonText = responseText.trim();
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

            List<String> queries = objectMapper.readValue(jsonText, new TypeReference<List<String>>() {
            });
            return queries.stream()
                    .filter(q -> q != null && !q.trim().isEmpty())
                    .limit(MAX_REFINED_QUERIES)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse refined queries JSON", e);
            throw new RuntimeException("Failed to parse refined queries response: " + responseText, e);
        }
    }

    /**
     * Synthesizes initial and expanded retrieval results with weighted scoring.
     */
    private com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult synthesizeResults(
            com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult initial,
            List<com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult> expandedResults,
            int maxResults) {

        // Combine all expert IDs with weighted scores
        Map<String, Double> combinedScores = new HashMap<>();

        // Add initial results with initial weight
        for (String expertId : initial.expertIds()) {
            double score = initial.relevanceScores().getOrDefault(expertId, 0.0);
            combinedScores.put(expertId, score * SYNTHESIS_WEIGHT_INITIAL);
        }

        // Add expanded results with expanded weight
        for (com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult expanded : expandedResults) {
            for (String expertId : expanded.expertIds()) {
                double expandedScore = expanded.relevanceScores().getOrDefault(expertId, 0.0);
                double weightedScore = expandedScore * SYNTHESIS_WEIGHT_EXPANDED;

                // For duplicates: use weighted average
                if (combinedScores.containsKey(expertId)) {
                    double existingScore = combinedScores.get(expertId);
                    combinedScores.put(expertId, existingScore + weightedScore);
                } else {
                    combinedScores.put(expertId, weightedScore);
                }
            }
        }

        // Sort by combined score and limit to maxResults
        List<String> synthesizedIds = combinedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Build final relevance scores map
        Map<String, Double> finalScores = new HashMap<>();
        for (String expertId : synthesizedIds) {
            finalScores.put(expertId, combinedScores.get(expertId));
        }

        return new com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult(synthesizedIds, finalScores);
    }

    /**
     * Builds expert contexts from enriched experts (similar to QueryService.buildExpertContexts).
     */
    private List<AnswerGenerationService.ExpertContext> buildExpertContexts(
            List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> experts,
            com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult) {

        List<AnswerGenerationService.ExpertContext> contexts = new ArrayList<>();

        for (com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert : experts) {
            // Extract skills from matched skills
            List<String> skills = new ArrayList<>();
            if (expert.matchedSkills() != null) {
                if (expert.matchedSkills().mustHave() != null) {
                    skills.addAll(expert.matchedSkills().mustHave());
                }
                if (expert.matchedSkills().niceToHave() != null) {
                    skills.addAll(expert.matchedSkills().niceToHave());
                }
            }

            // Extract project names from relevant projects
            List<String> projects = expert.relevantProjects() != null
                    ? expert.relevantProjects().stream()
                    .map(com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject::name)
                    .filter(name -> name != null && !name.isEmpty())
                    .toList()
                    : List.of();

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            if (expert.relevanceScore() != null) {
                metadata.put("relevanceScore", expert.relevanceScore());
            }
            if (expert.skillMatch() != null && expert.skillMatch().matchScore() != null) {
                metadata.put("matchScore", expert.skillMatch().matchScore());
            }
            if (expert.seniority() != null) {
                metadata.put("seniority", expert.seniority());
            }

            AnswerGenerationService.ExpertContext context = new AnswerGenerationService.ExpertContext(
                    expert.id(),
                    expert.name(),
                    expert.email(),
                    expert.seniority(),
                    skills,
                    projects,
                    metadata
            );

            contexts.add(context);
        }

        return contexts;
    }
}

