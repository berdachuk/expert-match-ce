# Remote SSH Setup Guide

This guide explains how to start the ExpertMatch service on a remote server (via SSH) and test it using API clients from your local machine.

## Prerequisites

1. **Remote Server Access**: SSH access to the remote server (example IP: `192.168.0.73`)
2. **API Client**: Installed on your local machine (cURL, Postman, Insomnia, or similar)
3. **Database**: PostgreSQL with PgVector and Apache AGE on the remote server
4. **Network Access**: Ability to connect from your local machine to the remote server on port 8080

## Step 1: Connect to Remote Server

```bash
ssh user@192.168.0.73
# Replace 'user' with your actual username
```

## Step 2: Navigate to Project Directory

```bash
cd ~/projects-ai/expert-match-root/expert-match
```

## Step 3: Build and Start Custom Database Container

The project uses a custom Docker image with PostgreSQL 17, PgVector, and Apache AGE extensions.

### Build the Custom Database Image (if not already built)

```bash
# Build the custom PostgreSQL image with PgVector and Apache AGE
docker build -f docker/Dockerfile.dev -t expertmatch-postgres-dev:latest .

# This may take 5-10 minutes on first build as it compiles extensions
```

### Start the Database Container

```bash
# Start PostgreSQL container using docker-compose
docker compose -f docker-compose.dev.yml up -d

# Or if using the main compose file
docker compose up -d postgres
```

### Verify Database is Running

```bash
# Check if the container is running
docker ps | grep expertmatch-postgres

# Check container logs
docker logs expertmatch-postgres-dev

# Test database connection from inside the container
docker exec -it expertmatch-postgres-dev psql -U expertmatch -d expertmatch -c "SELECT 1;"

# Verify extensions are installed
docker exec -it expertmatch-postgres-dev psql -U expertmatch -d expertmatch -c "SELECT * FROM pg_extension WHERE extname IN ('vector', 'age');"
```

**Expected output**: Both `vector` and `age` extensions should be listed.

**Note**: The database runs on port **5433** on the host (mapped from container port 5432) to avoid conflicts with
existing PostgreSQL installations.

## Step 4: Setup and Verify Ollama (for Local Profile)

Ollama is required for the `local` profile. You can use either:

- **Local Ollama** (port 11434) - Default port, if Ollama is installed locally
- **Docker Ollama** (port 11435) - Alternative, runs in container

### Verify Which Ollama Service is Available

**First, check if local Ollama is running (default port 11434):**

```bash
# Test local Ollama connection (default port)
curl http://localhost:11434/api/tags
```

**If you get a response** (JSON with models or empty array), use **Local Ollama on port 11434**.

**If connection fails**, check Docker Ollama:

```bash
# Check if Docker Ollama container is running
docker ps | grep expertmatch-ollama

# Test Docker Ollama connection (port 11435)
curl http://localhost:11435/api/tags
```

### Option 1: Local Ollama (Port 11434) - Recommended if Available

**Why port 11434?** This is Ollama's default port when installed locally.

```bash
# Check if local Ollama is running
curl http://localhost:11434/api/tags

# If not running, start Ollama
ollama serve

# Check available models
ollama list

# Or using API
curl http://localhost:11434/api/tags | jq '.models[].name'
```

**Expected response**: JSON with available models or empty array `{"models":[]}` if no models are pulled yet.

**Verify required models are available:**

```bash
# Check if required models are present
curl -s http://localhost:11434/api/tags | jq '.models[].name' | grep -E "(qwen3:4b-instruct|qwen3-embedding|Qwen3-Reranker)"
```

**Note**: Use port **11434** in `OLLAMA_BASE_URL` environment variable.

### Option 2: Docker Ollama (Port 11435) - Alternative

**Why port 11435?** The Docker container maps Ollama's default port 11434 to host port 11435 to avoid conflicts with
local Ollama installations.

```bash
# Start Ollama container (if not already running)
docker compose up -d ollama

# Verify Docker Ollama is running
docker ps | grep expertmatch-ollama

# Test Docker Ollama connection
curl http://localhost:11435/api/tags

# Check available models
curl http://localhost:11435/api/tags | jq '.models[].name'
```

**Expected response**: JSON with available models or empty array `{"models":[]}` if no models are pulled yet.

**Verify required models are available:**

```bash
# Check if required models are present
curl -s http://localhost:11435/api/tags | jq '.models[].name' | grep -E "(qwen3:4b-instruct|qwen3-embedding|Qwen3-Reranker)"
```

