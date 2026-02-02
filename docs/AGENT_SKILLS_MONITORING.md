# Monitoring Agent Skills Usage

This guide explains how to monitor and observe when Agent Skills are being used in ExpertMatch.

## Overview

Agent Skills are discovered and invoked by the LLM during conversations. To see when skills are used, you can:

1. **Enable DEBUG logging** - See tool calls in application logs
2. **Check Execution Trace** - Tool calls are now included as separate steps in the Execution Trace

### Execution Trace

**Tool Calls in Execution Trace**: Agent Skills and all tool calls (Java @Tool methods, FileSystemTools) are now tracked
as separate steps in the Execution Trace. Each tool call includes:

- Tool name and type (AGENT_SKILL, JAVA_TOOL, FILE_SYSTEM_TOOL)
- Full parameters (JSON)
- Response (JSON)
- Skill name (for Agent Skills)
- Duration

To see tool calls in Execution Trace, enable it in your query request:

```json
{
  "query": "Find Java experts",
  "options": {
    "includeExecutionTrace": true
  }
}
```

The Execution Trace will include steps like:

- "Tool Call: Skill" (for Agent Skills)
- "Tool Call: expertQuery" (for Java @Tool methods)
- "Tool Call: readFile" (for FileSystemTools)

**What IS visible in Execution Trace**:

- LLM steps that may have used tools (e.g., "Generate Answer (RAG)")
- Token usage for LLM calls
- Model name used

**What is NOT visible in Execution Trace**:

- Which specific skills were called
- Tool call parameters
- Tool call responses
- Individual tool call timing

**To see Agent Skills usage**, you need to check the application logs (see below).

## Quick Start: Enable Skill Usage Logging

### Option 1: Enable Debug Logging (Recommended)

Start the application with the `debug` profile to enable verbose logging:

```bash
java -jar target/expert-match.jar \
  --spring.profiles.active=local,debug \
  --server.address=0.0.0.0 \
  --server.port=8093 \
  --expertmatch.tools.search.enabled=false
```

This enables DEBUG/TRACE logging for:

- Spring AI ChatClient
- Tool call advisors
- Agent Skills components

### Option 2: Enable Specific Loggers

Add these logging levels to `application-local.yml` or via command line:

```yaml
logging:
  level:
    # Spring AI ChatClient and advisors (logs tool calls)
    org.springframework.ai.chat.client: DEBUG
    org.springframework.ai.chat.client.advisor: DEBUG
    # Agent Skills (Spring AI Agent Utils)
    org.springaicommunity.agent.tools: DEBUG
    org.springaicommunity.agent: DEBUG
```

Or via command line:

```bash
java -jar target/expert-match.jar \
  --spring.profiles.active=local \
  --server.address=0.0.0.0 \
  --server.port=8093 \
  --expertmatch.tools.search.enabled=false \
  --logging.level.org.springframework.ai.chat.client=DEBUG \
  --logging.level.org.springframework.ai.chat.client.advisor=DEBUG \
  --logging.level.org.springaicommunity.agent.tools=DEBUG \
  --logging.level.org.springaicommunity.agent=DEBUG
```

## What Gets Logged

### 1. Skill Loading (Application Startup)

When the application starts, you'll see:

```
INFO  c.b.e.c.c.AgentSkillsConfiguration - Creating SkillsTool bean for Agent Skills
INFO  c.b.e.c.c.AgentSkillsConfiguration - Added local filesystem skills directory: .claude/skills
INFO  c.b.e.c.c.AgentSkillsConfiguration - Creating FileSystemTools bean for reading skill references
INFO  c.b.e.c.c.AgentSkillsConfiguration - Creating chatClientWithSkills with Agent Skills enabled
```

### 2. Tool Call Logging (SimpleLoggerAdvisor)

The `SimpleLoggerAdvisor` logs all tool calls, including:

- **Tool name**: Which tool/skill was called
- **Tool parameters**: Input arguments
- **Tool response**: Output from the tool

Example log output:

