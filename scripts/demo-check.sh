#!/usr/bin/env bash
# 데모 흐름 한 번에 확인: 서버 켠 상태에서 ./scripts/demo-check.sh
set -e
BASE=${BASE:-http://localhost:8080}
JSON='Content-Type: application/json'

echo "== 1. 서버 상태"
curl -s "$BASE/api/health"; echo

echo "== 2. 새 유저 + 첫 가챠"
START=$(curl -s -X POST "$BASE/api/demo/start" -H "$JSON" -d '{"nickname":"Demo","language":"English","level":"BEGINNER"}')
USER_ID=$(echo "$START" | sed -E 's/^\{"user":\{"id":([0-9]+).*/\1/')
echo "userId=$USER_ID"
echo "$START" | head -c 300; echo " ..."

STAGE=${STAGE:-5}
echo "== 3. 스테이지 $STAGE 시작"
curl -s -X POST "$BASE/api/stages/$STAGE/start" -H "$JSON" -d "{\"userId\":$USER_ID}"; echo

for MSG in "안녕하세요" "이거 얼마" "꽈배기 두 개 주세요" "카드 돼요?"; do
  echo "== 나: $MSG"
  curl -s -X POST "$BASE/api/stages/$STAGE/chat" -H "$JSON" -d "{\"userId\":$USER_ID,\"message\":\"$MSG\"}"; echo
done

echo "== 4. 표현 카드"
curl -s -X POST "$BASE/api/stages/$STAGE/phrase-card" -H "$JSON" -d "{\"userId\":$USER_ID}"; echo

echo "== 5. 지도 (서대문구 진행도 확인)"
curl -s "$BASE/api/users/$USER_ID/map" | grep -o '"code":"SEODAEMUN"[^}]*'; echo
echo "== 완료"
