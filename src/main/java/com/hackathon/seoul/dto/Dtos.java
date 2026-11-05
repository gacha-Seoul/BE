package com.hackathon.seoul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** 요청/응답 DTO 모음 (해커톤용으로 한 파일에) */
public final class Dtos {
    private Dtos() {}

    // ===== 요청 =====
    public record CreateUserRequest(String nickname, String language, String level) {}
    public record UserIdRequest(Long userId) {}
    public record ChatRequest(Long userId, String message) {}

    // ===== 유저 / 지도 =====
    public record UserResponse(Long id, String nickname, String language, String level, int coins) {}

    /** status: LOCKED(미해금) / UNLOCKED(도전 중) / CONQUERED(정복) */
    public record MapRegion(Long id, String code, String name, String nameEn, String status,
                            boolean playable, int clearedStages, int totalStages) {}
    public record MapResponse(Long userId, int coins, int gachaCost, int conqueredCount, List<MapRegion> regions) {}

    // ===== 지역 / 스테이지 =====
    public record MissionStatus(int index, String text, boolean done) {}
    public record StageSummary(Long id, String category, String categoryLabel, int orderNo,
                               String place, String npcRole, String situation,
                               int rewardCoin, boolean cleared, List<MissionStatus> missions) {}
    public record RegionDetail(Long id, String code, String name, String nameEn, String description,
                               String status, List<StageSummary> stages) {}
    public record GachaResponse(RegionDetail region, int coinsLeft) {}

    // ===== 대화 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Correction(String original, String better, String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(String korean, String romanization, String meaning) {}

    /** LLM이 돌려주는 JSON 형식 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LlmTurn(String npcReply, Correction correction, String culturalTip,
                          List<Suggestion> suggestions, List<Integer> newlyCompletedMissions) {}

    public record ChatResponse(
            String npcReply,
            Correction correction,          // 없으면 null
            String culturalTip,             // 없으면 null
            List<Suggestion> suggestions,   // 추천 답변 버튼 3개
            List<MissionStatus> missions,   // 미션 체크리스트
            boolean stageCleared,           // 이번 턴에 클리어?
            boolean regionConquered,        // 이번 턴에 지역 정복?
            int coinsEarned,
            int coins) {}

    public record StartResponse(String npcReply, List<Suggestion> suggestions, List<MissionStatus> missions) {}

    // ===== 현장 표현 카드 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Phrase(String korean, String romanization, String meaning, String whenToUse) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhraseCard(String title, List<Phrase> phrases) {}

    // ===== 데모 / 상태 =====
    public record DemoStartResponse(UserResponse user, GachaResponse gacha) {}
    public record HealthResponse(String status, String llmMode) {}
}
