# DB 구조

```mermaid
erDiagram
    REGION ||--o{ COURSE : has
    COURSE ||--o{ PLACE : "장소 4개"
    PLACE ||--o{ QUEST : "Q1~Q4"
    QUEST ||--o{ DIALOGUE_STEP : "대화 단계"
    APP_USER ||--o{ USER_COURSE : draws
    COURSE ||--o{ USER_COURSE : ""
    APP_USER ||--o{ QUEST_PROGRESS : plays
    QUEST ||--o{ QUEST_PROGRESS : ""
    APP_USER ||--o{ CHAT_MESSAGE : writes
    QUEST ||--o{ CHAT_MESSAGE : ""

    REGION {
        bigint id PK
        string code "SEODAEMUN"
        string name "서대문구"
        string name_en
    }
    COURSE {
        bigint id PK
        bigint region_id FK
        string category "CULTURE ..."
        int order_no "후보 카드 순서"
        string open_copy "뽑힌 뒤 캐릭터 대사"
        string image_key
    }
    PLACE {
        bigint id PK
        bigint course_id FK
        int order_no "순차 해금"
        string name "전통시장"
        string representative "영천시장"
        string candidates "연결 장소 목록"
        string pin_key
        string image_key
        string title "시작 화면 제목"
        string story "시작 화면 말풍선"
        string ending_text
    }
    QUEST {
        bigint id PK
        bigint place_id FK
        int order_no "순차 해금"
        string code "Q1"
        string name "길 찾기"
        string title "사진 속 전집 찾기"
        string story "상황 줄거리"
        string destination_key "목적지 테두리"
        string opening_ko "NPC 첫 마디 (+roman, en)"
        string rules "예외 처리 (AI 참고)"
        string complete_event "arrived 등"
        string complete_effects
        int reward_coin "1"
    }
    DIALOGUE_STEP {
        bigint id PK
        bigint quest_id FK
        int step_no
        string target "말하는 상대"
        string user_ko "플레이어 대사 (+roman, en)"
        string accepts "같이 인정하는 표현"
        string speaker "응답 NPC"
        string npc_ko "NPC 응답 (+roman, en)"
        string effects
        string required_event "selected 등"
        string retry_ko
    }
    APP_USER {
        bigint id PK
        string nickname
        string language
        string level
        int coins
        int free_draws "기본 5"
    }
    USER_COURSE {
        bigint id PK
        bigint user_id FK
        bigint course_id FK "user+course 유니크 (재뽑기 불가)"
        datetime drawn_at
        boolean completed
    }
    QUEST_PROGRESS {
        bigint id PK
        bigint user_id FK
        bigint quest_id FK "user+quest 유니크 (코인 중복 방지)"
        string status "IN_PROGRESS/DONE"
        int passed_steps
        string events "selected,arrived"
        datetime completed_at
    }
    CHAT_MESSAGE {
        bigint id PK
        bigint user_id FK
        bigint quest_id FK
        string role
        string content
    }
```

| 테이블 | 역할 | 데이터 |
|---|---|---|
| region | 서울 25개 구 | `data.sql` |
| course | 가챠 후보 5개 (지역·카테고리) | `data.sql` |
| place | 코스당 장소 4개 | `data.sql` |
| quest | 장소당 Q1~Q4 | `data.sql` |
| dialogue_step | 퀘스트의 대화 단계 (한국어·로마자·영어) | `data.sql` |
| app_user | 코인, 남은 무료 뽑기 | API |
| user_course | 뽑은 코스, 완료 여부 | 뽑기 |
| quest_progress | 통과한 단계 수, 받은 이벤트, 완료 | 퀘스트 진행 |
| chat_message | 대화 기록 (AI 맥락, 표현 카드) | 퀘스트 진행 |

**퀘스트 추가하는 법:** `data.sql`의 `quest`에 한 줄, `dialogue_step`에 단계 수만큼 줄을 추가하면 끝. 코드 수정 없음.
