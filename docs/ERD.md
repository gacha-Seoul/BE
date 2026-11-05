# DB 구조

```mermaid
erDiagram
    REGION ||--o{ STAGE : has
    APP_USER ||--o{ USER_REGION : unlocks
    REGION ||--o{ USER_REGION : ""
    APP_USER ||--o{ STAGE_PROGRESS : plays
    STAGE ||--o{ STAGE_PROGRESS : ""
    APP_USER ||--o{ CHAT_MESSAGE : writes
    STAGE ||--o{ CHAT_MESSAGE : ""

    REGION {
        bigint id PK
        string code "SEODAEMUN"
        string name "서대문구"
        string name_en
        string description
    }
    STAGE {
        bigint id PK
        bigint region_id FK
        string category "HISTORY/CULTURE/ART/TOURISM/LIVING"
        int order_no
        string place
        string npc_role
        string npc_persona
        string situation
        string missions "| 로 구분"
        string opening_line
        string starter_suggestions "| 로 구분"
        int reward_coin
    }
    APP_USER {
        bigint id PK
        string nickname
        string language
        string level
        int coins
    }
    USER_REGION {
        bigint id PK
        bigint user_id FK
        bigint region_id FK
        boolean conquered
        datetime unlocked_at
        datetime conquered_at
    }
    STAGE_PROGRESS {
        bigint id PK
        bigint user_id FK
        bigint stage_id FK
        string completed_missions "0,2,3"
        boolean cleared
    }
    CHAT_MESSAGE {
        bigint id PK
        bigint user_id FK
        bigint stage_id FK
        string role "USER/NPC"
        string content
        datetime created_at
    }
```

| 테이블 | 역할 | 데이터 출처 |
|---|---|---|
| region | 서울 25개 구 | `data.sql` (고정) |
| stage | 구별 5개 분야 스테이지 | `data.sql` (고정, 행 추가로 확장) |
| app_user | 플레이어, 코인 | API로 생성 |
| user_region | 가챠로 해금한 구, 정복 여부 | 가챠 시 생성 |
| stage_progress | 스테이지별 완료 미션, 클리어 여부 | 대화 시 생성 |
| chat_message | 대화 기록 (AI 맥락 + 표현 카드) | 대화 시 생성 |
