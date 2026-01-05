Classify the following query by choosing exactly one intent from the available options.

## Query
<query>

## Available Intents
You must choose exactly ONE of the following intents:

1. **EXPERT_SEARCH**: Query is looking for individual experts with specific skills or experience
2. **TEAM_FORMATION**: Query is requesting a team composition with multiple roles
3. **RFP_RESPONSE**: Query is for responding to an RFP (Request for Proposal) with expert assignments
4. **DOMAIN_INQUIRY**: Query is asking about a domain, technology, or general information

## Classification Requirements
- Choose the intent that best matches the query
- Provide confidence score (0-100) indicating how certain you are
- Explain your reasoning for the classification
- Extract any specific requirements mentioned in the query

## Response Format
Respond with a valid JSON object matching this structure:
{
  "intent": "EXPERT_SEARCH|TEAM_FORMATION|RFP_RESPONSE|DOMAIN_INQUIRY",
  "confidence": 0-100,
  "reasoning": "string explaining the classification",
  "extractedRequirements": {
    "skills": ["string"],
    "technologies": ["string"],
    "seniority": ["string"],
    "other": "any other extracted information"
  }
}

