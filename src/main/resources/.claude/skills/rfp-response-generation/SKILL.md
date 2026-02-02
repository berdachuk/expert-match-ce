---
name: rfp-response-generation
description: Generate RFP (Request for Proposal) responses by matching project requirements with experts and forming
optimal teams
---

# RFP Response Generation

This skill guides you on how to generate RFP (Request for Proposal) responses by matching project requirements with
experts and forming optimal teams.

## Overview

RFP response generation involves:

1. **Parse Requirements** - Extract project requirements from user input
2. **Match Experts** - Find experts matching project requirements
3. **Form Teams** - Compose optimal teams based on requirements
4. **Generate Response** - Create structured RFP response with team composition

## When to Use This Skill

Use this skill when:

- User provides project requirements and needs expert matching
- User asks to form a team for a project
- User wants RFP response with team composition
- User needs to match multiple experts to project requirements

## Available Tools

### Primary Tool: `matchProjectRequirements`

Use `matchProjectRequirements` to match project requirements with experts:

```java
matchProjectRequirements(requirements: {
    "skills": ["Java", "Spring Boot", "PostgreSQL"],
    "technical_stack": ["AWS", "Docker", "Kubernetes"],
    "seniority":"A4",
            "responsibilities": ["Architecture", "Team Lead", "Code Review"]
})
```

**Parameters:**

- `skills`: List of required skills
- `technical_stack`: List of required technologies
- `seniority`: Required seniority level (A3, A4, A5)
- `responsibilities`: List of role responsibilities

### Alternative: Use `expertQuery`

For natural language project requirements:

```java
expertQuery(query:"I need a team for microservices project with Java, Spring Boot, PostgreSQL, AWS, and A4 seniority", chatId:chatId)
```

### Supporting Tools

#### `findExperts`

Use when you have structured criteria for multiple roles:

```java
findExperts(
        skills:["Java", "Spring Boot"],
        seniority:"A4",
        technologies:["PostgreSQL", "Redis"],
        domain:"Finance"
)
```

#### `getExpertProfile`

Get detailed profiles for matched experts:

```java
getExpertProfile(expertId:"8760000000000420950", expertName:null)
```

#### `getProjectExperts`

Find experts who worked on similar projects:

```java
getProjectExperts(projectName:"Similar Project Name")
```

## RFP Response Structure

### 1. Executive Summary

- Project overview
- Team composition summary
- Key qualifications

### 2. Team Composition

For each team member:

- **Role**: Position title (e.g., "Senior Backend Engineer", "Tech Lead")
- **Expert**: Name and ID
- **Seniority**: Level (A3, A4, A5)
- **Key Skills**: Relevant skills for the role
- **Technologies**: Technologies they'll work with
- **Relevant Experience**: Projects demonstrating required expertise
- **Responsibilities**: What they'll be responsible for

### 3. Team Strengths

- Overall team capabilities
- Complementary skills
- Domain expertise
- Technology coverage

### 4. Project Approach

- How the team will approach the project
- Key methodologies or practices
- Risk mitigation strategies

### 5. Qualifications and Experience

- Team's combined experience
- Relevant project history
- Domain expertise
- Technology proficiency

## Team Formation Strategy

### Step 1: Parse Requirements

Extract from user input:

- **Roles Needed**: What positions are required
- **Skills per Role**: Skills needed for each role
- **Technologies**: Required technology stack
- **Seniority**: Required experience levels
- **Domain**: Industry or business domain
- **Project Type**: Type of project (e.g., microservices, data pipeline)

### Step 2: Match Experts to Roles

For each role:

1. Build query with role-specific requirements
2. Use `matchProjectRequirements` or `findExperts`
3. Rank experts by relevance
4. Select top candidates

### Step 3: Optimize Team Composition

Consider:

- **Skill Coverage**: Ensure all required skills are covered
- **Technology Coverage**: All technologies have experts
- **Seniority Mix**: Appropriate seniority distribution
- **Complementary Skills**: Experts with complementary expertise
- **Collaboration History**: Experts who worked together (if available)
- **Domain Expertise**: Relevant domain experience

### Step 4: Validate Team

Check:

- All requirements met
- No skill gaps
- Appropriate team size
- Balanced workload

### Step 5: Generate Response

Format team composition:

- List each team member with qualifications
- Highlight team strengths
- Explain why this team fits the project
- Provide project approach

## Best Practices

### 1. Match Requirements Precisely

- Extract all requirements accurately
- Match experts to specific role requirements
- Ensure skill and technology coverage

