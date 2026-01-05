# Test Data Generation Flow with Graph

## Overview

This document describes the complete test data generation flow when calling
`POST /api/v1/test-data/complete?size={tiny|small|medium|large|huge}`. The flow consists of three main phases executed
sequentially.

## High-Level Flow

```
POST /api/v1/test-data/complete?size=small
    ↓
IngestionController.generateCompleteDataset()
    ↓
┌─────────────────────────────────────────────────────────┐
│ Phase 1: Test Data Generation                           │
│ testDataGenerator.generateTestData(size)                │
└─────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────┐
│ Phase 2: Embedding Generation                           │
│ testDataGenerator.generateEmbeddings()                  │
└─────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────┐
│ Phase 3: Graph Construction                             │
│ graphBuilderService.buildGraph()                        │
└─────────────────────────────────────────────────────────┘
    ↓
Complete Dataset Ready
```

---

## Phase 1: Test Data Generation

**Method**: `TestDataGenerator.generateTestData(String size)`

**Note**: This phase also includes ingestion of expert profiles from JSON files (e.g., `siarhei-berdachuk-profile.json`)
using the JSON batch ingestion service. See Section "JSON Profile Ingestion" below for details.

### Step 1.1: Determine Data Volume

Based on `size` parameter:

- **tiny**: 5 employees, 5 projects, ~15 work experiences (3 per employee)
- **small**: 50 employees, 100 projects, ~250 work experiences (5 per employee) - default
- **medium**: 500 employees, 1,000 projects, ~4,000 work experiences (8 per employee)
- **large**: 2,000 employees, 4,000 projects, ~20,000 work experiences (10 per employee)
- **huge**: 50,000 employees, 100,000 projects, ~750,000 work experiences (15 per employee)

### Step 1.2: Generate Technology Catalog

**Method**: `generateTechnologies()`

- Creates normalized technology catalog in `expertmatch.technology` table
- Inserts ~20 base technologies (Java, Spring Boot, Python, React, etc.)
- Each technology includes:

      - `id`: MongoDB-compatible 24-char hex ID
    - `name`: Normalized technology name
    - `category`: Technology category (e.g., "Backend", "Frontend", "Database")
    - `normalized_name`: Lowercase normalized version for matching
    - `synonyms`: Array of alternative names

**Database Table**: `expertmatch.technology`

### Step 1.3: Generate Projects

**Method**: `generateProjects(int projectCount)`

- Generates project records with:

      - `id`: External system format (19-digit numeric string)
    - `name`: Generated using Datafaker (e.g., "Innovative Banking Platform")
    - `summary`: Project description
    - `project_type`: Random from predefined types (Web Application, Microservices, ETL Pipeline, etc.)
    - `technologies`: Array of technologies used in the project (randomly selected)

- Returns `Map<String, String>` mapping project names to project IDs

**Database Table**: `expertmatch.project`

### Step 1.4: Generate Employees

**Method**: `generateEmployees(int employeeCount)`

- Generates employee records with:

      - `id`: External system format (19-digit numeric string)
    - `name`: Generated using Datafaker (first + last name)
    - `email`: Unique email address (Datafaker-generated)
    - `seniority`: Random from levels A1-A5, B1-B3, C1-C2
    - `english_level`: Random from A1-C2

**Database Table**: `expertmatch.employee`

### Step 1.5: Generate Work Experience

**Method**: `generateWorkExperience(int employeeCount, int workExpPerEmployee, Map<String, String> projects)`

For each employee, generates multiple work experience records:

