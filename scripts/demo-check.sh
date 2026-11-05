#!/usr/bin/env bash
# 데모 흐름 한 번에 확인 (mock 모드): 서버 켠 상태에서 ./scripts/demo-check.sh
set -e
BASE=${BASE:-http://localhost:8080}
JSON='Content-Type: application/json'
post() { curl -s -X POST "$BASE$1" -H "$JSON" -d "$2"; echo; }
say()  { post "/api/quests/$1/talk" "{\"userId\":$U,\"message\":\"$2\"}"; }
ev()   { post "/api/quests/$1/event" "{\"userId\":$U,\"event\":\"$2\"}"; }
start(){ post "/api/quests/$1/start" "{\"userId\":$U}"; }

echo "== 서버"; curl -s "$BASE/api/health"; echo
U=$(curl -s -X POST "$BASE/api/users" -H "$JSON" -d '{"nickname":"Demo"}' | sed -E 's/^\{"id":([0-9]+).*/\1/')
echo "userId=$U"
echo "== 메인"; curl -s "$BASE/api/users/$U/main" | head -c 300; echo " ..."
echo "== 뽑기"; post "/api/users/$U/gacha" ""
echo "== 재뽑기 (막혀야 함)"; post "/api/users/$U/gacha" ""
echo "== 지역 지도"; curl -s "$BASE/api/courses/1?userId=$U"; echo

echo "== 전통시장"
start 1; say 1 "이거 어디예요?"; ev 1 arrived
start 2; say 2 "뭐가 맛있어요?"; say 2 "먹어봐도 돼요?"
start 3; say 3 "이거 얼마예요?"; ev 3 selected; say 3 "이거 얼마예요?"; say 3 "조금 깎아 주세요."; say 3 "네, 주세요."
start 4; say 4 "순대 주세요."; say 4 "순대만 주세요."; say 4 "포장해 주세요."

echo "== 지역 지도 (completed: true)"; curl -s "$BASE/api/courses/1?userId=$U" | head -c 300; echo
echo "== 메인 (코인 4, 뽑기 가능)"; curl -s "$BASE/api/users/$U/main" | head -c 200; echo
echo "== 두 번째 뽑기"; post "/api/users/$U/gacha" ""
