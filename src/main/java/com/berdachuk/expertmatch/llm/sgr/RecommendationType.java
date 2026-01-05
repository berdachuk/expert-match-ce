package com.berdachuk.expertmatch.llm.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enumeration of recommendation types for expert evaluation.
 */
public enum RecommendationType {
    @JsonProperty("STRONGLY_RECOMMEND")
    STRONGLY_RECOMMEND,

    @JsonProperty("RECOMMEND")
    RECOMMEND,

    @JsonProperty("CONDITIONAL")
    CONDITIONAL,

    @JsonProperty("NOT_RECOMMEND")
    NOT_RECOMMEND
}

