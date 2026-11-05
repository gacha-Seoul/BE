# API 명세 (프론트용)

> 서버를 켜고 http://localhost:8080/swagger-ui.html 에서 직접 눌러볼 수 있어요.
> 기본 주소: `http://localhost:8080`

모든 요청/응답은 JSON. 에러는 `{ "message": "..." }` + HTTP 상태코드(400/403/404/500).

### 1. 유저 생성
`POST /api/users`
```json
{ "nickname": "Emma", "language": "English", "level": "BEGINNER" }
```
→ `{ "id": 1, "nickname": "Emma", "language": "English", "level": "BEGINNER", "coins": 3 }`

`language`: 교정 설명을 받을 언어 (English, 日本語, 中文 등) / `level`: BEGINNER, INTERMEDIATE, ADVANCED

### 2. 3D 지도 상태
`GET /api/users/{userId}/map`
```json
{
  "userId": 1, "coins": 3, "gachaCost": 3, "conqueredCount": 0,
  "regions": [
    { "id": 13, "code": "SEODAEMUN", "name": "서대문구", "nameEn": "Seodaemun-gu",
      "status": "UNLOCKED", "playable": true, "clearedStages": 2, "totalStages": 5 }
  ]
}
```
- `status`: `LOCKED`(회색) / `UNLOCKED`(도전 중) / `CONQUERED`(색칠)
- `name`은 서울 구 GeoJSON의 구 이름과 동일 → 3D 블록 매칭 키로 사용

### 3. 가챠
`POST /api/users/{userId}/gacha` (body 없음)
```json
{ "region": { ...지역 상세(4번과 동일)... }, "coinsLeft": 0 }
```
코인 부족 → 400 `{ "message": "코인이 부족해요. ..." }`

### 4. 지역 상세 + 스테이지 5개
`GET /api/regions/{regionId}?userId=1`
```json
{
  "id": 13, "code": "SEODAEMUN", "name": "서대문구", "nameEn": "Seodaemun-gu",
  "description": "...", "status": "UNLOCKED",
  "stages": [
    { "id": 5, "category": "LIVING", "categoryLabel": "생활자원", "orderNo": 5,
      "place": "영천시장 꽈배기 가게", "npcRole": "꽈배기 가게 사장님",
      "situation": "...", "rewardCoin": 1, "cleared": false,
      "missions": [ { "index": 0, "text": "인사하기", "done": false } ] }
  ]
}
```

### 5. 스테이지 시작 (다시 시작도 동일)
`POST /api/stages/{stageId}/start`
```json
{ "userId": 1 }
```
→
```json
{
  "npcReply": "어서 와요~ 꽈배기 방금 나왔어! 따끈따끈해요.",
  "suggestions": [ { "korean": "안녕하세요", "romanization": null, "meaning": null } ],
  "missions": [ { "index": 0, "text": "인사하기", "done": false } ]
}
```
가챠로 안 뽑은 지역의 스테이지 → 403

### 6. 대화 한 턴 ⭐
`POST /api/stages/{stageId}/chat`
```json
{ "userId": 1, "message": "꽈배기 줘" }
```
→
```json
{
  "npcReply": "몇 개 줄까? 세 개면 좀 깎아 줄게~",
  "correction": { "original": "꽈배기 줘", "better": "꽈배기 주세요", "reason": "Use '주세요' to be polite." },
  "culturalTip": "Light bargaining is common in traditional markets.",
  "suggestions": [
    { "korean": "두 개 주세요", "romanization": "du gae juseyo", "meaning": "Two, please" }
  ],
  "missions": [ { "index": 2, "text": "개수 정해서 주문하기", "done": true } ],
  "stageCleared": false,
  "regionConquered": false,
  "coinsEarned": 0,
  "coins": 0
}
```
- `correction`, `culturalTip`은 없으면 `null`
- `stageCleared: true` → 클리어 연출 / `regionConquered: true` → 3D 지도 색칠 연출
- AI 호출이 실패해도 "한 번 더 말해 줄래요?" 기본 응답이 와서 화면이 멈추지 않음

### 7. 현장 표현 카드
`POST /api/stages/{stageId}/phrase-card`
```json
{ "userId": 1 }
```
→
```json
{ "title": "영천시장에서 쓸 표현",
  "phrases": [ { "korean": "이거 얼마예요?", "romanization": "igeo eolmayeyo?",
                 "meaning": "How much is this?", "whenToUse": "When asking the price" } ] }
```

### 8. 서버 상태
`GET /api/health` → `{ "status": "UP", "llmMode": "mock" }` (`mock` 또는 `real`)

### 9. 데모용
| API | 설명 |
|---|---|
| `POST /api/demo/start` (body: 1번과 동일, 생략 가능) | 새 유저 생성 + 첫 가챠(서대문구)까지 한 번에. 응답 `{ "user": {...}, "gacha": {...} }` |
| `POST /api/demo/reset` | 모든 유저 기록 삭제 (지역/스테이지 데이터는 유지) |

## 데모 흐름

```
POST /api/demo/start                 (유저 + 서대문구)
→ GET  /api/regions/13?userId={id}   (5개 스테이지)
→ POST /api/stages/5/start           (영천시장 꽈배기)
→ POST /api/stages/5/chat  (여러 번) → stageCleared
→ POST /api/stages/5/phrase-card
→ GET  /api/users/{id}/map           (3D 지도 갱신)
```
