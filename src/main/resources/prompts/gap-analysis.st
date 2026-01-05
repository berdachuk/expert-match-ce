Analyze the following expert search query and initial results:

Query: <query>

Initial Results: <expertCount> experts found

<if(expertSummaries)>
Expert Summaries:
<expertSummaries>
<endif>
Identify:
1. Information gaps - what information is missing from the initial results?
2. Ambiguities - what requirements are unclear or need clarification?
3. Missing context - what additional context would help improve expert matching?
4. Should we expand the search? Answer yes or no with brief reasoning.

Respond in JSON format:
{
  "identifiedGaps": ["gap1", "gap2"],
  "ambiguities": ["ambiguity1"],
  "missingInformation": ["missing1"],
  "needsExpansion": true/false,
  "reasoning": "brief explanation"
}

