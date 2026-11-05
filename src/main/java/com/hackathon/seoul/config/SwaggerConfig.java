package com.hackathon.seoul.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** http://localhost:8080/swagger-ui.html 에서 API를 직접 눌러 테스트 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("서울 가챠 API")
                .description("""
                        외국인 학습자가 서울의 지역을 뽑고 AI NPC와 한국어로 대화하는 게임 API입니다.

                        ### 전통시장 프론트 연동 순서
                        1. `POST /api/demo/start`로 사용자 생성과 첫 뽑기를 한 번에 실행하고 `user.id`를 저장합니다.
                           일반 게임 흐름은 `POST /api/users` → `POST /api/users/{userId}/gacha`입니다.
                        2. `GET /api/stages/5?userId=...`로 전통시장 소개와 퀘스트 목록을 조회합니다.
                        3. `POST /api/quests/501/start`로 Q01을 시작한 뒤 `/501/chat`으로 대화합니다.
                        4. 응답의 `questCompleted=true`를 확인하면 다음 과제를 시작합니다. 순서는 **501 → 502 → 503 → 504**입니다.
                        5. 코인과 지도 갱신은 대화 응답 또는 `GET /api/users/{userId}/map`을 사용합니다.

                        ### 코인과 화면 처리
                        - 가입 시 최초 코인 **1개 지급**, 가챠당 **1개 차감**, 각 퀘스트 최초 완료 시 보상합니다.
                        - `questCompleted`: 이번 과제 완료 / `stageCleared`: 장소 전체 완료 / `regionConquered`: 지역 전체 정복입니다.
                        - 전통시장 Q04 완료는 장소 완료입니다. 현재 초기 데이터에는 다른 장소도 있어 지역 정복과는 구분됩니다.
                        - 오류 본문은 `{ "message": "설명" }`입니다. 400=입력·코인 부족, 403=미해금,
                          404=대상 없음, 409=진행 순서·이미 완료·동시 요청 충돌입니다.
                        - `GET /api/health`의 `llmMode=mock`은 정해진 순서로 응답하는 연동용이며 `real`은 실제 AI 모드입니다.

                        **주의:** `POST /api/demo/reset`은 모든 사용자의 플레이 기록을 삭제합니다.
                        """)
                .version("0.0.1"));
    }
}
