# Endpoint Naming Proposal: Test Data Generation

## Current Situation

**Current URL**: `/api/v1/ingestion/*`

**Endpoints**:

- `POST /api/v1/ingestion/test-data` - Generate test data
- `POST /api/v1/ingestion/embeddings` - Generate embeddings
- `POST /api/v1/ingestion/build-graph` - Build graph relationships
- `POST /api/v1/ingestion/complete` - Complete pipeline

## Problem Analysis

### Why "ingestion" is not appropriate:

1. **Semantic Mismatch**:

- "Ingestion" typically refers to **importing/loading data from external sources** (Kafka, files, APIs, databases)
    - Test data generation is about **creating synthetic data internally**
    - These are fundamentally different operations

2. **Future Confusion**:

- If real ingestion endpoints are added later (Kafka consumers, file uploads, etc.), they would logically go under
      `/api/v1/ingestion/*`
    - Having test data generation under the same path would be confusing

3. **API Clarity**:

- Users expect "ingestion" to mean "bring data in from outside"
    - Test data generation is "create data internally for testing"

## Proposed Alternatives

### Option 1: `/api/v1/test-data`  **RECOMMENDED**

**Endpoints**:

- `POST /api/v1/test-data?size=small` - Generate test data
- `POST /api/v1/test-data/embeddings` - Generate embeddings
- `POST /api/v1/test-data/graph` - Build graph relationships
- `POST /api/v1/test-data/complete?size=small` - Complete pipeline

**Pros**:

- Clear and direct - immediately obvious what it does
- Common terminology in software development
- No confusion with actual data ingestion
- Simple and concise
- Matches the primary purpose (test data generation)

**Cons**:

- Slightly longer URL for embeddings/graph endpoints

### Option 2: `/api/v1/data-generation`

**Endpoints**:

- `POST /api/v1/data-generation/test-data?size=small`
- `POST /api/v1/data-generation/embeddings`
- `POST /api/v1/data-generation/graph`
- `POST /api/v1/data-generation/complete?size=small`

**Pros**:

- More descriptive
- Clearly indicates data generation (not ingestion)

**Cons**:

- Longer URLs
- "data-generation" is verbose

### Option 3: `/api/v1/setup`

**Endpoints**:

- `POST /api/v1/setup/test-data?size=small`
- `POST /api/v1/setup/embeddings`
- `POST /api/v1/setup/graph`
- `POST /api/v1/setup/complete?size=small`

**Pros**:

- Indicates setup/initialization purpose
- Common in development tools

**Cons**:

- "setup" is vague - could mean configuration, initialization, etc.
- Doesn't clearly indicate test data generation

### Option 4: `/api/v1/admin/test-data`

**Endpoints**:

- `POST /api/v1/admin/test-data?size=small`
- `POST /api/v1/admin/test-data/embeddings`
- `POST /api/v1/admin/test-data/graph`
- `POST /api/v1/admin/test-data/complete?size=small`

**Pros**:

- Emphasizes admin-only nature
- Groups admin operations together

**Cons**:

- If other admin endpoints exist, they might not fit under `/admin`
- Longer URLs

### Option 5: `/api/v1/seed`

**Endpoints**:

- `POST /api/v1/seed?size=small` - Generate test data
- `POST /api/v1/seed/embeddings` - Generate embeddings
- `POST /api/v1/seed/graph` - Build graph
- `POST /api/v1/seed/complete?size=small` - Complete pipeline

**Pros**:

- Common term in software development ("seeding" a database)
- Short and clear

**Cons**:

- "seed" typically means initial data, not regenerating data
- Less clear for embeddings/graph operations

## Recommendation: `/api/v1/test-data` 

### Rationale:

1. **Clarity**: Immediately clear that these endpoints generate test data
2. **Simplicity**: Short, direct, no ambiguity
3. **Standard Practice**: Common pattern in REST APIs
4. **Future-Proof**: Leaves `/ingestion` available for actual data ingestion endpoints
5. **User Experience**: Developers will immediately understand the purpose

### Proposed Endpoint Structure:

```
POST /api/v1/test-data?size={small|medium|large}
  → Generate test data (employees, projects, work experience)

POST /api/v1/test-data/embeddings
  → Generate embeddings for existing work experience records

POST /api/v1/test-data/graph
  → Build graph relationships from existing database data

POST /api/v1/test-data/complete?size={small|medium|large}
  → Complete pipeline: test data + embeddings + graph (recommended)
```

### Alternative Structure (if you prefer flatter):

```
POST /api/v1/test-data?size={small|medium|large}
POST /api/v1/test-data/embeddings
POST /api/v1/test-data/graph
POST /api/v1/test-data/complete?size={small|medium|large}
```

Or even simpler:

```
POST /api/v1/test-data?size={small|medium|large}
POST /api/v1/embeddings
POST /api/v1/graph
POST /api/v1/test-data/complete?size={small|medium|large}
```

But the nested structure (`/test-data/embeddings`, `/test-data/graph`) is clearer because embeddings and graph are
related to test data.

## Migration Impact

If we change from `/ingestion/*` to `/test-data/*`:

1. **OpenAPI Spec**: Update all endpoint paths
2. **Controller**: Update `@RequestMapping` or individual `@PostMapping` paths
3. **Tests**: Update test URLs
4. **Documentation**: Update all references
5. **Postman Collection**: Update endpoint URLs
6. **Frontend/UI**: Update API client calls (if any)

## Comparison Table

| Option                 | Clarity | Brevity | Standard | Future-Proof | Score     |
|------------------------|---------|---------|----------|--------------|-----------|
| `/test-data`           |    |    |     |         | **25/25** |
| `/data-generation`     |     |      |       |          | 16/25     |
| `/setup`               |       |     |       |           | 12/25     |
| `/admin/test-data`     |     |       |       |          | 15/25     |
| `/seed`                |      |    |      |           | 15/25     |
| `/ingestion` (current) |       |     |        |            | 10/25     |

## Conclusion

**Recommendation**: Change from `/api/v1/ingestion/*` to `/api/v1/test-data/*`

This provides:

- Better semantic clarity
- Future-proofing for actual ingestion endpoints
- Improved developer experience
- Standard REST API patterns

