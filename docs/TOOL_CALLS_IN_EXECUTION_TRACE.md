# Tool Calls in Execution Trace - Current Status

**Date:** 2026-01-18  
**Issue:** Tool calls are not appearing in Execution Trace

---

## Problem

When making queries, the Execution Trace shows:

- ✅ "Generate Answer (RAG) SUCCESS" step
- ✅ Token usage, model name, etc.
- ❌ **No tool calls** (no "Tool Call: ..." steps)

---

## Root Cause

**The RAG (Retrieval-Augmented Generation) pattern doesn't use tool calling.**

### Current Flow:

1. **Query Processing**: Expert data is retrieved BEFORE the LLM call
2. **RAG Prompt Building**: All expert information is included in the prompt
3. **LLM Call**: LLM receives prompt with all data and generates text
4. **Result**: No tool calls happen because LLM doesn't need tools

### Why No Tool Calls?

```java
// AnswerGenerationServiceImpl.generateAnswer()
String prompt = buildRAGPrompt(query, expertContexts, intent, conversationHistory);
// ↑ This prompt includes ALL expert data

ChatResponse response = chatClient.prompt()
    .user(prompt)  // ← Prompt with all data, no tool calling needed
    .call()
    .chatResponse();
```

The LLM sees:

- User query
- **All expert information** (skills, projects, metadata)
- Instructions to format the answer

Since all data is already in the prompt, the LLM just generates text - **no tool calls are made**.

---

## When Tool Calls WOULD Appear

Tool calls would appear if:

1. **LLM decides to call a tool** (e.g., `expertQuery`, `Skill`, `readFile`)
2. **Tool is actually invoked** by the LLM
3. **ToolCallTracingAdvisor captures** the tool call
4. **ExecutionTracer records** it in the trace

### Example Scenarios Where Tool Calls Happen:

1. **Direct Tool Invocation**:
   ```
   User: "Use the expertQuery tool to find Java experts"
   → LLM calls expertQuery tool
   → Tool call appears in Execution Trace
   ```

2. **Skill Invocation**:
   ```
   User: "Use the expert-matching-hybrid-retrieval skill"
   → LLM calls Skill tool with skill name
   → Tool call appears in Execution Trace
   ```

3. **File System Tools**:
   ```
   User: "Read the documentation file"
   → LLM calls readFile tool
   → Tool call appears in Execution Trace
   ```

---

## Current Implementation Status

### ✅ What's Working:

1. **ToolCallTracingAdvisor** is registered and active
2. **ExecutionTracer** is set before LLM calls
3. **Tool call recording logic** is implemented
4. **Tools are registered** on ChatClient (SkillsTool, Java @Tool methods, FileSystemTools)

### ❌ What's Not Happening:

1. **LLM is not calling tools** in RAG pattern
2. **No tool calls to capture** = nothing to record
3. **Execution Trace shows only LLM generation step**

---

## Verification

### Check if Advisor is Being Called:

```bash
tail -100 /tmp/expertmatch.log | grep "ToolCallTracingAdvisor"
```

**Expected**: `ToolCallTracingAdvisor.adviseCall() called, tracer: present`

### Check if Tool Calls Are Found:

```bash
tail -100 /tmp/expertmatch.log | grep -E "Found.*tool calls|No tool calls|getToolCalls"
```

**Current Result**: `No tool calls found in message` (because LLM doesn't call tools)

---

## Solutions

### Option 1: Modify RAG Pattern to Use Tool Calling

**Change**: Remove expert data from prompt, let LLM call `expertQuery` tool

**Pros**:

- Tool calls will appear in Execution Trace
- More transparent about what tools are used
- Better matches user expectations

**Cons**:

- Requires significant refactoring
- May change answer quality
- More LLM calls (tool call + generation)

### Option 2: Add Tool Call Information to RAG Step

**Change**: Add metadata to "Generate Answer (RAG)" step showing available tools

**Pros**:

- Minimal changes
- Shows tools are available
- Doesn't change current flow

**Cons**:

- Doesn't show actual tool calls
- May be misleading

### Option 3: Use Different Pattern for Tool-Heavy Queries

**Change**: Detect queries that should use tool calling, use different flow

**Pros**:

- Best of both worlds
- Tool calls when needed
- RAG when appropriate

**Cons**:

- More complex logic
- Need to detect when to use which pattern

---

## Recommended Approach

**For now**: Document that tool calls appear only when LLM actually calls tools, not in standard RAG flow.

**Future**: Consider Option 3 - use tool calling pattern when user explicitly requests tool use or when expert data is
not pre-retrieved.

---

## Testing Tool Call Tracking

To verify tool call tracking works, test with a query that forces tool use:

```bash
curl -X POST http://localhost:8093/api/v1/query \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{
    "query": "Call the expertQuery tool with query=\"Java experts\"",
    "options": {
      "includeExecutionTrace": true
    }
  }'
```

**Expected**: If LLM calls the tool, it should appear in Execution Trace.

---

## Conclusion

**Tool calls are not appearing because the LLM is not calling tools** in the current RAG implementation. The
infrastructure is in place to track tool calls, but no tool calls are happening because the RAG pattern includes all
data in the prompt.

To see tool calls, either:

1. Modify queries to explicitly request tool use
2. Refactor RAG pattern to use tool calling
3. Use a different pattern that relies on tool calling
