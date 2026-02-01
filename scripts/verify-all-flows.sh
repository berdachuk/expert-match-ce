#!/bin/bash
# Verify all ExpertMatch application flows with curl.
# Usage: ./scripts/verify-all-flows.sh [BASE_URL]
# Default BASE_URL: http://localhost:8093

set -e
BASE="${1:-http://localhost:8093}"
API="$BASE/api/v1"
H="Content-Type: application/json"
XUSER="X-User-Id: curl-verify-user"

PASS=0
FAIL=0

check() {
  local name="$1"
  local code="$2"
  local expected="${3:-200}"
  if [ "$code" = "$expected" ]; then
    echo "  OK   $name"
    PASS=$((PASS + 1))
    return 0
  else
    echo "  FAIL $name (got $code, expected $expected)"
    FAIL=$((FAIL + 1))
    return 1
  fi
}

echo "=============================================="
echo "ExpertMatch flow verification - $BASE"
echo "=============================================="

echo ""
echo "--- System ---"
check "GET /actuator/health" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/actuator/health")"
check "GET $API/health" "$(curl -s -o /dev/null -w '%{http_code}' "$API/health")"
check "GET $API/metrics" "$(curl -s -o /dev/null -w '%{http_code}' "$API/metrics")"

echo ""
echo "--- Query ---"
check "GET $API/query/examples" "$(curl -s -o /dev/null -w '%{http_code}' "$API/query/examples")"
CODE=$(curl -s -o /tmp/q.json -w '%{http_code}' -X POST -H "$H" -H "$XUSER" "$API/query" -d '{"query":"Java expert","options":{"maxResults":2,"rerank":false}}' --max-time 120)
check "POST $API/query" "$CODE"

echo ""
echo "--- Chat ---"
check "GET $API/chats" "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" -H "$XUSER" "$API/chats")"
CREATE_CODE=$(curl -s -o /tmp/create_chat.json -w '%{http_code}' -X POST -H "$H" -H "$XUSER" "$API/chats" -d '{"name":"Verify"}')
CHAT_ID=$(jq -r '.id // empty' /tmp/create_chat.json 2>/dev/null)
check "POST $API/chats (create)" "$CREATE_CODE" "201"
if [ -n "$CHAT_ID" ] && [ "$CHAT_ID" != "null" ]; then
  check "GET $API/chats/$CHAT_ID" "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" -H "$XUSER" "$API/chats/$CHAT_ID")"
  check "GET $API/chats/$CHAT_ID/history" "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" -H "$XUSER" "$API/chats/$CHAT_ID/history")"
  check "PATCH $API/chats/$CHAT_ID" "$(curl -s -o /dev/null -w '%{http_code}' -X PATCH -H "$H" -H "$XUSER" "$API/chats/$CHAT_ID" -d '{"name":"Updated"}')"
  check "DELETE $API/chats/$CHAT_ID" "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE -H "$H" -H "$XUSER" "$API/chats/$CHAT_ID")"
fi

echo ""
echo "--- Test data ---"
check "POST $API/test-data?size=tiny" "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" "$API/test-data?size=tiny")"
check "POST $API/test-data/embeddings" "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" "$API/test-data/embeddings" --max-time 60)"
check "POST $API/test-data/graph" "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" "$API/test-data/graph")"
check "POST $API/test-data/complete?size=tiny&clear=true" "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" "$API/test-data/complete?size=tiny&clear=true" --max-time 90)"

echo ""
echo "--- Ingestion ---"
check "POST $API/ingestion/json-profiles?directory=classpath:data" "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" "$API/ingestion/json-profiles?directory=classpath:data" --max-time 30)"

echo ""
echo "--- Streaming ---"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "$H" -H "Accept: text/event-stream" -H "$XUSER" "$API/query-stream" -d '{"query":"Java","options":{"maxResults":1}}' --max-time 45)
check "POST $API/query-stream (SSE)" "$CODE"

echo ""
echo "--- Docs ---"
check "GET $API/openapi.json" "$(curl -s -o /dev/null -w '%{http_code}' "$API/openapi.json")"

echo ""
echo "=============================================="
echo "Result: $PASS passed, $FAIL failed"
echo "=============================================="
[ "$FAIL" -eq 0 ]
