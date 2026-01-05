package com.berdachuk.expertmatch.api;

import com.berdachuk.expertmatch.api.model.*;
import com.berdachuk.expertmatch.chat.ConversationHistoryRepository;
import com.berdachuk.expertmatch.data.ChatRepository;
import com.berdachuk.expertmatch.query.QueryRequest;
import com.berdachuk.expertmatch.query.QueryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.HashMap;
import java.util.List;

/**
 * MapStruct mapper for converting between generated API models and domain models.
 * This replaces the manual ApiModelMapper with type-safe, compile-time generated mappers.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ApiMapper {

    // Query-related mappings

    @Mapping(target = "options", source = "options", qualifiedByName = "mapQueryOptions")
    QueryRequest toDomainQueryRequest(com.berdachuk.expertmatch.api.model.QueryRequest apiRequest);

    @Named("mapQueryOptions")
    default QueryRequest.QueryOptions mapQueryOptions(QueryOptions apiOptions) {
        if (apiOptions == null) {
            return null;
        }
        return new QueryRequest.QueryOptions(
                apiOptions.getMaxResults(),
                apiOptions.getMinConfidence(),
                apiOptions.getIncludeSources(),
                apiOptions.getIncludeEntities(),
                apiOptions.getRerank(),
                apiOptions.getDeepResearch(),
                apiOptions.getUseCascadePattern(),  // useCascadePattern - now available in API model
                apiOptions.getUseRoutingPattern(),  // useRoutingPattern - now available in API model
                apiOptions.getUseCyclePattern(),    // useCyclePattern - now available in API model
                apiOptions.getIncludeExecutionTrace()
        );
    }

    @Mapping(target = "chatId", source = "chatId")
    @Mapping(target = "messageId", source = "messageId")
    @Mapping(target = "processingTimeMs", source = "processingTimeMs")
    @Mapping(target = "experts", source = "experts", qualifiedByName = "mapExpertMatches")
    @Mapping(target = "sources", source = "sources", qualifiedByName = "mapSources")
    @Mapping(target = "entities", source = "entities", qualifiedByName = "mapEntities")
    @Mapping(target = "summary", source = "summary", qualifiedByName = "mapMatchSummary")
    @Mapping(target = "executionTrace", source = "executionTrace", qualifiedByName = "mapExecutionTrace")
    com.berdachuk.expertmatch.api.model.QueryResponse toApiQueryResponse(QueryResponse domainResponse);

    @Named("mapExpertMatches")
    default List<ExpertMatch> mapExpertMatches(List<QueryResponse.ExpertMatch> domainExperts) {
        if (domainExperts == null) {
            return List.of();
        }
        return domainExperts.stream()
                .map(this::toApiExpertMatch)
                .toList();
    }

    @Mapping(target = "language", source = "language", qualifiedByName = "mapLanguageProficiency")
    @Mapping(target = "skillMatch", source = "skillMatch", qualifiedByName = "mapSkillMatch")
    @Mapping(target = "matchedSkills", source = "matchedSkills", qualifiedByName = "mapMatchedSkills")
    @Mapping(target = "relevantProjects", source = "relevantProjects", qualifiedByName = "mapRelevantProjects")
    @Mapping(target = "experience", source = "experience", qualifiedByName = "mapExperience")
    ExpertMatch toApiExpertMatch(QueryResponse.ExpertMatch domainExpert);

    @Named("mapLanguageProficiency")
    default LanguageProficiency mapLanguageProficiency(QueryResponse.LanguageProficiency domainLang) {
        if (domainLang == null) {
            return null;
        }
        LanguageProficiency lang = new LanguageProficiency();
        lang.setEnglish(domainLang.english());
        return lang;
    }

    @Named("mapSkillMatch")
    default SkillMatch mapSkillMatch(QueryResponse.SkillMatch domainSkillMatch) {
        if (domainSkillMatch == null) {
            return null;
        }
        SkillMatch skillMatch = new SkillMatch();
        skillMatch.setMustHaveMatched(domainSkillMatch.mustHaveMatched());
        skillMatch.setMustHaveTotal(domainSkillMatch.mustHaveTotal());
        skillMatch.setNiceToHaveMatched(domainSkillMatch.niceToHaveMatched());
        skillMatch.setNiceToHaveTotal(domainSkillMatch.niceToHaveTotal());
        skillMatch.setMatchScore(domainSkillMatch.matchScore());
        return skillMatch;
    }

    @Named("mapMatchedSkills")
    default MatchedSkills mapMatchedSkills(QueryResponse.MatchedSkills domainMatchedSkills) {
        if (domainMatchedSkills == null) {
            return null;
        }
        MatchedSkills matchedSkills = new MatchedSkills();
        matchedSkills.setMustHave(domainMatchedSkills.mustHave());
        matchedSkills.setNiceToHave(domainMatchedSkills.niceToHave());
        return matchedSkills;
    }

    @Named("mapRelevantProjects")
    default List<RelevantProject> mapRelevantProjects(List<QueryResponse.RelevantProject> domainProjects) {
        if (domainProjects == null) {
            return List.of();
        }
        return domainProjects.stream()
                .map(domainProject -> {
                    RelevantProject apiProject = new RelevantProject();
                    apiProject.setName(domainProject.name());
                    apiProject.setTechnologies(domainProject.technologies());
                    apiProject.setRole(domainProject.role());
                    apiProject.setDuration(domainProject.duration());
                    return apiProject;
                })
                .toList();
    }

    @Named("mapExperience")
    default Experience mapExperience(QueryResponse.Experience domainExperience) {
        if (domainExperience == null) {
            return null;
        }
        Experience experience = new Experience();
        experience.setEtlPipelines(domainExperience.etlPipelines());
        experience.setHighPerformanceServices(domainExperience.highPerformanceServices());
        experience.setSystemArchitecture(domainExperience.systemArchitecture());
        experience.setMonitoring(domainExperience.monitoring());
        experience.setOnCall(domainExperience.onCall());
        return experience;
    }

    @Named("mapSources")
    default List<Source> mapSources(List<QueryResponse.Source> domainSources) {
        if (domainSources == null) {
            return List.of();
        }
        return domainSources.stream()
                .map(domainSource -> {
                    Source apiSource = new Source();
                    try {
                        apiSource.setType(Source.TypeEnum.fromValue(domainSource.type()));
                    } catch (IllegalArgumentException e) {
                        apiSource.setType(Source.TypeEnum.EXPERT);
                    }
                    apiSource.setId(domainSource.id());
                    apiSource.setTitle(domainSource.title());
                    apiSource.setRelevanceScore(domainSource.relevanceScore());
                    if (domainSource.metadata() != null) {
                        apiSource.setMetadata(new HashMap<>(domainSource.metadata()));
                    }
                    return apiSource;
                })
                .toList();
    }

    @Named("mapEntities")
    default List<Entity> mapEntities(List<QueryResponse.Entity> domainEntities) {
        if (domainEntities == null) {
            return List.of();
        }
        return domainEntities.stream()
                .map(domainEntity -> {
                    Entity apiEntity = new Entity();
                    try {
                        apiEntity.setType(Entity.TypeEnum.fromValue(domainEntity.type()));
                    } catch (IllegalArgumentException e) {
                        apiEntity.setType(Entity.TypeEnum.PERSON);
                    }
                    apiEntity.setName(domainEntity.name());
                    apiEntity.setId(domainEntity.id());
                    return apiEntity;
                })
                .toList();
    }

    @Named("mapMatchSummary")
    default MatchSummary mapMatchSummary(QueryResponse.MatchSummary domainSummary) {
        if (domainSummary == null) {
            return null;
        }
        MatchSummary apiSummary = new MatchSummary();
        apiSummary.setTotalExpertsFound(domainSummary.totalExpertsFound());
        apiSummary.setPerfectMatches(domainSummary.perfectMatches());
        apiSummary.setGoodMatches(domainSummary.goodMatches());
        apiSummary.setPartialMatches(domainSummary.partialMatches());
        return apiSummary;
    }

    @Named("mapExecutionTrace")
    default com.berdachuk.expertmatch.api.model.ExecutionTrace mapExecutionTrace(
            com.berdachuk.expertmatch.query.ExecutionTrace.ExecutionTraceData domainTrace) {
        if (domainTrace == null) {
            return null;
        }
        com.berdachuk.expertmatch.api.model.ExecutionTrace apiTrace = new com.berdachuk.expertmatch.api.model.ExecutionTrace();
        apiTrace.setTotalDurationMs(domainTrace.totalDurationMs());
        apiTrace.setTotalTokenUsage(mapTokenUsage(domainTrace.totalTokenUsage()));
        apiTrace.setSteps(domainTrace.steps().stream()
                .map(this::mapExecutionStep)
                .toList());
        return apiTrace;
    }

    default com.berdachuk.expertmatch.api.model.ExecutionStep mapExecutionStep(
            com.berdachuk.expertmatch.query.ExecutionTrace.ExecutionStep domainStep) {
        com.berdachuk.expertmatch.api.model.ExecutionStep apiStep = new com.berdachuk.expertmatch.api.model.ExecutionStep();
        apiStep.setName(domainStep.name());
        apiStep.setService(domainStep.service());
        apiStep.setMethod(domainStep.method());
        apiStep.setDurationMs(domainStep.durationMs());
        apiStep.setStatus(com.berdachuk.expertmatch.api.model.ExecutionStep.StatusEnum.fromValue(domainStep.status()));
        apiStep.setInputSummary(domainStep.inputSummary());
        apiStep.setOutputSummary(domainStep.outputSummary());
        apiStep.setLlmModel(domainStep.llmModel() != null
                ? org.openapitools.jackson.nullable.JsonNullable.of(domainStep.llmModel())
                : org.openapitools.jackson.nullable.JsonNullable.undefined());
        apiStep.setTokenUsage(mapTokenUsage(domainStep.tokenUsage()));
        return apiStep;
    }

    default com.berdachuk.expertmatch.api.model.TokenUsage mapTokenUsage(
            com.berdachuk.expertmatch.query.ExecutionTrace.TokenUsage domainTokenUsage) {
        if (domainTokenUsage == null) {
            return null;
        }
        com.berdachuk.expertmatch.api.model.TokenUsage apiTokenUsage = new com.berdachuk.expertmatch.api.model.TokenUsage();
        apiTokenUsage.setInputTokens(domainTokenUsage.inputTokens() != null
                ? org.openapitools.jackson.nullable.JsonNullable.of(domainTokenUsage.inputTokens())
                : org.openapitools.jackson.nullable.JsonNullable.undefined());
        apiTokenUsage.setOutputTokens(domainTokenUsage.outputTokens() != null
                ? org.openapitools.jackson.nullable.JsonNullable.of(domainTokenUsage.outputTokens())
                : org.openapitools.jackson.nullable.JsonNullable.undefined());
        apiTokenUsage.setTotalTokens(domainTokenUsage.totalTokens() != null
                ? org.openapitools.jackson.nullable.JsonNullable.of(domainTokenUsage.totalTokens())
                : org.openapitools.jackson.nullable.JsonNullable.undefined());
        return apiTokenUsage;
    }

    // Chat-related mappings

    @Mapping(target = "createdAt", expression = "java(domainChat.createdAt() != null ? domainChat.createdAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "updatedAt", expression = "java(domainChat.updatedAt() != null ? domainChat.updatedAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "lastActivityAt", expression = "java(domainChat.lastActivityAt() != null ? domainChat.lastActivityAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    Chat toApiChat(ChatRepository.Chat domainChat);

    List<Chat> toApiChatList(List<ChatRepository.Chat> domainChats);

    @Mapping(target = "messageType", expression = "java(com.berdachuk.expertmatch.api.model.ConversationMessage.MessageTypeEnum.fromValue(domainMessage.messageType()))")
    @Mapping(target = "createdAt", expression = "java(domainMessage.createdAt() != null ? domainMessage.createdAt().atOffset(java.time.ZoneOffset.UTC) : null)")
    @Mapping(target = "tokensUsed", expression = "java(domainMessage.tokensUsed() != null ? org.openapitools.jackson.nullable.JsonNullable.of(domainMessage.tokensUsed()) : org.openapitools.jackson.nullable.JsonNullable.undefined())")
    com.berdachuk.expertmatch.api.model.ConversationMessage toApiConversationMessage(ConversationHistoryRepository.ConversationMessage domainMessage);

    List<com.berdachuk.expertmatch.api.model.ConversationMessage> toApiConversationMessageList(List<ConversationHistoryRepository.ConversationMessage> domainMessages);
}

