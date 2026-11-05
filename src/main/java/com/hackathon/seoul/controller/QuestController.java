package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.QuestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    /** 퀘스트 시작 / 재개 → 첫 대화 단계 */
    @PostMapping("/{questId}/start")
    public QuestStartResponse start(@PathVariable Long questId, @RequestBody UserIdRequest req) {
        return questService.start(questId, req.userId());
    }

    /** 3-3 플레이어 발화 (음성 인식 텍스트) → 판정 */
    @PostMapping("/{questId}/talk")
    public TurnResponse talk(@PathVariable Long questId, @RequestBody TalkRequest req) {
        return questService.talk(questId, req);
    }

    /** 화면 이벤트: {"userId":1, "event":"arrived"} */
    @PostMapping("/{questId}/event")
    public TurnResponse event(@PathVariable Long questId, @RequestBody EventRequest req) {
        return questService.event(questId, req);
    }

    /** 현장 표현 카드 */
    @PostMapping("/{questId}/phrase-card")
    public PhraseCard phraseCard(@PathVariable Long questId, @RequestBody UserIdRequest req) {
        return questService.phraseCard(questId, req.userId());
    }
}
