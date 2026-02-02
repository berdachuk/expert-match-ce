#!/bin/bash
# Restart ExpertMatch service with local profile and verify Agent Skills configuration

set -e

SERVICE_HOST="${1:-192.168.0.73}"
SERVICE_PORT=8093
LOG_FILE="/tmp/expertmatch.log"
JAR_PATH="/home/berdachuk/projects-ai/expert-match-root/expert-match/target/expert-match.jar"
WORK_DIR="/home/berdachuk/projects-ai/expert-match-root/expert-match"

echo "🔄 Restarting ExpertMatch Service on ${SERVICE_HOST}"
echo ""

# Step 1: Stop existing service
echo "1️⃣ Stopping existing service..."
ssh ${SERVICE_HOST} "ps aux | grep '[j]ava.*expert-match.jar' | awk '{print \$2}' | head -1 | xargs kill 2>/dev/null && echo '✅ Service stopped' || echo 'ℹ️  Service was not running'"
sleep 2

# Step 2: Start service with local profile
echo ""
echo "2️⃣ Starting service with local profile..."
ssh ${SERVICE_HOST} "cd ${WORK_DIR} && nohup java -jar ${JAR_PATH} --spring.profiles.active=local --server.address=0.0.0.0 --server.port=${SERVICE_PORT} > ${LOG_FILE} 2>&1 &"
sleep 5

# Step 3: Wait for startup
echo ""
echo "3️⃣ Waiting for service to start..."
for i in {1..30}; do
    if ssh ${SERVICE_HOST} "curl -s http://localhost:${SERVICE_PORT}/actuator/health > /dev/null 2>&1"; then
        echo "✅ Service is healthy"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Service failed to start after 30 seconds"
        exit 1
    fi
    sleep 1
done

# Step 4: Verify Agent Skills configuration
echo ""
echo "4️⃣ Verifying Agent Skills configuration..."
echo ""

echo "📋 Checking ChatClient configuration:"
ssh ${SERVICE_HOST} "tail -500 ${LOG_FILE} | grep -E 'Creating.*ChatClient|chatClientWithSkills|chatClientWithToolSearch' | tail -5"

echo ""
echo "📋 Checking SkillsTool initialization:"
ssh ${SERVICE_HOST} "tail -500 ${LOG_FILE} | grep -E 'Creating SkillsTool|Added.*skills directory|skills resource|skills found' | tail -5"

echo ""
echo "📋 Checking Tool Search status:"
ssh ${SERVICE_HOST} "tail -500 ${LOG_FILE} | grep -E 'Tool Search|toolSearch|ToolSearchConfiguration' | tail -3"

echo ""
echo "📋 Checking application startup:"
ssh ${SERVICE_HOST} "tail -100 ${LOG_FILE} | grep -E 'Started ExpertMatchApplication|Application startup' | tail -2"

echo ""
echo "✅ Service restart complete!"
echo ""
echo "🌐 Service URL: http://${SERVICE_HOST}:${SERVICE_PORT}"
echo "📊 Health Check: http://${SERVICE_HOST}:${SERVICE_PORT}/actuator/health"
echo "📝 Log File: ${LOG_FILE} on ${SERVICE_HOST}"
