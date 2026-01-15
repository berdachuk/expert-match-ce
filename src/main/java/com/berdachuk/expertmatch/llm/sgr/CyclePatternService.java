package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;

import java.util.List;


/**
 * Service interface for cyclepattern operations.
 */
public interface CyclePatternService {
    /**
     * Evaluates multiple experts using the Cycle pattern.
     * Compares experts against each other to determine relative strengths and weaknesses.
     *
     * @param query          The user's query
     * @param expertContexts List of expert contexts to evaluate
     * @return List of expert evaluations with comparison results
     */
    List<ExpertEvaluation> evaluateMultipleExperts(String query,
                                                   List<AnswerGenerationService.ExpertContext> expertContexts);
}
