# API 명세 (프론트용)

기본 주소 `http://localhost:8080`, Swagger `/swagger-ui.html`.
요청/응답 JSON, 에러 `{ "message": "..." }`.
400: 잘못된 요청/코인 부족, 403: 지역 미해금, 404: 대상 없음, 409: 이전 퀘스트 미완료/시작 전/이미 완료.

## 1. 사용자

`POST /api/users` (body 생략 가능)

```json
{"nickname":"Emma","language":"English","level":"BEGINNER"}
```

응답: `{"id":1,"nickname":"Emma","language":"English","level":"BEGINNER","coins":1}`.
`GET /api/users/{userId}`도 같은 형식이다.

## 2. 지도

`GET /api/users/{userId}/map`

```json
{
  "userId":1,"coins":0,"gachaCost":1,"conqueredCount":0,
  "regions":[{
    "id":13,"code":"SEODAEMUN","name":"서대문구","nameEn":"Seodaemun-gu",
    "status":"UNLOCKED","playable":true,"clearedStages":0,"totalStages":5
  }]
}
```

서울 25개 구를 반환한다. 지역 상태는 `LOCKED / UNLOCKED / CONQUERED`.
장소의 모든 퀘스트를 완료하면 `clearedStages`가 증가한다. 퀘스트가 있는 지역만 `playable=true`다.

## 3. 가챠

`POST /api/users/{userId}/gacha`

```json
{"requestId":"550e8400-e29b-41d4-a716-446655440000"}
```

**변경: body에 `requestId` 필수(1~100자).** 프론트에서 뽑기를 누를 때 UUID를 만들고, 통신 재시도 시 같은 ID를 보낸다. 새 뽑기는 새 ID를 사용한다.

응답: `{"region":{...지역 상세...},"coinsLeft":0,"requestId":"..."}`.
같은 사용자·요청 ID는 재차감 없이 같은 지역과 최초 뽑기 직후 잔액을 반환한다. 지역 상세 진행 정보는 최신 상태다.
다른 요청 ID로 재호출하면 새로운 뽑기다. 보유하지 않은 지역 중에서 선택하며 코인이 부족하면 400.
가입 시 최초 무료 코인 1개를 지급하며, 첫 가챠부터 매번 코인 1개를 차감한다. 별도 무료 횟수는 없다.
첫 가챠는 서대문구로 고정한다.

## 4. 지역·장소·퀘스트 조회

`GET /api/regions/{regionId}?userId=1`

```json
{
  "id":13,"code":"SEODAEMUN","name":"서대문구","nameEn":"Seodaemun-gu",
  "description":"...","status":"UNLOCKED",
  "stages":[{
    "id":5,"category":"CULTURE","categoryLabel":"문화","orderNo":5,
    "place":"영천시장 · 전통시장 퀘스트","introStory":"친구랑 먹을 간식을 사러 왔어요...",
    "imageUrl":null,"cleared":false,"completedQuests":0,"totalQuests":4,
    "quests":[{
      "id":501,"code":"Q01","title":"사진 속 전집 찾기","orderNo":1,
      "npcRole":"시장 입구 채소 좌판 상인","situation":"...","rewardCoin":1,
      "status":"AVAILABLE",
      "missions":[{"index":0,"text":"전집 위치 묻기","done":false},{"index":1,"text":"전집 도착하기","done":false}]
    }]
  }]
}
```

**변경: `Stage`는 장소다. 기존 `npcRole / situation / rewardCoin / missions`는 하위 `quests[]`로 이동했다.**
`GET /api/stages/{stageId}?userId=1`은 위 `stages[]`의 원소 하나를 반환한다.
분야는 `HISTORY / CULTURE / ART / TOURISM / LIVING`. 같은 분야에 장소가 여러 개 있을 수 있다.
퀘스트 상태는 `LOCKED / AVAILABLE / IN_PROGRESS / COMPLETED`. 앞 과제를 완료해야 다음 과제를 시작할 수 있다.
미션의 `index`는 해당 퀘스트 안에서 0부터 시작한다.

## 5. 퀘스트 시작·재시작

`POST /api/quests/{questId}/start`

```json
{"userId":1}
```

```json
{
  "npcReply":"뭐 찾으세요?",
  "suggestions":[{"korean":"전집이 어디예요?","romanization":null,"meaning":null}],
  "missions":[{"index":0,"text":"전집 위치 묻기","done":false},{"index":1,"text":"전집 도착하기","done":false}],
  "questId":501,"status":"IN_PROGRESS","context":{}
}
```

미완료 과제는 해당 과제의 대화·임시 상태만 초기화한다. 완료 과제는 기록과 확정 주문을 유지하고 보상을 재지급하지 않는다.
완료 과제에 재입장할 때도 `npcReply`는 콘텐츠의 고정 첫 대사다. 확정 주문 표시는 응답 `context`를 사용한다.