```
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool call: Skill
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool parameters: {skillName=expert-matching-hybrid-retrieval, ...}
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool response: {...}
```

### 3. Skill Discovery and Invocation

When the LLM discovers and uses a skill, you'll see:

```
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool call: Skill
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool parameters: {skillName=expert-matching-hybrid-retrieval}
```

## Monitoring Methods

### Method 1: Real-time Log Monitoring

Watch logs in real-time:

```bash
# Monitor all logs
tail -f /tmp/expertmatch.log | grep -i "skill\|tool\|advisor"

# Monitor only skill-related logs
tail -f /tmp/expertmatch.log | grep -E "Skill|skill|Agent Skills|SimpleLoggerAdvisor"

# Monitor with context (5 lines before/after)
tail -f /tmp/expertmatch.log | grep -i "skill" -A 5 -B 5
```

### Method 2: Search Historical Logs

Search for skill usage in logs:

```bash
# Find all skill invocations
grep -i "Tool call.*Skill" /tmp/expertmatch.log

# Find skill discovery
grep -i "skillName" /tmp/expertmatch.log

# Find all tool calls (including skills)
grep -i "SimpleLoggerAdvisor" /tmp/expertmatch.log
```

### Method 3: Enable Debug Profile

Use the debug profile for comprehensive logging:

```bash
java -jar target/expert-match.jar \
  --spring.profiles.active=local,debug \
  --server.address=0.0.0.0 \
  --server.port=8093 \
  --expertmatch.tools.search.enabled=false
```

Logs will be written to `logs/expert-match-debug.log` with detailed information.

### Method 4: Check Application Logs

View the current log file:

```bash
# View recent logs
tail -100 /tmp/expertmatch.log

# View logs with skill-related entries highlighted
tail -100 /tmp/expertmatch.log | grep --color=always -i "skill\|tool"

# View logs from a specific time
grep "2026-01-16 21:" /tmp/expertmatch.log | grep -i "skill"
```

## Understanding Log Output

### Skill Loading Logs

```
INFO  c.b.e.c.c.AgentSkillsConfiguration - Creating SkillsTool bean for Agent Skills
INFO  c.b.e.c.c.AgentSkillsConfiguration - Added local filesystem skills directory: .claude/skills
```

This confirms skills are loaded at startup.

### Tool Call Logs (SimpleLoggerAdvisor)

```
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool call: Skill
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool parameters: {skillName=expert-matching-hybrid-retrieval, ...}
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool response: {...}
```

This shows:

- **Tool call**: The skill was invoked
- **Parameters**: Which skill and what parameters
- **Response**: What the skill returned

### ChatClient Logs

```
DEBUG o.s.a.c.c.ChatClient - Processing message with tools: [Skill, expertQuery, ...]
```

This shows which tools (including skills) are available to the LLM.

## Example: Monitoring Skill Usage

### Step 1: Start with Debug Logging

```bash
java -jar target/expert-match.jar \
  --spring.profiles.active=local,debug \
  --server.address=0.0.0.0 \
  --server.port=8093 \
  --expertmatch.tools.search.enabled=false \
  > /tmp/expertmatch.log 2>&1 &
```

### Step 2: Monitor Logs in Real-time

```bash
tail -f /tmp/expertmatch.log | grep -E "Skill|skill|SimpleLoggerAdvisor|Tool call"
```

### Step 3: Make a Query

Send a query to the API:

```bash
curl -X POST http://192.168.0.73:8093/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Find experts in Java and Spring Boot",
    "options": {
      "includeExecutionTrace": true
    }
  }'
```

### Step 4: Observe Logs

You should see logs like:

```
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool call: Skill
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool parameters: {skillName=expert-matching-hybrid-retrieval}
DEBUG o.s.a.c.c.advisor.SimpleLoggerAdvisor - Tool response: {...}
```

## Troubleshooting

### No Skill Logs Appearing

1. **Check if skills are enabled**:
   ```bash
   grep "Agent Skills" /tmp/expertmatch.log
   ```

