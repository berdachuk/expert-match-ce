#!/bin/bash
# Stop ExpertMatch service (local or remote)
# Usage: ./scripts/stop-service.sh [local|HOST]
#   local - stop on current machine (default if no SSH needed)
#   HOST  - stop on remote host via SSH (default: 192.168.0.73)

SERVICE_HOST="${1:-192.168.0.73}"

stop_local() {
    echo "Stopping ExpertMatch service on this machine..."
    pid=$(ps aux | grep '[j]ava.*expert-match.jar' | awk '{print $2}' | head -1)
    if [ -n "$pid" ]; then
        kill "$pid" 2>/dev/null && echo "Service stopped (PID $pid)" || echo "Failed to stop PID $pid"
    else
        echo "Service was not running"
    fi
}

stop_remote() {
    echo "Stopping ExpertMatch service on ${SERVICE_HOST}..."
    ssh "${SERVICE_HOST}" "ps aux | grep '[j]ava.*expert-match.jar' | awk '{print \$2}' | head -1 | xargs kill 2>/dev/null && echo 'Service stopped' || echo 'Service was not running'"
}

if [ "$SERVICE_HOST" = "local" ]; then
    stop_local
else
    stop_remote
fi
