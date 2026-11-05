package com.hackathon.seoul.controller;

import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quests")
public class QuestController {
    private final ChatService chat;
    public QuestController(ChatService chat) { this.chat = chat; }
    @PostMapping("/{questId}/start")
    public StartResponse start(@PathVariable Long questId, @RequestBody UserIdRequest req) {
        return chat.startQuest(questId, req.userId());
    }
    @PostMapping("/{questId}/chat")
    public ChatResponse chat(@PathVariable Long questId, @RequestBody ChatRequest req) {
        return chat.chatQuest(questId, req);
    }
    @PostMapping("/{questId}/phrase-card")
    public PhraseCard phraseCard(@PathVariable Long questId, @RequestBody UserIdRequest req) {
        return chat.questPhraseCard(questId, req.userId());
    }
}
