package com.berdachuk.expertmatch.llm.sgr;

import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;


/**
 * Service interface for expertevaluation operations.
 */
public interface ExpertEvaluationService {
    /**
     * Evaluates a single expert using the Cascade pattern.
     * Performs multi-step evaluation with progressive refinement.
     *
     * @param query         The user's query
     * @param expertContext The expert context to evaluate
     * @return Expert evaluation result with detailed assessment
     */
    ExpertEvaluation evaluateWithCascade(String query,
                                         AnswerGenerationService.ExpertContext expertContext);
}
