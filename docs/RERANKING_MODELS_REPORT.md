# Reranking Models Available in Ollama

**Date**: 2025-12-04  
**Purpose**: Document available reranking models similar to `qwen3-reranker-8b` for Ollama  
**Source**: Ollama Library and Qwen3 Reranker Series

## Executive Summary

✅ **Primary Model**: `dengcao/Qwen3-Reranker-8B` (multiple quantization versions)  
✅ **Alternative Models**: `dengcao/Qwen3-Reranker-0.6B`, `dengcao/Qwen3-Reranker-4B`  
⚠️ **Model Name Format**: Uses `dengcao/Qwen3-Reranker-8B:QUANTIZATION` format, not `qwen3-reranker-8b`

## Available Reranking Models

### 1. Qwen3-Reranker-8B (Recommended)

**Model Name**: `dengcao/Qwen3-Reranker-8B`  
**Parameters**: 8 billion  
**Context Length**: 32K tokens  
**Languages**: 100+ languages  
**Developer**: Alibaba Cloud

#### Available Quantization Versions

| Quantization  | Size  | Quality   | Recommended For                   |
|---------------|-------|-----------|-----------------------------------|
| `Q3_K_M`      | 4.1GB | Good      | Resource-constrained environments |
| `Q4_K_M`      | 5.0GB | Very Good | **Recommended** - Best balance    |
| `Q5_K_M`      | 5.8GB | Excellent | High-quality reranking            |
| `Q8_0`        | 8.7GB | Excellent | Maximum quality                   |
| `F16`         | 16GB  | Maximum   | Research/development              |

**Pull Command**:
```bash
# Recommended: Q4_K_M (best balance)
ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M

# Or other versions:
ollama pull dengcao/Qwen3-Reranker-8B:Q5_K_M
ollama pull dengcao/Qwen3-Reranker-8B:Q8_0
```

**Usage in Configuration**:
```yaml
spring:
  ai:
    ollama:
      reranking:
        options:
          model: dengcao/Qwen3-Reranker-8B:Q4_K_M
```

---

### 2. Qwen3-Reranker-0.6B (Lightweight)

**Model Name**: `dengcao/Qwen3-Reranker-0.6B`  
**Parameters**: 0.6 billion  
**Context Length**: 32K tokens  
**Languages**: 100+ languages  
**Size**: ~639MB (Q8_0)

**Use Case**: Resource-constrained environments, fast inference

**Pull Command**:
```bash
ollama pull dengcao/Qwen3-Reranker-0.6B:Q8_0
```

**Usage in Configuration**:
```yaml
spring:
  ai:
    ollama:
      reranking:
        options:
          model: dengcao/Qwen3-Reranker-0.6B:Q8_0
```

---

### 3. Qwen3-Reranker-4B (Medium)

**Model Name**: `dengcao/Qwen3-Reranker-4B`  
**Parameters**: 4 billion  
**Context Length**: 32K tokens  
**Languages**: 100+ languages

**Use Case**: Balance between quality and resource usage

**Pull Command**:
```bash
ollama pull dengcao/Qwen3-Reranker-4B:Q4_K_M
```

---

## Model Comparison

| Model                   | Parameters  | Size (Q4_K_M)  | Quality  | Speed     | Use Case                 |
|-------------------------|-------------|----------------|----------|-----------|--------------------------|
| **Qwen3-Reranker-8B**   | 8B          | 5.0GB          | ⭐⭐⭐⭐⭐    | Medium    | Production, high quality |
| **Qwen3-Reranker-4B**   | 4B          | ~2.5GB         | ⭐⭐⭐⭐     | Fast      | Balanced performance     |
| **Qwen3-Reranker-0.6B** | 0.6B        | 639MB          | ⭐⭐⭐      | Very Fast | Resource-limited         |

---

## Installation Instructions

### Step 1: Pull the Model

Choose the model and quantization level that fits your needs:

```bash
# Recommended: Qwen3-Reranker-8B with Q4_K_M quantization
ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Step 2: Verify Installation

Check that the model is available:

```bash
# List all models
ollama list

# Test the model
ollama run dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Step 3: Update Configuration

Update `application-local.yml` (or your profile configuration):

```yaml
spring:
  ai:
    ollama:
      reranking:
        options:
          model: dengcao/Qwen3-Reranker-8B:Q4_K_M

expertmatch:
  retrieval:
    reranking:
      enabled: true
      provider: ollama
      model: dengcao/Qwen3-Reranker-8B:Q4_K_M
```

---

## Model Name Format

### Important: Model Name Format

The correct model name format in Ollama is:

- ✅ **Correct**: `dengcao/Qwen3-Reranker-8B:Q4_K_M`
- ❌ **Incorrect**: `qwen3-reranker-8b`
- ❌ **Incorrect**: `qwen3-reranker:8b`

**Format**: `dengcao/Qwen3-Reranker-{SIZE}:{QUANTIZATION}`

Where:

- `{SIZE}`: `8B`, `4B`, or `0.6B`
- `{QUANTIZATION}`: `Q3_K_M`, `Q4_K_M`, `Q5_K_M`, `Q8_0`, or `F16`

---

## Testing the Model

### Test Reranking with Ollama API

```bash
# Test reranking (example - actual API may vary)
curl http://localhost:11434/api/generate -d '{
  "model": "dengcao/Qwen3-Reranker-8B:Q4_K_M",
  "prompt": "Query: Java expert\nDocument 1: Senior Java developer with Spring Boot experience\nDocument 2: Python developer with Django experience",
  "stream": false
}'
```

