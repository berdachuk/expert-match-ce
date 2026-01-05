# Quick Start: Remote Server Testing

## Quick Commands

### On Remote Server (192.168.0.73)

```bash
# 1. Connect to server
ssh user@192.168.0.73

# 2. Navigate to project
cd ~/projects-ai/expert-match-root/expert-match

# 3. Start database container (if not already running)
docker compose -f docker-compose.dev.yml up -d

# 4. Set profile (choose one)
export SPRING_PROFILES_ACTIVE=local  # For Ollama
# OR
export SPRING_PROFILES_ACTIVE=dev    # For Azure OpenAI

# 5. Start service
mvn spring-boot:run -P local  # or -P dev

# 6. Verify (in another terminal)
curl http://localhost:8093/actuator/health
```

### In API Client (Local Machine)

1. **Set Base URL**: Configure your API client with `base_url` = `http://192.168.0.73:8080`
2. **Test**: Run a health check request to verify connectivity

### If Direct Connection Fails

Use SSH port forwarding on your local machine:

```bash
ssh -L 8093:localhost:8093 user@192.168.0.73
```

Then use `http://localhost:8093` in your API client.

## Full Documentation

See `docs/REMOTE_SSH_SETUP.md` for complete instructions.
