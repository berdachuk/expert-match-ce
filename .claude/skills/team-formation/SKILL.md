---
name: team-formation
description: Form optimal teams by matching experts based on skills, technologies, collaboration history, and project
requirements
---

# Team Formation

This skill guides you on how to form optimal teams by matching experts based on skills, technologies, collaboration
history, and project requirements.

## Overview

Team formation in ExpertMatch involves:

1. **Analyze Requirements** - Understand project needs and team structure
2. **Match Experts** - Find experts matching role requirements
3. **Optimize Composition** - Balance skills, seniority, and collaboration
4. **Validate Team** - Ensure team meets all requirements

## When to Use This Skill

Use this skill when:

- User wants to form a team for a project
- User needs to compose a team with specific roles
- User asks about team composition or structure
- User wants to find complementary experts

## Available Tools

### Primary Tools

#### `matchProjectRequirements`

Match experts to project requirements:

```java
matchProjectRequirements(requirements: {
    "skills": ["Java", "Spring Boot"],
    "technical_stack": ["PostgreSQL", "AWS"],
    "seniority":"A4",
            "responsibilities": ["Architecture", "Team Lead"]
})
```

#### `findExperts`

Find experts with specific criteria:

```java
findExperts(
        skills:["Java", "Spring Boot"],
        seniority:"A4",
        technologies:["PostgreSQL"],
        domain:"Finance"
)
```

#### `expertQuery`

Natural language expert search:

```java
expertQuery(query:"Find Java developers with Spring Boot experience for team formation", chatId:chatId)
```

### Collaboration Tools

#### `findCollaboratingExperts`

Find experts who worked together:

```java
findCollaboratingExperts(expertId:"8760000000000420950")
```

#### `getProjectExperts`

Find team members from a project:

```java
getProjectExperts(projectName:"Project Name")
```

This helps identify experts who have worked together successfully.

### Profile Tools

#### `getExpertProfile`

Get detailed expert profiles:

```java
getExpertProfile(expertId:"8760000000000420950", expertName:null)
```

## Team Formation Strategy

### Step 1: Understand Requirements

Extract from user input:

- **Project Type**: What kind of project (microservices, data pipeline, web app, etc.)
- **Roles Needed**: What positions are required
- **Skills Required**: Skills needed for each role
- **Technologies**: Required technology stack
- **Seniority**: Required experience levels
- **Team Size**: How many experts needed
- **Domain**: Industry or business domain

### Step 2: Define Role Requirements

For each role, specify:

- **Role Title**: Position name
- **Required Skills**: Must-have skills
- **Preferred Skills**: Nice-to-have skills
- **Technologies**: Technologies to work with
- **Seniority**: Required level
- **Responsibilities**: What they'll do

### Step 3: Match Experts to Roles

For each role:

1. Build query with role-specific requirements
2. Use `matchProjectRequirements` or `findExperts`
3. Rank experts by relevance
4. Consider collaboration history if available
5. Select top candidates

### Step 4: Optimize Team Composition

Consider these factors:

#### Skill Coverage

- Ensure all required skills are covered
- Avoid skill gaps
- Include complementary skills

#### Technology Coverage

- All technologies have experts
- Balanced technology expertise
- Consider technology combinations

#### Seniority Mix

- Appropriate distribution (e.g., 1 senior, 2 mid-level)
- Balance experience levels
- Ensure leadership if needed

#### Collaboration History

- Prefer experts who worked together
- Use `findCollaboratingExperts` to find relationships
- Consider team chemistry

#### Domain Expertise

- Relevant domain experience
- Industry knowledge
- Business context understanding

#### Complementary Skills

- Experts with complementary expertise
- Different specializations
- Balanced skill sets

### Step 5: Validate Team

Check:

- ✅ All requirements met
- ✅ No critical skill gaps
- ✅ Appropriate team size
- ✅ Balanced workload
- ✅ Good collaboration potential
- ✅ Domain expertise covered

### Step 6: Present Team Composition

Format team presentation:

- List each team member with role
- Show qualifications and experience
- Highlight team strengths
- Explain team composition rationale

## Team Formation Patterns

### Pattern 1: Role-Based Formation

**Approach:**

1. Define roles (e.g., Backend Engineer, Frontend Engineer, DevOps)
2. Match experts to each role
3. Compose team with role assignments

**Example:**

```
Roles:
- Backend Engineer: Java, Spring Boot, PostgreSQL
- Frontend Engineer: React, TypeScript
- DevOps Engineer: AWS, Docker, Kubernetes

Team:
- Backend: John Doe (A4, Java expert)
- Frontend: Jane Smith (A3, React expert)
- DevOps: Bob Johnson (A4, AWS expert)
```

### Pattern 2: Skill-Based Formation

**Approach:**

1. Identify required skills
2. Find experts with those skills
3. Compose team ensuring skill coverage

**Example:**

```
Required Skills: Java, Spring Boot, PostgreSQL, AWS, Docker

Team:
- Expert 1: Java, Spring Boot, PostgreSQL (Backend)
- Expert 2: AWS, Docker, Kubernetes (DevOps)
- Expert 3: Java, Spring Boot, AWS (Full-stack)
```

