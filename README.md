# 서울 가챠 BE

가챠로 서울의 구를 뽑고, 구마다 5개 분야(역사·문화·예술·관광·생활자원) 스테이지에서 AI NPC와 한국어 상황 회화를 하며 미션을 깨는 외국인 대상 서비스의 백엔드.

- API 명세: [docs/API.md](docs/API.md)
- DB 구조: [docs/ERD.md](docs/ERD.md)
- Swagger (서버 실행 후): http://localhost:8080/swagger-ui.html

## 실행

1. Java 17 이상
2. IntelliJ: **File → Open** → 이 폴더 선택 → Gradle 로딩 기다리기
3. `SeoulGachaApplication` 실행 → 콘솔에 `Started SeoulGachaApplication` 나오면 성공
4. 확인: http://localhost:8080/api/health → `{"status":"UP","llmMode":"mock"}`
5. 데모 흐름 한 번에 테스트: `./scripts/demo-check.sh`

### Gradle wrapper (gradlew)
처음 한 번만: IntelliJ 오른쪽 **Gradle** 탭 → Tasks → build setup → **wrapper** 더블클릭.
`gradlew`, `gradlew.bat`, `gradle/wrapper/`가 생기면 커밋해서 올린다. (팀원 모두 같은 Gradle 버전 사용)

### AI 설정 (.env)

```bash
cp .env.example .env
```

- `LLM_MOCK=true` (기본): API 키 없이 가짜 응답. 한 마디 할 때마다 미션 1개 완료. 프론트 연동/리허설용
- `LLM_MOCK=false` + `LLM_API_KEY=...`: 실제 AI
- `.env`는 GitHub에 올라가지 않는다. **API 키는 절대 코드에 쓰지 말 것**

## 게임 규칙 (application.yml `game.*`)

| 항목 | 값 |
|---|---|
| 시작 코인 | 3 |
| 가챠 1회 | 3코인 |
| 스테이지 클리어 | 스테이지별 `reward_coin` (기본 1) |
| 지역 정복 (5개 스테이지 모두 클리어) | 보너스 +2 |
| 첫 가챠 | 무조건 서대문구 (`first-region-code`, 비우면 완전 랜덤) |

미션 완료 판정: AI는 "이번 말로 달성한 미션 번호"를 **제안**만 하고, 저장·클리어·코인 지급은 **서버가 결정**한다.
AI 응답이 실패하거나 15초를 넘기면 "한 번 더 말해 줄래요?" 기본 응답으로 대체되어 화면이 멈추지 않는다.

## 프로젝트 구조

```
BE/
├── README.md
├── .env.example           AI 키 설정 예시 (복사해서 .env로)
├── CLAUDE.md, AGENTS.md   AI 코딩 도구용 프로젝트 규칙
├── .editorconfig, .gitattributes, .gitignore
├── build.gradle, settings.gradle
├── docs/
│   ├── API.md             API 명세 (프론트용)
│   └── ERD.md             DB 구조
├── scripts/
│   └── demo-check.sh      데모 흐름 자동 테스트
└── src/main/
    ├── java/com/hackathon/seoul/
    │   ├── controller/    User, Region, Stage, Demo(health/reset)
    │   ├── service/       Chat(대화·판정), Gacha, Region, User, Demo
    │   ├── domain/        Region, Stage, AppUser, UserRegion, StageProgress, ChatMessage
    │   ├── repository/
    │   ├── llm/           LlmClient, OpenAiLlmClient(실제), MockLlmClient, PromptFactory
    │   ├── dto/           Dtos.java (요청·응답 record 모음)
    │   ├── config/        CORS, Swagger
    │   └── common/        GameException, 전역 에러 처리
    └── resources/
        ├── application.yml
        ├── data.sql       서울 25개 구 + 스테이지 데이터
        └── prompts/
            ├── npc-chat.txt     NPC 대화 규칙
            └── phrase-card.txt  표현 카드 생성 규칙
```

## 자주 바꾸는 곳

| 하고 싶은 것 | 수정할 파일 |
|---|---|
| NPC 말투·대화 규칙 바꾸기 | `resources/prompts/npc-chat.txt` (재시작하면 반영) |
| 지역/스테이지 추가·수정 | `resources/data.sql` 의 `stage` INSERT |
| 코인, 가챠 비용 | `application.yml` 의 `game.*` |
| API 추가 | controller + `docs/API.md` 같이 수정 |
