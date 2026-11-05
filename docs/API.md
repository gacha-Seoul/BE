# API 명세 (프론트용)

> 서버를 켜고 http://localhost:8080/swagger-ui.html 에서 직접 눌러볼 수 있어요.
> 기본 주소 `http://localhost:8080` · 요청/응답 JSON · 에러는 `{ "message": "..." }` + 상태코드(400/403/404/500)
> 화면 번호는 기획 화면 명세 기준 (1-1 메인 가챠, 2-1 지역 지도, 3-1 퀘스트 안내, 3-2 목적지, 3-3 대화, 3-4 완료, 3-5 장소 완료)

## 전체 흐름

```
시작            POST /api/users                         → userId (코인 0, 무료 뽑기 5)
1-1 메인 가챠   GET  /api/users/{id}/main               → 후보 카드 5개, 뽑기 n회 남음, 코인
     뽑기       POST /api/users/{id}/gacha              → 첫 뽑기는 서대문구 · 문화
2-1 지역 지도   GET  /api/courses/{courseId}?userId=    → 장소 4개, 캐릭터 위치, 지역 완료
3-1 퀘스트 안내 GET  /api/places/{placeId}?userId=      → 제목, 스토리, Q1~Q4 상태, n/4
     Start      POST /api/quests/{questId}/start        → 첫 대화 단계
3-3 대화        POST /api/quests/{questId}/talk         → 음성 판정 → NPC 대사, 다음 단계
     화면 이벤트 POST /api/quests/{questId}/event        → 도착, 상품·프레임 선택
3-4 완료        talk/event 응답의 questCompleted, coinsEarned
```

---

## 1. 유저

### 유저 생성
`POST /api/users`
```json
{ "nickname": "Emma", "language": "English", "level": "BEGINNER" }
```
→ `{ "id": 1, "nickname": "Emma", "language": "English", "level": "BEGINNER", "coins": 0, "freeDraws": 5, "drawsLeft": 5 }`

- `language`: 교정 설명 언어 / `level`: `BEGINNER` · `INTERMEDIATE` · `ADVANCED`
- 한/영 전환은 프론트에서 (모든 대사에 한국어·로마자·영어가 같이 옴)

---

## 2. 1-1 메인 가챠 화면

### 화면 데이터
`GET /api/users/{userId}/main`
```json
{
  "userId": 1, "coins": 0, "freeDraws": 5, "drawsLeft": 5,
  "canDraw": true, "drawBlockedReason": null, "currentCourseId": null,
  "candidates": [
    { "courseId": 1, "regionId": 13, "regionCode": "SEODAEMUN", "regionName": "서대문구", "regionNameEn": "Seodaemun-gu",
      "category": "CULTURE", "categoryLabel": "문화", "imageKey": "course_seodaemun_culture",
      "status": "NOT_DRAWN", "playable": true }
  ]
}
```
| 필드 | 쓰는 법 |
|---|---|
| `drawsLeft` | "뽑기 {n}회 남음" (무료 + 코인) |
| `coins` | 우측 상단 보유 코인 |
| `canDraw` | `false`면 뽑기 버튼 비활성화 |
| `drawBlockedReason` | `NO_DRAWS`(횟수 없음) / `COMPLETE_A_QUEST`(재뽑기 불가, 퀘스트 완료 후 가능) / `ALL_DRAWN` |
| `candidates` | 후보 카드 5개 + 지도 핀 (`regionCode`로 핀 매칭). `status`: `NOT_DRAWN` / `DRAWN` / `COMPLETED` |
| `playable` | `false`면 아직 퀘스트 데이터 없음 (준비 중) |
| `currentCourseId` | 마지막으로 뽑은 코스 (지역 지도로 돌아가기용) |

화면 진입, 퀘스트에서 돌아올 때마다 이 API로 저장된 값을 다시 불러오면 돼요.

### 뽑기
`POST /api/users/{userId}/gacha` (body 없음)
```json
{
  "result": { "courseId": 1, "regionName": "서대문구", "categoryLabel": "문화", "status": "DRAWN", ... },
  "openCopy": "서대문구의 문화 퀘스트가 열렸어. 첫 장소를 확인해 봐",
  "places": [ { "id": 1, "orderNo": 1, "name": "전통시장", "representative": "영천시장",
                "candidates": "영천시장, 포방터시장, 인왕시장, 백련시장", "status": "AVAILABLE", ... } ],
  "coins": 0, "freeDraws": 4, "drawsLeft": 4
}
```
- 1-2 뽑기 결과 팝업을 이 응답 하나로 그릴 수 있어요 (지역명, 카테고리, 캐릭터 대사, 장소 4개와 연결 장소 리스트)
- 무료 횟수 먼저, 다 쓰면 코인 1개 차감 (결과 저장과 같이 처리)
- 첫 뽑기는 서대문구 · 문화 고정, 이후 안 뽑은 후보 중 랜덤
- 재뽑기 불가: 뽑은 뒤 퀘스트를 1개 이상 완료해야 다음 뽑기 가능
- 불가하면 400 + 이유 메시지. 추첨 중 버튼 비활성화는 프론트에서