### Pattern 3: Project-Based Formation

**Approach:**

1. Find similar projects
2. Get experts from those projects
3. Compose team based on successful project teams

**Example:**

```
Similar Project: E-commerce Platform
→ getProjectExperts("E-commerce Platform")
→ Team: Experts who worked on similar project
```

### Pattern 4: Collaboration-Based Formation

**Approach:**

1. Start with a key expert
2. Find collaborators
3. Build team around collaboration network

**Example:**

```
Key Expert: John Doe
→ findCollaboratingExperts("John Doe")
→ Team: John Doe + his collaborators
```

## Best Practices

### 1. Balance Team Composition

- Mix seniority levels appropriately
- Include complementary skills
- Ensure technology coverage
- Consider domain expertise

### 2. Consider Collaboration History

- Prefer experts who worked together
- Use collaboration data when available
- Build on successful team patterns

### 3. Optimize for Project Needs

- Match team to project type
- Ensure required skills covered
- Include relevant domain expertise
- Consider project complexity

### 4. Provide Justification

- Explain team composition
- Highlight team strengths
- Show how team meets requirements
- Demonstrate complementary skills

### 5. Handle Constraints

- Work within team size limits
- Respect availability constraints
- Consider budget or cost factors
- Adapt to project timeline

## Response Templates

### Template 1: Role-Based Team

```
**Team Formation for [Project Name]**

**Team Structure:**
- Total Members: [N]
- Roles: [Role 1], [Role 2], [Role 3]

**Team Composition:**

**[Role 1]**: [Expert Name] (Seniority: [Level])
- Skills: [Skills]
- Technologies: [Technologies]
- Relevant Projects: [Projects]
- Why Selected: [Explanation]

**[Role 2]**: [Expert Name] (Seniority: [Level])
- Skills: [Skills]
- Technologies: [Technologies]
- Relevant Projects: [Projects]
- Why Selected: [Explanation]

**Team Strengths:**
- [Strength 1]: [Explanation]
- [Strength 2]: [Explanation]
- [Strength 3]: [Explanation]

**Collaboration:**
[If experts worked together: "These experts have collaborated on [Project], ensuring good team chemistry."]
```

### Template 2: Skill-Based Team

```
**Team Formation Based on Required Skills**

**Required Skills:** [Skills]
**Technologies:** [Technologies]

**Team Members:**

1. **[Expert Name]** (Seniority: [Level])
   - Covers Skills: [Skills]
   - Technologies: [Technologies]
   - Relevant Experience: [Projects]

2. **[Expert Name]** (Seniority: [Level])
   - Covers Skills: [Skills]
   - Technologies: [Technologies]
   - Relevant Experience: [Projects]

**Skill Coverage:**
- [Skill 1]: Covered by [Expert(s)]
- [Skill 2]: Covered by [Expert(s)]
- [Skill 3]: Covered by [Expert(s)]

**Team Balance:**
[Explanation of how team balances skills, seniority, and experience]
```

### Template 3: Project-Based Team

```
**Team Formation Based on Similar Project**

**Reference Project:** [Project Name]
**Team from Similar Project:**

1. **[Expert Name]** (Role: [Role])
   - Skills: [Skills]
   - Experience: [Projects]

2. **[Expert Name]** (Role: [Role])
   - Skills: [Skills]
   - Experience: [Projects]

**Why This Team:**
- Proven collaboration on [Project]
- Relevant experience with [Technologies]
- Successful delivery of [Project Type]

**Adaptations for New Project:**
[Any adjustments needed for the new project]
```

## Common Scenarios

### Scenario 1: Form Team for New Project

```
User: "Form a team for a microservices project with Java, Spring Boot, PostgreSQL, AWS"

→ Analyze: Extract requirements
→ Define Roles: Backend, DevOps, etc.
→ Match: Find experts for each role
→ Compose: Form balanced team
→ Present: Team composition with justification
```

### Scenario 2: Find Complementary Experts

```
User: "Find experts to complement John Doe's skills"

→ Get Profile: getExpertProfile("John Doe")
→ Analyze: Identify skill gaps
→ Match: Find experts with complementary skills
→ Compose: Form team around John Doe
→ Present: Team with complementary expertise
```

### Scenario 3: Reuse Successful Team

```
User: "Form a team similar to the E-commerce Platform project"

→ Get Team: getProjectExperts("E-commerce Platform")
→ Analyze: Extract team composition and skills
→ Match: Find similar experts or reuse team
→ Present: Team based on successful project
```

## Integration with Other Skills

### With RFP Response Generation

Combine team formation with RFP:

```
User: "Generate RFP response with team"
→ Form Team: Use team-formation skill
→ Generate RFP: Use rfp-response-generation skill
→ Present: Complete RFP with team composition
```

### With Expert Matching

Use expert matching for team formation:

```
User: "Form team with Java and Spring Boot experts"
→ Match Experts: Use expert-matching-hybrid-retrieval skill
→ Compose Team: Use team-formation skill
→ Present: Team composition
```

## References

- See `references/team-formation-guide.md` for detailed team formation strategies
- See `references/collaboration-patterns.md` for collaboration-based team formation
- See `assets/example-teams.md` for example team compositions