**Note**: Use port **11435** in `OLLAMA_BASE_URL` environment variable.

### Pull Required Models

**For Local Ollama (port 11434) - Recommended:**

```bash
# Pull models using local Ollama (default port)
ollama pull qwen3:4b-instruct-2507-q4_K_M
ollama pull qwen3-embedding-8b
ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M

# Verify models are available
ollama list
```

**For Docker Ollama (port 11435):**

```bash
# Pull models using Docker Ollama (specify host)
OLLAMA_HOST=http://localhost:11435 ollama pull qwen3:4b-instruct-2507-q4_K_M
OLLAMA_HOST=http://localhost:11435 ollama pull qwen3-embedding-8b
OLLAMA_HOST=http://localhost:11435 ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M

# Verify models are available
curl http://localhost:11435/api/tags | jq '.models[].name'
```

## Step 5: Configure Environment Variables

Set required environment variables based on your profile:

### For Local Profile (Ollama):

**Choose the port based on which Ollama service is available:**

**If Local Ollama is available (port 11434) - Recommended:**

```bash
export SPRING_PROFILES_ACTIVE=local
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export SPRING_DATASOURCE_USERNAME=expertmatch
export SPRING_DATASOURCE_PASSWORD=expertmatch
export OLLAMA_BASE_URL=http://localhost:11434  # Local Ollama (default port)
export OLLAMA_MODEL=qwen3:4b-instruct-2507-q4_K_M
export OLLAMA_EMBEDDING_MODEL=qwen3-embedding-8b
```

**If only Docker Ollama is available (port 11435):**

```bash
export SPRING_PROFILES_ACTIVE=local
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export SPRING_DATASOURCE_USERNAME=expertmatch
export SPRING_DATASOURCE_PASSWORD=expertmatch
export OLLAMA_BASE_URL=http://localhost:11435  # Docker Ollama (mapped port)
export OLLAMA_MODEL=qwen3:4b-instruct-2507-q4_K_M
export OLLAMA_EMBEDDING_MODEL=qwen3-embedding-8b
```

**Quick verification before setting variables:**

```bash
# Check which Ollama is available
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo " Local Ollama is available on port 11434"
    export OLLAMA_BASE_URL=http://localhost:11434
elif curl -s http://localhost:11435/api/tags > /dev/null 2>&1; then
    echo " Docker Ollama is available on port 11435"
    export OLLAMA_BASE_URL=http://localhost:11435
else
    echo " No Ollama service found. Start local Ollama or Docker Ollama container."
fi
```

### For Dev Profile (Azure OpenAI):

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch
export SPRING_DATASOURCE_USERNAME=expertmatch
export SPRING_DATASOURCE_PASSWORD=expertmatch
export AZURE_OPENAI_API_KEY=your-azure-api-key
export AZURE_OPENAI_ENDPOINT=https://your-openai-endpoint.openai.azure.com
export DIAL_MODEL_NAME=gpt-4
```

**Note**: For Azure OpenAI, ensure you have access to your Azure OpenAI endpoint. Ollama is not required for the `dev`
profile.

## Step 6: Start the Service

### Option 1: Run with Maven (Development)

```bash
# Using local profile
mvn spring-boot:run -P local

# Using dev profile (Azure OpenAI)
mvn spring-boot:run -P dev

# Or using environment variable
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

### Option 2: Run as JAR (Production-like)

```bash
# Build the application
mvn clean package -DskipTests

# Run the JAR
java -jar target/expert-match.jar --spring.profiles.active=local

# Or with environment variables
export SPRING_PROFILES_ACTIVE=local
java -jar target/expert-match.jar
```

### Option 3: Run in Background (Using nohup or screen)

**Using nohup:**

```bash
nohup mvn spring-boot:run -P local > expert-match.log 2>&1 &
echo $! > expert-match.pid
```

**Using screen (recommended):**

```bash
# Start a new screen session
screen -S expert-match

# Run the service
mvn spring-boot:run -P local

# Detach: Press Ctrl+A, then D
# Reattach: screen -r expert-match
```

**Using tmux:**

```bash
# Start a new tmux session
tmux new -s expert-match

# Run the service
mvn spring-boot:run -P local

# Detach: Press Ctrl+B, then D
# Reattach: tmux attach -t expert-match
```

## Step 7: Verify Service is Running

On the remote server, check if the service is listening:

