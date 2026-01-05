# Citus Configuration Guide for ExpertMatch

## Overview

Citus is a PostgreSQL extension that transforms PostgreSQL into a distributed database. It provides:

- **Horizontal Scaling**: Distribute data across multiple nodes for improved performance
- **Columnar Storage**: Efficient storage format for analytical queries (read-heavy workloads)
- **Distributed Queries**: Automatic query distribution across shards
- **Real-time Analytics**: Fast aggregations and analytical queries on large datasets

For ExpertMatch, Citus can be used in two modes:

1. **Single-Node Mode**: Use Citus for columnar storage and future scalability (recommended for MVP)
2. **Multi-Node Mode**: Distribute tables across multiple PostgreSQL nodes (for production scaling)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Installation Options](#installation-options)
3. [Single-Node Configuration](#single-node-configuration)
4. [Multi-Node Configuration](#multi-node-configuration)
5. [Columnar Storage Setup](#columnar-storage-setup)
6. [Distributed Tables Setup](#distributed-tables-setup)
7. [Migration from Current Setup](#migration-from-current-setup)
8. [Performance Tuning](#performance-tuning)
9. [Verification and Testing](#verification-and-testing)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- PostgreSQL 17 (already configured in ExpertMatch)
- Docker and Docker Compose (for containerized setup)
- Understanding of PostgreSQL extensions
- Access to database with superuser privileges

---

## Installation Options

### Option 1: Single-Node Citus (Recommended for MVP)

Use Citus in single-node mode for:

- Columnar storage benefits
- Future scalability without immediate multi-node complexity
- Testing and development

### Option 2: Multi-Node Citus (For Production Scaling)

Use Citus in multi-node mode for:

- Horizontal scaling across multiple database servers
- High-volume analytical queries
- Large-scale data distribution

**Note**: This guide focuses on **Single-Node Configuration** first, with multi-node setup as an advanced option.

---

## Single-Node Configuration

### Step 1: Update Dockerfile

Modify `docker/Dockerfile.dev` to install Citus:

```dockerfile
# Use the official Apache AGE image with PostgreSQL 17 and AGE 1.6.0
FROM apache/age:release_PG17_1.6.0

# Install necessary packages for building extensions
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    git \
    ca-certificates \
    postgresql-server-dev-17 \
    curl \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Clone, build, and install pgVector 0.8.0
RUN git config --global http.sslverify false && \
    git clone --branch v0.8.0 https://github.com/pgvector/pgvector.git /pgvector && \
    cd /pgvector && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config && \
    make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config install && \
    cd / && \
    rm -rf /pgvector

# Install Citus
# Option A: Install from Citus repository (recommended for production)
RUN curl -s https://install.citusdata.com/community/deb.sh | bash && \
    apt-get install -y postgresql-17-citus

# Option B: Build from source (alternative)
# RUN git clone --branch v12.1 https://github.com/citusdata/citus.git /citus && \
#     cd /citus && \
#     make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config && \
#     make PG_CONFIG=/usr/lib/postgresql/17/bin/pg_config install && \
#     cd / && \
#     rm -rf /citus

# Update PostgreSQL configuration to preload extensions
RUN if [ -f /usr/share/postgresql/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector,citus'/" /usr/share/postgresql/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector,citus'" >> /usr/share/postgresql/postgresql.conf.sample; \
    elif [ -f /usr/share/postgresql/17/postgresql.conf.sample ]; then \
        sed -i "s/shared_preload_libraries = 'age'/shared_preload_libraries = 'age,vector,citus'/" /usr/share/postgresql/17/postgresql.conf.sample || \
        echo "shared_preload_libraries = 'age,vector,citus'" >> /usr/share/postgresql/17/postgresql.conf.sample; \
    else \
        echo "shared_preload_libraries = 'age,vector,citus'" >> /etc/postgresql/postgresql.conf || true; \
    fi

# Expose the default PostgreSQL port
EXPOSE 5432

# Set the default command to run PostgreSQL
CMD ["postgres"]
```

### Step 2: Update Database Initialization Script

Modify `docker/init-db.sql` to enable Citus:

```sql
-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS age;

-- Enable Citus extension
CREATE EXTENSION IF NOT EXISTS citus;

-- Configure Citus for single-node mode (coordinator only)
-- This allows using Citus features without multi-node setup
SELECT citus_set_coordinator_host('localhost', 5432);

-- Load Apache AGE
LOAD 'age';

-- Set search path for AGE
SET search_path = ag_catalog, "$user", public;

-- Create schema for application
CREATE SCHEMA IF NOT EXISTS expertmatch;

-- Set default schema
SET search_path = expertmatch, public;

-- Note: Tables will be created via application migrations
-- This script only sets up extensions and schema
```

### Step 3: Update Database Migration

Modify `src/main/resources/db/migration/V1__initial_schema.sql` to include Citus extension:

```sql
-- ExpertMatch Database Schema
-- Version 1.0.0
-- Consolidated schema with external system ID support

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;

-- Try to enable AGE extension (optional - may not be available in all environments)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS age;
EXCEPTION
    WHEN OTHERS THEN
        -- AGE extension not available, continue without it
        NULL;
END $$;

-- Try to enable Citus extension (optional - may not be available in all environments)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS citus;
    -- Configure Citus for single-node mode if not already configured
    -- This is safe to run multiple times
    PERFORM citus_set_coordinator_host('localhost', 5432);
EXCEPTION
    WHEN OTHERS THEN
        -- Citus extension not available, continue without it
        -- This allows the application to work without Citus if needed
        NULL;
END $$;

-- ... rest of the schema remains the same ...
```

---

## Columnar Storage Setup

Citus columnar storage is ideal for analytical queries on large tables. For ExpertMatch, consider using columnar storage
for:

- `work_experience` table (large, read-heavy analytical queries)
- `conversation_history` table (analytical queries on chat history)
- `entity` table (read-heavy lookups)

### Converting Tables to Columnar Storage

Add a new migration file `V2__enable_citus_columnar.sql`:

```sql
-- Migration: Enable Citus Columnar Storage
-- Version: 2.0.0
-- Description: Convert selected tables to Citus columnar storage for improved analytical query performance

-- Note: This migration requires Citus extension to be installed
-- If Citus is not available, this migration will be skipped gracefully

DO $$
BEGIN
    -- Check if Citus is available
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'citus') THEN
        
        -- Convert work_experience table to columnar storage
        -- Columnar storage is optimized for analytical queries (read-heavy workloads)
        -- Note: This requires recreating the table, so data migration is needed
        -- For existing databases, use ALTER TABLE ... USING columnar instead
        
        -- Example: Convert work_experience to columnar (if table is empty or during initial setup)
        -- ALTER TABLE expertmatch.work_experience SET ACCESS METHOD columnar;
        
        -- For existing tables with data, use this approach:

        -- 1. Create new columnar table
        -- 2. Copy data
        -- 3. Drop old table
        -- 4. Rename new table
        
        -- For now, we'll create a function to check and convert tables
        -- This allows gradual migration
        
        RAISE NOTICE 'Citus extension found. Columnar storage can be enabled.';
        RAISE NOTICE 'To enable columnar storage, use: ALTER TABLE expertmatch.work_experience SET ACCESS METHOD columnar;';
        
    ELSE
        RAISE NOTICE 'Citus extension not available. Skipping columnar storage setup.';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        -- Silently continue if Citus is not available
        RAISE NOTICE 'Could not check Citus availability: %', SQLERRM;
END $$;
```

### Manual Columnar Conversion (For Existing Tables)

If you have existing data, use this approach:

```sql
-- Step 1: Create new columnar table with same structure
CREATE TABLE expertmatch.work_experience_columnar (
    LIKE expertmatch.work_experience INCLUDING ALL
) USING columnar;

-- Step 2: Copy data
INSERT INTO expertmatch.work_experience_columnar
SELECT * FROM expertmatch.work_experience;

-- Step 3: Verify data
SELECT COUNT(*) FROM expertmatch.work_experience_columnar;
SELECT COUNT(*) FROM expertmatch.work_experience;
-- Both should match

-- Step 4: Drop old table and rename
BEGIN;
DROP TABLE expertmatch.work_experience;
ALTER TABLE expertmatch.work_experience_columnar RENAME TO work_experience;
COMMIT;

-- Step 5: Recreate indexes (columnar tables support indexes)
CREATE INDEX work_experience_employee_id_idx ON expertmatch.work_experience(employee_id);
CREATE INDEX work_experience_embedding_idx ON expertmatch.work_experience 
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
-- ... recreate other indexes ...
```

**Important Notes on Columnar Storage:**

- Columnar storage is optimized for **analytical queries** (SELECT, aggregations, scans)
- **Not ideal** for frequent INSERT/UPDATE operations (use regular tables for transactional workloads)
- Consider hybrid approach: regular tables for transactional data, columnar for analytical tables
- Vector indexes (HNSW) work with columnar storage

---

## Distributed Tables Setup

For multi-node Citus setup, you need to distribute tables across worker nodes. This is an advanced configuration.

### Prerequisites for Multi-Node Setup

1. Multiple PostgreSQL instances (coordinator + workers)
2. Network connectivity between nodes
3. Citus installed on all nodes

### Configuration Steps

#### 1. Update Docker Compose for Multi-Node

Create `docker-compose.citus.yml`:

```yaml
version: '3.8'

services:
  # Citus Coordinator (main database)
  postgres-coordinator:
    build:
      context: .
      dockerfile: docker/Dockerfile.dev
    image: expertmatch-postgres-citus:latest
    container_name: expertmatch-postgres-coordinator
    environment:
      POSTGRES_USER: expertmatch
      POSTGRES_PASSWORD: expertmatch
      POSTGRES_DB: expertmatch
    ports:

      - "5433:5432"
    volumes:

      - ~/data/expertmatch-postgres-coordinator:/var/lib/postgresql/data
    command: postgres -c shared_preload_libraries='age,vector,citus'
    networks:

      - expertmatch-network

  # Citus Worker 1
  postgres-worker-1:
    build:
      context: .
      dockerfile: docker/Dockerfile.dev
    image: expertmatch-postgres-citus:latest
    container_name: expertmatch-postgres-worker-1
    environment:
      POSTGRES_USER: expertmatch
      POSTGRES_PASSWORD: expertmatch
      POSTGRES_DB: expertmatch
    ports:

      - "5434:5432"
    volumes:

      - ~/data/expertmatch-postgres-worker-1:/var/lib/postgresql/data
    command: postgres -c shared_preload_libraries='age,vector,citus'
    networks:

      - expertmatch-network

  # Citus Worker 2
  postgres-worker-2:
    build:
      context: .
      dockerfile: docker/Dockerfile.dev
    image: expertmatch-postgres-citus:latest
    container_name: expertmatch-postgres-worker-2
    environment:
      POSTGRES_USER: expertmatch
      POSTGRES_PASSWORD: expertmatch
      POSTGRES_DB: expertmatch
    ports:

      - "5435:5432"
    volumes:

      - ~/data/expertmatch-postgres-worker-1:/var/lib/postgresql/data
    command: postgres -c shared_preload_libraries='age,vector,citus'
    networks:

      - expertmatch-network

networks:
  expertmatch-network:
    driver: bridge
```

#### 2. Configure Coordinator to Know About Workers

After starting the containers, connect to the coordinator and add workers:

```sql
-- Connect to coordinator (port 5433)
-- Add worker nodes
SELECT citus_add_node('postgres-worker-1', 5432);
SELECT citus_add_node('postgres-worker-2', 5432);

-- Verify worker nodes
SELECT * FROM citus_get_active_worker_nodes();
```

#### 3. Distribute Tables

Choose distribution columns and distribute tables:

```sql
-- Distribute work_experience by employee_id (hash distribution)
-- This ensures all work experience for an employee is on the same shard
SELECT create_distributed_table('expertmatch.work_experience', 'employee_id');

-- Distribute employee table by id
SELECT create_distributed_table('expertmatch.employee', 'id');

-- Distribute project table by id
SELECT create_distributed_table('expertmatch.project', 'id');

-- Reference tables (small, frequently joined tables)
-- These are replicated to all nodes
SELECT create_reference_table('expertmatch.technology');
SELECT create_reference_table('expertmatch.entity');
```

**Distribution Strategy:**

- **Hash Distribution**: For large tables (work_experience, employee, project)
- **Reference Tables**: For small, frequently joined tables (technology, entity)
- **Local Tables**: For tables that don't need distribution (chat, conversation_history)

---

## Migration from Current Setup

### Migration Steps

1. **Backup Current Database**
   ```bash
   pg_dump -h localhost -p 5433 -U expertmatch -d expertmatch > expertmatch_backup.sql
   ```

2. **Update Docker Configuration**
    - Update `Dockerfile.dev` with Citus installation
    - Update `init-db.sql` with Citus extension
    - Rebuild Docker image: `docker-compose -f docker-compose.dev.yml build`

3. **Stop Current Services**
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

4. **Start Updated Services**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

5. **Verify Citus Installation**
   ```sql
   -- Connect to database
   psql -h localhost -p 5433 -U expertmatch -d expertmatch
   
   -- Check extensions
   \dx
   -- Should show: citus, vector, age
   
   -- Verify Citus
   SELECT * FROM citus_version();
   ```

6. **Enable Columnar Storage (Optional)**
   ```sql
   -- For new installations, tables can be created with columnar storage
   -- For existing installations, follow the migration steps in "Columnar Storage Setup"
   ```

---

## Performance Tuning

### Citus Configuration Parameters

Add to `postgresql.conf` or set via environment variables:

```conf
# Citus Configuration
citus.shard_count = 32                    # Number of shards per table
citus.shard_replication_factor = 1         # Replication factor (1 for single-node)
citus.max_adaptive_executor_pool_size = 16 # Max connections per query
citus.task_executor_type = 'adaptive'      # Query execution strategy

# Columnar Storage Configuration
columnar.compression = 'zstd'              # Compression algorithm
columnar.stripe_row_limit = 150000         # Rows per stripe
columnar.chunk_row_limit = 10000          # Rows per chunk
```

### Query Optimization Tips

1. **Use Distribution Columns in WHERE Clauses**
   ```sql
   -- Good: Uses distribution column
   SELECT * FROM work_experience WHERE employee_id = '123';
   
   -- Less efficient: Requires cross-shard query
   SELECT * FROM work_experience WHERE project_name = 'Project X';
   ```

2. **Co-locate Related Tables**
   ```sql
   -- Distribute related tables by the same column
   SELECT create_distributed_table('work_experience', 'employee_id');
   SELECT create_distributed_table('employee', 'id');
   -- Use colocate_with to ensure same shard placement
   SELECT create_distributed_table('employee', 'id', colocate_with => 'work_experience');
   ```

3. **Use Reference Tables for Small Lookup Tables**
   ```sql
   -- Technology table is small and frequently joined
   SELECT create_reference_table('technology');
   ```

---

## Verification and Testing

### Verify Citus Installation

```sql
-- Check Citus version
SELECT * FROM citus_version();

-- Check active worker nodes (single-node will show coordinator only)
SELECT * FROM citus_get_active_worker_nodes();

-- Check distributed tables
SELECT * FROM citus_tables;

-- Check table distribution
SELECT 
    table_name,
    distribution_column,
    shard_count,
    table_size
FROM citus_tables;
```

### Test Columnar Storage

```sql
-- Check if table uses columnar storage
SELECT 
    schemaname,
    tablename,
    amname as access_method
FROM pg_tables t
JOIN pg_class c ON c.relname = t.tablename
JOIN pg_am a ON a.oid = c.relam
WHERE schemaname = 'expertmatch'
  AND tablename = 'work_experience';

-- Should show: columnar (if using columnar) or heap (if using regular storage)
```

### Performance Testing

```sql
-- Test analytical query performance
EXPLAIN ANALYZE
SELECT 
    e.name,
    COUNT(we.id) as experience_count,
    array_agg(DISTINCT we.technologies) as all_technologies
FROM expertmatch.employee e
JOIN expertmatch.work_experience we ON e.id = we.employee_id
GROUP BY e.id, e.name
HAVING COUNT(we.id) > 5
ORDER BY experience_count DESC
LIMIT 10;
```

---

## Troubleshooting

### Issue: Citus Extension Not Found

**Error**: `ERROR: extension "citus" does not exist`

**Solution**:

1. Verify Citus is installed in Docker image
2. Check `shared_preload_libraries` includes 'citus'
3. Restart PostgreSQL after adding Citus to shared_preload_libraries

```sql
-- Check if Citus is loaded
SHOW shared_preload_libraries;
-- Should include 'citus'

-- If not, restart PostgreSQL and try again
```

### Issue: Cannot Create Columnar Table

**Error**: `ERROR: access method "columnar" does not exist`

**Solution**:

1. Ensure Citus extension is created: `CREATE EXTENSION citus;`
2. Verify Citus version supports columnar storage (Citus 10.0+)

### Issue: Distributed Query Performance Issues

**Symptoms**: Slow queries on distributed tables

**Solutions**:

1. Check if distribution column is used in WHERE clause
2. Verify co-location of related tables
3. Check shard count (too many shards can hurt performance)
4. Use `EXPLAIN` to analyze query plan

```sql
-- Analyze query plan
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM work_experience WHERE employee_id = '123';
```

### Issue: Vector Indexes Not Working with Columnar

**Note**: HNSW indexes work with columnar storage, but verify compatibility:

```sql
-- Check index type
SELECT 
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'expertmatch'
  AND tablename = 'work_experience';
```

---

## Best Practices

1. **Start with Single-Node**: Use single-node Citus for MVP, upgrade to multi-node when needed
2. **Use Columnar for Analytical Tables**: Convert read-heavy tables to columnar storage
3. **Keep Transactional Tables Regular**: Use regular (heap) storage for tables with frequent INSERT/UPDATE
4. **Monitor Shard Distribution**: Ensure even data distribution across shards
5. **Test Performance**: Benchmark queries before and after Citus setup
6. **Backup Before Migration**: Always backup before converting tables to columnar or distributing

---

## References

- [Citus Documentation](https://docs.citusdata.com/)
- [Citus Columnar Storage](https://docs.citusdata.com/en/stable/develop/columnar.html)
- [Citus Distributed Tables](https://docs.citusdata.com/en/stable/develop/reference_ddl.html)
- [Citus Performance Tuning](https://docs.citusdata.com/en/stable/admin_guide/performance.html)

---

## Summary

This guide provides step-by-step instructions for configuring Citus in ExpertMatch:

1. **Single-Node Setup**: Recommended for MVP, enables columnar storage
2. **Multi-Node Setup**: For production horizontal scaling
3. **Columnar Storage**: Optimize analytical queries on large tables
4. **Distributed Tables**: Scale across multiple database nodes

Choose the configuration that best fits your current needs and scale requirements.

