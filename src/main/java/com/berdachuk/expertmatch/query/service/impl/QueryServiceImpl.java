package com.berdachuk.expertmatch.query.service.impl;

import com.berdachuk.expertmatch.chat.repository.ChatRepository;
import com.berdachuk.expertmatch.chat.repository.ConversationHistoryRepository;
import com.berdachuk.expertmatch.chat.service.ConversationHistoryManager;
import com.berdachuk.expertmatch.core.util.IdGenerator;
import com.berdachuk.expertmatch.employee.service.ExpertEnrichmentService;
import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import com.berdachuk.expertmatch.query.domain.EntityExtractor;
import com.berdachuk.expertmatch.query.domain.QueryParser;
import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.query.service.ExecutionTracer;
import com.berdachuk.expertmatch.query.service.ExpertContextHolder;
import com.berdachuk.expertmatch.query.service.QueryService;
import com.berdachuk.expertmatch.retrieval.service.DeepResearchService;
import com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for processing expert discovery queries.
 */
@Slf4j
@Service
@ConditionalOnBean(AnswerGenerationService.class)
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    @Lazy
    private final QueryParser queryParser;
    private final EntityExtractor entityExtractor;
    private final HybridRetrievalService retrievalService;
    private final DeepResearchService deepResearchService;
    private final AnswerGenerationService answerGenerationService;
    private final ExpertEnrichmentService enrichmentService;
    private final ConversationHistoryRepository historyRepository;
    private final ConversationHistoryManager historyManager;
    private final ChatRepository chatRepository;

    /**
     * Processes a query and returns expert recommendations.
     */
    @Transactional
    @Override
    public QueryResponse processQuery(QueryRequest request, String chatId, String userId) {
        long startTime = System.currentTimeMillis();
        String queryId = IdGenerator.generateId();
        log.info("Processing query [{}]: '{}' (chatId: {}, userId: {})", queryId, request.query(), chatId, userId);

        // Create execution tracer if enabled
        ExecutionTracer tracer = (request.options().includeExecutionTrace() != null && request.options().includeExecutionTrace())
                ? new ExecutionTracer()
                : null;

        // Set ThreadLocal for tool call tracking
        if (tracer != null) {
            ExecutionTracer.setCurrent(tracer);
        }

        try {

            // 1. Save user message to conversation history
            if (tracer != null) {
                tracer.startStep("Save User Message", "ConversationHistoryRepository", "saveMessage");
            }
            int userSequenceNumber = historyRepository.getNextSequenceNumber(chatId);
            historyRepository.saveMessage(
                    chatId,
                    "user",
                    "user",
                    request.query(),
                    userSequenceNumber,
                    null // User messages don't consume tokens
            );
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId + ", Role: user", "Sequence: " + userSequenceNumber);
            }

            // 2. Retrieve conversation history for context (with token counting and summarization)
            log.info("[QUERY] Query [{}] - Loading conversation history for chatId: {}", queryId, chatId);
            List<ConversationHistoryRepository.ConversationMessage> conversationHistory =
                    historyManager.getOptimizedHistory(chatId, true, tracer);

            if (!conversationHistory.isEmpty()) {
                log.info("[QUERY] Query [{}] - Conversation history loaded: {} messages - will be included in prompt",
                        queryId, conversationHistory.size());
            } else {
                log.info("[QUERY] Query [{}] - No conversation history found - query will be processed without context",
                        queryId);
            }

            // 3. Parse query and extract requirements (with routing pattern if enabled)
            log.info("Parsing query and extracting requirements...");
            boolean useRoutingPattern = request.options().useRoutingPattern() != null && request.options().useRoutingPattern();
            if (tracer != null) {
                tracer.startStep("Parse Query", "QueryParser", "parse");
            }
            com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = queryParser.parse(request.query(), useRoutingPattern, tracer);
            log.info("Query parsed - Intent: {}, Skills: {}, Technologies: {}",
                    parsedQuery.intent(), parsedQuery.skills().size(), parsedQuery.technologies().size());
            if (tracer != null) {
                // QueryParser tracks its own steps, so we just need to track the overall parse step
                tracer.endStep("Query: " + request.query(),
                        "Intent: " + parsedQuery.intent() + ", Skills: " + parsedQuery.skills().size() +
                                ", Technologies: " + parsedQuery.technologies().size());
            }

            // 4. Extract entities
            log.info("Extracting entities from query...");
            if (tracer != null) {
                tracer.startStep("Extract Entities", "EntityExtractor", "extract");
            }
            EntityExtractor.ExtractedEntities entities = entityExtractor.extract(request.query(), tracer);
            log.info("Entities extracted - Persons: {}, Organizations: {}, Technologies: {}, Projects: {}, Domains: {}",
                    entities.persons().size(), entities.organizations().size(), entities.technologies().size(),
                    entities.projects().size(), entities.domains().size());
            if (tracer != null) {
                // EntityExtractor tracks its own steps, so we just need to track the overall extract step
                int totalEntities = entities.persons().size() + entities.organizations().size() +
                        entities.technologies().size() + entities.projects().size() + entities.domains().size();
                tracer.endStep("Query: " + request.query(), "Total entities: " + totalEntities);
            }

            // 5. Perform hybrid GraphRAG retrieval (with deep research if enabled)
            com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult;
            if (request.options().deepResearch() != null && request.options().deepResearch()) {
                log.info("Starting deep research retrieval...");
                if (tracer != null) {
                    tracer.startStep("Deep Research", "DeepResearchService", "performDeepResearch");
                }
                retrievalResult = deepResearchService.performDeepResearch(request, parsedQuery, tracer);
                log.info("Deep research completed: {} experts found", retrievalResult.expertIds().size());
                if (tracer != null) {
                    tracer.endStep("Query: " + request.query(), "Experts found: " + retrievalResult.expertIds().size());
                }
            } else {
                log.info("Starting hybrid retrieval...");
                if (tracer != null) {
                    tracer.startStep("Hybrid Retrieval", "HybridRetrievalService", "retrieve");
                }
                retrievalResult = retrievalService.retrieve(request, parsedQuery, tracer);
                log.info("Hybrid retrieval completed: {} experts found", retrievalResult.expertIds().size());
                if (tracer != null) {
                    tracer.endStep("Query: " + request.query(), "Experts found: " + retrievalResult.expertIds().size());
                }
            }

            // 6. Enrich expert recommendations with detailed data
            log.info("Enriching {} experts with detailed data...", retrievalResult.expertIds().size());
            if (tracer != null) {
                tracer.startStep("Enrich Experts", "ExpertEnrichmentService", "enrichExperts");
            }
            List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> experts = enrichmentService.enrichExperts(
                    retrievalResult, parsedQuery);
            log.info("Expert enrichment completed: {} experts enriched", experts.size());
            if (tracer != null) {
                tracer.endStep("Expert IDs: " + retrievalResult.expertIds().size(),
                        "Enriched: " + experts.size() + " experts with full details");
            }

            // 7. Build expert contexts for LLM
            log.info("Building expert contexts for LLM...");
            if (tracer != null) {
                tracer.startStep("Build Expert Contexts", "QueryService", "buildExpertContexts");
            }
            List<AnswerGenerationService.ExpertContext> expertContexts = buildExpertContexts(experts, retrievalResult);
            log.info("Expert contexts built: {} contexts", expertContexts.size());
            if (tracer != null) {
                tracer.endStep("Experts: " + experts.size(), "Contexts: " + expertContexts.size());
            }

            // Store expert contexts in ThreadLocal for tool access
            log.info("🔧 Storing {} expert contexts in ExpertContextHolder for tool access", expertContexts.size());
            ExpertContextHolder.set(expertContexts);
            String answer;
            try {
                // 8. Generate answer using LLM with enriched expert data and conversation context
                // Support SGR patterns (Cascade or Cycle) if enabled
                // Note: We pass null for expertContexts to force LLM to use getRetrievedExperts() tool
                if (request.options().useCascadePattern() != null && request.options().useCascadePattern()) {
                    log.info("Generating answer using LLM with Cascade pattern...");
                } else if (request.options().useCyclePattern() != null && request.options().useCyclePattern()) {
                    log.info("Generating answer using LLM with Cycle pattern...");
                } else {
                    log.info("Generating answer using LLM with tool calling...");
                }
                if (tracer != null) {
                    tracer.startStep("Generate Answer", "AnswerGenerationService", "generateAnswer");
                }
                answer = generateAnswer(
                        request.query(),
                        null, // Don't pass expertContexts - LLM will use getRetrievedExperts() tool
                        parsedQuery.intent(),
                        conversationHistory,
                        request.options().useCascadePattern() != null && request.options().useCascadePattern(),
                        request.options().useCyclePattern() != null && request.options().useCyclePattern(),
                        tracer
                );
                log.info("Answer generation completed. Answer length: {}", answer != null ? answer.length() : 0);
                if (tracer != null) {
                    // AnswerGenerationService tracks its own step, so we just track the overall step
                    tracer.endStep("Query: " + request.query() + ", Experts: " + expertContexts.size(),
                            "Answer: " + (answer != null ? answer.length() : 0) + " characters");
                }
            } finally {
                // Clean up ThreadLocal after answer generation
                log.info("🔧 Clearing ExpertContextHolder after answer generation");
                ExpertContextHolder.clear();
            }

            // 9. Save assistant response to conversation history
            if (tracer != null) {
                tracer.startStep("Save Assistant Response", "ConversationHistoryRepository", "saveMessage");
            }
            String assistantMessageId = IdGenerator.generateId();
            int assistantSequenceNumber = historyRepository.getNextSequenceNumber(chatId);
            // Estimate tokens used (rough approximation: ~4 characters per token)
            Integer tokensUsed = answer != null ? (int) Math.ceil(answer.length() / 4.0) : null;
            historyRepository.saveMessage(
                    chatId,
                    "assistant",
                    "assistant",
                    answer,
                    assistantSequenceNumber,
                    tokensUsed
            );
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId + ", Answer: " + (answer != null ? answer.length() : 0) + " chars",
                        "Sequence: " + assistantSequenceNumber);
            }

            // 10. Update chat metadata (message count and last activity)
            if (tracer != null) {
                tracer.startStep("Update Chat Metadata", "ChatRepository", "updateLastActivity");
            }
            chatRepository.updateLastActivity(chatId);
            if (tracer != null) {
                tracer.endStep("ChatId: " + chatId, "Last activity updated");
            }

            // 11. Build sources and entities
            if (tracer != null) {
                tracer.startStep("Build Sources and Entities", "QueryService", "buildSources/buildResponseEntities");
            }
            List<com.berdachuk.expertmatch.query.domain.QueryResponse.Source> sources = buildSources(experts);
            List<com.berdachuk.expertmatch.query.domain.QueryResponse.Entity> responseEntities = buildResponseEntities(entities);
            if (tracer != null) {
                tracer.endStep("Experts: " + experts.size() + ", Entities: " + entities.persons().size() +
                                entities.organizations().size() + entities.technologies().size() + entities.projects().size() +
                                entities.domains().size(),
                        "Sources: " + sources.size() + ", Response entities: " + responseEntities.size());
            }

            // 12. Calculate confidence and summary
            if (tracer != null) {
                tracer.startStep("Calculate Confidence and Summary", "QueryService", "calculateConfidence/calculateSummary");
            }
            double confidence = calculateConfidence(retrievalResult);
            com.berdachuk.expertmatch.query.domain.QueryResponse.MatchSummary summary = calculateSummary(experts);
            if (tracer != null) {
                tracer.endStep("Experts: " + experts.size(), "Confidence: " + confidence +
                        ", Summary: " + summary.totalExpertsFound() + " total");
            }

            long processingTime = System.currentTimeMillis() - startTime;

            // Build execution trace if enabled
            com.berdachuk.expertmatch.query.domain.ExecutionTrace.ExecutionTraceData executionTrace = tracer != null ? tracer.buildTrace() : null;

            return new QueryResponse(
                    answer,
                    experts,
                    sources,
                    responseEntities,
                    confidence,
                    queryId,
                    chatId,
                    assistantMessageId,
                    processingTime,
                    summary,
                    executionTrace
            );
        } finally {
            // Clear ThreadLocal after request processing
            if (tracer != null) {
                ExecutionTracer.clear();
            }
        }
    }

    /**
     * Builds expert contexts for LLM prompt from enriched experts.
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

    /**
     * Generates answer text using LLM with conversation context.
     * Supports SGR patterns (Cascade or Cycle) if enabled.
     */
    private String generateAnswer(
            String query,
            List<AnswerGenerationService.ExpertContext> expertContexts,
            String intent,
            List<ConversationHistoryRepository.ConversationMessage> conversationHistory,
            boolean useCascadePattern,
            boolean useCyclePattern,
            ExecutionTracer tracer) {
        return answerGenerationService.generateAnswer(
                query,
                expertContexts,
                intent,
                conversationHistory,
                useCascadePattern,
                useCyclePattern,
                tracer
        );
    }

    /**
     * Builds source citations from expert recommendations.
     */
    private List<com.berdachuk.expertmatch.query.domain.QueryResponse.Source> buildSources(List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> experts) {
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.Source> sources = new ArrayList<>();

        for (com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert : experts) {
            // Add expert as a source
            Map<String, Object> expertMetadata = new HashMap<>();
            expertMetadata.put("expertId", expert.id());
            expertMetadata.put("seniority", expert.seniority());
            if (expert.skillMatch() != null && expert.skillMatch().matchScore() != null) {
                expertMetadata.put("matchScore", expert.skillMatch().matchScore());
            }

            sources.add(new com.berdachuk.expertmatch.query.domain.QueryResponse.Source(
                    "expert",
                    expert.id(),
                    expert.name(),
                    expert.relevanceScore(),
                    expertMetadata
            ));

            // Add relevant projects as sources
            if (expert.relevantProjects() != null) {
                for (com.berdachuk.expertmatch.query.domain.QueryResponse.RelevantProject project : expert.relevantProjects()) {
                    Map<String, Object> projectMetadata = new HashMap<>();
                    projectMetadata.put("expertId", expert.id());
                    projectMetadata.put("expertName", expert.name());
                    if (project.technologies() != null) {
                        projectMetadata.put("technologies", project.technologies());
                    }
                    if (project.role() != null) {
                        projectMetadata.put("role", project.role());
                    }

                    sources.add(new com.berdachuk.expertmatch.query.domain.QueryResponse.Source(
                            "project",
                            project.name() + "_" + expert.id(), // Unique ID
                            project.name(),
                            expert.relevanceScore() != null ? expert.relevanceScore() * 0.9 : 0.0, // Slightly lower than expert
                            projectMetadata
                    ));
                }
            }
        }

        return sources;
    }

    /**
     * Builds response entities.
     */
    private List<com.berdachuk.expertmatch.query.domain.QueryResponse.Entity> buildResponseEntities(
            EntityExtractor.ExtractedEntities entities) {
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.Entity> responseEntities = new ArrayList<>();

        // Convert extracted entities to response format
        entities.persons().forEach(e ->
                responseEntities.add(new com.berdachuk.expertmatch.query.domain.QueryResponse.Entity("person", e.name(), e.id())));
        entities.technologies().forEach(e ->
                responseEntities.add(new com.berdachuk.expertmatch.query.domain.QueryResponse.Entity("technology", e.name(), e.id())));
        entities.domains().forEach(e ->
                responseEntities.add(new com.berdachuk.expertmatch.query.domain.QueryResponse.Entity("domain", e.name(), e.id())));

        return responseEntities;
    }

    /**
     * Calculates overall confidence score.
     */
    private double calculateConfidence(com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult) {
        if (retrievalResult.expertIds().isEmpty()) {
            return 0.0;
        }

        // Average relevance scores
        return retrievalResult.relevanceScores().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates match summary statistics.
     */
    private com.berdachuk.expertmatch.query.domain.QueryResponse.MatchSummary calculateSummary(List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> experts) {
        int total = experts.size();
        long perfectMatches = experts.stream()
                .filter(e -> e.skillMatch() != null &&
                        e.skillMatch().matchScore() != null &&
                        e.skillMatch().matchScore() >= 0.9)
                .count();
        long goodMatches = experts.stream()
                .filter(e -> e.skillMatch() != null &&
                        e.skillMatch().matchScore() != null &&
                        e.skillMatch().matchScore() >= 0.7 &&
                        e.skillMatch().matchScore() < 0.9)
                .count();
        long partialMatches = total - perfectMatches - goodMatches;

        return new com.berdachuk.expertmatch.query.domain.QueryResponse.MatchSummary(
                total,
                (int) perfectMatches,
                (int) goodMatches,
                (int) partialMatches
        );
    }

}

