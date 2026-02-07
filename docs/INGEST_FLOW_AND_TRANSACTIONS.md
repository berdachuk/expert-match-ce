# Ingest Flow and Transaction Configuration

## Ingest flow (async: POST /api/v1/ingestion/database/async)

1. **Clear** (if `clear=true`)
    - `graphBuilderService.clearGraph()` – Apache AGE graph (REQUIRES_NEW transaction)
    - `testDataGenerator.clearTestData()` – target DB: work_experience, employees, projects, technologies (single
      @Transactional)
2. **Ingest**
    - `databaseIngestionService.ingestAll(batch)` – reads from external DB, writes to target DB (single @Transactional
      on primary)
3. **Embeddings**
    - `testDataGenerator.generateEmbeddings()` – reads work_experience from target, updates embeddings (no single
      transaction; per-record updates)
4. **Graph**
    - `graphBuilderService.buildGraph()` – reads from target DB, writes to Apache AGE (graph operations use their own
      transactions)

## Data sources

| Step   | Read from               | Write to                    |
|--------|-------------------------|-----------------------------|
| Clear  | -                       | Target DB + Apache AGE      |
| Ingest | External DB (read-only) | Target DB (primary)         |
| Embed  | Target DB               | Target DB (work_experience) |
| Graph  | Target DB               | Apache AGE                  |

## Transaction configuration

| Component                        | Method                       | Transaction                  | Notes                                                                                                                                                      |
|----------------------------------|------------------------------|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DatabaseIngestionServiceImpl     | ingestAll / ingestFromOffset | @Transactional (default)     | One transaction for all target DB writes. External reads are not in this transaction (different DataSource). On exception: full rollback of ingest writes. |
| TestDataGenerator                | clearTestData                | @Transactional               | One transaction for all deletes (work_experience, employees, projects, technologies). On failure: rollback.                                                |
| GraphBuilderServiceImpl          | clearGraph                   | @Transactional(REQUIRES_NEW) | Own transaction so graph clear commits even when called from async flow.                                                                                   |
| GraphBuilderServiceImpl          | buildGraph                   | (none on method)             | Uses GraphService.executeCypher (each has @Transactional).                                                                                                 |
| ProfileProcessor                 | processProfile               | (none)                       | Participates in caller’s transaction (ingestFromOffset).                                                                                                   |
| ExternalWorkExperienceRepository | findFromOffset               | (none)                       | Uses external DataSource; no participation in primary transaction.                                                                                         |

## Async execution

- The async job runs in `CompletableFuture.runAsync(...)`.
- Service calls (e.g. `databaseIngestionService.get().ingestAll(batch)`) go through Spring proxies, so `@Transactional`
  is applied and the transaction uses the **primary DataSource** (target DB).
- If ingest throws, the ingest transaction rolls back and no target data from that run is committed. Clear (if run) and
  graph clear are already committed (separate transactions).
- If embeddings or graph build fails after ingest, ingest is already committed; re-run can regenerate embeddings/graph
  without re-ingesting.

## Target database: same for test data generator and ingest

The **target (primary)** database (`spring.datasource.*`) is the same for:

- **Test data generator**: `clearTestData()`, `generateEmbeddings()`, `generateTestData()` use `EmployeeRepository`,
  `WorkExperienceRepository`, `ProjectRepository`, `TechnologyRepository` (all use the default/primary DataSource).
- **Ingest**: `ProfileProcessor.processProfile()` uses the same `EmployeeRepository`, `WorkExperienceRepository`,
  `ProjectRepository` (same primary DataSource).
- **Test data stats**: `GET /api/v1/test-data/stats` reads from the same repositories (same target DB).

There is no separate "test data database"; both test data generator and ingest write to the same target DB. The only
other DataSource is **external** (read-only, for ingestion source).

## Verification

- **Target DB**: `GET /api/v1/test-data/stats` (employees, workExperiences, projects, technologies).
- **Target and external settings**: `GET /api/v1/ingestion/database/settings` (target.url, target.usedBy, external.*).
- **External DB**: `GET /api/v1/ingestion/database/verify`.
