---
name: query-classification
description: Classify user queries by intent and type to route to appropriate tools and retrieval strategies
---

# Query Classification

This skill guides you on how to classify user queries in ExpertMatch to determine the appropriate tools and retrieval
strategies.

## Overview

Query classification analyzes user queries to determine:

1. **Query Type** - What the user wants to accomplish
2. **Query Intent** - The underlying goal
3. **Required Information** - What data needs to be extracted
4. **Tool Selection** - Which tools to use

## Query Types

### 1. Expert Matching Query

**Characteristics:**

- User wants to find experts matching criteria
- Mentions skills, technologies, seniority, or experience
- Examples:
    - "Find Java developers"
    - "Who has experience with Spring Boot?"
    - "I need a senior backend engineer"

**Tools to Use:**

- `expertQuery` (primary)
- `findExperts` (if structured criteria available)

**Retrieval Strategy:**

- Hybrid retrieval (vector + graph + keyword)
- Focus on skills and technologies

### 2. Expert Profile Query

**Characteristics:**

- User asks about a specific expert
- Provides expert name or ID
- Examples:
    - "Who is John Doe?"
    - "Show me expert profile for ID 8760000000000420950"
    - "Tell me about [Expert Name]"

**Tools to Use:**

- `getExpertProfile` (primary)
- `expertQuery` (if name provided, for name matching)

**Retrieval Strategy:**

- Direct lookup by ID or name
- Name matching if needed

### 3. Project-Based Query

**Characteristics:**

- User asks about experts who worked on a project
- Mentions project name or ID
- Examples:
    - "Who worked on project X?"
    - "Find experts from project Y"
    - "Show me the team for project Z"

**Tools to Use:**

- `getProjectExperts` (primary)
- `expertQuery` (if project name mentioned)

**Retrieval Strategy:**

- Graph traversal (project-expert relationships)
- Work experience lookup

### 4. Collaboration Query

**Characteristics:**

- User asks about expert relationships
- Mentions collaboration or team composition
- Examples:
    - "Who worked with John Doe?"
    - "Find experts who collaborated on project X"
    - "Show me the team that worked together"

**Tools to Use:**

- `findCollaboratingExperts` (primary)
- `getProjectExperts` (for project-based collaboration)

**Retrieval Strategy:**

- Graph traversal (expert-expert relationships)
- Project-based relationship discovery

### 5. Technology Discovery Query

**Characteristics:**

- User asks about technology usage
- Wants to find experts by technology
- Examples:
    - "Who uses Java?"
    - "Find experts with AWS experience"
    - "Show me all technologies used by experts"

**Tools to Use:**

- `findExpertsByTechnology` (single technology)
- `findExpertsByTechnologies` (multiple technologies)
- `vectorSearch` (semantic technology search)

**Retrieval Strategy:**

- Graph traversal (technology-expert relationships)
- Vector search for semantic technology matching

### 6. Domain/Industry Query

**Characteristics:**

- User asks about domain expertise
- Mentions industry or business domain
- Examples:
    - "Find experts in Finance"
    - "Who has Healthcare experience?"
    - "Show me E-commerce experts"

**Tools to Use:**

- `findExpertsByDomain` (primary)
- `expertQuery` (with domain extraction)

**Retrieval Strategy:**

- Graph traversal (domain-expert relationships)
- Vector search for domain-related projects

### 7. RFP Response Query

**Characteristics:**

- User provides project requirements
- Wants team formation or expert matching for RFP
- Examples:
    - "I need a team for project X with requirements..."
    - "Match these project requirements with experts"
    - "Form a team for [project description]"

**Tools to Use:**

- `matchProjectRequirements` (primary)
- `expertQuery` (with project requirements)

**Retrieval Strategy:**

- Hybrid retrieval with project requirements
- Team composition analysis

### 8. Comparison Query

**Characteristics:**

- User wants to compare experts or technologies
- Asks for differences or similarities
- Examples:
    - "Compare Java vs Python experts"
    - "What's the difference between these experts?"
    - "Show me experts with different skill sets"

**Tools to Use:**

- Multiple `expertQuery` calls
- `findExperts` with different criteria
- Combine results for comparison

**Retrieval Strategy:**