```bash
# Check if port 8093 is listening
netstat -tlnp | grep 8093
# Or
ss -tlnp | grep 8093

# Test health endpoint locally on the server
curl http://localhost:8093/actuator/health

# Check application logs
tail -f expert-match.log
# Or if using screen/tmux, check the session
```

## Step 8: Configure Firewall (if needed)

If the service is not accessible from your local machine, configure the firewall:

```bash
# Ubuntu/Debian
sudo ufw allow 8080/tcp
sudo ufw reload

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

## Step 9: Test Connection from Local Machine

From your local machine, test if you can reach the service:

```bash
# Test health endpoint
curl http://192.168.0.73:8080/actuator/health

# Test API endpoint
curl http://192.168.0.73:8080/api/v1/health
```

If you get connection refused or timeout:

- Check firewall rules on the remote server
- Verify the service is binding to `0.0.0.0` (not just `127.0.0.1`)
- Check network routing between your local machine and remote server

## Step 10: Configure API Client for Remote Access

### Method 1: Direct Connection (Recommended if network allows)

1. **Configure Base URL**:

- Set your API client base URL to: `http://192.168.0.73:8080`
    - For cURL: Use the full URL in requests
    - For API clients with variables: Set `base_url` variable

2. **Configure JWT Tokens** (if OAuth2 is enabled):
- Set JWT token in your API client
    - Set `jwt_token` to your JWT token (for regular user)
    - Set `jwt_token_admin` to your admin JWT token (optional)

### Method 2: SSH Port Forwarding (If direct connection is blocked)

If you cannot directly access port 8080 on the remote server, use SSH port forwarding:

**On your local machine:**

```bash
# Forward remote port 8080 to local port 8080
ssh -L 8093:localhost:8093 user@192.168.0.73

# Or forward to a different local port
ssh -L 8094:localhost:8093 user@192.168.0.73
```

Then in your API client:

- Set `base_url` to: `http://localhost:8093` (or `http://localhost:8094` if using different port)

**Keep the SSH session open** while testing with your API client.

## Step 11: Test with API Client

### Quick Test: Health Check

1. Open your API client
2. Make a GET request to: `http://192.168.0.73:8080/api/v1/system/health`
3. Expected response: `200 OK` with health status

### Complete Testing Workflow

1. **Authentication Setup** (if OAuth2 enabled):
- Test health check endpoint: `GET http://192.168.0.73:8080/api/v1/system/health`
    - Verify service is accessible

2. **Generate Test Data** (requires ADMIN role):
- POST `http://192.168.0.73:8080/api/v1/ingestion/test-data?size=small`
    - Wait for completion (may take a few minutes)

3. **List Chats**:

- GET `http://192.168.0.73:8080/api/v1/chat`
    - Note the default chat ID from the response

4. **Process Query**:

- POST `http://192.168.0.73:8080/api/v1/query` with query payload
    - Verify response contains experts and answer

5. **View Conversation History**:

- GET `http://192.168.0.73:8080/api/v1/chat/{chatId}/history?page=0&size=20`
    - Verify messages are returned

## Troubleshooting

### Service Won't Start

**Check logs:**

```bash
# If using nohup
tail -f expert-match.log

# If using screen
screen -r expert-match

# Check for errors
grep -i error expert-match.log
```

**Common issues:**

- Port 8080 already in use: Change port in `application.yml` or kill existing process
- Database connection failed: See "Database Connection Issues" below
- Missing environment variables: Check all required variables are set

### Database Connection Issues

**Check if container is running:**

```bash
# Check container status
docker ps | grep expertmatch-postgres-dev

# If not running, start it
docker compose -f docker-compose.dev.yml up -d

# Check container logs for errors
docker logs expertmatch-postgres-dev
```

**Test database connection:**

```bash
# Test from inside container
docker exec -it expertmatch-postgres-dev psql -U expertmatch -d expertmatch -c "SELECT 1;"

# Test from host (if psql is installed)
psql -h localhost -p 5433 -U expertmatch -d expertmatch -c "SELECT 1;"
```

**Verify extensions:**

```bash
# Check if PgVector and Apache AGE are installed
docker exec -it expertmatch-postgres-dev psql -U expertmatch -d expertmatch -c "SELECT extname, extversion FROM pg_extension WHERE extname IN ('vector', 'age');"
```

**Common database issues:**

- Container not running: Start with `docker compose -f docker-compose.dev.yml up -d`
- Port 5433 already in use: Change port mapping in `docker-compose.dev.yml` or stop conflicting service
- Extensions not found: Rebuild the image: `docker build -f docker/Dockerfile.dev -t expertmatch-postgres-dev:latest .`
- Connection refused: Verify container is running and port mapping is correct (5433:5432)