- **Basic Fields**:

      - `id`: MongoDB-compatible 24-char hex ID
    - `employee_id`: References employee
    - `project_id`: References project (or null if project doesn't exist)
    - `project_name`: Project name
    - `project_summary`: Project description
    - `role`: Job role (e.g., "Backend Developer", "Frontend Developer")
    - `start_date` / `end_date`: Date range (typically 1-3 years)
    - `technologies`: Array of technologies used
    - `responsibilities`: Generated responsibilities text
    - `customer_name`: Customer name (Datafaker-generated company name)
    - `industry`: Industry domain (e.g., "Banking", "E-commerce", "Healthcare")

- **Metadata JSONB** (CSV-aligned structure):
  ```json
  {
    "company": "Company Name",
    "company_url": "https://...",
    "is_company_internal": true/false,
    "team": "Team Name",
    "tools": "Comma-separated tools",
    "tools_ref": [{"name": "Git", "category": "Version Control"}],
    "technologies_ref": [{"name": "Java", "category": "Backend"}],
    "customer_description": "Customer description",
    "position": "Job position",
    "project_role": "Primary role",
    "primary_project_role": {"role": "...", "participation": "..."},
    "extra_project_roles": [...],
    "all_project_roles": [...],
    "participation": "Participation description",
    "project_description": "Project description"
  }
  ```

**Database Table**: `expertmatch.work_experience`

**Note**: Embeddings are NOT generated at this stage (they come in Phase 2).

---

## Phase 2: Embedding Generation

**Method**: `TestDataGenerator.generateEmbeddings()`

### Step 2.1: Query Work Experience Records

- Queries all `work_experience` records where `embedding IS NULL`
- Retrieves:

      - `id`: Work experience record ID
    - `project_summary`: Project description
    - `responsibilities`: Responsibilities text
    - `technologies`: Array of technologies

### Step 2.2: Build Embedding Text

For each record, builds text from:

1. `project_summary` (if present)
2. `responsibilities` (if present)
3. `technologies` (formatted as "Technologies: Java, Spring Boot, ...")

**Example**:

```
"Built a microservices banking platform. Developed REST APIs using Spring Boot. Technologies: Java, Spring Boot, PostgreSQL, Docker"
```

### Step 2.3: Generate Embedding Vector

- Calls `EmbeddingService.generateEmbedding(text)`
- Uses configured embedding model (OpenAI, DIAL, Ollama, etc.)
- Returns vector of floats (typically 1024 or 1536 dimensions)

### Step 2.4: Normalize and Store Embedding

- Normalizes embedding to 1536 dimensions (database schema requirement):

      - If 1024-dim: pads with zeros to 1536
    - If 1536-dim: uses as-is
    - If other: pads or truncates as needed
- Updates `work_experience` table:

      - `embedding`: Vector stored as PostgreSQL `vector` type
    - `embedding_dimension`: Original dimension (1024 or 1536)

**Database Fields**: `expertmatch.work_experience.embedding`, `embedding_dimension`

**Progress Logging**: Logs progress every 100 records with:

- Processed count / total count
- Success / failed counts
- Items per second
- Average embedding generation time

---

## Phase 3: Graph Construction

**Method**: `GraphBuilderService.buildGraph()`

### Step 3.1: Ensure Graph Exists

- Checks if `expertmatch_graph` exists in Apache AGE
- Creates graph if it doesn't exist using `ag_catalog.create_graph()`

### Step 3.2: Create Vertices

Creates graph vertices in this order:

#### 3.2.1: Expert Vertices

- **Source**: `expertmatch.employee` table
- **Vertex Type**: `Expert`
- **Properties**:

      - `id`: Employee ID (external system format)
    - `name`: Employee name
    - `email`: Employee email
    - `seniority`: Seniority level
- **Cypher**: `CREATE (e:Expert {id: $id, name: $name, email: $email, seniority: $seniority})`

#### 3.2.2: Project Vertices

- **Source**: `expertmatch.project` table
- **Vertex Type**: `Project`
- **Properties**:

      - `id`: Project ID (external system format)
    - `name`: Project name
    - `summary`: Project summary
    - `project_type`: Project type
- **Cypher**: `CREATE (p:Project {id: $id, name: $name, summary: $summary, project_type: $project_type})`

#### 3.2.3: Technology Vertices

- **Source**: `expertmatch.technology` table
- **Vertex Type**: `Technology`
- **Properties**:

      - `id`: Technology ID (MongoDB-compatible)
    - `name`: Technology name
    - `category`: Technology category
    - `normalized_name`: Normalized name for matching
- **Cypher**: `CREATE (t:Technology {id: $id, name: $name, category: $category, normalized_name: $normalized_name})`

#### 3.2.4: Domain Vertices

- **Source**: Distinct industries from `expertmatch.work_experience.industry`
- **Vertex Type**: `Domain`
- **Properties**:

      - `id`: Generated MongoDB-compatible ID
    - `name`: Industry/domain name (e.g., "Banking", "E-commerce")
- **Cypher**: `CREATE (d:Domain {id: $id, name: $name})`

#### 3.2.5: Customer Vertices

- **Source**: Distinct customer names from `expertmatch.work_experience.customer_name`
- **Vertex Type**: `Customer`
- **Properties**:

      - `id`: Generated customer ID (external system format, or generated if null)
    - `name`: Customer name
- **Cypher**: `MERGE (c:Customer {id: $id}) SET c.name = $name`

### Step 3.3: Create Graph Indexes

Creates GIN indexes on JSONB properties for efficient property queries:

- Expert properties
- Project properties
- Technology properties
- Domain properties
- Customer properties

**Note**: Apache AGE automatically creates indexes on `id` columns.

### Step 3.4: Create Relationships

Creates graph relationships using batch `UNWIND` operations:

#### 3.4.1: Expert-Project Relationships

- **Relationship Type**: `PARTICIPATED_IN`
- **Source**: `expertmatch.work_experience` table
- **Direction**: `Expert -[:PARTICIPATED_IN]-> Project`
- **Properties**:

      - `role`: Role in project
    - `start_date`: Start date
    - `end_date`: End date
- **Cypher**:
  `MATCH (e:Expert {id: $expertId}), (p:Project {id: $projectId}) CREATE (e)-[:PARTICIPATED_IN {role: $role, start_date: $startDate, end_date: $endDate}]->(p)`

#### 3.4.2: Expert-Customer Relationships

- **Relationship Type**: `WORKED_FOR`
- **Source**: `expertmatch.work_experience` table (where `customer_name` is not null)
- **Direction**: `Expert -[:WORKED_FOR]-> Customer`
- **Cypher**: `MATCH (e:Expert {id: $expertId}), (c:Customer {id: $customerId}) CREATE (e)-[:WORKED_FOR]->(c)`

#### 3.4.3: Project-Technology Relationships

- **Relationship Type**: `USES`
- **Source**: `expertmatch.project.technologies` array
- **Direction**: `Project -[:USES]-> Technology`
- **Cypher**: `MATCH (p:Project {id: $projectId}), (t:Technology {id: $techId}) CREATE (p)-[:USES]->(t)`

#### 3.4.4: Project-Domain Relationships

- **Relationship Type**: `IN_DOMAIN`
- **Source**: `expertmatch.work_experience` (distinct project-industry pairs)
- **Direction**: `Project -[:IN_DOMAIN]-> Domain`
- **Cypher**: `MATCH (p:Project {id: $projectId}), (d:Domain {name: $domainName}) CREATE (p)-[:IN_DOMAIN]->(d)`

#### 3.4.5: Project-Customer Relationships

- **Relationship Type**: `FOR_CUSTOMER`
- **Source**: `expertmatch.work_experience` (distinct project-customer pairs)
- **Direction**: `Project -[:FOR_CUSTOMER]-> Customer`
- **Cypher**: `MATCH (p:Project {id: $projectId}), (c:Customer {id: $customerId}) CREATE (p)-[:FOR_CUSTOMER]->(c)`

### Step 3.5: Logging and Summary

Logs execution times for:

- Total graph build time
- Vertices creation time
- Relationships creation time
- Individual vertex/relationship type creation times

---

## Data Flow Summary

```
┌─────────────────────────────────────────────────────────────┐
│ Phase 1: Test Data Generation                               │
├─────────────────────────────────────────────────────────────┤
│ 1. Technology Catalog → expertmatch.technology             │
│ 2. Projects → expertmatch.project                          │
│ 3. Employees → expertmatch.employee                       │
│ 4. Work Experience → expertmatch.work_experience           │
│    (with metadata JSONB, but NO embeddings yet)            │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Phase 2: Embedding Generation                               │
├─────────────────────────────────────────────────────────────┤
│ 1. Query work_experience WHERE embedding IS NULL           │
│ 2. Build text from project_summary + responsibilities + tech│
│ 3. Generate embedding vector (1024 or 1536 dim)             │
│ 4. Normalize to 1536 dimensions                             │
│ 5. Update work_experience.embedding and embedding_dimension │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ Phase 3: Graph Construction                                 │
├─────────────────────────────────────────────────────────────┤
│ 1. Create Graph (if not exists)                             │
│ 2. Create Vertices:                                         │
│    - Expert (from employee)                                 │
│    - Project (from project)                                │
│    - Technology (from technology)                           │
│    - Domain (from work_experience.industry)                 │
│    - Customer (from work_experience.customer_name)          │
│ 3. Create Indexes (GIN on JSONB properties)                 │
│ 4. Create Relationships:                                    │
│    - Expert -[:PARTICIPATED_IN]-> Project                   │
│    - Expert -[:WORKED_FOR]-> Customer                       │
│    - Project -[:USES]-> Technology                          │
│    - Project -[:IN_DOMAIN]-> Domain                         │
│    - Project -[:FOR_CUSTOMER]-> Customer                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
              Complete Dataset Ready
```

---

## Database Tables Involved

1. **`expertmatch.technology`**: Technology catalog
2. **`expertmatch.project`**: Project records
3. **`expertmatch.employee`**: Employee records
4. **`expertmatch.work_experience`**: Work experience records with embeddings
5. **Apache AGE Graph**: `expertmatch_graph` with vertices and relationships

---

## Key Features

### LLM-Based Constant Expansion (Optional)

If `ConstantExpansionService` is enabled (via `expertmatch.ingestion.constant-expansion.enabled=true`):

- Technologies, tools, project types, team names are expanded using LLM
- Expansion happens **lazily** on first call to any getter method
- Results are cached for subsequent calls
- Falls back to base constants if LLM expansion fails

### Idempotency

- **Technology Catalog**: Uses `INSERT ... ON CONFLICT DO NOTHING`
- **Projects/Employees**: Uses external system IDs (19-digit numeric strings)
- **Work Experience**: Uses MongoDB-compatible IDs (24-char hex)
- **Graph Vertices**: Uses `MERGE` for Customer (on `id`), `CREATE` for others
- **Graph Relationships**: Uses `MERGE` to prevent duplicates

### Performance Optimizations

- **Batch Operations**: Graph relationships created using `UNWIND` for batch processing
- **Progress Logging**: Embedding generation logs progress every 100 records
- **Index Creation**: Graph indexes created after vertices exist
- **Vector Normalization**: Embeddings normalized to 1536 dimensions for consistency

---

## Example: Small Dataset Generation

For `size=small`:

- **10 employees** created
- **10 projects** created
- **~50 work experiences** created (5 per employee)
- **~50 embeddings** generated (one per work experience)
- **Graph**: 10 Expert vertices, 10 Project vertices, ~20 Technology vertices, ~5 Domain vertices, ~5 Customer vertices
- **Relationships**: ~50 PARTICIPATED_IN, ~50 WORKED_FOR, ~50 USES, ~10 IN_DOMAIN, ~10 FOR_CUSTOMER

---

## Error Handling

- **Technology Generation**: Warnings logged for duplicate technologies
- **Project Generation**: Warnings logged for failed project inserts
- **Embedding Generation**: Failed embeddings are logged but don't stop the process
- **Graph Building**: Graph creation errors are handled gracefully (e.g., graph already exists)

---

---

## JSON Profile Ingestion

### Overview

In addition to synthetic test data generation, the system supports ingestion of expert profiles from JSON files. This
allows importing real-world expert data with structured project history.

### JSON Profile Format

Profiles can be provided in two formats:

1. **Array Format** (multiple profiles):

```json
[
  {
    "employee": {
      "id": "...",
      "name": "..."
    },
    "projects": [
      ...
    ]
  },
  {
    "employee": {
      "id": "...",
      "name": "..."
    },
    "projects": [
      ...
    ]
  }
]
```

2. **Single Object Format** (backward compatible):

```json
{
  "employee": {
    "id": "...",
    "name": "..."
  },
  "projects": [
    ...
  ]
}
```

### Ingestion Flow

```
POST /api/v1/ingestion/json-profiles?directory=classpath:data
    ↓
JsonProfileIngestionService.ingestFromDirectory()
    ↓
┌─────────────────────────────────────────────────────────┐
│ For each JSON file:                                     │
│ 1. JsonProfileParser.parseProfilesFromResource()        │
│    - Detects array vs single object format              │
│    - Parses into List<EmployeeProfile>                  │
└─────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────┐
│ For each EmployeeProfile:                                │
│ 2. ProfileProcessor.processProfile()                    │
│    - Validates profile (required: id, name)             │
│    - Applies defaults for missing optional fields        │
│    - Inserts/updates employee record                     │
│    - Creates work experience records for each project   │
│    - Returns ProcessingResult                           │
└─────────────────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────────────────┐
│ 3. Aggregate Results                                     │
│    - Collects all ProcessingResult objects              │
│    - Calculates success/error counts                    │
│    - Returns IngestionResult                            │
└─────────────────────────────────────────────────────────┘
```

### Partial Data Handling

The system gracefully handles missing optional fields:

- **Employee fields**: email, seniority, languageEnglish, availabilityStatus → defaults applied
- **Profile fields**: summary, projects → optional (employee-only profiles supported)
- **Project fields**: projectCode, endDate, technologies, responsibilities, industry → defaults applied
- **Required fields**: Employee `id` and `name`, Project `projectName` and `startDate`

### Integration with Test Data Generation

JSON profiles are automatically ingested during test data generation:

- `TestDataGenerator.generateTestData()` includes `generateSiarheiBerdachukData()`
- Uses `JsonProfileParser` to load profile from `classpath:data/siarhei-berdachuk-profile.json`
- Uses shared model classes for consistency

### API Usage

**Ingest from directory:**

```bash
POST /api/v1/ingestion/json-profiles?directory=classpath:data
```

**Ingest from single file:**

```bash
POST /api/v1/ingestion/json-profiles?file=classpath:data/siarhei-berdachuk-profile.json
```

**Default (classpath:data):**

```bash
POST /api/v1/ingestion/json-profiles
```

### Error Recovery

- **Per-profile errors**: Logged, included in result, processing continues
- **Per-project errors**: Logged, included in result, processing continues
- **Invalid JSON**: File skipped, error logged, other files continue
- **Database errors**: Record skipped, error logged, processing continues

All errors are included in the `IngestionResult` response for reporting.

---

## Last Updated

2026-01-04 (Added JSON Profile Ingestion section)