---

## 3. 2-1 지역 퀘스트 지도

`GET /api/courses/{courseId}?userId=1`
```json
{
  "courseId": 1, "regionCode": "SEODAEMUN", "regionName": "서대문구", "category": "CULTURE", "categoryLabel": "문화",
  "openCopy": "서대문구의 문화 퀘스트가 열렸어. 첫 장소를 확인해 봐",
  "coins": 0, "currentPlaceId": 1, "completed": false,
  "places": [
    { "id": 1, "orderNo": 1, "name": "전통시장", "representative": "영천시장",
      "candidates": "영천시장, 포방터시장, 인왕시장, 백련시장",
      "pinKey": "pin_market", "imageKey": "place_market", "status": "AVAILABLE", "doneQuests": 0, "totalQuests": 4 },
    { "id": 2, "orderNo": 2, "name": "대학가", "representative": "신촌 연세로", "status": "COMING_SOON", ... },
    { "id": 3, "orderNo": 3, "name": "뷰티스토어", "status": "COMING_SOON", ... },
    { "id": 4, "orderNo": 4, "name": "느좋카페", "status": "COMING_SOON", ... }
  ]
}
```
| 필드 | 쓰는 법 |
|---|---|
| `places[].status` | `AVAILABLE`(클릭 → 3-1) / `LOCKED`(잠금) / `DONE`(재수행 불가) / `COMING_SOON`(준비 중) |
| `currentPlaceId` | 캐릭터를 올릴 장소 (진행 중 장소, 다 끝나면 마지막 완료 장소) |
| `completed` | `true`면 "‘서대문구’ 완료!" 팝업 |
| `pinKey` | 지역 지도 위 핀 위치 키 (프론트 리소스와 맞추기) |

---

## 4. 3-1 퀘스트 시작 안내

`GET /api/places/{placeId}?userId=1`
```json
{
  "id": 1, "courseId": 1, "regionName": "서대문구", "categoryLabel": "문화", "name": "전통시장",
  "title": "시장 간식, 두 사람 몫",
  "story": "친구랑 먹을 간식을 사러 왔어요. ...",
  "coins": 0, "doneQuests": 0, "totalQuests": 4, "progress": "0/4", "currentQuestId": 1,
  "quests": [
    { "id": 1, "code": "Q1", "orderNo": 1, "name": "길 찾기", "title": "사진 속 전집 찾기",
      "story": "사진 속 전집을 찾아야 해요. ...", "status": "AVAILABLE" },
    { "id": 2, "code": "Q2", "orderNo": 2, "name": "시식하기", "title": "갓 부친 전, 한 입 먼저", "status": "LOCKED", ... }
  ]
}
```
- `story`(장소): Start 전에 캐릭터 말풍선으로 한 번 표시
- `progress`: "퀘스트 n/4" (Start를 눌러도 안 늘고, 완료 확정 때 늘어남)
- `currentQuestId`: 강조할 퀘스트. 그 퀘스트의 `name`을 말풍선 상단, `story`를 상황 안내로

---

## 5. 퀘스트 진행 ⭐

### Start → 첫 대화 단계
`POST /api/quests/{questId}/start`
```json
{ "userId": 1 }
```
→
```json
{
  "questId": 1, "code": "Q1", "name": "길 찾기", "title": "사진 속 전집 찾기", "story": "...",
  "destinationKey": "jeonjip", "totalSteps": 1,
  "opening": { "speaker": "채소 가게 상인", "ko": "뭐 찾으세요?", "roman": "Mwo chajeuseyo?", "en": "What are you looking for?" },
  "step": { "stepNo": 1, "totalSteps": 1, "target": "채소 가게 상인",
            "user": { "speaker": "나", "ko": "이거 어디예요?", "roman": "Igeo eodiyeyo?", "en": "Where is this place?" } },
  "waitingEvent": null, "coins": 0
}
```
- `destinationKey`: 3-2 화면 목적지 테두리 (도착 범위 판정은 프론트)
- `opening`: NPC가 먼저 말하는 퀘스트만 있음. 없으면 `null`
- `step.user`: 하단 플레이어 패널 대사 = 힌트 (한국어, 로마자, 영어). 음성 듣기는 프론트 TTS (느린 음성은 rate 0.7)
- 이탈 후 다시 부르면 처음부터. 완료한 퀘스트는 400 (재수행 불가)

### 3-3 발화 판정
`POST /api/quests/{questId}/talk`
```json
{ "userId": 1, "message": "여기 어디예요?" }
```
→
```json
{
  "passed": true,
  "npc": { "speaker": "채소 가게 상인", "ko": "빨간 천막 지나서 방앗간 옆이에요.",
           "roman": "Ppalgan cheonmak jinaseo banggatgan yeopieyo.", "en": "Go past the red tent. It's next to the rice mill." },
  "correction": null,
  "step": null,
  "stepProgress": "1/1",
  "waitingEvent": "arrived",
  "effects": [ { "type": "HIGHLIGHT", "value": "jeonjip" } ],
  "questCompleted": false, "coinsEarned": 0, "coins": 0, "nextQuestId": null,
  "placeProgress": "0/4", "placeCompleted": false, "placeEnding": null, "courseCompleted": false
}
```

