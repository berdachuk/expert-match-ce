# AI Provider Configuration Guide

This guide explains how to configure different AI providers (Ollama or OpenAI) for chat, embedding, and reranking
components in ExpertMatch.

## Overview

ExpertMatch supports configuring different AI providers independently for each component:

- **Chat/LLM**: Query processing and answer generation
- **Embedding**: Vector embeddings for semantic search
- **Reranking**: Semantic reranking of search results

Each component can use either:

- **Ollama**: Local or remote Ollama instance (native API)
- **OpenAI**: OpenAI or OpenAI-compatible providers (Azure OpenAI, etc.)

## Configuration Methods

### Method 1: Environment Variables (Recommended)

Set environment variables for each component:

```bash
# Chat Configuration
export CHAT_PROVIDER=ollama  # or 'openai' for OpenAI
export CHAT_BASE_URL=http://localhost:11434  # or https://api.openai.com for OpenAI
export CHAT_API_KEY=  # Not required for local Ollama, required for OpenAI
export CHAT_MODEL=qwen3:30b-a3b-instruct-2507-q4_K_M  # or gpt-4 for OpenAI
export CHAT_TEMPERATURE=0.7

# Embedding Configuration
export EMBEDDING_PROVIDER=ollama
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=qwen3-embedding:8b

# Reranking Configuration
export RERANKING_PROVIDER=ollama
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M
export RERANKING_TEMPERATURE=0.1
```

### Method 2: Application Configuration Files

Configure in `application.yml` or profile-specific files (`application-local.yml`, `application-dev.yml`):

```yaml
spring:
  ai:
    custom:
      chat:
        provider: ${CHAT_PROVIDER:openai}
        base-url: ${CHAT_BASE_URL:https://api.openai.com}
        api-key: ${CHAT_API_KEY:}
        model: ${CHAT_MODEL:gpt-4}
        temperature: ${CHAT_TEMPERATURE:0.7}
      embedding:
        provider: ${EMBEDDING_PROVIDER:ollama}
        base-url: ${EMBEDDING_BASE_URL:http://localhost:11434}
        model: ${EMBEDDING_MODEL:qwen3-embedding:8b}
        dimensions: ${EMBEDDING_DIMENSIONS:1024}
      reranking:
        provider: ${RERANKING_PROVIDER:ollama}
        base-url: ${RERANKING_BASE_URL:http://localhost:11434}
        model: ${RERANKING_MODEL:dengcao/Qwen3-Reranker-8B:Q4_K_M}
        temperature: ${RERANKING_TEMPERATURE:0.1}
```

## Configuration Properties

### Chat Configuration

| Property                            | Environment Variable | Default  | Description                    |
|-------------------------------------|----------------------|----------|--------------------------------|
| `spring.ai.custom.chat.provider`    | `CHAT_PROVIDER`      | `openai` | Provider: `ollama` or `openai` |
| `spring.ai.custom.chat.base-url`    | `CHAT_BASE_URL`      | -        | Base URL for chat service      |
| `spring.ai.custom.chat.api-key`     | `CHAT_API_KEY`       | -        | API key (required for OpenAI)  |
| `spring.ai.custom.chat.model`       | `CHAT_MODEL`         | -        | Model name                     |
| `spring.ai.custom.chat.temperature` | `CHAT_TEMPERATURE`   | `0.7`    | Temperature for generation     |

### Embedding Configuration

| Property                                | Environment Variable   | Default  | Description                        |
|-----------------------------------------|------------------------|----------|------------------------------------|
| `spring.ai.custom.embedding.provider`   | `EMBEDDING_PROVIDER`   | `openai` | Provider: `ollama` or `openai`     |
| `spring.ai.custom.embedding.base-url`   | `EMBEDDING_BASE_URL`   | -        | Base URL for embedding service     |
| `spring.ai.custom.embedding.api-key`    | `EMBEDDING_API_KEY`    | -        | API key (required for OpenAI)      |
| `spring.ai.custom.embedding.model`      | `EMBEDDING_MODEL`      | -        | Model name                         |
| `spring.ai.custom.embedding.dimensions` | `EMBEDDING_DIMENSIONS` | `1536`   | Embedding dimensions (OpenAI only) |

### Reranking Configuration

| Property                                 | Environment Variable    | Default  | Description                                 |
|------------------------------------------|-------------------------|----------|---------------------------------------------|
| `spring.ai.custom.reranking.provider`    | `RERANKING_PROVIDER`    | `ollama` | Provider: `ollama` or `openai`              |
| `spring.ai.custom.reranking.base-url`    | `RERANKING_BASE_URL`    | -        | Base URL for reranking service              |
| `spring.ai.custom.reranking.api-key`     | `RERANKING_API_KEY`     | -        | API key (required for OpenAI)               |
| `spring.ai.custom.reranking.model`       | `RERANKING_MODEL`       | -        | Model name                                  |
| `spring.ai.custom.reranking.temperature` | `RERANKING_TEMPERATURE` | `0.1`    | Temperature (lower is better for reranking) |

## Provider-Specific Configuration

### Ollama Provider

**Base URL Examples:**

- Local: `http://localhost:11434`
- Remote: `http://your-ollama-server:11434`

**Model Examples:**

- Chat: `qwen3:4b-instruct-2507-q4_K_M`, `qwen3:30b-a3b-instruct-2507-q4_K_M`
- Embedding: `qwen3-embedding:8b`, `qwen3-embedding:0.6b`
- Reranking: `dengcao/Qwen3-Reranker-8B:Q4_K_M`, `dengcao/Qwen3-Reranker-0.6B:Q8_0`

