# ExpertMatch Documentation

Welcome to the ExpertMatch documentation!

## About ExpertMatch

**ExpertMatch** is an enterprise-grade expert discovery and team formation system that matches project requirements with
qualified experts from the company database, enabling rapid RFP responses and optimal team composition.

ExpertMatch leverages a **Hybrid GraphRAG architecture** combining:

- **Vector Similarity Search** (PgVector) - Semantic matching based on project experiences
- **Graph Traversal** (Apache AGE) - Relationship-based discovery
- **Keyword Search** - Traditional text matching
- **Semantic Reranking** - Precision optimization
- **LLM Orchestration** - Natural language answer generation

## Quick Links

### Getting Started

- [Quick Start Guide](QUICK_START.md) - Get up and running quickly

### Core Documentation

- [Product Overview](ExpertMatch.md) - Complete product requirements document
- [Expert Search Flow](ExpertMatch-Expert-Search-Flow.md) - Detailed flow documentation with diagrams
- [Query Processing Flow](QUERY_PROCESSING_FLOW_DETAILED.md) - Step-by-step API query processing
- [SGR Usage](ExpertMatch-SGR-Usage.md) - Schema-Guided Reasoning patterns and deep research
- [SGR Benefits](ExpertMatch-SGR-Benefits.md) - Benefits and value proposition of SGR patterns
- [Smart Tool Selection](SMART_TOOL_SELECTION.md) - Tool Search Tool pattern, 34-64% token savings, dynamic tool
  discovery
- [Apache AGE Benefits](ExpertMatch-Apache-AGE-Benefits.md) - Benefits of graph database capabilities
- [Cypher & Apache AGE Tutorial](CYPHER_APACHE_AGE_TUTORIAL.md) - Comprehensive tutorial on Cypher language and Apache
  AGE graph database

### API Documentation

- [API Endpoints](API_ENDPOINTS.md) - Complete API reference
- [Authorization Guide](AUTHORIZATION_GUIDE.md) - Authentication and authorization
- [Swagger UI Testing Guide](SWAGGER_UI_TESTING_GUIDE.md) - Testing with Swagger UI

### Configuration

- [Embedding Model Recommendation](EMBEDDING_MODEL_RECOMMENDATION.md) - Model selection guide
- [Reranking Models Report](RERANKING_MODELS_REPORT.md) - Reranking model analysis
- [Citus Configuration](CITUS_CONFIGURATION.md) - Database scaling configuration
- [Conversation History Management](CONVERSATION_HISTORY_MANAGEMENT.md) - Token counting and summarization
- [Smart Tool Selection](SMART_TOOL_SELECTION.md) - Tool Search Tool pattern, 34-64% token savings, dynamic tool
  discovery

### Data Ingestion

- [Test Data Generation Flow](TEST_DATA_GENERATION_FLOW.md) - Complete test data generation and JSON profile ingestion
  flow
- [Ingestion Endpoints Analysis](INGESTION_ENDPOINTS_ANALYSIS.md) - Analysis of implemented ingestion endpoints
- [JSON Batch Ingestion](JSON_BATCH_INGESTION_PROPOSAL.md) - JSON batch ingestion implementation (completed)

### Development

- [Development Guide](DEVELOPMENT_GUIDE.md) - **Fullstack development setup and workflow**
- [Testing Guide](TESTING.md) - Testing patterns, Testcontainers setup, and best practices
- [Coding Rules](CODING_RULES.md) - Development guidelines and conventions

## Key Features

- ✅ **Expert Matching**: Automatically matches project requirements with qualified experts
- ✅ **SGR Deep Research**: Multi-step iterative retrieval for complex queries with gap analysis and query refinement
- ✅ **SGR Patterns**: Full Schema-Guided Reasoning implementation (Cascade, Routing, Cycle patterns) for structured LLM
  reasoning
- ✅ **Smart Tool Selection**: Tool Search Tool pattern with 34-64% token savings through dynamic tool discovery
- ✅ **RFP Response Support**: Quickly identifies experts for RFP responses
- ✅ **Team Formation**: Recommends optimal team compositions
- ✅ **Hybrid GraphRAG**: Combines vector, graph, and keyword search
- ✅ **Customer Relationships**: Expert-Customer (WORKED_FOR) and Project-Customer (FOR_CUSTOMER) graph relationships
- ✅ **Multi-Source Data Integration**: Work experience, Jira, presale materials
- ✅ **JSON Batch Ingestion**: Import expert profiles from JSON files (array or single object format) with partial data
  support
- ✅ **Conversation History**: Intelligent context management with token counting and summarization
- ✅ **Enterprise Scale**: PostgreSQL with Citus for horizontal scaling

## Architecture Overview

ExpertMatch uses a modern, scalable fullstack architecture:

- **Backend**: Spring Boot 3.5.9, Java 21
- **Frontend**: Thymeleaf (server-side rendering)
- **UI Library**: Bootstrap/Custom CSS
- **Styling**: Tailwind CSS 3.4.3
- **Database**: PostgreSQL 17 with PgVector and Apache AGE
- **AI/ML**: Spring AI 1.1.1 for LLM orchestration
- **Vector Search**: PgVector with HNSW indexing
- **Graph Database**: Apache AGE for relationship traversal
- **Deployment**: Docker Compose for local development

## Documentation Structure

This documentation is organized into several sections:

1. **Overview** - Product overview and benefits
2. **Architecture** - System architecture and flows
3. **Development** - Setup and development guides
4. **Quick Start** - Getting started guides
5. **Configuration** - Configuration and optimization guides
6. **API** - API documentation and testing guides
7. **Migration** - Migration guides and reviews

## Contributing

For development guidelines, see:

- [Development Guide](DEVELOPMENT_GUIDE.md) - **Start here for fullstack development**
- [Coding Rules](CODING_RULES.md)
- [Testing Guide](TESTING.md)

## Support

For questions or issues, please refer to the relevant documentation section or contact the ExpertMatch team.

---

*Last updated: 2026-01-04 (Added JSON Batch Ingestion feature)*

