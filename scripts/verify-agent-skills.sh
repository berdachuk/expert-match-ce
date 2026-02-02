#!/bin/bash
# Verify Agent Skills configuration from logs

SERVICE_HOST="${1:-192.168.0.73}"
LOG_FILE="/tmp/expertmatch.log"

echo "🔍 Verifying Agent Skills Configuration"
echo "======================================"
echo ""

echo "1️⃣ ChatClient Configuration:"
echo "----------------------------"
ssh ${SERVICE_HOST} "tail -1000 ${LOG_FILE} | grep -E 'Creating.*ChatClient|chatClientWithSkills|chatClientWithToolSearch|@Primary' | tail -10"
echo ""

echo "2️⃣ SkillsTool Initialization:"
echo "-------------------------------"
ssh ${SERVICE_HOST} "tail -1000 ${LOG_FILE} | grep -E 'Creating SkillsTool|Added.*skills directory|skills resource|skills found|No skills found' | tail -10"
echo ""

echo "3️⃣ Tool Search Status:"
echo "----------------------"
ssh ${SERVICE_HOST} "tail -1000 ${LOG_FILE} | grep -E 'ToolSearchConfiguration|Tool Search Tool|toolSearchToolCallAdvisor' | tail -5"
echo ""

echo "4️⃣ Application Startup:"
echo "-----------------------"
ssh ${SERVICE_HOST} "tail -200 ${LOG_FILE} | grep -E 'Started ExpertMatchApplication|Application startup|REAL LLM CONFIG' | tail -5"
echo ""

echo "5️⃣ Configuration Properties:"
echo "-----------------------------"
ssh ${SERVICE_HOST} "tail -1000 ${LOG_FILE} | grep -E 'expertmatch.skills.enabled|expertmatch.tools.search.enabled' | tail -5"
echo ""

echo "✅ Verification complete!"
echo ""
echo "Expected Results:"
echo "  ✅ 'Creating chatClientWithSkills' should appear"
echo "  ✅ 'Creating SkillsTool' should appear"
echo "  ✅ 'Added local filesystem skills directory' should appear"
echo "  ❌ 'ToolSearchConfiguration' should NOT appear (Tool Search disabled)"
echo "  ❌ 'chatClientWithToolSearch' should NOT appear"