- Multiple retrieval queries
- Result aggregation and comparison

## Classification Process

### Step 1: Analyze Query Text

Extract key indicators:

- **Keywords**: Skills, technologies, names, projects
- **Question Words**: Who, what, where, when, why, how
- **Intent Signals**: "Find", "Show", "Who", "Compare", "Match"
- **Entity Types**: Expert names, project names, technologies, domains

### Step 2: Determine Query Type

Use classification rules:

1. **Name mentioned** → Expert Profile Query or Person Name Matching
2. **Project mentioned** → Project-Based Query
3. **Technology mentioned** → Technology Discovery Query
4. **Domain mentioned** → Domain/Industry Query
5. **Requirements provided** → RFP Response Query or Expert Matching Query
6. **Comparison words** → Comparison Query
7. **Collaboration words** → Collaboration Query

### Step 3: Extract Required Information

Parse query to extract:

- **Skills**: List of required skills
- **Technologies**: List of technologies
- **Seniority**: Level (A3, A4, A5)
- **Domain**: Industry or business domain
- **Expert Name/ID**: For profile queries
- **Project Name/ID**: For project queries
- **Requirements**: For RFP queries

### Step 4: Select Tools

Choose appropriate tools based on:

- Query type
- Available information
- User intent
- Conversation context

### Step 5: Execute and Format Response

- Execute selected tools
- Format response according to query type
- Provide relevant context and explanations

## Classification Examples

### Example 1: Expert Matching

```
Query: "Find Java developers with Spring Boot experience"

Classification:
- Type: Expert Matching Query
- Intent: Find experts matching criteria
- Information: Skills=["Java", "Spring Boot"]
- Tool: expertQuery("Find Java developers with Spring Boot experience")
- Strategy: Hybrid retrieval
```

### Example 2: Expert Profile

```
Query: "Who is John Doe?"

Classification:
- Type: Expert Profile Query
- Intent: Get expert information
- Information: Expert Name="John Doe"
- Tool: getExpertProfile(expertName: "John Doe")
- Strategy: Name matching + direct lookup
```

### Example 3: Project-Based

```
Query: "Who worked on the E-commerce Platform project?"

Classification:
- Type: Project-Based Query
- Intent: Find project team members
- Information: Project Name="E-commerce Platform"
- Tool: getProjectExperts(projectName: "E-commerce Platform")
- Strategy: Graph traversal (project-expert relationships)
```

### Example 4: Technology Discovery

```
Query: "Find experts who use AWS"

Classification:
- Type: Technology Discovery Query
- Intent: Discover technology usage
- Information: Technology="AWS"
- Tool: findExpertsByTechnology(technology: "AWS")
- Strategy: Graph traversal (technology-expert relationships)
```

### Example 5: RFP Response

```
Query: "I need a team with Java, Spring Boot, PostgreSQL, and A4 seniority"

Classification:
- Type: RFP Response Query
- Intent: Match project requirements
- Information: Technologies=["Java", "Spring Boot", "PostgreSQL"], Seniority="A4"
- Tool: matchProjectRequirements(requirements: {...})
- Strategy: Hybrid retrieval with requirements matching
```

## Best Practices

### 1. Use Context

- Consider previous conversation
- Remember user's previous queries
- Use chat history for disambiguation

### 2. Handle Ambiguity

- Ask clarifying questions when uncertain
- Provide multiple interpretations
- Suggest alternatives

### 3. Progressive Refinement

- Start with broad classification
- Refine based on results
- Allow user to narrow down

### 4. Combine Strategies

- Use multiple tools when needed
- Combine retrieval strategies
- Aggregate results intelligently

### 5. Provide Feedback

- Explain classification decision
- Show what information was extracted
- Confirm understanding with user

## Integration with Query Parser

ExpertMatch's `QueryParser` automatically extracts:

- Skills
- Technologies
- Seniority levels
- Domains
- Projects
- Organizations
- Persons

Use parsed information to:

- Select appropriate tools
- Refine retrieval strategies
- Format responses

## References

- See `references/query-classification.st` for the classification prompt template
- See `references/query-parsing.md` for query parsing details
- See `references/intent-detection.md` for intent detection strategies
