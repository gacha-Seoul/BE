package com.hackathon.seoul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** 요청/응답 DTO 모음 (해커톤용으로 한 파일에) */
public final class Dtos {
    private Dtos() {}

    // ===== 요청 =====
    @Schema(description = "사용자 생성 요청. 각 항목과 본문 전체 생략 가능")
    public record CreateUserRequest(
            @Schema(description = "사용자 닉네임. 생략 시 Traveler", example = "Emma")
            String nickname,
            @Schema(description = "교정 이유와 표현 뜻을 설명할 언어. 생략 시 English", example = "English")
            String language,
            @Schema(description = "한국어 수준. 생략 시 BEGINNER", example = "BEGINNER")
            String level) {}
    @Schema(description = "가챠 요청. 재시도 중복 차감을 막기 위한 requestId 필수")
    public record GachaRequest(
            @Schema(description = "뽑기 한 번마다 생성하는 UUID. 통신 재시도에는 같은 값, 새 뽑기에는 새 값 사용", requiredMode = Schema.RequiredMode.REQUIRED, example = "550e8400-e29b-41d4-a716-446655440000", minLength = 1, maxLength = 100)
            String requestId) {}
    @Schema(description = "퀘스트 시작·표현 카드 요청")
    public record UserIdRequest(
            @Schema(description = "사용자 생성 응답의 id. 이후 요청에 동일한 값을 사용", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
            Long userId) {}
    @Schema(description = "NPC 대화 한 턴 요청")
    public record ChatRequest(
            @Schema(description = "사용자 생성 응답의 id. 이후 요청에 동일한 값을 사용", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
            Long userId,
            @Schema(description = "사용자가 NPC에게 보내는 한국어 문장. 양끝 공백 제거 후 1~300자", requiredMode = Schema.RequiredMode.REQUIRED, example = "조금 깎아 주세요", minLength = 1, maxLength = 300)
            String message) {}

    // ===== 유저 / 지도 =====
    @Schema(description = "사용자 정보와 현재 코인 잔액")
    public record UserResponse(
            @Schema(description = "사용자ID. 이후 API 요청에서 userId로 사용")
            Long id,
            @Schema(description = "사용자 닉네임. 생략 시 Traveler", example = "Emma")
            String nickname,
            @Schema(description = "교정 이유와 표현 뜻을 설명할 언어. 생략 시 English", example = "English")
            String language,
            @Schema(description = "한국어 수준. 생략 시 BEGINNER", example = "BEGINNER")
            String level,
            @Schema(description = "사용자의 현재 코인 잔액", example = "1")
            int coins) {}

    /** status: LOCKED(미해금) / UNLOCKED(도전 중) / CONQUERED(정복) */
    @Schema(description = "지도에 표시할 서울 구 상태")
    public record MapRegion(
            @Schema(description = "지역 ID. 서대문구는 13")
            Long id,
            @Schema(description = "3D 지도와 매칭할 지역 코드. 예: SEODAEMUN")
            String code,
            @Schema(description = "서울 구 이름. 3D 지도 GeoJSON 이름과 매칭", example = "서대문구")
            String name,
            @Schema(description = "서울 구 영문 이름", example = "Seodaemun-gu")
            String nameEn,
            @Schema(description = "지역 상태: LOCKED=미해금, UNLOCKED=도전 중, CONQUERED=정복", allowableValues = {"LOCKED", "UNLOCKED", "CONQUERED"})
            String status,
            @Schema(description = "등록된 퀘스트가 있어 가챠 후보가 될 수 있는 지역인지 여부")
            boolean playable,
            @Schema(description = "이 지역에서 모든 퀘스트를 완료한 장소 수")
            int clearedStages,
            @Schema(description = "이 지역에 등록된 전체 장소 수")
            int totalStages) {}
    @Schema(description = "3D 지도 전체 상태")
    public record MapResponse(
            @Schema(description = "사용자 생성 응답의 id. 이후 요청에 동일한 값을 사용", example = "1")
            Long userId,
            @Schema(description = "사용자의 현재 코인 잔액", example = "1")
            int coins,
            @Schema(description = "뽑기 1회 비용. 기본 1코인", example = "1")
            int gachaCost,
            @Schema(description = "모든 과제를 완료해 정복한 지역 수")
            int conqueredCount,
            @Schema(description = "서울 25개 구 목록과 사용자별 해금·정복 상태")
            List<MapRegion> regions) {}

    // ===== 지역 / 스테이지 =====
    @Schema(description = "퀘스트의 미션 달성 상태")
    public record MissionStatus(
            @Schema(description = "해당 퀘스트 안의 미션 번호. 0부터 시작", example = "0")
            int index,
            @Schema(description = "미션 표시 문구", example = "전집 위치 묻기")
            String text,
            @Schema(description = "서버가 판정한 미션 달성 여부", example = "false")
            boolean done) {}
    @Schema(description = "장소 안의 개별 퀘스트와 진행 상태")
    public record QuestSummary(
            @Schema(description = "퀘스트 ID. 전통시장 과제는 501~504")
            Long id,
            @Schema(description = "퀘스트 표시 코드. Q01~Q04")
            String code,
            @Schema(description = "화면에 표시할 제목")
            String title,
            @Schema(description = "콘텐츠 표시·진행 순서", example = "1")
            int orderNo,
            @Schema(description = "이 퀘스트에서 대화할 NPC 역할", example = "전집 상인")
            String npcRole,
            @Schema(description = "NPC와 대화할 상황 설명")
            String situation,
            @Schema(description = "해당 퀘스트 최초 완료 시 지급할 코인", example = "1")
            int rewardCoin,
            @Schema(description = "과제 상태: LOCKED=잠김, AVAILABLE=시작 가능, IN_PROGRESS=진행 중, COMPLETED=완료", allowableValues = {"LOCKED", "AVAILABLE", "IN_PROGRESS", "COMPLETED"})
            String status,
            @Schema(description = "이 퀘스트의 미션 체크리스트")
            List<MissionStatus> missions) {}
    @Schema(description = "장소 소개와 하위 퀘스트 목록")
    public record StageSummary(
            @Schema(description = "장소 ID. 전통시장은 5")
            Long id,
            @Schema(description = "분야 코드: HISTORY / CULTURE / ART / TOURISM / LIVING", example = "CULTURE")
            String category,
            @Schema(description = "화면에 표시할 분야 이름", example = "문화")
            String categoryLabel,
            @Schema(description = "콘텐츠 표시·진행 순서", example = "1")
            int orderNo,
            @Schema(description = "장소 이름", example = "영천시장 · 전통시장 퀘스트")
            String place,
            @Schema(description = "장소 입장 시 보여줄 이야기·상황 소개")
            String introStory,
            @Schema(description = "장소 이미지 주소. 등록된 이미지가 없으면 null")
            String imageUrl,
            @Schema(description = "이 장소의 모든 퀘스트를 완료했는지 여부")
            boolean cleared,
            @Schema(description = "완료한 퀘스트 수. 장소 진행률 분자", example = "0")
            int completedQuests,
            @Schema(description = "장소의 전체 퀘스트 수. 장소 진행률 분모", example = "4")
            int totalQuests,
            @Schema(description = "순서대로 진행하는 퀘스트 목록")
            List<QuestSummary> quests) {}
    @Schema(description = "지역 상세와 장소 목록")
    public record RegionDetail(
            @Schema(description = "지역 ID. 서대문구는 13")
            Long id,
            @Schema(description = "3D 지도와 매칭할 지역 코드. 예: SEODAEMUN")
            String code,
            @Schema(description = "서울 구 이름. 3D 지도 GeoJSON 이름과 매칭", example = "서대문구")
            String name,
            @Schema(description = "서울 구 영문 이름", example = "Seodaemun-gu")
            String nameEn,
            @Schema(description = "지역 소개 문구")
            String description,
            @Schema(description = "지역 상태: LOCKED / UNLOCKED / CONQUERED", allowableValues = {"LOCKED", "UNLOCKED", "CONQUERED"})
            String status,
            @Schema(description = "지역에 속한 장소 목록")
            List<StageSummary> stages) {}
    @Schema(description = "지역 뽑기 결과")
    public record GachaResponse(
            @Schema(description = "가챠로 해금한 지역의 상세 정보와 장소·퀘스트 목록")
            RegionDetail region,
            @Schema(description = "해당 뽑기 직후 잔액. 같은 requestId 재시도 시 당시 잔액 반환", example = "0")
            int coinsLeft,
            @Schema(description = "뽑기 한 번마다 생성하는 UUID. 통신 재시도에는 같은 값, 새 뽑기에는 새 값 사용", example = "550e8400-e29b-41d4-a716-446655440000")
            String requestId) {}

    // ===== 대화 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "화면에 표시할 Correction 정보")
    public record Correction(
            @Schema(description = "사용자가 말한 원래 표현", example = "이거 얼마")
            String original,
            @Schema(description = "더 자연스럽거나 정중한 한국어 표현", example = "이거 얼마예요?")
            String better,
            @Schema(description = "교정 이유. 사용자 설명 언어로 표시")
            String reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "화면에 표시할 Suggestion 정보")
    public record Suggestion(
            @Schema(description = "한국어 표현", example = "포장해 주세요")
            String korean,
            @Schema(description = "한국어 발음의 로마자 표기. 없으면 null", example = "pojanghae juseyo")
            String romanization,
            @Schema(description = "사용자 설명 언어로 된 뜻. 없으면 null", example = "To go, please.")
            String meaning) {}

    /** LLM이 돌려주는 JSON 형식 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LlmTurn(String npcReply, Correction correction, String culturalTip,
                          List<Suggestion> suggestions, List<Integer> newlyCompletedMissions, Map<String, Object> contextUpdates) {}

    @Schema(description = "NPC 응답, 서버 완료 판정, 보상과 주문 상태")
    public record ChatResponse(
            @Schema(description = "NPC 말풍선에 표시할 한국어 답변")
            String npcReply,
            @Schema(description = "문장 교정 정보. 교정이 필요 없으면 null")
            Correction correction,
            @Schema(description = "문화 팁. 없으면 null, 있으면 사용자 설명 언어로 표시")
            String culturalTip,
            @Schema(description = "다음 사용자 발화로 사용할 추천 답변 버튼 목록")
            List<Suggestion> suggestions,
            @Schema(description = "이 퀘스트의 미션 체크리스트")
            List<MissionStatus> missions,
            @Schema(description = "이번 턴에 장소의 모든 퀘스트를 처음 완료했는지 여부. 장소 완료 연출에 사용", example = "false")
            boolean stageCleared,
            @Schema(description = "이번 턴에 지역 전체 퀘스트를 처음 완료해 정복했는지 여부. 지도 갱신에 사용", example = "false")
            boolean regionConquered,
            @Schema(description = "이번 턴에 지급한 코인 수. 이미 완료한 과제에는 재지급하지 않음", example = "0")
            int coinsEarned,
            @Schema(description = "사용자의 현재 코인 잔액", example = "1")
            int coins,
            @Schema(description = "현재 대화하는 퀘스트 ID. 전통시장 Q01~Q04는 501~504", example = "501")
            Long questId,
            @Schema(description = "이번 턴에 퀘스트를 처음 완료했는지 여부. true면 다음 퀘스트를 시작", example = "false")
            boolean questCompleted,
            @Schema(description = "서버에 저장된 주문·진행 상태. 과제마다 필드가 다르며 클라이언트는 표시용으로만 사용")
            Map<String, Object> context) {}

    @Schema(description = "과제 시작 응답. NPC 첫 대사와 현재 진행 상태")
    public record StartResponse(
            @Schema(description = "NPC 말풍선에 표시할 한국어 답변")
            String npcReply,
            @Schema(description = "다음 사용자 발화로 사용할 추천 답변 버튼 목록")
            List<Suggestion> suggestions,
            @Schema(description = "이 퀘스트의 미션 체크리스트")
            List<MissionStatus> missions,
            @Schema(description = "현재 대화하는 퀘스트 ID. 전통시장 Q01~Q04는 501~504", example = "501")
            Long questId,
            @Schema(description = "저장된 과제 상태. 완료 과제 재입장 시 COMPLETED 유지", allowableValues = {"IN_PROGRESS", "COMPLETED"})
            String status,
            @Schema(description = "서버에 저장된 주문·진행 상태. 과제마다 필드가 다르며 클라이언트는 표시용으로만 사용")
            Map<String, Object> context) {}

    // ===== 현장 표현 카드 =====
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "화면에 표시할 Phrase 정보")
    public record Phrase(
            @Schema(description = "한국어 표현", example = "포장해 주세요")
            String korean,
            @Schema(description = "한국어 발음의 로마자 표기. 없으면 null", example = "pojanghae juseyo")
            String romanization,
            @Schema(description = "사용자 설명 언어로 된 뜻. 없으면 null", example = "To go, please.")
            String meaning,
            @Schema(description = "이 표현을 쓰는 상황. 사용자 설명 언어로 표시")
            String whenToUse) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "복습 표현 카드")
    public record PhraseCard(
            @Schema(description = "화면에 표시할 제목")
            String title,
            @Schema(description = "이 과제의 대화에서 뽑은 복습 표현 목록")
            List<Phrase> phrases) {}

    // ===== 데모 / 상태 =====
    @Schema(description = "데모용 사용자 생성·첫 가챠 응답")
    public record DemoStartResponse(
            @Schema(description = "생성된 사용자 정보. user.id를 이후 요청의 userId로 사용")
            UserResponse user,
            @Schema(description = "첫 가챠 결과. 기본 서대문구 해금, 코인 잔액 0")
            GachaResponse gacha) {}
    @Schema(description = "서버 연결과 AI 모드 확인")
    public record HealthResponse(
            @Schema(description = "서버가 정상 응답하면 UP", allowableValues = {"UP"})
            String status,
            @Schema(description = "mock: 고정 연동 시나리오 / real: 실제 AI 모드. 외부 AI 호출 성공을 보장하는 값은 아님", example = "mock")
            String llmMode) {}
}
