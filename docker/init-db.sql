-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS age;

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

