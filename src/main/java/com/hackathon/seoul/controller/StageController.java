package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.ChatService;
import com.hackathon.seoul.service.RegionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stages")
public class StageController {

    private final ChatService chatService;
    private final RegionService regionService;

    public StageController(ChatService chatService, RegionService regionService) {
        this.chatService = chatService;
        this.regionService = regionService;
    }

    @GetMapping("/{stageId}")
    public StageSummary get(@PathVariable Long stageId, @RequestParam Long userId) {
        return regionService.stage(userId, stageId);
    }

    /** 스테이지 시작: NPC 첫 마디 + 추천 답변 + 미션 목록 */
    @PostMapping("/{stageId}/start")
    public StartResponse start(@PathVariable Long stageId, @RequestBody UserIdRequest req) {
        return chatService.start(stageId, req.userId());
    }

    /** 대화 한 턴 */
    @PostMapping("/{stageId}/chat")
    public ChatResponse chat(@PathVariable Long stageId, @RequestBody ChatRequest req) {
        return chatService.chat(stageId, req);
    }

    /** 현장 표현 카드 */
    @PostMapping("/{stageId}/phrase-card")
    public PhraseCard phraseCard(@PathVariable Long stageId, @RequestBody UserIdRequest req) {
        return chatService.phraseCard(stageId, req.userId());
    }
}
