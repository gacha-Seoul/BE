package com.hackathon.seoul.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.llm.*;
import com.hackathon.seoul.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 스테이지 대화 엔진 (모든 스테이지 공통).
 * - LLM: NPC 대사, 교정, 추천 답변, "이번 말로 달성한 미션 번호" 제안
 * - 서버: 미션 저장, 클리어 판정, 코인 지급, 지역 정복 판정 (LLM이 코인을 직접 못 건드림)
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int HISTORY_LIMIT = 20;
    private static final int MAX_MESSAGE_LENGTH = 300;

    private final Finder finder;
    private final StageRepository stageRepo;
    private final StageProgressRepository progressRepo;
    private final ChatMessageRepository messageRepo;
    private final LlmClient llm;
    private final PromptFactory prompts;
    private final ObjectMapper objectMapper;
    private final GameProperties props;

    public ChatService(Finder finder, StageRepository stageRepo, StageProgressRepository progressRepo,
                       ChatMessageRepository messageRepo, LlmClient llm, PromptFactory prompts,
                       ObjectMapper objectMapper, GameProperties props) {
        this.finder = finder;
        this.stageRepo = stageRepo;
        this.progressRepo = progressRepo;
        this.messageRepo = messageRepo;
        this.llm = llm;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    /** 스테이지 시작(또는 다시 시작): 대화 기록 초기화 + NPC 첫 마디 */
    @Transactional
    public StartResponse start(Long stageId, Long userId) {
        AppUser user = finder.user(userId);
        Stage stage = finder.stage(stageId);
        finder.unlockedRegion(userId, stage.getRegion().getId());

        StageProgress progress = getOrCreateProgress(user, stage);
        progress.resetIfNotCleared();
        messageRepo.deleteHistory(userId, stageId);
        messageRepo.save(new ChatMessage(user, stage, ChatMessage.Role.NPC, stage.getOpeningLine()));

        List<Suggestion> suggestions = stage.getStarterSuggestionList().stream()
                .map(k -> new Suggestion(k, null, null)).toList();
        return new StartResponse(stage.getOpeningLine(), suggestions, RegionService.missions(stage, progress));
    }

    @Transactional
    public ChatResponse chat(Long stageId, ChatRequest req) {
        if (req == null || req.message() == null || req.message().isBlank()) {
            throw new GameException(HttpStatus.BAD_REQUEST, "message가 비어 있어요.");
        }
        String message = req.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) message = message.substring(0, MAX_MESSAGE_LENGTH);

        AppUser user = finder.user(req.userId());
        Stage stage = finder.stage(stageId);
        UserRegion userRegion = finder.unlockedRegion(user.getId(), stage.getRegion().getId());
        StageProgress progress = getOrCreateProgress(user, stage);

        messageRepo.save(new ChatMessage(user, stage, ChatMessage.Role.USER, message));

        // 1) LLM 호출
        String system = prompts.chatSystemPrompt(user, stage, progress.getCompletedSet());
        LlmTurn turn = callChat(system, history(user.getId(), stageId));

        messageRepo.save(new ChatMessage(user, stage, ChatMessage.Role.NPC, turn.npcReply()));

        // 2) 서버가 미션/클리어/코인 판정
        boolean stageCleared = progress.markCompleted(turn.newlyCompletedMissions(), stage.getMissionList().size());
        int coinsEarned = 0;
        boolean regionConquered = false;

        if (stageCleared) {
            coinsEarned += stage.getRewardCoin();
            if (!userRegion.isConquered() && allStagesCleared(user.getId(), stage.getRegion().getId())) {
                userRegion.conquer();
                regionConquered = true;
                coinsEarned += props.regionConquerBonus;
            }
            user.addCoins(coinsEarned);
        }

        return new ChatResponse(
                turn.npcReply(),
                turn.correction(),
                turn.culturalTip(),
                turn.suggestions() == null ? List.of() : turn.suggestions(),
                RegionService.missions(stage, progress),
                stageCleared,
                regionConquered,
                coinsEarned,
                user.getCoins());
    }

    /** 대화 기록으로 현장 표현 카드 생성 */
    @Transactional(readOnly = true)
    public PhraseCard phraseCard(Long stageId, Long userId) {
        AppUser user = finder.user(userId);
        Stage stage = finder.stage(stageId);

        StringBuilder transcript = new StringBuilder();
        for (ChatMessage m : messageRepo.findByUserIdAndStageIdOrderByIdAsc(userId, stageId)) {
            transcript.append(m.getRole() == ChatMessage.Role.USER ? "학습자: " : "NPC: ")
                    .append(m.getContent()).append("\n");
        }
        if (transcript.isEmpty()) transcript.append("(대화 없음. 이 장소에서 기본적으로 쓸 표현을 골라 줘.)");

        String raw = llm.completeJson(LlmPurpose.PHRASE_CARD, prompts.phraseCardSystemPrompt(user, stage),
                List.of(new LlmMessage("user", transcript.toString())));
        try {
            return objectMapper.readValue(stripCodeFence(raw), PhraseCard.class);
        } catch (Exception e) {
            log.warn("표현 카드 파싱 실패: {}", raw, e);
            throw new GameException(HttpStatus.BAD_GATEWAY, "표현 카드를 만들지 못했어요. 다시 시도해 주세요.");
        }
    }

    // ===== 내부 =====

    private LlmTurn callChat(String system, List<LlmMessage> history) {
        String raw = null;
        try {
            raw = llm.completeJson(LlmPurpose.CHAT, system, history);
            LlmTurn turn = objectMapper.readValue(stripCodeFence(raw), LlmTurn.class);
            if (turn.npcReply() == null || turn.npcReply().isBlank()) throw new IllegalStateException("npcReply 없음");
            return turn;
        } catch (Exception e) {
            // 데모 중 LLM이 실패해도 화면이 멈추지 않게 안전한 기본 응답
            log.warn("LLM 응답 처리 실패. raw={}", raw, e);
            return new LlmTurn("미안해요, 잘 못 들었어요. 한 번 더 말해 줄래요?", null, null, List.of(), List.of());
        }
    }

    private List<LlmMessage> history(Long userId, Long stageId) {
        List<ChatMessage> all = messageRepo.findByUserIdAndStageIdOrderByIdAsc(userId, stageId);
        List<ChatMessage> recent = all.size() > HISTORY_LIMIT ? all.subList(all.size() - HISTORY_LIMIT, all.size()) : all;
        return recent.stream()
                .map(m -> new LlmMessage(m.getRole() == ChatMessage.Role.USER ? "user" : "assistant", m.getContent()))
                .toList();
    }

    private boolean allStagesCleared(Long userId, Long regionId) {
        long total = stageRepo.countByRegionId(regionId);
        long cleared = progressRepo.findByUserIdAndStageRegionId(userId, regionId).stream()
                .filter(StageProgress::isCleared).count();
        return total > 0 && cleared >= total;
    }

    private StageProgress getOrCreateProgress(AppUser user, Stage stage) {
        return progressRepo.findByUserIdAndStageId(user.getId(), stage.getId())
                .orElseGet(() -> progressRepo.save(new StageProgress(user, stage)));
    }

    /** 모델이 ```json ... ``` 로 감싸서 줄 때 대비 */
    private static String stripCodeFence(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("```")) {
            int first = s.indexOf('\n');
            int last = s.lastIndexOf("```");
            if (first > 0 && last > first) s = s.substring(first + 1, last).trim();
        }
        return s;
    }
}
