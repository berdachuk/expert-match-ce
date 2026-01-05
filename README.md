# ExpertMatch Setup Guide

Welcome to ExpertMatch! This comprehensive guide provides setup instructions for different development environments.

## Table of Contents

- [Quick Start](#quick-start)
- [Docker Development Setup](#docker-development-setup)
- [Local Development Setup](#local-development-setup)
- [Configuration Options](#configuration-options)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)
- [Additional Resources](#additional-resources)

## Quick Start

For a fast setup, follow these basic steps:

1. **Start Database Services**:
   ```bash
   docker compose up -d
   ```

2. **Run Application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Access**:
    - Application: http://localhost:8080
    - API Docs: http://localhost:8080/swagger-ui.html
    - Health Check: http://localhost:8080/actuator/health

## Docker Development Setup

This section explains how to set up ExpertMatch in development mode using Docker for PostgreSQL with PgVector and Apache
AGE extensions.

### Prerequisites

- Docker and Docker Compose installed
- Java 21 JDK
- Maven 3.9+ or later
- Ollama (optional - can use Docker Ollama or local installation)
- Port 5433 available for PostgreSQL (or choose another port if busy)
- Port 11435 available for Ollama (or choose another port if busy)
- `~/data` directory (or create it: `mkdir -p ~/data`)

**Note**:

- Database data will be stored in `~/data/expertmatch-postgres` on your host machine
- Ollama data will be stored in `~/data/expertmatch-ollama` on your host machine (if using Docker Ollama)

### Step 1: Build Custom PostgreSQL Docker Image

The custom Docker image includes PostgreSQL 17 with PgVector and Apache AGE extensions pre-installed.

```bash
cd expert-match
docker build -f docker/Dockerfile.test -t expertmatch-postgres-dev:latest .
```

This builds a PostgreSQL image with:

- PostgreSQL 17
- PgVector extension (v0.8.1)
- Apache AGE extension
- All required dependencies

**Note**: The build may take 5-10 minutes the first time as it compiles the extensions.

### Step 2: Start PostgreSQL Container

**Option 1: Using development compose file (recommended)**

```bash
cd expert-match
docker compose -f docker-compose.dev.yml up -d
```

**Option 2: Using main compose file**

```bash
cd expert-match
docker compose up -d
```

Both configurations use port 5433 to avoid conflicts with your existing PostgreSQL on port 5432.

### Step 3: Configure Application

**Option 1: Update `application.yml` directly**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/expertmatch  # Note: port 5433
    username: ${DB_USERNAME:expertmatch}
    password: ${DB_PASSWORD:expertmatch}
```

**Option 2: Use environment variables (Recommended)**

```bash
export DB_USERNAME=expertmatch
export DB_PASSWORD=expertmatch
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
```

### Step 4: Configure Ollama

**Option 1: Using Docker Ollama (Recommended for Development)**

```bash
# Check if Docker Ollama is running
curl http://localhost:11435/api/tags

# Pull required models (using Docker Ollama on port 11435)
OLLAMA_HOST=http://localhost:11435 ollama pull qwen3:4b-instruct-2507-q4_K_M
OLLAMA_HOST=http://localhost:11435 ollama pull qwen3-embedding-8b
OLLAMA_HOST=http://localhost:11435 ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M
```

**Option 2: Using Local Ollama**

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start Ollama
ollama serve

# Pull required models
ollama pull qwen3:4b-instruct-2507-q4_K_M
ollama pull qwen3-embedding-8b
ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Step 5: Start ExpertMatch Application

```bash
# Basic start
mvn spring-boot:run

# With environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export OLLAMA_BASE_URL=http://localhost:11435
mvn spring-boot:run

# With debug profile for verbose logging
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local,debug
```

## Local Development Setup

This section explains how to set up ExpertMatch for local development with:

- **LLM Inference**: Local Ollama (default port 11434)
- **Embeddings**: Local Ollama (default port 11434)
- **Reranking**: Local Ollama (default port 11434)

### Prerequisites

1. Local Ollama installed and running on port 11434
2. PostgreSQL database (Docker or local)

### Setup Steps

1. **Ensure Local Ollama is Running**:
   ```bash
   curl http://localhost:11434/api/tags
   ollama serve
   ```

2. **Pull Required Models**:
   ```bash
   ollama pull qwen3:4b-instruct-2507-q4_K_M  # Or qwen3:30b-a3b-instruct-2507-q4_K_M for better quality
   ollama pull qwen3-embedding-8b
   ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M
   ```

3. **Run with Local Profile**:
   ```bash
   export SPRING_PROFILES_ACTIVE=local
   mvn spring-boot:run
   ```

### Configuration Details

**LLM Inference (Local Ollama)**

- Endpoint: `http://localhost:11434`
- Provider: `ollama` (native Ollama API)
- Model: `qwen3:30b-a3b-instruct-2507-q4_K_M` (default, better quality) or `qwen3:4b-instruct-2507-q4_K_M` (faster)
- No API key required

**Embeddings (Local Ollama)**

- Endpoint: `http://localhost:11434`
- Model: `qwen3-embedding-8b`
- No API key required

**Reranking (Local Ollama)**

- Endpoint: `http://localhost:11434`
- Model: `dengcao/Qwen3-Reranker-8B:Q4_K_M` (recommended, 5.0GB)
- Alternative: `dengcao/Qwen3-Reranker-0.6B:Q8_0` (lightweight, 639MB)

## Configuration Options

### Environment Variables

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export DB_USERNAME=expertmatch
export DB_PASSWORD=expertmatch

# AI Provider Configuration
export OLLAMA_BASE_URL=http://localhost:11435
export OLLAMA_MODEL=qwen3:4b-instruct-2507-q4_K_M
export EMBEDDING_MODEL=qwen3-embedding-8b

# OpenAI/Compatible providers
export OPENAI_API_KEY=your-api-key-here
export OPENAI_BASE_URL=https://api.openai.com
export OPENAI_CHAT_MODEL=gpt-4-turbo-preview
export OPENAI_EMBEDDING_MODEL=text-embedding-3-large
```

### AI Provider Support

ExpertMatch supports multiple AI providers:

**1. Ollama (Local or Remote)**

- Default provider for local development
- No API key required

**2. OpenAI (Standard)**

- Set `OPENAI_API_KEY` with your OpenAI API key
- Models: `gpt-4`, `gpt-3.5-turbo`, `text-embedding-3-large`

**3. OpenAI-Compatible Providers**

- Set `OPENAI_API_KEY` with your provider's API key
- Set `OPENAI_BASE_URL` to your provider's endpoint
- Use provider-specific model names

## Troubleshooting

### Common Issues

**Port Already in Use**

```yaml
# Change port in docker-compose.dev.yml
ports:
  - "5434:5432"  # Use port 5434 instead
```

**Container Won't Start**

```bash
docker logs expertmatch-postgres-dev
```

**Database Connection Errors**

```bash
docker ps | grep expertmatch-postgres-dev
docker exec -it expertmatch-postgres-dev psql -U expertmatch -d expertmatch -c "SELECT 1;"
```

**Extensions Not Available**
```bash
docker build -f docker/Dockerfile.test -t expertmatch-postgres-dev:latest .
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d
```

## Quick Start Script

Create a `start-dev.sh` script for easy startup:

```bash
#!/bin/bash

# Start PostgreSQL container
echo "Starting PostgreSQL container..."
docker compose -f docker-compose.dev.yml up -d

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL..."
sleep 5

until docker exec expertmatch-postgres-dev pg_isready -U expertmatch; do
  echo "Waiting for PostgreSQL to be ready..."
  sleep 2
done

echo "PostgreSQL is ready!"

# Set environment variables
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export DB_USERNAME=expertmatch
export DB_PASSWORD=expertmatch

# Start application
echo "Starting ExpertMatch application..."
mvn spring-boot:run
```

Make it executable and run:
```bash
chmod +x start-dev.sh
./start-dev.sh
```

## Documentation

### Serving MkDocs Documentation

ExpertMatch uses MkDocs with Material theme for documentation. To serve the documentation:

1. **Install Dependencies**:
   ```bash
   pip install -r requirements-docs.txt
   ```

2. **Start Development Server**:
   ```bash
   mkdocs serve --dev-addr 0.0.0.0:8103
   ```

   The documentation will be available at:
   - Locally: `http://127.0.0.1:8103` or `http://localhost:8103`
   - Remotely: `http://192.168.0.73:8103` (example remote server)

3. **Build Static Site** (optional):
   ```bash
   mkdocs build
   ```

   This generates static HTML files in the `site/` directory.

**Note**: The development server includes live reload - changes to documentation files will automatically refresh in the
browser.

For more details, see:

- [MkDocs Setup Guide](docs/MKDOCS_SETUP.md)
- [README_MKDOCS.md](docs/README_MKDOCS.md)

## Additional Resources

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [PgVector Documentation](https://github.com/pgvector/pgvector)
- [Apache AGE Documentation](https://age.apache.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Ollama Documentation](https://ollama.ai/docs)
