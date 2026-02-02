#!/bin/bash
# Verify test data admin API with curl.
# Usage: ./scripts/verify-test-data-curl.sh [BASE_URL]
# Default BASE_URL: http://localhost:8093

set -e
BASE="${1:-http://localhost:8093}"

echo "Verifying test data endpoints at $BASE"
echo ""

echo "=== 1. Health ==="
curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health" && echo " OK" || (echo " FAIL"; exit 1)
echo ""

echo "=== 2. GET /api/v1/test-data/sizes ==="
curl -s "$BASE/api/v1/test-data/sizes" | head -c 400
echo ""
echo ""

echo "=== 3. POST /api/v1/test-data/complete/async (size=tiny) ==="
RESP=$(curl -s -X POST "$BASE/api/v1/test-data/complete/async?size=tiny&clear=false")
echo "$RESP"
JOB_ID=$(echo "$RESP" | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
echo "jobId: $JOB_ID"
echo ""

if [ -n "$JOB_ID" ]; then
  echo "=== 4. GET /api/v1/test-data/progress/$JOB_ID (poll once) ==="
  sleep 2
  curl -s "$BASE/api/v1/test-data/progress/$JOB_ID" | head -c 500
  echo ""
  echo ""
fi

echo "=== 5. POST /api/v1/test-data (sync, existing endpoint) ==="
curl -s -X POST "$BASE/api/v1/test-data?size=tiny&clear=false" | head -c 200
echo ""
echo ""

echo "Done."
