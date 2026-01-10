# Conversation History Management

**Date:** 2025-12-28  
**Status:** Implemented

---

## Overview

ExpertMatch implements intelligent conversation history management with token counting and automatic summarization to
ensure conversation context fits within LLM context window limits while maintaining useful context for follow-up
queries.

## Architecture

### Components

1. **TokenCountingService**: Estimates tokens in text messages and conversation history
2. **ConversationHistoryManager**: Manages history retrieval, token counting, and summarization
3. **Summarization Prompt Template**: LLM prompt for condensing old messages

### Flow

```
QueryService.processQuery()
    ↓
ConversationHistoryManager.getOptimizedHistory()
    ↓
1. Fetch messages from database (up to 50)
    ↓
2. Count tokens for all messages
    ↓
3. Check if within limits
    ├─ Yes → Return history as-is
    └─ No → Optimize history
        ├─ Keep recent messages (half of max-messages)
        ├─ Summarize older messages using LLM
        └─ Combine summary + recent messages
            └─ If still exceeds limits → Recursively optimize
```

## Token Counting

### Estimation Algorithm

- **Method**: ~4 characters per token (conservative estimate for English text)
- **Accuracy**: Reasonable approximation for most use cases
- **Note**: Actual token counts may vary based on:
- Language (non-English may have different ratios)
    - Model tokenizer (different models tokenize differently)
    - Special characters and whitespace

### Token Counting Methods

1. **estimateTokens(String text)**: Counts tokens in raw text
2. **estimateFormattedMessageTokens(String role, String content)**: Counts tokens including formatting ("User: " or "Assistant: " prefix)
3. **estimateHistoryTokens(List<Message>)**: Counts tokens for entire history including section headers
4. **estimateSectionTokens(String sectionText)**: Counts tokens for prompt sections

## History Optimization

### Strategy

When history exceeds limits, the system:

1. **Splits History**:
- Recent messages: Keep as-is (half of `max-messages`)
    - Older messages: Summarize

2. **Summarization**:
- Uses LLM with `summarize-history.st` prompt template
    - Preserves key context, requirements, and decisions
    - Maintains information about experts, technologies, and skills
    - Removes redundant information
    - Limited to `max-summary-tokens` (default: 500)

3. **Combination**:
- Creates synthetic summary message: `[Previous conversation summary] {summary}`
    - Combines: Summary + Recent messages
    - Checks if still within limits

4. **Recursive Optimization**:
- If still exceeds token limit, further optimizes by summarizing oldest messages
    - Continues until within limits

### Example

**Before Optimization:**

- 20 messages, ~4000 tokens

**After Optimization:**

- 1 summary message (~500 tokens) + 5 recent messages (~1000 tokens)
- Total: ~1500 tokens (within 2000 token limit)

## Configuration

### Properties

```yaml
expertmatch:
  chat:
    history:
      # Maximum tokens for conversation history in context
      # Default: 2000 tokens
      max-tokens: ${EXPERTMATCH_CHAT_HISTORY_MAX_TOKENS:2000}

      # Maximum number of recent messages to include
      # Default: 10 messages
      max-messages: ${EXPERTMATCH_CHAT_HISTORY_MAX_MESSAGES:10}

      # Maximum tokens for summarized context
      # Default: 500 tokens per summary
      max-summary-tokens: ${EXPERTMATCH_CHAT_HISTORY_MAX_SUMMARY_TOKENS:500}
```

### Environment Variables

```bash
# Override defaults
export EXPERTMATCH_CHAT_HISTORY_MAX_TOKENS=3000
export EXPERTMATCH_CHAT_HISTORY_MAX_MESSAGES=15
export EXPERTMATCH_CHAT_HISTORY_MAX_SUMMARY_TOKENS=800
```

### Recommended Values

**For models with 256K context window (e.g., devstral-small-2:24b-cloud):**

- `max-tokens`: 2000-4000 (leaves room for query, expert info, instructions)
- `max-messages`: 10-20
- `max-summary-tokens`: 500-1000

**For models with smaller context windows:**

- `max-tokens`: 1000-2000
- `max-messages`: 5-10
- `max-summary-tokens`: 300-500

## Usage in Query Processing

### Integration

`QueryService` uses `ConversationHistoryManager` instead of direct repository calls:

```java
// Old approach (removed)
List<ConversationMessage> history = historyRepository.getHistory(chatId, 0, 11, "sequence_number,desc");

// New approach
List<ConversationMessage> history = historyManager.getOptimizedHistory(chatId, true, tracer);
```

### Benefits

1. **Automatic Optimization**: No manual intervention needed
2. **Token-Aware**: Prevents context window overflow
3. **Context Preservation**: Summarization maintains key information
4. **Configurable**: Adjust limits per environment
5. **Transparent**: Works automatically without changing query logic

## Summarization Prompt

The summarization uses a dedicated prompt template (`summarize-history.st`):

```
You are a conversation summarization assistant. Your task is to create a concise 
summary of a conversation history that preserves key information while reducing 
token count.

## Conversation History to Summarize

<history>

## Instructions

Create a concise summary that:

- Preserves important context, requirements, and decisions
- Maintains key information about experts, technologies, and skills mentioned
- Removes redundant information and verbose explanations
- Keeps the summary factual and objective
- Focuses on information that would be useful for future queries in this conversation
```

## Performance Considerations

### Token Counting

- **Speed**: Very fast (simple character counting)
- **Accuracy**: Good approximation for English text
- **Future Enhancement**: Could use model-specific tokenizers (e.g., tiktoken for OpenAI)

### Summarization

- **Cost**: Additional LLM call when history exceeds limits
- **Latency**: Adds ~1-3 seconds when summarization is needed
- **Frequency**: Only triggered when history exceeds limits
- **Caching**: Could cache summaries for repeated queries (future enhancement)

## Limitations

1. **Token Estimation**: Uses approximation (~4 chars/token), not exact counting
2. **Summarization Quality**: Depends on LLM model quality
3. **No Persistent Summaries**: Summaries are generated on-demand, not stored
4. **Single Summarization**: All old messages summarized together, not incrementally

## Future Enhancements

1. **Model-Specific Tokenizers**: Use actual tokenizers (tiktoken, etc.) for accurate counting
2. **Persistent Summaries**: Store summaries in database to avoid re-summarization
3. **Incremental Summarization**: Summarize messages incrementally as they age
4. **Summary Caching**: Cache summaries for frequently accessed chats
5. **Configurable Strategies**: Allow different summarization strategies (aggressive vs. conservative)

## Testing

### Unit Tests

- `TokenCountingServiceTest`: Test token estimation accuracy
- `ConversationHistoryManagerTest`: Test optimization logic

### Integration Tests

- Test with various history sizes
- Test summarization quality
- Test recursive optimization
- Test configuration overrides

## Related Documentation

- [Query Processing Flow](./QUERY_PROCESSING_FLOW_DETAILED.md)
- [ExpertMatch Architecture](./ExpertMatch.md)
- [Configuration Guide](./AI_PROVIDER_CONFIGURATION.md)

