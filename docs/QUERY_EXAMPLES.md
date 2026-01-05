# ExpertMatch Query Examples for Manual Testing

This document provides a comprehensive list of query examples for manual testing of the ExpertMatch system.

**Note**:

- These examples are also available via the API endpoint `GET /api/v1/query/examples` (public endpoint, no
  authentication required)
- Examples can be accessed through the UI's "Examples" button next to the query input field
- Examples are loaded from `src/main/resources/query-examples.json` resource file
- Examples are cached after first load for better performance
- To add or modify examples, edit the `query-examples.json` file and restart the service

## Table of Contents

1. [Basic Expert Search Queries](#basic-expert-search-queries)
2. [Technology-Specific Queries](#technology-specific-queries)
3. [Seniority Level Queries](#seniority-level-queries)
4. [Language Requirements](#language-requirements)
5. [Team Formation Queries](#team-formation-queries)
6. [RFP Response Queries](#rfp-response-queries)
7. [Complex Multi-Criteria Queries](#complex-multi-criteria-queries)
8. [SGR Pattern Queries](#sgr-pattern-queries)
9. [Edge Cases and Boundary Conditions](#edge-cases-and-boundary-conditions)
10. [Testing Different Options](#testing-different-options)
11. [cURL Examples](#curl-examples)
12. [Testing Checklist](#testing-checklist)
13. [Notes](#notes)

---

## Basic Expert Search Queries

### Simple Technology Query

```json
{
  "query": "Looking for experts in Java and Spring Boot",
  "options": {
    "maxResults": 10,
    "minConfidence": 0.7
  }
}
```

### Multiple Technologies

```json
{
  "query": "Find experts with experience in Java, Spring Boot, and AWS",
  "options": {
    "maxResults": 15,
    "minConfidence": 0.6
  }
}
```

### Skills-Based Query

```json
{
  "query": "Need experts with microservices architecture and cloud deployment skills",
  "options": {
    "maxResults": 10
  }
}
```

### Domain-Specific Query

```json
{
  "query": "Looking for experts in financial services domain with banking experience",
  "options": {
    "maxResults": 10
  }
}
```

---

## Technology-Specific Queries

### Backend Technologies

```json
{
  "query": "Find Java developers with Spring Boot, Hibernate, and PostgreSQL experience",
  "options": {
    "maxResults": 10
  }
}
```

### Frontend Technologies

```json
{
  "query": "Looking for React and TypeScript experts with Next.js experience",
  "options": {
    "maxResults": 10
  }
}
```

### Cloud and DevOps

```json
{
  "query": "Need AWS experts with Kubernetes, Docker, and CI/CD pipeline experience",
  "options": {
    "maxResults": 15
  }
}
```

### Data Technologies

```json
{
  "query": "Find experts in Apache Kafka, Spark, and data streaming architectures",
  "options": {
    "maxResults": 10
  }
}
```

### Mobile Development

```json
{
  "query": "Looking for iOS and Android developers with Swift and Kotlin experience",
  "options": {
    "maxResults": 10
  }
}
```

### Full Stack

```json
{
  "query": "Need full-stack developers with Node.js, React, and MongoDB experience",
  "options": {
    "maxResults": 10
  }
}
```

---

## Seniority Level Queries

### Senior Level

```json
{
  "query": "Looking for senior Java developers with 10+ years of experience",
  "options": {
    "maxResults": 10
  }
}
```

### Lead/Architect Level

```json
{
  "query": "Find lead architects with microservices and cloud architecture experience",
  "options": {
    "maxResults": 5
  }
}
```

### Mid-Level

```json
{
  "query": "Need mid-level developers with 3-5 years of Spring Boot experience",
  "options": {
    "maxResults": 15
  }
}
```

### Junior Level

```json
{
  "query": "Looking for junior developers with Java and Spring Boot basics",
  "options": {
    "maxResults": 20
  }
}
```

---

## Language Requirements

### English Proficiency

```json
{
  "query": "Find Java experts with fluent English (C1 level or higher)",
  "options": {
    "maxResults": 10
  }
}
```

### Multiple Languages

```json
{
  "query": "Looking for developers with English and German language skills",
  "options": {
    "maxResults": 10
  }
}
```

---

## Team Formation Queries

### Basic Team Formation

```json
{
  "query": "I need to form a team for a microservices project",
  "options": {
    "maxResults": 20
  }
}
```

### Specific Team Roles

```json
{
  "query": "Form a team with backend developers, frontend developers, and DevOps engineers for a cloud migration project",
  "options": {
    "maxResults": 15
  }
}
```

### Team with Technologies

```json
{
  "query": "Build a team for a Spring Boot and React application with AWS infrastructure",
  "options": {
    "maxResults": 20
  }
}
```

### Agile Team Formation

```json
{
  "query": "Need to form an agile team with scrum master, developers, and QA engineers",
  "options": {
    "maxResults": 15
  }
}
```

---

## RFP Response Queries

### Basic RFP Query

```json
{
  "query": "I need experts for an RFP response for a banking system modernization project",
  "options": {
    "maxResults": 10
  }
}
```

### RFP with Specific Requirements

```json
{
  "query": "RFP response team needed for cloud migration project requiring AWS, Kubernetes, and microservices expertise",
  "options": {
    "maxResults": 15
  }
}
```

---

## Complex Multi-Criteria Queries

### Technology + Domain + Seniority

```json
{
  "query": "Find senior Java developers with financial services domain experience and Spring Boot expertise",
  "options": {
    "maxResults": 10,
    "minConfidence": 0.8
  }
}
```

### Multiple Skills + Language

```json
{
  "query": "Looking for full-stack developers with React, Node.js, and MongoDB experience, fluent in English",
  "options": {
    "maxResults": 10
  }
}
```

### Project-Specific Requirements

```json
{
  "query": "Need experts for a microservices architecture project with Kubernetes, Spring Boot, event-driven patterns, and AWS cloud infrastructure",
  "options": {
    "maxResults": 15,
    "minConfidence": 0.7
  }
}
```

### Industry + Technology Combination

```json
{
  "query": "Find healthcare domain experts with HIPAA compliance experience and modern cloud technologies",
  "options": {
    "maxResults": 10
  }
}
```

---

## SGR Pattern Queries

### Deep Research Pattern

```json
{
  "query": "Find experts for a complex microservices architecture project with Kubernetes, Spring Boot, and event-driven patterns",
  "options": {
    "maxResults": 20,
    "minConfidence": 0.7,
    "deepResearch": true
  }
}
```

### Cascade Pattern (Structured Expert Evaluation)

```json
{
  "query": "Looking for senior Java developers with Spring Boot and AWS experience",
  "options": {
    "maxResults": 5,
    "useCascadePattern": true
  }
}
```

### Routing Pattern (LLM-Based Classification)

```json
{
  "query": "I need to form a team for a cloud migration project",
  "options": {
    "maxResults": 15,
    "useRoutingPattern": true
  }
}
```

### Cycle Pattern (Multiple Expert Evaluations)

```json
{
  "query": "Find the best Java experts with Spring Boot and microservices experience",
  "options": {
    "maxResults": 10,
    "useCyclePattern": true
  }
}
```

### Combined SGR Patterns

```json
{
  "query": "Find experts for a complex microservices project with Kubernetes and Spring Boot",
  "options": {
    "maxResults": 15,
    "deepResearch": true,
    "useRoutingPattern": true,
    "useCascadePattern": true
  }
}
```

---

## Edge Cases and Boundary Conditions

### Very Short Query

```json
{
  "query": "Java experts",
  "options": {
    "maxResults": 10
  }
}
```

### Very Long Query

```json
{
  "query": "Looking for highly experienced senior software architects with extensive background in enterprise Java development, specifically Spring Framework ecosystem including Spring Boot, Spring Cloud, Spring Security, and Spring Data. Must have deep expertise in microservices architecture patterns, RESTful API design, event-driven architectures, and distributed systems. Should be proficient in cloud platforms especially AWS with services like EC2, S3, RDS, Lambda, API Gateway, and ECS/EKS. Experience with containerization using Docker and orchestration with Kubernetes is essential. Knowledge of CI/CD pipelines, DevOps practices, monitoring and observability tools, database design with both SQL and NoSQL databases, caching strategies, message queues like Kafka or RabbitMQ, and security best practices is required. Domain experience in financial services or healthcare is preferred. Must have excellent communication skills and ability to lead technical discussions.",
  "options": {
    "maxResults": 10
  }
}
```

### Maximum Results

```json
{
  "query": "Find Java developers",
  "options": {
    "maxResults": 100
  }
}
```

### Minimum Results

```json
{
  "query": "Find senior Java architects",
  "options": {
    "maxResults": 1
  }
}
```

### High Confidence Threshold

```json
{
  "query": "Find Java experts with Spring Boot",
  "options": {
    "maxResults": 10,
    "minConfidence": 0.95
  }
}
```

### Low Confidence Threshold

```json
{
  "query": "Find developers",
  "options": {
    "maxResults": 20,
    "minConfidence": 0.3
  }
}
```

### No Options (Default Values)

```json
{
  "query": "Looking for Java experts with Spring Boot experience"
}
```

### With Chat ID (Conversation Context)

```json
{
  "query": "Find more experts similar to the previous ones",
  "chatId": "507f1f77bcf86cd799439011",
  "options": {
    "maxResults": 10
  }
}
```

---

## Testing Different Options

### Without Sources

```json
{
  "query": "Find Java experts",
  "options": {
    "maxResults": 10,
    "includeSources": false
  }
}
```

### Without Entities

```json
{
  "query": "Find Java experts",
  "options": {
    "maxResults": 10,
    "includeEntities": false
  }
}
```

### Without Reranking

```json
{
  "query": "Find Java experts",
  "options": {
    "maxResults": 10,
    "rerank": false
  }
}
```

### Minimal Options

```json
{
  "query": "Find Java experts",
  "options": {
    "maxResults": 5,
    "includeSources": false,
    "includeEntities": false,
    "rerank": false
  }
}
```

### Maximum Options Enabled

```json
{
  "query": "Find experts for a complex microservices project",
  "options": {
    "maxResults": 20,
    "minConfidence": 0.7,
    "includeSources": true,
    "includeEntities": true,
    "rerank": true,
    "deepResearch": true,
    "useCascadePattern": true,
    "useRoutingPattern": true,
    "useCyclePattern": true
  }
}
```

---

## cURL Examples

**Note**: All examples include user authentication headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`). These headers
are now documented in the OpenAPI specification and will appear in Swagger UI. For local development without Spring
Gateway, you can set these headers manually.

### Basic Query

```bash
curl -X 'POST' \
  -H 'X-User-Id: user-123' \
  -H 'X-User-Roles: ROLE_USER' \
  -H 'X-User-Email: user@example.com' \
  'http://localhost:8093/api/v1/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "query": "Looking for experts in Java, Spring Boot, and AWS",
  "options": {
    "maxResults": 10,
    "minConfidence": 0.7
  }
}'
```

### With Deep Research

```bash
curl -X 'POST' \
  'http://localhost:8093/api/v1/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user-123' \
  -H 'X-User-Roles: ROLE_USER' \
  -H 'X-User-Email: user@example.com' \
  -d '{
  "query": "Find experts for a microservices architecture project with Kubernetes and Spring Boot",
  "options": {
    "maxResults": 20,
    "deepResearch": true
  }
}'
```

### Team Formation Query

```bash
curl -X 'POST' \
  'http://localhost:8093/api/v1/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user-123' \
  -H 'X-User-Roles: ROLE_USER' \
  -H 'X-User-Email: user@example.com' \
  -d '{
  "query": "I need to form a team for a cloud migration project",
  "options": {
    "maxResults": 15,
    "useRoutingPattern": true
  }
}'
```

### With Chat Context

```bash
curl -X 'POST' \
  'http://localhost:8093/api/v1/query' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: user-123' \
  -H 'X-User-Roles: ROLE_USER' \
  -H 'X-User-Email: user@example.com' \
  -d '{
  "query": "Find more experts similar to the previous ones",
  "chatId": "507f1f77bcf86cd799439011",
  "options": {
    "maxResults": 10
  }
}'
```

---

## Testing Checklist

### Basic Functionality

- [ ] Simple technology query returns results
- [ ] Multiple technologies query works
- [ ] Seniority level filtering works
- [ ] Language requirements are considered
- [ ] Response includes answer, experts, sources, and entities

### Query Types

- [ ] Expert search queries work
- [ ] Team formation queries are classified correctly
- [ ] RFP response queries are classified correctly
- [ ] Intent classification is accurate

### Options Testing

- [ ] `maxResults` limits results correctly
- [ ] `minConfidence` filters low-confidence results
- [ ] `includeSources` includes/excludes sources
- [ ] `includeEntities` includes/excludes entities
- [ ] `rerank` affects result ordering
- [ ] `deepResearch` enables deep research pattern
- [x] `useCascadePattern` enables cascade pattern (✅ Available in OpenAPI spec as of 2025-12-21)
- [x] `useRoutingPattern` enables routing pattern (✅ Available in OpenAPI spec as of 2025-12-21)
- [x] `useCyclePattern` enables cycle pattern (✅ Available in OpenAPI spec as of 2025-12-21)
- [ ] `includeExecutionTrace` includes execution trace in response

### SGR Patterns

- [ ] Deep research returns more comprehensive results
- [ ] Cascade pattern provides structured evaluation
- [ ] Routing pattern correctly classifies queries
- [ ] Cycle pattern evaluates multiple experts
- [ ] Combined patterns work together

### Edge Cases

- [ ] Very short queries work
- [ ] Very long queries (up to 5000 chars) work
- [ ] Maximum results (100) works
- [ ] Minimum results (1) works
- [ ] High confidence threshold filters correctly
- [ ] Low confidence threshold includes more results
- [ ] Default options are applied when not specified
- [ ] Chat context is used when provided

### Error Handling

- [ ] Empty query returns 400 error
- [ ] Query exceeding 5000 chars returns 400 error
- [ ] Invalid chatId format returns 400 error
- [ ] Invalid options values return 400 error
- [ ] Missing required fields return 400 error

---

## Notes

- All queries should be tested with proper authentication headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`)
- **User authentication headers are now documented in the OpenAPI specification** and will appear in Swagger UI for all
  endpoints
- Base URL: `http://localhost:8093/api/v1` (adjust for your environment)
- Default options are applied if not specified:

      - `maxResults`: 10
    - `minConfidence`: 0.7
    - `includeSources`: true
    - `includeEntities`: true
    - `rerank`: true
    - `deepResearch`: false
  - `useCascadePattern`: false (✅ Available in OpenAPI spec as of 2025-12-21)
  - `useRoutingPattern`: false (✅ Available in OpenAPI spec as of 2025-12-21)
  - `useCyclePattern`: false (✅ Available in OpenAPI spec as of 2025-12-21)
  - `includeExecutionTrace`: false
- Chat ID must be a 24-character hexadecimal string if provided
- Query text has a maximum length of 5000 characters

---

**Last Updated**: 2025-12-21  
**Note**: User authentication headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`) are now documented in the OpenAPI
specification and will appear in Swagger UI for all endpoints.

