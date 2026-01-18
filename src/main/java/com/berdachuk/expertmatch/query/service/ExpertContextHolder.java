package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;

import java.util.List;

/**
 * ThreadLocal-based holder for expert contexts during query processing.
 * Allows tools to access expert data without passing through AnswerGenerationService.
 */
public class ExpertContextHolder {

    private static final ThreadLocal<List<AnswerGenerationService.ExpertContext>> CONTEXT = new ThreadLocal<>();

    /**
     * Sets the expert contexts for the current thread.
     *
     * @param contexts List of expert contexts to store
     */
    public static void set(List<AnswerGenerationService.ExpertContext> contexts) {
        CONTEXT.set(contexts);
    }

    /**
     * Gets the expert contexts for the current thread.
     *
     * @return List of expert contexts, or null if not set
     */
    public static List<AnswerGenerationService.ExpertContext> get() {
        return CONTEXT.get();
    }

    /**
     * Clears the expert contexts for the current thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Checks if expert contexts are available for the current thread.
     *
     * @return true if contexts are set and not empty, false otherwise
     */
    public static boolean hasContexts() {
        List<AnswerGenerationService.ExpertContext> contexts = CONTEXT.get();
        return contexts != null && !contexts.isEmpty();
    }
}
