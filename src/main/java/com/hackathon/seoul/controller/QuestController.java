package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hackathon.seoul.service.ChatService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "04. 퀘스트 · NPC 대화 (전통시장 연동)", description = "전통시장: 501(Q01 전집 찾기) → 502(Q02 시식) → 503(Q03 흥정) → 504(Q04 순대 포장). 각 과제에서 start 후 chat을 호출하세요.")
@RestController
@RequestMapping("/api/quests")
public class QuestController {
    private final ChatService chat;
    public QuestController(ChatService chat) { this.chat = chat; }
    @Operation(summary = "퀘스트 시작 · NPC 첫 대사 받기",
            description = "장소를 해금한 뒤 과제 입장 시 호출합니다. npcReply를 말풍선으로 표시하고 suggestions를 추천 답변 버튼으로 표시하세요. 미완료 과제를 다시 시작하면 해당 과제의 대화·임시 주문 상태가 초기화됩니다. 완료 과제의 확정 상태는 유지됩니다. 미해금 지역 403, 대상 없음 404, 이전 과제 미완료 409입니다.")
    @PostMapping("/{questId}/start")
    public StartResponse start(@Parameter(description = "과제 ID. 전통시장 Q01~Q04는 501~504", example = "501") @PathVariable Long questId, @RequestBody UserIdRequest req) {
        return chat.startQuest(questId, req.userId());
    }
    @Operation(summary = "NPC와 대화 · 완료 여부와 보상 받기",
            description = "start 이후 사용자 한국어 메시지를 보냅니다(양끝 공백 제거 후 1~300자). npcReply는 NPC 말풍선, correction은 교정, suggestions는 추천 답변입니다. questCompleted=true면 다음 과제를 시작하세요. stageCleared=true는 장소 완료, regionConquered=true는 지역 정복 연출 기준입니다. coinsEarned는 이번 보상, coins는 현재 잔액입니다. context는 서버가 저장한 주문 상태로 화면 표시용이며 클라이언트가 수정해 보내지 않습니다. Q03은 금액 없는 흥정 후 9,000원 제안 → 다음 구매 수락으로 완료됩니다. LLM 실패 시 재질문하며 진행은 유지합니다. 입력 오류 400, 미해금 403, 대상 없음 404, 이전 과제 미완료·시작 전·이미 완료 409입니다.")
    @PostMapping("/{questId}/chat")
    public ChatResponse chat(@Parameter(description = "과제 ID. 전통시장 Q01~Q04는 501~504", example = "501") @PathVariable Long questId, @RequestBody ChatRequest req) {
        return chat.chatQuest(questId, req);
    }
    @Operation(summary = "대화에서 배운 한국어 표현 카드 생성",
            description = "시작한 과제의 대화 기록으로 복습 카드를 생성합니다. phrases의 korean은 한국어 표현, romanization은 발음 표기, meaning / whenToUse는 사용자의 설명 언어로 표시됩니다. 완료한 과제도 복습할 수 있습니다. 미해금 403, 대상 없음 404, 이전 과제 미완료·시작 전 409, 카드 파싱 실패 502입니다.")
    @PostMapping("/{questId}/phrase-card")
    public PhraseCard phraseCard(@Parameter(description = "과제 ID. 전통시장 Q01~Q04는 501~504", example = "501") @PathVariable Long questId, @RequestBody UserIdRequest req) {
        return chat.questPhraseCard(questId, req.userId());
    }
}
