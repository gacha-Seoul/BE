package com.hackathon.seoul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** 요청/응답 DTO 모음. 화면 번호는 기획 화면 명세 기준 (1-1 메인 가챠, 2-1 지역 지도, 3-1 퀘스트 안내, 3-3 대화) */
public final class Dtos {
    private Dtos() {}

    // ===== 요청 =====
    public record CreateUserRequest(String nickname, String language, String level) {}
    public record UserIdRequest(Long userId) {}
    /** 음성 인식 결과 텍스트 */
    public record TalkRequest(Long userId, String message) {}
    /** 화면 이벤트: arrived(도착), selected(상품 선택), selectedFrame(프레임 선택) ... */
    public record EventRequest(Long userId, String event) {}

    // ===== 유저 =====
    public record UserResponse(Long id, String nickname, String language, String level,
                               int coins, int freeDraws, int drawsLeft) {}

    // ===== 1-1 메인 가챠 화면 =====
    /** status: NOT_DRAWN / DRAWN / COMPLETED · playable=false 면 준비 중 */
    public record CandidateCard(Long courseId, Long regionId, String regionCode, String regionName, String regionNameEn,
                                String category, String categoryLabel, String imageKey, String status, boolean playable) {}

    /**
     * drawsLeft = 무료 + 코인 ("뽑기 n회 남음")
     * drawBlockedReason: null(가능) / NO_DRAWS(횟수 없음) / COMPLETE_A_QUEST(퀘스트 완료 후 가능) / ALL_DRAWN
     */
    public record MainResponse(Long userId, int coins, int freeDraws, int drawsLeft, boolean canDraw,
                               String drawBlockedReason, Long currentCourseId, List<CandidateCard> candidates) {}

    /** 1-2 뽑기 결과 팝업: 지역·카테고리, 캐릭터 대사, 장소 4개(대표·연결 장소) */
    public record GachaResponse(CandidateCard result, String openCopy, List<PlaceCard> places,
                                int coins, int freeDraws, int drawsLeft) {}

    // ===== 2-1 지역 퀘스트 지도 =====
    /** status: LOCKED / AVAILABLE / DONE / COMING_SOON(퀘스트 데이터 없음) */
    public record PlaceCard(Long id, int orderNo, String name, String representative, String candidates,
                            String pinKey, String imageKey, String status, int doneQuests, int totalQuests) {}

    public record CourseMapResponse(Long courseId, String regionCode, String regionName, String category,
                                    String categoryLabel, String openCopy, int coins,
                                    Long currentPlaceId, boolean completed, List<PlaceCard> places) {}

    // ===== 3-1 퀘스트 시작 안내 =====
    /** status: LOCKED / AVAILABLE / DONE */
    public record QuestCard(Long id, String code, int orderNo, String name, String title, String story, String status) {}

    public record PlaceDetail(Long id, Long courseId, String regionName, String categoryLabel, String name,
                              String title, String story, int coins, int doneQuests, int totalQuests,
                              String progress, Long currentQuestId, List<QuestCard> quests) {}

    // ===== 3-3 대화 =====
    /** 대사 한 줄: 한국어 / 로마자 / 영어 (음성은 프론트 TTS) */
    public record Line(String speaker, String ko, String roman, String en) {}

    /** 현재 대화 단계: 하단 플레이어 패널에 보여줄 대사 (= 힌트) */
    public record StepView(int stepNo, int totalSteps, String target, Line user) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Correction(String original, String better, String reason) {}

    /** type: HIGHLIGHT / PRICE / SHOW_CHOICE / CART_ADD / FRIEND_MESSAGE / SHOW_PHOTO / COUNTDOWN ... */
    public record Effect(String type, String value) {}

    public record QuestStartResponse(Long questId, String code, String name, String title, String story,
                                     String destinationKey, int totalSteps, Line opening, StepView step,
                                     String waitingEvent, int coins) {}

    public record TurnResponse(
            boolean passed,             // 이번 발화로 단계 통과?
            Line npc,                   // NPC 말풍선 (통과: 명세 대사 / 실패: 다시 묻기). 없으면 null
            Correction correction,      // 표현 교정 (없으면 null)
            StepView step,              // 다음 단계 (퀘스트 대화 끝나면 null)
            String stepProgress,        // "2/3" (통과한 단계 / 전체)
            String waitingEvent,        // 프론트가 보내야 할 화면 이벤트 (없으면 null)
            List<Effect> effects,       // 화면 처리
            boolean questCompleted,     // 퀘스트 완료 → 3-4 완료 화면
            int coinsEarned,
            int coins,
            Long nextQuestId,
            String placeProgress,       // "1/4"
            boolean placeCompleted,     // 장소 4/4 → 3-5 화면
            String placeEnding,
            boolean courseCompleted) {} // 지역 완료 팝업

    /** AI 판정 결과 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LlmJudgement(List<Integer> passedSteps, String npcReply, Correction correction) {}

    // ===== 표현 카드 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Phrase(String korean, String romanization, String meaning, String whenToUse) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhraseCard(String title, List<Phrase> phrases) {}

    // ===== 데모 / 상태 =====
    public record HealthResponse(String status, String llmMode) {}
    public record DemoStartResponse(UserResponse user, GachaResponse gacha) {}
}
