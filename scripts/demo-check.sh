#!/usr/bin/env bash
# mock 모드 서버를 실행한 뒤 사용. 실패 응답이나 예상과 다른 진행 상태는 오류로 처리한다.
set -euo pipefail
BASE=${BASE:-http://localhost:8080}
HEADER='Content-Type: application/json'
START=$(curl --fail --silent --show-error -X POST "$BASE/api/demo/start" -H "$HEADER" -d '{"nickname":"Demo","language":"English","level":"BEGINNER"}')
USER_ID=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["user"]["id"])' <<< "$START")
echo "userId=$USER_ID"
for QUEST in 501 502 503 504; do
  curl --fail --silent --show-error -X POST "$BASE/api/quests/$QUEST/start" -H "$HEADER" -d "{\"userId\":$USER_ID}" > /dev/null
  case "$QUEST" in
    501) MESSAGES=("전집이 어디예요?" "전집에 도착했어요") ;;
    502) MESSAGES=("네, 먹어볼게요") ;;
    503) MESSAGES=("호박전 두 팩 주세요" "조금 깎아 주세요" "네, 주세요") ;;
    504) MESSAGES=("순대 한 팩 주세요" "간은 빼 주세요" "포장해 주세요") ;;
  esac
  for MESSAGE in "${MESSAGES[@]}"; do
    RESPONSE=$(curl --fail --silent --show-error -X POST "$BASE/api/quests/$QUEST/chat" -H "$HEADER" -d "{\"userId\":$USER_ID,\"message\":\"$MESSAGE\"}")
    echo "$RESPONSE"
  done
  python3 -c 'import json,sys; r=json.load(sys.stdin); assert r["questCompleted"], r' <<< "$RESPONSE"
done
curl --fail --silent --show-error -X POST "$BASE/api/quests/504/phrase-card" -H "$HEADER" -d "{\"userId\":$USER_ID}"
echo
curl --fail --silent --show-error "$BASE/api/users/$USER_ID/map"
echo