### Verify Model is Running

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check specific model
curl http://localhost:11434/api/show -d '{
  "name": "dengcao/Qwen3-Reranker-8B:Q4_K_M"
}'
```

---

## Performance Characteristics

### Qwen3-Reranker-8B (Q4_K_M)

- **Inference Speed**: ~50-100ms per query-document pair (depends on hardware)
- **Memory Usage**: ~5-6GB RAM
- **Accuracy**: High-quality reranking, comparable to state-of-the-art models
- **Context Window**: 32K tokens
- **Multilingual**: Supports 100+ languages

### Resource Requirements

| Model                    | RAM (Min)  | RAM (Recommended)  | GPU (Optional)  |
|--------------------------|------------|--------------------|-----------------|
| Qwen3-Reranker-8B:Q4_K_M | 6GB        | 8GB+               | 4GB+ VRAM       |
| Qwen3-Reranker-4B:Q4_K_M | 3GB        | 4GB+               | 2GB+ VRAM       |
| Qwen3-Reranker-0.6B:Q8_0 | 1GB        | 2GB+               | Not required    |

---

## Integration with Spring AI

### Configuration Example

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      reranking:
        options:
          model: dengcao/Qwen3-Reranker-8B:Q4_K_M

expertmatch:
  retrieval:
    reranking:
      enabled: true
      provider: ollama
      model: dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Environment Variables

You can also configure via environment variables:

```bash
export OLLAMA_RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M
export RERANKING_MODEL=dengcao/Qwen3-Reranker-8B:Q4_K_M
```

---

## Alternative Reranking Models

### E2Rank (Not in Ollama)

- **Type**: Embedding + Reranking model
- **Special Token**: Requires `<|endoftext|>` token
- **Source**: [Alibaba-NLP/E2Rank](https://github.com/Alibaba-NLP/E2Rank)
- **Note**: Not directly available in Ollama, requires custom setup

### REARANK (Research Model)

- **Type**: Reasoning-based reranking agent
- **Method**: Reinforcement learning
- **Performance**: Comparable to GPT-4
- **Source**: [arXiv:2505.20046](https://arxiv.org/abs/2505.20046)
- **Note**: Research model, not production-ready in Ollama

---

## Troubleshooting

### Issue: Model Not Found

**Error**: `model not found` or `model 'qwen3-reranker-8b' not found`

**Solution**:
1. Use the correct model name: `dengcao/Qwen3-Reranker-8B:Q4_K_M`
2. Pull the model first: `ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M`
3. Verify with: `ollama list`

### Issue: Model Name Mismatch

**Error**: Configuration uses `qwen3-reranker-8b` but model is `dengcao/Qwen3-Reranker-8B:Q4_K_M`

**Solution**: Update configuration to use exact model name:
```yaml
model: dengcao/Qwen3-Reranker-8B:Q4_K_M
```

### Issue: Out of Memory

**Error**: `out of memory` or `OOM`

**Solution**:
1. Use a smaller model: `dengcao/Qwen3-Reranker-0.6B:Q8_0`
2. Use lower quantization: `Q3_K_M` instead of `Q4_K_M`
3. Increase available RAM or use GPU

### Issue: Slow Inference

**Solution**:
1. Use GPU acceleration (if available)
2. Use smaller model: `Qwen3-Reranker-0.6B`
3. Use lower quantization: `Q3_K_M` or `Q4_K_M`

---

## Recommendations

### For Production Use

✅ **Recommended**: `dengcao/Qwen3-Reranker-8B:Q4_K_M`
- Best balance of quality and resource usage
- 5.0GB size, excellent performance
- Supports 32K context window

### For Development/Testing

✅ **Recommended**: `dengcao/Qwen3-Reranker-0.6B:Q8_0`
- Fast inference
- Low resource usage (639MB)
- Good enough for testing

### For Maximum Quality

✅ **Recommended**: `dengcao/Qwen3-Reranker-8B:Q8_0` or `F16`
- Highest quality reranking
- Requires more resources (8.7GB or 16GB)

---

## Quick Start

1. **Pull the model**:
   ```bash
   ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M
   ```

2. **Update configuration**:
   ```yaml
   spring:
     ai:
       ollama:
         reranking:
           options:
             model: dengcao/Qwen3-Reranker-8B:Q4_K_M
   ```

3. **Restart application**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local
   ```

4. **Verify**:

    - Check logs for reranking model initialization
   - Test a query with reranking enabled

---

## References

- **Ollama Library**: [Qwen3-Reranker-8B](https://ollama.com/dengcao/Qwen3-Reranker-8B)
- **Ollama Library**: [Qwen3-Reranker-0.6B](https://ollama.com/dengcao/Qwen3-Reranker-0.6B)
- **E2Rank**: [GitHub Repository](https://github.com/Alibaba-NLP/E2Rank)
- **REARANK Paper**: [arXiv:2505.20046](https://arxiv.org/abs/2505.20046)

---

## Summary

✅ **Primary Recommendation**: `dengcao/Qwen3-Reranker-8B:Q4_K_M`  
✅ **Model Format**: `dengcao/Qwen3-Reranker-{SIZE}:{QUANTIZATION}`  
✅ **Available Sizes**: 8B, 4B, 0.6B  
✅ **Quantization Options**: Q3_K_M, Q4_K_M, Q5_K_M, Q8_0, F16  
✅ **Integration**: Update `application-local.yml` with correct model name

**Next Steps**:
1. Pull the recommended model: `ollama pull dengcao/Qwen3-Reranker-8B:Q4_K_M`
2. Update configuration with correct model name
3. Test reranking functionality

---

**Report Generated**: 2025-12-04  
**Last Updated**: 2025-12-21

