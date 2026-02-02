# Service Restart Guide - Agent Skills Verification

**Date:** 2026-01-16  
**Purpose:** Restart service and verify Agent Skills configuration

---

## Quick Restart Commands

### On 192.168.0.73:

```bash
# 1. Stop existing service
ps aux | grep "[j]ava.*expert-match.jar" | awk '{print $2}' | head -1 | xargs kill

# 2. Start with local profile
cd /home/berdachuk/projects-ai/expert-match-root/expert-match
nohup java -jar target/expert-match.jar \
  --spring.profiles.active=local \
  --server.address=0.0.0.0 \
  --server.port=8093 \
  > /tmp/expertmatch.log 2>&1 &

# 3. Wait for startup (10-15 seconds)
sleep 15

# 4. Check health
curl http://localhost:8093/actuator/health
```

---

## Verification Steps

### Step 1: Check Service Status

```bash
# Check if service is running
ps aux | grep "[j]ava.*expert-match.jar"

# Check health endpoint
curl http://localhost:8093/actuator/health
```

**Expected**: Service should be running and health check should return `{"status":"UP"}`

### Step 2: Verify ChatClient Configuration

```bash
tail -500 /tmp/expertmatch.log | grep -E "Creating.*ChatClient|chatClientWithSkills|chatClientWithToolSearch"
```

**Expected Output**:

```
✅ "Creating chatClientWithSkills with Agent Skills enabled"
❌ Should NOT see "Creating chatClientWithToolSearch" (Tool Search disabled)
```

### Step 3: Verify SkillsTool Initialization

```bash
tail -500 /tmp/expertmatch.log | grep -E "Creating SkillsTool|Added.*skills directory|skills resource|skills found"
```

**Expected Output**:

```
✅ "Creating SkillsTool bean for Agent Skills"
✅ "Added local filesystem skills directory: .claude/skills"
✅ "Creating FileSystemTools bean for reading skill references"
```

### Step 4: Verify Tool Search is Disabled

```bash
tail -500 /tmp/expertmatch.log | grep -E "ToolSearchConfiguration|Tool Search Tool|toolSearchToolCallAdvisor"
```

**Expected Output**:

- ❌ Should be EMPTY (no Tool Search messages)

### Step 5: Check Application Startup

```bash
tail -200 /tmp/expertmatch.log | grep -E "Started ExpertMatchApplication|Application startup"
```

**Expected Output**:

```
✅ "Started ExpertMatchApplication" (with no errors)
```

---

## Complete Verification Script

```bash
#!/bin/bash
echo "🔍 Verifying Agent Skills Configuration"
echo "======================================"
echo ""

echo "1️⃣ ChatClient Configuration:"
tail -1000 /tmp/expertmatch.log | grep -E "Creating.*ChatClient|chatClientWithSkills|chatClientWithToolSearch" | tail -5
echo ""

echo "2️⃣ SkillsTool Initialization:"
tail -1000 /tmp/expertmatch.log | grep -E "Creating SkillsTool|Added.*skills directory|skills resource|skills found" | tail -5
echo ""

echo "3️⃣ Tool Search Status:"
tail -1000 /tmp/expertmatch.log | grep -E "ToolSearchConfiguration|Tool Search Tool" | tail -3
echo ""

echo "4️⃣ Application Startup:"
tail -200 /tmp/expertmatch.log | grep -E "Started ExpertMatchApplication" | tail -2
echo ""

echo "✅ Verification complete!"
```

---

## Expected Log Messages

### ✅ Should Appear:

1. **Agent Skills Configuration**:
   ```
   Creating SkillsTool bean for Agent Skills
   Added local filesystem skills directory: .claude/skills
   Creating FileSystemTools bean for reading skill references
   Creating ToolCallTracingAdvisor bean for tool call tracking
   ```

2. **ChatClient Creation**:
   ```
   Creating chatClientWithSkills with Agent Skills enabled
   ```

3. **Application Startup**:
   ```
   Started ExpertMatchApplication in X.XXX seconds
   ```

### ❌ Should NOT Appear:

1. **Tool Search Configuration**:
   ```
   Creating ToolSearchConfiguration
   Creating chatClientWithToolSearch
   ToolSearchToolCallAdvisor
   ```

---

## Troubleshooting

### Issue: Service won't start

**Check logs**:

```bash
tail -100 /tmp/expertmatch.log
```

**Common issues**:

- Port 8093 already in use: `lsof -i :8093`
- JAR file not found: Check `target/expert-match.jar` exists
- Database connection failed: Check PostgreSQL is running

### Issue: Skills not loading

**Check configuration**:

```bash
# Verify local profile is active
grep "spring.profiles.active" /tmp/expertmatch.log

# Verify skills directory exists
ls -la .claude/skills/

# Check for errors
grep -i "error\|exception\|failed" /tmp/expertmatch.log | tail -10
```

### Issue: Wrong ChatClient created

**Check conditions**:

```bash
# Should see chatClientWithSkills, NOT chatClientWithToolSearch
grep "Creating.*ChatClient" /tmp/expertmatch.log
```

**Fix**: Ensure `expertmatch.tools.search.enabled=false` in `application-local.yml`

---

## Test Agent Skills

After restart, test with a query:

```bash
curl -X POST http://localhost:8093/api/v1/query \
  -H "Content-Type: application/json" \
  -H "X-User-Id: test-user" \
  -d '{
    "query": "Use the expert-matching-hybrid-retrieval skill to find Java experts",
    "options": {
      "includeExecutionTrace": true
    }
  }' | python3 -m json.tool
```

**Check Execution Trace** for tool calls with skill names.

---

## Quick Status Check

```bash
# One-liner to check everything
echo "Service:" && ps aux | grep "[j]ava.*expert-match.jar" | grep -v grep && \
echo "Health:" && curl -s http://localhost:8093/actuator/health | python3 -m json.tool && \
echo "ChatClient:" && tail -500 /tmp/expertmatch.log | grep "Creating.*ChatClient" | tail -1 && \
echo "SkillsTool:" && tail -500 /tmp/expertmatch.log | grep "Creating SkillsTool" | tail -1
```
