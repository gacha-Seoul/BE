package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.seoul.service.ChatService;
import com.hackathon.seoul.service.RegionService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "03. 장소 조회 · 기존 장소 대화", description = "Stage는 장소입니다. 장소 정보는 이 API로 조회하고, 전통시장 대화는 퀘스트 API 사용을 권장합니다.")
@RestController
@RequestMapping("/api/stages")
public class StageController {

    private final ChatService chatService;
    private final RegionService regionService;

    public StageController(ChatService chatService, RegionService regionService) {
        this.chatService = chatService;
        this.regionService = regionService;
    }

    @Operation(summary = "장소 소개 · 퀘스트 진행률 조회",
            description = "장소 입장 화면에서 소개 문구, 이미지, 퀘스트 목록과 completedQuests / totalQuests를 표시합니다. 전통시장은 stageId=5이며 Q01~Q04의 questId는 501~504입니다. 없는 사용자·장소는 404입니다.")
    @GetMapping("/{stageId}")
    public StageSummary get(@Parameter(description = "장소 ID. 기본 전통시장은 5", example = "5") @PathVariable Long stageId, @Parameter(description = "사용자 생성 또는 demo/start 응답의 사용자 ID", example = "1") @RequestParam Long userId) {
        return regionService.stage(userId, stageId);
    }

    /** 스테이지 시작: NPC 첫 마디 + 추천 답변 + 미션 목록 */
    @Operation(summary = "현재 퀘스트 자동 선택 · 시작 (기존 경로)",
            description = "장소의 첫 미완료 퀘스트를 자동 선택해 시작합니다. 모두 완료했다면 마지막 과제를 선택합니다. 미완료 과제의 대화와 임시 상태가 초기화됩니다. 응답 questId를 확인하세요. 전통시장 신규 연동은 POST /api/quests/{questId}/start를 권장합니다. 미해금 지역은 403, 없는 대상은 404입니다.")
    @PostMapping("/{stageId}/start")
    public StartResponse start(@Parameter(description = "장소 ID. 기본 전통시장은 5", example = "5") @PathVariable Long stageId, @RequestBody UserIdRequest req) {
        return chatService.start(stageId, req.userId());
    }

    /** 대화 한 턴 */
    @Operation(summary = "현재 퀘스트에 대화 보내기 (기존 경로)",
            description = "장소의 첫 미완료 과제에 메시지를 보냅니다. 앞 과제를 완료하면 대상이 다음 과제로 바뀌므로 먼저 start가 필요합니다. 전통시장은 POST /api/quests/{questId}/chat을 사용하면 과제를 명확히 지정할 수 있습니다. 입력 오류 400, 미해금 403, 대상 없음 404, 시작 전·완료된 과제 409입니다.")
    @PostMapping("/{stageId}/chat")
    public ChatResponse chat(@Parameter(description = "장소 ID. 기본 전통시장은 5", example = "5") @PathVariable Long stageId, @RequestBody ChatRequest req) {
        return chatService.chat(stageId, req);
    }

    /** 현장 표현 카드 */
    @Operation(summary = "현재 퀘스트의 표현 카드 생성 (기존 경로)",
            description = "장소의 첫 미완료 과제(전부 완료했다면 마지막 과제)의 대화로 복습 표현 카드를 만듭니다. 완료한 특정 과제를 복습하려면 POST /api/quests/{questId}/phrase-card를 사용하세요. 미해금 403, 대상 없음 404, 시작 전 409입니다.")
    @PostMapping("/{stageId}/phrase-card")
    public PhraseCard phraseCard(@Parameter(description = "장소 ID. 기본 전통시장은 5", example = "5") @PathVariable Long stageId, @RequestBody UserIdRequest req) {
        return chatService.phraseCard(stageId, req.userId());
    }
}
