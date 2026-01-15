package com.berdachuk.expertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration for Spring AI PromptTemplate with StringTemplate renderer.
 * Uses custom delimiters (< and >) to avoid conflicts with JSON syntax in templates.
 */
@Slf4j
@Configuration
public class PromptTemplateConfig {

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptResource;
    @Value("classpath:/prompts/cascade-evaluation.st")
    private Resource cascadeEvaluationResource;
    @Value("classpath:/prompts/cycle-evaluation.st")
    private Resource cycleEvaluationResource;
    @Value("classpath:/prompts/query-classification.st")
    private Resource queryClassificationResource;
    @Value("classpath:/prompts/query-refinement.st")
    private Resource queryRefinementResource;
    @Value("classpath:/prompts/skill-extraction.st")
    private Resource skillExtractionResource;
    @Value("classpath:/prompts/seniority-extraction.st")
    private Resource seniorityExtractionResource;
    @Value("classpath:/prompts/language-extraction.st")
    private Resource languageExtractionResource;
    @Value("classpath:/prompts/technology-extraction.st")
    private Resource technologyExtractionResource;
    @Value("classpath:/prompts/domain-extraction.st")
    private Resource domainExtractionResource;
    @Value("classpath:/prompts/person-extraction.st")
    private Resource personExtractionResource;
    @Value("classpath:/prompts/organization-extraction.st")
    private Resource organizationExtractionResource;
    @Value("classpath:/prompts/technology-entity-extraction.st")
    private Resource technologyEntityExtractionResource;
    @Value("classpath:/prompts/project-extraction.st")
    private Resource projectExtractionResource;
    @Value("classpath:/prompts/gap-analysis.st")
    private Resource gapAnalysisResource;
    @Value("classpath:/prompts/reranking.st")
    private Resource rerankingResource;
    @Value("classpath:/prompts/constant-expansion-technologies.st")
    private Resource constantExpansionTechnologiesResource;
    @Value("classpath:/prompts/constant-expansion-tools.st")
    private Resource constantExpansionToolsResource;
    @Value("classpath:/prompts/constant-expansion-project-types.st")
    private Resource constantExpansionProjectTypesResource;
    @Value("classpath:/prompts/constant-expansion-team-names.st")
    private Resource constantExpansionTeamNamesResource;
    @Value("classpath:/prompts/constant-expansion-technology-categories.st")
    private Resource constantExpansionTechnologyCategoriesResource;
    @Value("classpath:/prompts/constant-expansion-technology-synonyms.st")
    private Resource constantExpansionTechnologySynonymsResource;
    @Value("classpath:/prompts/summarize-history.st")
    private Resource summarizeHistoryResource;
    @Value("classpath:/prompts/name-matching.st")
    private Resource nameMatchingResource;

    @Bean
    public StTemplateRenderer stTemplateRenderer() {
        return StTemplateRenderer.builder()
                .startDelimiterToken('<')
                .endDelimiterToken('>')
                .build();
    }

    /**
     * Creates a PromptTemplate for RAG prompts.
     */
    @Bean
    public PromptTemplate ragPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(ragPromptResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for Cascade pattern evaluation.
     */
    @Bean
    public PromptTemplate cascadeEvaluationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(cascadeEvaluationResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for Cycle pattern evaluation.
     */
    @Bean
    public PromptTemplate cycleEvaluationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(cycleEvaluationResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for query classification (Routing pattern).
     */
    @Bean
    public PromptTemplate queryClassificationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(queryClassificationResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for query refinement.
     */
    @Bean
    public PromptTemplate queryRefinementPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(queryRefinementResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for skill extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("skillExtractionPromptTemplate")
    public PromptTemplate skillExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(skillExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for seniority extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("seniorityExtractionPromptTemplate")
    public PromptTemplate seniorityExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(seniorityExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for language extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("languageExtractionPromptTemplate")
    public PromptTemplate languageExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(languageExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for technology extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("technologyExtractionPromptTemplate")
    public PromptTemplate technologyExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(technologyExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for domain extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("domainExtractionPromptTemplate")
    public PromptTemplate domainExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(domainExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for person extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("personExtractionPromptTemplate")
    public PromptTemplate personExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(personExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for organization extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("organizationExtractionPromptTemplate")
    public PromptTemplate organizationExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(organizationExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for technology entity extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("technologyEntityExtractionPromptTemplate")
    public PromptTemplate technologyEntityExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(technologyEntityExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for project extraction.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("projectExtractionPromptTemplate")
    public PromptTemplate projectExtractionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(projectExtractionResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for gap analysis.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("gapAnalysisPromptTemplate")
    public PromptTemplate gapAnalysisPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(gapAnalysisResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for semantic reranking.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("rerankingPromptTemplate")
    public PromptTemplate rerankingPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(rerankingResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for technologies constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("technologiesExpansionPromptTemplate")
    public PromptTemplate technologiesExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionTechnologiesResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for tools constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("toolsExpansionPromptTemplate")
    public PromptTemplate toolsExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionToolsResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for project types constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("projectTypesExpansionPromptTemplate")
    public PromptTemplate projectTypesExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionProjectTypesResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for team names constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("teamNamesExpansionPromptTemplate")
    public PromptTemplate teamNamesExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionTeamNamesResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for technology categories constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("technologyCategoriesExpansionPromptTemplate")
    public PromptTemplate technologyCategoriesExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionTechnologyCategoriesResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for technology synonyms constant expansion.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("technologySynonymsExpansionPromptTemplate")
    public PromptTemplate technologySynonymsExpansionPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(constantExpansionTechnologySynonymsResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for summarizing conversation history.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("summarizeHistoryPromptTemplate")
    public PromptTemplate summarizeHistoryPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(summarizeHistoryResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for name matching using LLM.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("nameMatchingPromptTemplate")
    public PromptTemplate nameMatchingPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(nameMatchingResource)
                .build();
    }
}

