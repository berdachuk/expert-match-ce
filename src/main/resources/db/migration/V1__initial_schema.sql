-- ExpertMatch Database Schema
-- Version 1.0.0 (MVP - Consolidated)
-- Consolidated schema with external system ID support
-- External system uses 19-digit numeric strings for entity IDs (VARCHAR(74))
-- Internal IDs (chat, conversation) use MongoDB-compatible 24-character hex strings (CHAR(24))
-- Includes: core tables, tool_metadata, graph schema, functions, and triggers

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable pg_trgm extension for fuzzy text matching (similarity search)
-- Used for person name similarity search to handle typos and variations
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Try to enable AGE extension (optional - may not be available in all environments)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS age;
EXCEPTION
    WHEN OTHERS THEN
        -- AGE extension not available, continue without it
        NULL;
END $$;

-- Create schema for application (Flyway will create it if needed)
-- Note: Flyway creates the schema automatically when schemas are configured
-- CREATE SCHEMA IF NOT EXISTS expertmatch;

-- Set default schema
SET search_path = expertmatch, public;

-- Try to load and configure Apache AGE (if available)
DO $$
BEGIN
    LOAD 'age';
    SET search_path = ag_catalog, "$user", public, expertmatch;
EXCEPTION
    WHEN OTHERS THEN
        -- AGE not available, continue with expertmatch schema
        SET search_path = expertmatch, public;
END $$;

-- ============================================
-- Core Tables
-- ============================================

-- Chat table (internal IDs - MongoDB-compatible)
CREATE TABLE expertmatch.chat (
    id CHAR(24) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    metadata JSONB
);

-- Partial unique index for default chat per user (PostgreSQL 9.2+)
-- This ensures only one default chat per user
CREATE UNIQUE INDEX chat_user_default_unique ON expertmatch.chat(user_id, is_default) 
    WHERE is_default = TRUE;

CREATE INDEX chat_user_id_idx ON expertmatch.chat(user_id);
CREATE INDEX chat_is_default_idx ON expertmatch.chat(user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX chat_last_activity_at_idx ON expertmatch.chat(last_activity_at DESC);

-- Conversation History table (internal IDs - MongoDB-compatible)
CREATE TABLE expertmatch.conversation_history (
    id CHAR(24) PRIMARY KEY,
    chat_id CHAR(24) NOT NULL REFERENCES expertmatch.chat(id) ON DELETE CASCADE,
    message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('user', 'assistant', 'system')),
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    sequence_number INT NOT NULL,
    tokens_used INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    CONSTRAINT conversation_history_chat_sequence_unique UNIQUE(chat_id, sequence_number)
);

CREATE INDEX conversation_history_chat_id_idx ON expertmatch.conversation_history(chat_id);
CREATE INDEX conversation_history_chat_id_sequence_idx ON expertmatch.conversation_history(chat_id, sequence_number);
CREATE INDEX conversation_history_chat_id_created_at_idx ON expertmatch.conversation_history(chat_id, created_at);

-- Chat Memory Summary table (internal IDs - MongoDB-compatible)
CREATE TABLE expertmatch.chat_memory_summary (
    id CHAR(24) PRIMARY KEY,
    chat_id CHAR(24) NOT NULL REFERENCES expertmatch.chat(id) ON DELETE CASCADE,
    message_range_start INT NOT NULL,
    message_range_end INT NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(chat_id, message_range_start, message_range_end)
);

CREATE INDEX chat_memory_summary_chat_id_idx ON expertmatch.chat_memory_summary(chat_id);