| 필드 | 쓰는 법 |
|---|---|
| `passed` | `true`: 단계 통과 → NPC 대사 보여주고 NEXT로 다음 단계 / `false`: 같은 단계 유지 |
| `npc` | NPC 말풍선. 통과면 명세 대사(한·로마자·영), 실패면 다시 묻는 대사(한국어만) |
| `correction` | `{original, better, reason}` 표현 교정 (없으면 `null`) |
| `step` | 다음 단계 하단 패널. 대화가 끝나면 `null` |
| `stepProgress` | "[현재 / 전체]" 대화 단계 |
| `waitingEvent` | 프론트가 보내야 할 이벤트 (아래 표). 없으면 `null` |
| `effects` | 화면 처리 (아래 표) |
| `questCompleted` | `true` → 3-4 "‘{name}’ 미션 완료!" + `coinsEarned` |
| `nextQuestId` | 다음 퀘스트 (3-4 NEXT → 3-1) |
| `placeCompleted`, `placeEnding` | 4/4 → 3-5 "‘{장소}’ 퀘스트 완료!" |
| `courseCompleted` | 지역 완료 → 2-1에서 완료 팝업 |

- 무음·인식 실패면 호출하지 말고 재시도 (빈 message는 400)
- 한 문장에 여러 단계 내용이 들어 있으면 한 번에 여러 단계 통과 (예: "간 빼고 순대 포장해 주세요")

### 화면 이벤트
`POST /api/quests/{questId}/event`
```json
{ "userId": 1, "event": "arrived" }
```
→ 발화 판정과 같은 형식 (`passed`는 항상 `false`, 완료되면 `questCompleted: true`)

| 퀘스트 | event | 언제 보내나 |
|---|---|---|
| Q1 길 찾기 | `arrived` | 안내 후 전집 도착 범위에 들어갔을 때 |
| Q3 흥정하기 | `selected` | 호박전 2팩 선택 (가격 묻기 전에 필요. 없으면 NPC "어떤 거요?") |

### effects 종류

| type | value | 화면 처리 |
|---|---|---|
| `HIGHLIGHT` | `jeonjip`, `bunsik` | 목적지 테두리 강조 |
| `SAMPLE_EAT` | `hobakjeon` | 시식용 전 한 조각 줄이기 |
| `SHOW_SELECT` | `호박전 2팩` | 상품 선택 안내 |
| `PRICE` | `10000`, `9000` | 가격 표시 변경 |
| `CART_ADD` | `호박전 2팩\|9000` | 장바구니 추가 (`\|` 뒤는 가격) |
| `SHOW_CHOICE` | `순대만\|순대와 간` | 선택지 그림 |

### 현장 표현 카드
`POST /api/quests/{questId}/phrase-card` · `{ "userId": 1 }`
→ `{ "title": "...", "phrases": [ { "korean": "...", "romanization": "...", "meaning": "...", "whenToUse": "..." } ] }`

---

## 6. 데이터 id (데모)

| 장소 (place id) | Q1 | Q2 | Q3 | Q4 |
|---|---|---|---|---|
| 전통시장 (1) | 1 길 찾기 | 2 시식하기 | 3 흥정하기 | 4 음식 구매하기 |

대학가(2), 뷰티스토어(3), 느좋카페(4)는 퀘스트 데이터가 없어서 `COMING_SOON`.

### 전통시장 진행 예시 (mock 모드: 말하면 현재 단계 통과)

| 순서 | 호출 | 결과 |
|---|---|---|
| 1 | `start` 1 | opening "뭐 찾으세요?" |
| 2 | `talk` 1 | "빨간 천막 지나서 방앗간 옆이에요." · waitingEvent `arrived` |
| 3 | `event` 1 `arrived` | 완료 · +1 코인 · 1/4 |
| 4 | `start` 2 → `talk` ×2 | "호박전이요…" → "네, 여기 잘라 놓은 거…" · 완료 2/4 |
| 5 | `start` 3 → `event` `selected` → `talk` ×3 | 만 원 → 구천 원 → 완료 3/4 |
| 6 | `start` 4 → `talk` ×3 | "간도 넣어드려요?" → "여기서 드세요?" → "네, 순대만 담아드릴게요." · 4/4 · placeCompleted · courseCompleted |
| 7 | `GET /api/users/1/main` | 코인 4, 뽑기 가능 (`canDraw: true`) |

---

## 7. 기타

| API | 설명 |
|---|---|
| `GET /api/health` | `{ "status": "UP", "llmMode": "mock" }` |
| `POST /api/demo/start` (body: 유저 생성과 동일, 생략 가능) | 새 유저 + 첫 뽑기(서대문구 · 문화) |
| `POST /api/demo/reset` | 모든 유저 기록 삭제 |
