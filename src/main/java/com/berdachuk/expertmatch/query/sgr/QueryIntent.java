package com.berdachuk.expertmatch.query.sgr;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enumeration of query intents for routing pattern.
 */
public enum QueryIntent {
    @JsonProperty("EXPERT_SEARCH")
    EXPERT_SEARCH,

    @JsonProperty("TEAM_FORMATION")
    TEAM_FORMATION,

    @JsonProperty("RFP_RESPONSE")
    RFP_RESPONSE,

    @JsonProperty("DOMAIN_INQUIRY")
    DOMAIN_INQUIRY
}