-- Employee table (external system IDs - VARCHAR(74))
-- External system format: "8760000000000420950" (19-digit numeric string)
CREATE TABLE expertmatch.employee (
    id VARCHAR(74) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    seniority VARCHAR(10),
    language_english VARCHAR(10),
    availability_status VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX employee_email_idx ON expertmatch.employee(email);
CREATE INDEX employee_seniority_idx ON expertmatch.employee(seniority);

-- Work Experience table
-- employee_id: External system ID (VARCHAR(74))
-- project_id: External system ID (VARCHAR(74), nullable - for external system references)
-- customer_id: External system ID (VARCHAR(74), nullable - for external system references)
-- id: Internal ID (CHAR(24) - MongoDB-compatible)
CREATE TABLE expertmatch.work_experience (
    id CHAR(24) PRIMARY KEY,
    employee_id VARCHAR(74) NOT NULL REFERENCES expertmatch.employee(id) ON DELETE CASCADE,
    project_id VARCHAR(74), -- External system project_id (nullable)
    customer_id VARCHAR(74), -- External system customer_id (nullable)
    project_name VARCHAR(255),
    project_summary TEXT,
    project_link VARCHAR(500),
    role VARCHAR(100),
    start_date DATE,
    end_date DATE,
    technologies TEXT[], -- Array of technologies
    responsibilities TEXT,
    customer_name VARCHAR(255),
    industry VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Vector embedding for semantic search
    -- Supports both 1024 (Ollama BAAI/bge-m3) and 1536 (OpenAI/DIAL text-embedding-3-large) dimensions
    -- Smaller embeddings (1024) are padded to 1536 when stored
    embedding vector(1536),
    -- Track the actual embedding dimension used (1024 or 1536)
    embedding_dimension INT,
    metadata JSONB
);

CREATE INDEX work_experience_employee_id_idx ON expertmatch.work_experience(employee_id);
CREATE INDEX work_experience_project_id_idx ON expertmatch.work_experience(project_id);
CREATE INDEX work_experience_customer_id_idx ON expertmatch.work_experience(customer_id);
CREATE INDEX work_experience_project_name_idx ON expertmatch.work_experience(project_name);
CREATE INDEX work_experience_technologies_idx ON expertmatch.work_experience USING GIN(technologies);
-- HNSW index for vector similarity search (supports 1536 dimensions)
CREATE INDEX work_experience_embedding_idx ON expertmatch.work_experience 
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
-- Index on embedding dimension for filtering/querying
CREATE INDEX work_experience_embedding_dimension_idx ON expertmatch.work_experience(embedding_dimension);

-- Project table (external system IDs - VARCHAR(74))
-- External system format: "4060741400384209073" (19-digit numeric string)
CREATE TABLE expertmatch.project (
    id VARCHAR(74) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    summary TEXT,
    link VARCHAR(500),
    project_type VARCHAR(100),
    technologies TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX project_name_idx ON expertmatch.project(name);
CREATE INDEX project_technologies_idx ON expertmatch.project USING GIN(technologies);

-- Technology table (internal IDs - MongoDB-compatible)
-- Technologies are internal-only, no external system IDs
CREATE TABLE expertmatch.technology (
    id CHAR(24) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    normalized_name VARCHAR(255) NOT NULL, -- Normalized for matching
    category VARCHAR(100),
    synonyms TEXT[], -- Array of synonyms/variations
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX technology_name_idx ON expertmatch.technology(name);
CREATE INDEX technology_normalized_name_idx ON expertmatch.technology(normalized_name);
CREATE INDEX technology_synonyms_idx ON expertmatch.technology USING GIN(synonyms);

-- Entity table (for entity extraction) - internal IDs
CREATE TABLE expertmatch.entity (
    id CHAR(24) PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- 'person', 'organization', 'technology', 'project', 'domain'
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX entity_type_idx ON expertmatch.entity(type);
CREATE INDEX entity_name_idx ON expertmatch.entity(name);
CREATE INDEX entity_normalized_name_idx ON expertmatch.entity(normalized_name);

-- Tool metadata table (for tool search)
-- Stores tool metadata with embeddings for semantic search using PgVector
CREATE TABLE expertmatch.tool_metadata (
    id VARCHAR(255) PRIMARY KEY,
    tool_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    tool_class VARCHAR(255) NOT NULL,
    method_name VARCHAR(255) NOT NULL,
    parameters JSONB,
    -- Vector embedding for semantic search (1536 dimensions to match existing schema)
    -- Supports both 1024 (Ollama) and 1536 (OpenAI/DIAL) dimensions
    -- Smaller embeddings (1024) are padded to 1536 when stored
    embedding vector(1536),
    -- Track the actual embedding dimension used (1024 or 1536)
    embedding_dimension INT,
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- HNSW index for vector similarity search (matching work_experience_embedding_idx configuration)
CREATE INDEX tool_metadata_embedding_idx ON expertmatch.tool_metadata 
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- Index on tool name for keyword search
CREATE INDEX tool_metadata_tool_name_idx ON expertmatch.tool_metadata(tool_name);
CREATE INDEX tool_metadata_tool_class_idx ON expertmatch.tool_metadata(tool_class);
CREATE INDEX tool_metadata_method_name_idx ON expertmatch.tool_metadata(method_name);

-- Index on embedding dimension for filtering/querying
CREATE INDEX tool_metadata_embedding_dimension_idx ON expertmatch.tool_metadata(embedding_dimension);

-- ============================================
-- Graph Schema (Apache AGE)
-- ============================================

-- Create graph for expert relationships (if AGE is available)
DO $$
BEGIN
    SELECT create_graph('expertmatch_graph');
EXCEPTION
    WHEN OTHERS THEN
        -- AGE not available or graph already exists, continue
        NULL;
END $$;

-- Create vertex labels
-- Note: Apache AGE uses Cypher queries, these will be created via application code
-- Example structure:
-- - Expert vertices (from employees)
-- - Project vertices (from projects)
-- - Technology vertices (from technologies)
-- - Relationships: PARTICIPATED_IN, USED, WORKED_WITH, etc.

-- ============================================
-- Functions
-- ============================================

-- Function to generate MongoDB-compatible ID (for internal IDs)
CREATE OR REPLACE FUNCTION expertmatch.generate_objectid() 
RETURNS CHAR(24) AS $$
DECLARE
    chars TEXT := '0123456789abcdef';
    result CHAR(24) := '';
    i INT;
BEGIN
    FOR i IN 1..24 LOOP
        result := result || substr(chars, floor(random() * 16)::int + 1, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION expertmatch.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at (must be created after function definition)
-- Triggers for updated_at
CREATE TRIGGER update_chat_updated_at BEFORE UPDATE ON expertmatch.chat
    FOR EACH ROW EXECUTE FUNCTION expertmatch.update_updated_at_column();

CREATE TRIGGER update_employee_updated_at BEFORE UPDATE ON expertmatch.employee
    FOR EACH ROW EXECUTE FUNCTION expertmatch.update_updated_at_column();

CREATE TRIGGER update_project_updated_at BEFORE UPDATE ON expertmatch.project
    FOR EACH ROW EXECUTE FUNCTION expertmatch.update_updated_at_column();

CREATE TRIGGER update_entity_updated_at BEFORE UPDATE ON expertmatch.entity
    FOR EACH ROW EXECUTE FUNCTION expertmatch.update_updated_at_column();

CREATE TRIGGER update_tool_metadata_updated_at BEFORE UPDATE ON expertmatch.tool_metadata
    FOR EACH ROW EXECUTE FUNCTION expertmatch.update_updated_at_column();