### Ollama Connection Issues (Local Profile Only)

**Verify Ollama is accessible:**

**For Docker Ollama (port 11435):**

```bash
# Check if container is running
docker ps | grep expertmatch-ollama

# If not running, start it
docker compose up -d ollama

# Test connection
curl http://localhost:11435/api/tags

# Check container logs
docker logs expertmatch-ollama
```

**For Local Ollama (port 11434) - Check first:**

```bash
# Check if Ollama process is running
ps aux | grep ollama

# Test connection
curl http://localhost:11434/api/tags

# If not running, start Ollama
ollama serve

# Verify models are available
ollama list
# Or using API
curl http://localhost:11434/api/tags | jq '.models[].name'
```

**For Docker Ollama (port 11435) - If local is not available:**

```bash
# Check if container is running
docker ps | grep expertmatch-ollama

# Test connection
curl http://localhost:11435/api/tags

# Verify models are available
curl http://localhost:11435/api/tags | jq '.models[].name'
```

**Common Ollama issues:**

- **Connection refused**: Ollama is not running. Start Docker container or local Ollama service.
- **Port conflict**: If port 11435 is busy, change it in `docker-compose.yml` or use local Ollama on 11434.
- **Models not found**: Pull required models (see Step 4).
- **Wrong port**: Verify `OLLAMA_BASE_URL` matches your setup:
- Docker Ollama: `http://localhost:11435`
    - Local Ollama: `http://localhost:11434`
- **Container won't start**: Check Docker logs: `docker logs expertmatch-ollama`

### Cannot Connect from Local Machine

**Check service binding:**

```bash
# On remote server, verify service is listening on all interfaces
netstat -tlnp | grep 8080
# Should show: 0.0.0.0:8080 or :::8080, not 127.0.0.1:8080
```

**Check firewall:**

```bash
# On remote server
sudo ufw status
# Or
sudo firewall-cmd --list-all
```

**Test from remote server itself:**

```bash
# On remote server
curl http://localhost:8093/actuator/health
curl http://192.168.0.73:8080/actuator/health
```

### API Client Connection Errors

**Connection Refused:**

- Verify service is running on remote server
- Check firewall allows port 8080
- Verify network connectivity: `ping 192.168.0.73`

**Timeout:**

- Check if service is accessible from remote server itself
- Verify network routing
- Try SSH port forwarding method

**401 Unauthorized:**

- If OAuth2 is enabled, ensure JWT token is set in your API client
- Verify token is valid and not expired
- Check token has required claims (`sub`, `aud`, `authorities`)

**403 Forbidden:**

- For ingestion endpoints, ensure token has `ROLE_ADMIN`
- Check `jwt_token_admin` is set correctly

### Service Binding Issues

If the service only binds to `127.0.0.1`, update `application.yml`:

```yaml
server:
  address: 0.0.0.0  # Bind to all interfaces
  port: 8080
```

Or set environment variable:

```bash
export SERVER_ADDRESS=0.0.0.0
```

## Advanced: Running as a Service

For production-like setup, you can run the service as a systemd service:

**Create service file** (`/etc/systemd/system/expert-match.service`):

```ini
[Unit]
Description=ExpertMatch Service
After=network.target postgresql.service

[Service]
Type=simple
User=your-user
# Note: Systemd requires absolute paths - replace ~/projects-ai with your actual home directory path
# Example: if home is /home/username, use /home/username/projects-ai/expert-match-root/expert-match
WorkingDirectory=~/projects-ai/expert-match-root/expert-match
Environment="SPRING_PROFILES_ACTIVE=local"
Environment="SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/expertmatch"
ExecStart=/usr/bin/java -jar ~/projects-ai/expert-match-root/expert-match/target/expert-match.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**Enable and start:**

```bash
sudo systemctl daemon-reload
sudo systemctl enable expert-match
sudo systemctl start expert-match
sudo systemctl status expert-match
```

## Summary

1.  Connect to remote server via SSH
2.  Navigate to project directory
3.  Set environment variables
4.  Start service (using Maven, JAR, or systemd)
5.  Verify service is running and accessible
6.  Configure API client with remote server URL (`http://192.168.0.73:8080`)
7.  Test endpoints using API client

## Additional Resources

- **Development Setup**: `docs/DEVELOPMENT_SETUP.md`
- **API Documentation**: `http://192.168.0.73:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://192.168.0.73:8080/api/v1/openapi.json`

