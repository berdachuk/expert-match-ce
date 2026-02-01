#!/bin/bash
# Start ExpertMatch service on remote server with local profile
# Usage: ./scripts/start-remote-local.sh

set -e

# Navigate to project directory
cd "$(dirname "$0")/.." || exit 1

# Set local profile and port for remote access (192.168.0.73:8093)
export SPRING_PROFILES_ACTIVE=local
export SERVER_PORT=8093

# Check if JAR exists, use it if available, otherwise use Maven
if [ -f "target/expert-match.jar" ]; then
    echo "Starting service using JAR file..."
    nohup java -jar target/expert-match.jar \
        --spring.profiles.active=local \
        --server.port=8093 \
        > /tmp/expert-match.log 2>&1 &
    echo "Service started in background. PID: $!"
    echo "Logs: tail -f /tmp/expert-match.log"
else
    echo "JAR not found, starting with Maven..."
    nohup mvn spring-boot:run -P local -Dspring-boot.run.arguments="--server.port=8093" \
        > /tmp/expert-match.log 2>&1 &
    echo "Service started in background. PID: $!"
    echo "Logs: tail -f /tmp/expert-match.log"
fi

echo ""
echo "Service should be accessible at: http://192.168.0.73:8093"
echo "Health check: curl http://localhost:8093/actuator/health"
echo "To stop: kill $!"




