---
name: rag-answer-generation
description: Generate accurate, well-formatted answers using RAG (Retrieval-Augmented Generation) with proper citations
and context integration
---

# RAG Answer Generation

This skill guides you on how to generate high-quality answers using ExpertMatch's RAG (Retrieval-Augmented Generation)
system, which combines retrieved expert information with LLM generation.

## Overview

RAG answer generation in ExpertMatch follows a structured approach:

1. **Retrieve** relevant expert information using hybrid retrieval
2. **Augment** the LLM context with retrieved expert data
3. **Generate** a comprehensive, well-formatted answer with citations

## When to Use This Skill

Use this skill when:

- User asks questions about experts or expert matching
- You need to provide detailed expert information
- User requests explanations about expert capabilities or experience
- You need to format expert search results in a user-friendly way

## Answer Structure

### 1. Introduction

- Brief summary of what was found
- Number of experts matching the query
- Overall relevance or quality indicator

### 2. Expert Listings

For each expert, include:

**Essential Information:**

- Name and identifier
- Seniority level
- Relevance score (if available)

**Key Qualifications:**

- Matched skills and technologies
- Years of experience (if available)
- Domain expertise

**Relevant Projects:**

- Top 2-3 most relevant projects
- Project name, role, duration
- Key technologies used

**Experience Highlights:**

- Notable experience areas (ETL pipelines, high-performance services, system architecture, monitoring, on-call)
- Language proficiency (if relevant)

### 3. Context and Insights

- Why these experts match the query
- Notable patterns or strengths
- Any gaps or considerations

### 4. Citations

- Reference expert IDs
- Reference project IDs (if available)
- Link to external profiles (if configured)

## Citation Format

When referencing experts or projects, use clear citations:

**Format:**

- Expert: `[Expert Name (ID: ...)]`
- Project: `[Project Name]`
- Technology: `[Technology Name]`

**Example:**

```
John Doe (ID: 8760000000000420950) has 5+ years of experience with Java and Spring Boot.
He worked on [E-commerce Platform] where he built high-performance microservices using [PostgreSQL] and [Redis].
```

## Answer Generation Workflow

### Step 1: Retrieve Expert Information

Use `expertQuery` or specialized retrieval tools to get expert data:

```java
QueryResponse response = expertQuery(query, chatId);
List<ExpertMatch> experts = response.experts();
```

### Step 2: Analyze Retrieved Data

Review the retrieved experts:

- Check relevance scores
- Identify key skills and technologies
- Note relevant projects
- Consider experience highlights

### Step 3: Structure the Answer

Organize information hierarchically:

1. Most relevant experts first
2. Group by common themes (technologies, domains, seniority)
3. Highlight unique strengths

### Step 4: Generate Natural Language Response

Convert structured data into natural, readable text:

- Use clear, concise language
- Avoid jargon unless necessary
- Explain technical terms when helpful
- Use bullet points for lists
- Use paragraphs for detailed descriptions

### Step 5: Add Citations and Context

- Cite expert IDs and names
- Reference specific projects
- Link to external profiles (if available)
- Provide context about why experts match

## Best Practices

### 1. Prioritize Relevance

- Always present most relevant experts first
- Use relevance scores to guide ordering
- Filter out low-relevance matches if too many results

### 2. Provide Context

- Explain why experts match the query
- Highlight unique qualifications
- Note any gaps or limitations

### 3. Use Structured Formatting

- Use headers for sections
- Use bullet points for lists
- Use tables for comparisons (if multiple experts)
- Use bold for emphasis (expert names, key skills)

### 4. Be Concise but Complete

- Include essential information
- Avoid overwhelming with too much detail
- Provide enough context for decision-making

### 5. Handle Edge Cases

- No results: Explain why and suggest alternative queries
- Too many results: Summarize top matches and suggest filters
- Ambiguous queries: Clarify and provide options

### 6. Maintain Conversation Context

- Reference previous queries when relevant
- Build on previous expert mentions
- Use chat history to provide continuity

## Answer Templates

### Template 1: Single Expert Query

```
Found 1 expert matching your requirements:

**[Expert Name]** (Seniority: [Level], Relevance: [Score])
- **Key Skills**: [Skills]
- **Technologies**: [Technologies]
- **Experience**: [Years] years
- **Domain**: [Domain]

**Relevant Projects:**
- [Project 1]: [Role], [Duration] - [Technologies]
- [Project 2]: [Role], [Duration] - [Technologies]

**Experience Highlights:**
- [Highlight 1]
- [Highlight 2]

[Expert Name] matches your requirements because [explanation].
```

### Template 2: Multiple Experts Query

```
Found [N] experts matching your requirements:

**Top Matches:**

1. **[Expert 1 Name]** (Seniority: [Level])
   - Key Skills: [Skills]
   - Relevant Projects: [Project 1], [Project 2]
   - [Brief explanation of match]

2. **[Expert 2 Name]** (Seniority: [Level])
   - Key Skills: [Skills]
   - Relevant Projects: [Project 1], [Project 2]
   - [Brief explanation of match]

[Additional experts...]

**Summary:**
These experts share common strengths in [technologies/skills]. [Expert 1] stands out for [reason], while [Expert 2] excels in [area].
```

### Template 3: Technology-Based Query

```
Found [N] experts with experience in [Technology]:

**Experts by Seniority:**

**Senior ([Level]):**
- [Expert 1]: [Years] years, [Key Projects]
- [Expert 2]: [Years] years, [Key Projects]

**Mid-Level ([Level]):**
- [Expert 3]: [Years] years, [Key Projects]

**Key Projects Using [Technology]:**
- [Project 1]: [Experts involved]
- [Project 2]: [Experts involved]

These experts have demonstrated expertise in [Technology] across [domains/types] of projects.
```

## Integration with Prompt Templates

ExpertMatch uses `rag-prompt.st` template for programmatic RAG answer generation. When using ChatClient with tools, you
can:

1. Retrieve experts using `expertQuery`
2. Format the response using the skill guidelines
3. The LLM will automatically structure the answer based on this skill

## Common Scenarios

### Scenario 1: General Expert Search

```
User: "Find Java developers"
→ Retrieve: expertQuery("Java developers")
→ Generate: List experts with Java experience, highlight relevant projects
```

### Scenario 2: Specific Requirements

```
User: "Find senior Java developers with Spring Boot and PostgreSQL"
→ Retrieve: findExperts(technologies: ["Java", "Spring Boot", "PostgreSQL"], seniority: "A4")
→ Generate: Focused list with detailed qualifications
```

### Scenario 3: Project-Based Discovery

```
User: "Who worked on project X?"
→ Retrieve: getProjectExperts(projectName: "X")
→ Generate: List of experts with their roles and contributions
```

### Scenario 4: Comparison Query

```
User: "Compare experts with Java vs Python experience"
→ Retrieve: Multiple queries for Java and Python
→ Generate: Side-by-side comparison with strengths of each
```

## References

- See `references/rag-prompt.st` for the RAG prompt template
- See `references/citation-format.md` for citation guidelines
- See `references/answer-templates.md` for more templates