## 6. 대화

`POST /api/quests/{questId}/chat`

```json
{"userId":1,"message":"순대 한 팩 포장해 주세요. 간은 빼 주세요."}
```

```json
{
  "npcReply":"네, 순대만 포장해 드릴게요.",
  "correction":null,"culturalTip":null,"suggestions":[],
  "missions":[{"index":0,"text":"순대 한 팩 주문하기","done":true},{"index":1,"text":"간 빼 달라고 하기","done":true},{"index":2,"text":"포장 주문 확정하기","done":true}],
  "stageCleared":true,"regionConquered":false,"coinsEarned":1,"coins":4,
  "questId":504,"questCompleted":true,
  "context":{"item":"SUNDAE","quantity":1,"excludeLiver":true,"takeout":true,"orderConfirmed":true}
}
```

- `start` 후 호출한다. `message`는 공백 제외 1~300자. 무관한 말/LLM 실패 시 진행 상태를 유지한다.
- `questCompleted`: 이번 턴에 해당 퀘스트를 처음 완료했음. 다음 퀘스트를 시작하는 기준이다.
- `stageCleared`: 이번 턴에 장소의 모든 퀘스트를 완료했음. 장소 완료 연출에 사용한다.
- `regionConquered`: 지역 전체 퀘스트를 완료해 이번에 정복했음.
- `coinsEarned`: 해당 턴에 지급한 퀘스트 보상 + 정복 보너스. 보상은 최초 1회만 지급한다.
- 완료 과제의 추가 채팅은 409. 현재 화면은 유지하고 다음 퀘스트를 시작한다.
- `context`는 서버 판정 상태다. 프론트가 직접 전송/수정하지 않는다. 규칙에 따라 필드가 달라진다.
- 전통시장: 501(Q01 전집 찾기) → 502(Q02 시식) → 503(Q03 두 팩 흥정) → 504(Q04 순대 포장).
- Q03은 모델 제안과 무관하게 서버가 9,000원으로 가격을 제한하고, 가격 제시 이후 수락을 확인한다.
- "조금 깎아 주세요"처럼 금액 없는 흥정도 `negotiationRequested: true` 제안으로 서버에 할인 가격을 저장한다. 해당 턴에는 완료하지 않고, 다음 "네, 주세요"에서 품목·수량·구매 수락을 확인해 완료한다.
- Q04는 순대 한 팩·간 제외·포장·확정이 모두 필요하다.
- LLM 오류 시 기본 재질문을 반환하고 보상·진행 상태는 바뀌지 않는다.
- `mock` 모드는 발화 의미와 무관하게 고정 순서로 진행하는 연동용이다. 실제 대화 판정은 `LLM_MOCK=false`로 확인한다.

시장 `context`의 필드:

| 과제 | 필드 |
|---|---|
| Q01 | `arrivedAt: "JEON_STORE"` |
| Q02 | `sampleAccepted: true` |
| Q03 | `selectedItem: "PUMPKIN_JEON"`, `quantity`, `negotiationRequested`, `offeredPrice`, `acceptedPrice`, `budget`, `purchaseConfirmed` |
| Q04 | `item: "SUNDAE"`, `quantity`, `excludeLiver`, `takeout`, `orderConfirmed` |

기존 장소의 단일 퀘스트는 `context.completedMissions`에 달성한 미션 번호 배열을 저장한다.

## 7. 표현 카드

`POST /api/quests/{questId}/phrase-card`, body `{"userId":1}`.
시작한 과제의 대화로 생성한다. 지역 해금 및 앞 퀘스트 완료가 필요하다.

```json
{"title":"오늘 배운 표현","phrases":[{"korean":"포장해 주세요","romanization":"pojanghae juseyo","meaning":"To go, please.","whenToUse":"When ordering takeaway"}]}
```

## 8. 기존 장소 대화 경로

`POST /api/stages/{stageId}/start`, `/chat`, `/phrase-card`도 남아 있다. body와 응답은 퀘스트 API와 동일하다.
장소의 첫 미완료 퀘스트를 자동 선택한다. 전부 완료했으면 마지막 퀘스트를 선택한다.
**여러 과제의 화면 상태를 정확히 연결하려면 `/api/quests/{questId}/...`를 사용한다.**

## 9. 데모

| API | 설명 |
|---|---|
| `GET /api/health` | `{"status":"UP","llmMode":"mock"}` |
| `POST /api/demo/start` | 사용자 생성 + 첫 가챠. body는 사용자 생성과 동일, 생략 가능. `{ "user": {...}, "gacha": {...} }` |
| `POST /api/demo/reset` | 사용자·진행·대화·코인·뽑기 내역 삭제. 콘텐츠는 유지 |

기본 시드 기준 mock 연동 확인: `./scripts/demo-check.sh`.