**Notes:**

- No API key required
- Uses native Ollama API (not OpenAI-compatible)
- Embedding dimensions are fixed by model (e.g., 1024 for qwen3-embedding:8b)

### OpenAI Provider

**Base URL Examples:**

- OpenAI: `https://api.openai.com` (default, can be omitted)
- Azure OpenAI: `https://YOUR_RESOURCE.openai.azure.com`
- Other compatible: `https://api.provider.com/v1`

**Model Examples:**

- Chat: `gpt-4`, `gpt-3.5-turbo`, `gpt-4-turbo-preview`
- Embedding: `text-embedding-3-large`, `text-embedding-ada-002`
- Reranking: Typically uses chat models like `gpt-4`, `gpt-3.5-turbo`

**Notes:**

- API key required
- Uses OpenAI-compatible API format
- Embedding dimensions configurable (1536 recommended for text-embedding-3-large)

## Configuration Examples

### Example 1: All Ollama (Local Development)

```bash
export CHAT_PROVIDER=ollama
export CHAT_BASE_URL=http://localhost:11434
export CHAT_MODEL=qwen3:4b-instruct-2507-q4_K_M

export EMBEDDING_PROVIDER=ollama
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=qwen3-embedding:8b

export RERANKING_PROVIDER=ollama
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Example 2: All OpenAI (Production)

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=https://api.openai.com
export CHAT_API_KEY=sk-...
export CHAT_MODEL=gpt-4

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=https://api.openai.com
export EMBEDDING_API_KEY=sk-...
export EMBEDDING_MODEL=text-embedding-3-large
export EMBEDDING_DIMENSIONS=1536

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=https://api.openai.com
export RERANKING_API_KEY=sk-...
export RERANKING_MODEL=gpt-4
```

### Example 3: Mixed Providers (Cost Optimization)

Use OpenAI for chat (best quality), Ollama for embedding and reranking (cost-effective):

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=https://api.openai.com
export CHAT_API_KEY=sk-...
export CHAT_MODEL=gpt-4

export EMBEDDING_PROVIDER=ollama
export EMBEDDING_BASE_URL=http://localhost:11434
export EMBEDDING_MODEL=qwen3-embedding:8b

export RERANKING_PROVIDER=ollama
export RERANKING_BASE_URL=http://localhost:11434
export RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Example 4: Azure OpenAI for All Components

```bash
export CHAT_PROVIDER=openai
export CHAT_BASE_URL=https://YOUR_RESOURCE.openai.azure.com
export CHAT_API_KEY=azure-key
export CHAT_MODEL=gpt-4

export EMBEDDING_PROVIDER=openai
export EMBEDDING_BASE_URL=https://YOUR_RESOURCE.openai.azure.com
export EMBEDDING_API_KEY=azure-key
export EMBEDDING_MODEL=text-embedding-3-large

export RERANKING_PROVIDER=openai
export RERANKING_BASE_URL=https://YOUR_RESOURCE.openai.azure.com
export RERANKING_API_KEY=azure-key
export RERANKING_MODEL=gpt-4
```

## Fallback Behavior

If custom configuration is not provided, the application falls back to auto-configured models:

1. **Auto-configuration**: Uses `spring.ai.ollama.*` or `spring.ai.openai.*` properties
2. **Profile-based selection**:

     - `local` profile: Prefers Ollama
    - `dev`, `staging`, `prod` profiles: Prefer OpenAI
3. **Multiple models**: If both Ollama and OpenAI are configured, selects based on active profile

## Verification

To verify your configuration:

1. Check application logs for provider selection:
   ```
   Creating custom ChatModel with provider: openai, base URL: https://api.openai.com
   Creating custom EmbeddingModel with provider: ollama, base URL: http://localhost:11434
   Creating custom reranking ChatModel with provider: ollama, base URL: http://localhost:11434
   ```

2. Test each component:

     - **Chat**: Submit a query and check response quality
    - **Embedding**: Generate embeddings and verify dimensions
    - **Reranking**: Enable reranking and check result ordering

3. Monitor performance and costs:

     - Track API usage for OpenAI components
    - Monitor Ollama server performance
    - Adjust providers based on requirements

## Troubleshooting

### Issue: Custom configuration not being used

**Solution**:

- Verify environment variables are set correctly
- Check that `spring.ai.custom.*` properties are in the correct profile's configuration file
- Ensure base URLs are accessible and correct

### Issue: Provider mismatch errors

**Solution**:

- Verify provider value is exactly `ollama` or `openai` (case-insensitive)
- Check that base URLs match the provider type (Ollama uses native API, OpenAI uses compatible API)

### Issue: API key errors

**Solution**:

- Ensure API keys are set for OpenAI providers
- For Ollama, API key is optional (can be any value or omitted)
- Verify API keys have correct permissions

## Best Practices

1. **Cost Optimization**: Use Ollama for embedding and reranking (high volume, lower cost), OpenAI for chat (
   quality-critical)
2. **Performance**: Use local Ollama for faster responses, remote OpenAI for better quality
3. **Reliability**: Configure fallback providers in case primary provider is unavailable
4. **Security**: Never commit API keys to version control, use environment variables or secure vaults
5. **Testing**: Test each provider configuration independently before deploying

## Related Documentation

- [Quick Start Guide](QUICK_START.md) - Basic setup and configuration
- [Development Guide](DEVELOPMENT_GUIDE.md) - Detailed development instructions

