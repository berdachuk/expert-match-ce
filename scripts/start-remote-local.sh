#!/bin/bash
# Start ExpertMatch service on remote server with local profile
# Usage: ./scripts/start-remote-local.sh

set -e

# Navigate to project directory
cd "$(dirname "$0")/.." || exit 1

# Set local profile
export SPRING_PROFILES_ACTIVE=local

# Check if JAR exists, use it if available, otherwise use Maven
if [ -f "target/expert-match.jar" ]; then
    echo "Starting service using JAR file..."
    nohup java -jar target/expert-match.jar \
        --spring.profiles.active=local \
        > /tmp/expert-match.log 2>&1 &
    echo "Service started in background. PID: $!"
    echo "Logs: tail -f /tmp/expert-match.log"
else
    echo "JAR not found, starting with Maven..."
    nohup mvn spring-boot:run -P local \
        > /tmp/expert-match.log 2>&1 &
    echo "Service started in background. PID: $!"
    echo "Logs: tail -f /tmp/expert-match.log"
fi

echo ""
echo "Service should be accessible at: http://192.168.0.73:8093"
echo "Health check: curl http://localhost:8093/actuator/health"
echo "To stop: kill $!"