### 2. Consider Team Dynamics

- Balance seniority levels
- Include complementary skills
- Consider collaboration history
- Ensure domain expertise

### 3. Provide Justification

- Explain why each expert matches
- Highlight relevant experience
- Show how team covers requirements
- Demonstrate team strengths

### 4. Handle Gaps

- Identify missing skills or technologies
- Suggest alternatives or training
- Be transparent about limitations
- Offer to find additional experts if needed

### 5. Format Professionally

- Use clear structure
- Highlight key qualifications
- Provide detailed experience
- Include relevant project examples

## Response Templates

### Template 1: Single Role Team

```
**Team Composition for [Project Name]**

**Role: [Role Title]**
- **Expert**: [Expert Name] (ID: [ID])
- **Seniority**: [Level]
- **Key Skills**: [Skills]
- **Technologies**: [Technologies]

**Relevant Experience:**
- [Project 1]: [Role], [Duration] - [Key Technologies]
- [Project 2]: [Role], [Duration] - [Key Technologies]

**Responsibilities:**
- [Responsibility 1]
- [Responsibility 2]

**Why This Expert:**
[Explanation of match and relevant experience]
```

### Template 2: Multi-Role Team

```
**Team Composition for [Project Name]**

**Team Overview:**
- Total Members: [N]
- Combined Experience: [Years] years
- Key Technologies: [Technologies]
- Domain Expertise: [Domains]

**Team Members:**

1. **[Role 1]**: [Expert Name] (Seniority: [Level])
   - Skills: [Skills]
   - Technologies: [Technologies]
   - Relevant Projects: [Projects]
   - Responsibilities: [Responsibilities]

2. **[Role 2]**: [Expert Name] (Seniority: [Level])
   - Skills: [Skills]
   - Technologies: [Technologies]
   - Relevant Projects: [Projects]
   - Responsibilities: [Responsibilities]

[Additional roles...]

**Team Strengths:**
- [Strength 1]: [Explanation]
- [Strength 2]: [Explanation]
- [Strength 3]: [Explanation]

**Project Approach:**
[How the team will approach the project]
```

### Template 3: Requirements-Based Matching

```
**Expert Matching for Project Requirements**

**Requirements:**
- Skills: [Skills]
- Technologies: [Technologies]
- Seniority: [Level]
- Domain: [Domain]

**Matched Experts:**

1. **[Expert Name]** (Seniority: [Level])
   - Match Score: [Score]
   - Matched Skills: [Skills]
   - Relevant Projects: [Projects]
   - [Explanation of match]

2. **[Expert Name]** (Seniority: [Level])
   - Match Score: [Score]
   - Matched Skills: [Skills]
   - Relevant Projects: [Projects]
   - [Explanation of match]

**Team Composition Recommendation:**
[Suggested team structure and roles]
```

## Common Scenarios

### Scenario 1: Complete Project Requirements

```
User: "I need a team for a microservices project. Requirements: Java, Spring Boot, PostgreSQL, AWS, Docker, Kubernetes. Need A4 seniority. Roles: Backend Engineer, DevOps Engineer."

→ Parse: Extract roles, skills, technologies, seniority
→ Match: Find experts for each role
→ Compose: Form team with role assignments
→ Generate: RFP response with team composition
```

### Scenario 2: Natural Language Requirements

```
User: "Form a team for building a finance platform with Java and Spring Boot. Need experienced developers."

→ Parse: Extract skills, domain, seniority (inferred)
→ Match: Use expertQuery with requirements
→ Compose: Select top experts forming balanced team
→ Generate: RFP response with team details
```

### Scenario 3: Similar Project Matching

```
User: "I need a team similar to the E-commerce Platform project."

→ Retrieve: getProjectExperts("E-commerce Platform")
→ Analyze: Extract skills and technologies from project
→ Match: Find experts with similar experience
→ Generate: RFP response with team based on similar project
```

## Integration with Other Skills

### With Expert Matching

Combine RFP generation with expert search:

```
User: "Find experts for RFP with requirements X"
→ Use expert-matching-hybrid-retrieval skill
→ Match experts to requirements
→ Form team composition
→ Generate RFP response
```

### With Query Classification

Classify RFP queries:

```
User: "I need a team for project X"
→ Classify as RFP Response Query
→ Extract requirements
→ Match experts
→ Generate response
```

## References

- See `references/rfp-template.md` for RFP response templates
- See `references/team-formation.md` for team composition strategies
- See `assets/example-rfp-response.md` for example RFP responses
