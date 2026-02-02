---
name: expert-matching-hybrid-retrieval
description: Expert matching using hybrid retrieval strategy combining vector search, graph traversal, and keyword
matching with RRF fusion
---

# Expert Matching Hybrid Retrieval

This skill guides you on how to use ExpertMatch's hybrid retrieval system to find experts matching user requirements.

## Overview

ExpertMatch uses a sophisticated hybrid retrieval approach that combines three complementary search strategies:

1. **Vector Search** - Semantic similarity search using embeddings
2. **Graph Traversal** - Relationship-based search using Apache AGE graph database
3. **Keyword Matching** - Traditional text-based search

Results from all three strategies are fused using Reciprocal Rank Fusion (RRF) to produce the final ranked list of
experts.

## When to Use This Skill

Use this skill when:

- User asks to find experts with specific skills, technologies, or experience
- User needs experts for a project or team
- User wants to discover experts by domain, technology, or collaboration patterns
- User has natural language queries about expert requirements

## Available Tools

### Primary Tool: `expertQuery`

The main tool for expert discovery is `expertQuery` from `ExpertMatchTools`. This tool processes natural language
queries and automatically:

1. Parses the query to extract:
    - Required skills
    - Technologies
    - Seniority levels
    - Domain/industry
    - Project types
    - Language requirements

2. Performs hybrid retrieval:
    - Vector search for semantic similarity
    - Graph traversal for relationship-based discovery
    - Keyword matching for exact/partial matches

3. Fuses results using RRF (Reciprocal Rank Fusion)

4. Enriches results with:
    - Relevance scores
    - Matched skills
    - Relevant projects
    - Experience details

**Usage:**

```
expertQuery(query: "Find Java developers with Spring Boot experience", chatId: "optional-chat-id")
```

### Advanced Tool: `findExperts`

Use `findExperts` when you have explicit, structured criteria:

**Parameters:**

- `skills`: List of required skills (e.g., ["Java", "Spring Boot", "PostgreSQL"])
- `seniority`: Seniority level (e.g., "A3", "A4", "A5")
- `technologies`: Required technologies (e.g., ["AWS", "Docker", "Kubernetes"])
- `domain`: Domain or industry (e.g., "Finance", "Healthcare", "E-commerce")

**Usage:**

```
findExperts(
  skills: ["Java", "Spring Boot"],
  seniority: "A4",
  technologies: ["PostgreSQL", "Redis"],
  domain: "Finance"
)
```

### Specialized Tools

#### `getExpertProfile`

Get detailed profile for a specific expert by ID or name:

```
getExpertProfile(expertId: "8760000000000420950", expertName: null)
```

#### `getProjectExperts`

Find experts who worked on a specific project:

```
getProjectExperts(projectId: "4060741400384209073", projectName: "Project Name")
```

#### `matchProjectRequirements`

Match project requirements with experts (useful for RFP responses):

```
matchProjectRequirements(requirements: {
  "skills": ["Java", "Spring Boot"],
  "technical_stack": ["PostgreSQL", "Redis"],
  "seniority": "A4",
  "responsibilities": ["Architecture", "Team Lead"]
})
```

### Retrieval Tools

For fine-grained control over retrieval strategies:

#### `vectorSearch`

Perform semantic similarity search:

```
vectorSearch(query: "Java Spring Boot developer", maxResults: 10, similarityThreshold: 0.7)
```

#### `findExpertsByTechnology`

Find experts by technology using graph traversal:

```
findExpertsByTechnology(technology: "Java")
```

#### `findExpertsByTechnologies`

Find experts who worked with multiple technologies (AND condition):

```
findExpertsByTechnologies(technologies: ["Java", "Spring Boot", "PostgreSQL"])
```

#### `findCollaboratingExperts`

Find experts who collaborated with a specific expert:

```
findCollaboratingExperts(expertId: "8760000000000420950")
```

#### `findExpertsByDomain`

Find experts by domain or industry:

```
findExpertsByDomain(domain: "Finance")
```

## Retrieval Strategy Selection

### Use Vector Search When:

- Query is natural language with semantic meaning
- User describes requirements in their own words
- Need to find experts with similar experience even if exact keywords don't match
- Example: "Find someone who built scalable microservices"

### Use Graph Traversal When:

- Need to find experts by specific technology relationships
- Want to discover collaboration patterns
- Need to find experts who worked with multiple technologies together
- Example: "Find experts who worked with both Java and AWS"

### Use Keyword Matching When:

- User provides exact skill names or technologies
- Need to match specific terms
- Example: "Find Java developers"

### Use Hybrid Retrieval (Recommended):

- For most queries, use `expertQuery` which automatically combines all strategies
- Provides best results by leveraging strengths of each approach
- RRF fusion ensures diverse and relevant results

## Response Format

When presenting expert matches to users:

1. **Start with summary**: "Found X experts matching your requirements"
2. **List top matches** with:
    - Name and ID
    - Seniority level
    - Relevance score
    - Key matched skills/technologies
    - Relevant projects (top 2-3)
3. **Include experience highlights**:
    - Years of experience
    - Domain expertise
    - Notable projects
4. **Provide context**: Explain why each expert matches the query

## Best Practices

1. **Always use `expertQuery` first** for natural language queries - it handles parsing and hybrid retrieval
   automatically
2. **Use structured tools** (`findExperts`, `matchProjectRequirements`) when you have explicit criteria
3. **Leverage graph traversal** for relationship-based queries (collaboration, technology combinations)
4. **Combine multiple tools** if needed - e.g., use `vectorSearch` to find candidates, then `getExpertProfile` for
   details
5. **Respect chat context** - pass `chatId` to `expertQuery` to maintain conversation context
6. **Filter by seniority** when user specifies level requirements
7. **Consider domain expertise** for industry-specific requirements

## Common Query Patterns

### Pattern 1: Technology-Based Search

```
User: "Find Java developers"
→ Use: expertQuery("Java developers")
```

### Pattern 2: Skill Combination

```
User: "Find experts with Java, Spring Boot, and PostgreSQL"
→ Use: findExperts(technologies: ["Java", "Spring Boot", "PostgreSQL"])
```

### Pattern 3: Seniority + Technology

```
User: "Find senior Java developers"
→ Use: expertQuery("senior Java developers")
   or: findExperts(technologies: ["Java"], seniority: "A4")
```

### Pattern 4: Project-Based Discovery

```
User: "Who worked on project X?"
→ Use: getProjectExperts(projectName: "X")
```

### Pattern 5: Collaboration Discovery

```
User: "Find experts who worked with John Doe"
→ Use: getExpertProfile(expertName: "John Doe") to get ID
   Then: findCollaboratingExperts(expertId: "...")
```

## References

- See `references/hybrid-retrieval-guide.md` for detailed technical documentation
- See `references/rrf-fusion.md` for RRF algorithm details
- See `references/query-parsing.md` for query parsing capabilities