2. **Verify logging level**:
   ```bash
   grep "logging.level" /tmp/expertmatch.log
   ```

3. **Check if SimpleLoggerAdvisor is configured**:
   ```bash
   grep "SimpleLoggerAdvisor" /tmp/expertmatch.log
   ```

### Skills Not Being Used

1. **Check if skills are loaded**:
   ```bash
   grep "Added.*skills directory" /tmp/expertmatch.log
   ```

2. **Verify ChatClient configuration**:
   ```bash
   grep "chatClientWithSkills" /tmp/expertmatch.log
   ```

3. **Check LLM responses**:
   Enable DEBUG logging for Spring AI to see LLM tool selection:
   ```yaml
   logging:
     level:
       org.springframework.ai: DEBUG
   ```

## Advanced Monitoring

### Custom Logging

To add custom logging for skill usage, you can:

1. **Add logging to AgentSkillsConfiguration**:
   ```java
   @Bean
   public ToolCallback skillsTool() {
       log.info("Creating SkillsTool bean for Agent Skills");
       // ... existing code ...
   }
   ```

2. **Monitor via Execution Trace**:
   Enable `includeExecutionTrace: true` in query options to see detailed step-by-step processing.

### Metrics Collection

For production monitoring, consider:

1. **Add metrics** for skill invocations
2. **Track skill usage** per skill name
3. **Monitor skill performance** (execution time, success rate)

## Execution Trace Limitations

### Current Behavior

The Execution Trace tracks high-level processing steps but **does not include individual tool calls** (including Agent
Skills). This is because:

1. **Tool calls happen inside LLM interactions**: Skills are invoked by the LLM during ChatClient calls, not as separate
   service calls
2. **ExecutionTrace tracks service-level steps**: It's designed to track major processing phases (Parse Query, Retrieve,
   Generate Answer)
3. **Tool calls are internal to LLM**: The LLM decides when to call tools, and these calls happen within a single LLM
   interaction

### What You'll See in Execution Trace

When Agent Skills are used, you'll see:

```json
{
  "name": "Generate Answer (RAG)",
  "service": "AnswerGenerationService",
  "method": "generateAnswer",
  "durationMs": 2340,
  "status": "SUCCESS",
  "inputSummary": "Query: Find Java experts, Experts: 5",
  "outputSummary": "Answer: 1234 characters",
  "llmModel": "OllamaChatModel (devstral-small-2:24b-cloud)",
  "tokenUsage": {
    "inputTokens": 1234,
    "outputTokens": 567,
    "totalTokens": 1801
  }
}
```

This step includes the LLM call that may have used Agent Skills, but doesn't show which skills were called.

### Future Enhancement

To see skills in Execution Trace, we would need to:

1. Create a custom `ToolCallAdvisor` that intercepts tool calls
2. Add tool call steps to ExecutionTrace
3. Track tool call parameters and responses

This is a potential future enhancement.

## Summary

To see when Agent Skills are used:

1. ✅ **Enable DEBUG logging** for Spring AI ChatClient and advisors (see logs)
2. ✅ **Check Execution Trace** - Tool calls are now included as separate steps with full details
3. ✅ **Use `SimpleLoggerAdvisor`** logs to see tool invocations in real-time

### Execution Trace (Recommended)

Enable Execution Trace in your query request to see all tool calls:

```json
{
  "query": "Find Java experts",
  "options": {
    "includeExecutionTrace": true
  }
}
```

The Execution Trace will show:

- All tool calls as separate steps
- Tool name, type, parameters, and response
- Skill name for Agent Skills
- Duration for each tool call

### Debug Logging (Real-time)

Enable DEBUG logging to see tool calls in real-time as they happen:

```bash
--logging.level.org.springframework.ai.chat.client=DEBUG
--logging.level.org.springaicommunity.agent.tools=DEBUG
```

Both methods provide visibility into Agent Skills usage, with Execution Trace providing structured, queryable data and
DEBUG logging providing real-time monitoring.
