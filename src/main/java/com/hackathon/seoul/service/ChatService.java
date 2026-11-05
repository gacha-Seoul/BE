package com.hackathon.seoul.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.*;

/** LLM 응답은 제안이다. 진행, 완료, 코인은 사용자 잠금 안에서 서버가 결정한다. */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final Finder finder;
    private final QuestRepository quests;
    private final QuestProgressRepository progress;
    private final ChatMessageRepository messages;
    private final CoinTransactionRepository coins;
    private final LlmClient llm;
    private final PromptFactory prompts;
    private final ObjectMapper json;
    private final GameProperties props;
    public ChatService(Finder finder, QuestRepository quests, QuestProgressRepository progress,
                       ChatMessageRepository messages, CoinTransactionRepository coins, LlmClient llm,
                       PromptFactory prompts, ObjectMapper json, GameProperties props) {
        this.finder = finder; this.quests = quests; this.progress = progress; this.messages = messages;
        this.coins = coins; this.llm = llm; this.prompts = prompts; this.json = json; this.props = props;
    }

    // 기존 장소 API는 현재 진행 가능한 첫 퀘스트를 선택한다.
    @Transactional
    public StartResponse start(Long stageId, Long userId) {
        finder.lockedUser(userId);
        return startQuest(currentQuest(stageId, userId).getId(), userId);
    }
    @Transactional
    public ChatResponse chat(Long stageId, ChatRequest req) {
        validateRequest(req);
        finder.lockedUser(req.userId());
        return chatQuest(currentQuest(stageId, req.userId()).getId(), req);
    }
    @Transactional(readOnly = true)
    public PhraseCard phraseCard(Long stageId, Long userId) {
        return questPhraseCard(currentQuest(stageId, userId).getId(), userId);
    }

    @Transactional
    public StartResponse startQuest(Long questId, Long userId) {
        AppUser user = finder.lockedUser(userId);
        Quest q = accessibleQuest(questId, userId);
        QuestProgress state = progress.findByUserIdAndQuestId(userId, questId)
                .orElseGet(() -> progress.save(new QuestProgress(user, q)));
        // 완료한 주문과 대화는 재입장 시에도 유지한다.
        if (!state.isCompleted()) {
            state.restart();
            messages.deleteHistory(state.getId());
            messages.save(new ChatMessage(state, ChatMessage.Role.NPC, q.getOpeningLine()));
        }
        return new StartResponse(q.getOpeningLine(), q.getStarterSuggestionList().stream()
                .map(x -> new Suggestion(x, null, null)).toList(), RegionService.missions(q, state),
                questId, state.getStatus().name(), context(state));
    }

    @Transactional
    public ChatResponse chatQuest(Long questId, ChatRequest req) {
        validateRequest(req);
        AppUser user = finder.lockedUser(req.userId());
        Quest q = accessibleQuest(questId, user.getId());
        QuestProgress state = progress.findByUserIdAndQuestId(user.getId(), questId)
                .orElseThrow(() -> new GameException(HttpStatus.CONFLICT, "퀘스트를 먼저 시작해 주세요."));
        if (state.isCompleted()) throw new GameException(HttpStatus.CONFLICT, "이미 완료한 퀘스트예요.");
        String message = req.message().trim();
        if (message.length() > 300) throw new GameException(HttpStatus.BAD_REQUEST, "message는 300자 이하여야 해요.");
        messages.save(new ChatMessage(state, ChatMessage.Role.USER, message));
        List<LlmMessage> history = messages.findByProgressIdOrderByIdAsc(state.getId()).stream()
                .map(x -> new LlmMessage(x.getRole() == ChatMessage.Role.USER ? "user" : "assistant", x.getContent())).toList();
        history = history.subList(Math.max(0, history.size() - 20), history.size());
        Map<String, Object> ctx = context(state);
        LlmTurn turn = callChat(prompts.chatSystemPrompt(user, q, ctx), history);
        messages.save(new ChatMessage(state, ChatMessage.Role.NPC, turn.npcReply()));
        boolean complete = evaluate(q, ctx, turn);
        state.updateContext(write(ctx));
        boolean newlyCompleted = complete && state.complete();
        int earned = 0;
        boolean conquered = false;
        if (newlyCompleted) {
            earned += credit(user, q.getRewardCoin(), CoinTransaction.Type.QUEST_CLEAR,
                    "QUEST_CLEAR:" + user.getId() + ":" + questId);
            UserRegion region = finder.unlockedRegion(user.getId(), q.getRegion().getId());
            long total = quests.countByStageRegionId(q.getRegion().getId());
            long done = progress.countByUserIdAndQuestStageRegionIdAndStatus(user.getId(), q.getRegion().getId(), QuestProgress.Status.COMPLETED);
            if (!region.isConquered() && total > 0 && done == total) {
                region.conquer(); conquered = true;
                earned += credit(user, props.regionConquerBonus, CoinTransaction.Type.REGION_CONQUER,
                        "REGION_CONQUER:" + user.getId() + ":" + q.getRegion().getId());
            }
        }
        boolean stageCleared = newlyCompleted && quests.findByStageIdOrderByOrderNoAsc(q.getStage().getId()).stream()
                .allMatch(x -> progress.findByUserIdAndQuestId(user.getId(), x.getId()).map(QuestProgress::isCompleted).orElse(false));
        return new ChatResponse(turn.npcReply(), turn.correction(), turn.culturalTip(),
                turn.suggestions() == null ? List.of() : turn.suggestions(), RegionService.missions(q, state),
                stageCleared, conquered, earned, user.getCoins(), questId, newlyCompleted, ctx);
    }

    @Transactional(readOnly = true)
    public PhraseCard questPhraseCard(Long questId, Long userId) {
        AppUser user = finder.user(userId);
        Quest q = accessibleQuest(questId, userId);
        QuestProgress state = progress.findByUserIdAndQuestId(userId, questId)
                .orElseThrow(() -> new GameException(HttpStatus.CONFLICT, "퀘스트를 먼저 시작해 주세요."));
        StringBuilder transcript = new StringBuilder();
        messages.findByProgressIdOrderByIdAsc(state.getId()).forEach(m -> transcript.append(m.getRole())
                .append(": ").append(m.getContent()).append("\n"));
        String raw = llm.completeJson(LlmPurpose.PHRASE_CARD, prompts.phraseCardSystemPrompt(user, q),
                List.of(new LlmMessage("user", transcript.toString())));
        try { return json.readValue(stripCodeFence(raw), PhraseCard.class); }
        catch (Exception e) { throw new GameException(HttpStatus.BAD_GATEWAY, "표현 카드를 만들지 못했어요."); }
    }

    private Quest currentQuest(Long stageId, Long userId) {
        Stage stage = finder.stage(stageId);
        finder.unlockedRegion(userId, stage.getRegion().getId());
        List<Quest> list = quests.findByStageIdOrderByOrderNoAsc(stageId);
        if (list.isEmpty()) throw new GameException(HttpStatus.NOT_FOUND, "등록된 퀘스트가 없어요.");
        return list.stream().filter(q -> !progress.findByUserIdAndQuestId(userId, q.getId())
                .map(QuestProgress::isCompleted).orElse(false)).findFirst().orElse(list.get(list.size() - 1));
    }
    private Quest accessibleQuest(Long questId, Long userId) {
        Quest q = quests.findById(questId).orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "퀘스트가 없어요: " + questId));
        finder.unlockedRegion(userId, q.getRegion().getId());
        boolean locked = quests.findByStageIdOrderByOrderNoAsc(q.getStage().getId()).stream()
                .filter(x -> x.getOrderNo() < q.getOrderNo())
                .anyMatch(x -> !progress.findByUserIdAndQuestId(userId, x.getId()).map(QuestProgress::isCompleted).orElse(false));
        if (locked) throw new GameException(HttpStatus.CONFLICT, "이전 퀘스트를 먼저 완료해 주세요.");
        return q;
    }
    private int credit(AppUser user, int amount, CoinTransaction.Type type, String key) {
        if (coins.existsByEventKey(key)) return 0;
        coins.save(new CoinTransaction(user, amount, type, key));
        user.addCoins(amount); return amount;
    }
    private static void validateRequest(ChatRequest req) {
        if (req == null || req.userId() == null || req.message() == null || req.message().isBlank())
            throw new GameException(HttpStatus.BAD_REQUEST, "userId와 message가 필요해요.");
    }
    private Map<String, Object> context(QuestProgress state) {
        try { return json.readValue(state.getContextJson(), new TypeReference<LinkedHashMap<String, Object>>() {}); }
        catch (Exception e) { throw new IllegalStateException("퀘스트 상태 JSON 오류", e); }
    }
    private String write(Map<String, Object> ctx) {
        try { return json.writeValueAsString(ctx); }
        catch (Exception e) { throw new IllegalStateException("퀘스트 상태 저장 실패", e); }
    }

    /** 모델이 임의의 상태/보상/완료 플래그를 저장하지 못하게 규칙별 필드를 제한한다. */
    private boolean evaluate(Quest q, Map<String, Object> ctx, LlmTurn turn) {
        Map<String, Object> update = turn.contextUpdates() == null ? Map.of() : turn.contextUpdates();
        return switch (q.getCompletionRule()) {
            case ALL_MISSIONS -> {
                Set<Integer> done = new TreeSet<>();
                Object existing = ctx.get("completedMissions");
                if (existing instanceof Collection<?> list) for (Object x : list) if (x instanceof Integer i) done.add(i);
                if (turn.newlyCompletedMissions() != null) for (Integer i : turn.newlyCompletedMissions())
                    if (i != null && i >= 0 && i < q.getMissionList().size()) done.add(i);
                ctx.put("completedMissions", done);
                yield !q.getMissionList().isEmpty() && done.size() == q.getMissionList().size();
            }
            case FIND_STORE -> {
                if ("JEON_STORE".equals(update.get("arrivedAt"))) ctx.put("arrivedAt", "JEON_STORE");
                yield "JEON_STORE".equals(ctx.get("arrivedAt"));
            }
            case ACCEPT_SAMPLE -> {
                copyBoolean(update, ctx, "sampleAccepted");
                yield Boolean.TRUE.equals(ctx.get("sampleAccepted"));
            }
            case CONFIRM_NEGOTIATION -> {
                boolean priceAlreadyProposed = Integer.valueOf(9000).equals(ctx.get("acceptedPrice"));
                boolean negotiationRequested = Boolean.TRUE.equals(update.get("negotiationRequested"));
                boolean changed = false;
                if ("PUMPKIN_JEON".equals(update.get("selectedItem"))) {
                    changed = !Objects.equals(ctx.get("selectedItem"), update.get("selectedItem"));
                    ctx.put("selectedItem", "PUMPKIN_JEON");
                }
                changed |= copyPositiveInteger(update, ctx, "quantity", 100);
                changed |= copyPositiveInteger(update, ctx, "offeredPrice", 100000);
                if (changed || negotiationRequested) ctx.put("purchaseConfirmed", false);
                // 가격·예산은 게임 설정이며 LLM이 임의로 확정할 수 없다.
                if (negotiationRequested) ctx.put("negotiationRequested", true);
                if (!ctx.isEmpty()) ctx.put("budget", 9000);
                if (ctx.containsKey("offeredPrice") || Boolean.TRUE.equals(ctx.get("negotiationRequested"))) {
                    ctx.put("acceptedPrice", 9000);
                }
                // 할인 요청 턴은 가격 제안까지만 처리하고, 다음 턴의 구매 수락을 별도로 확인한다.
                if (priceAlreadyProposed && !changed && !negotiationRequested) copyBoolean(update, ctx, "purchaseConfirmed");
                yield "PUMPKIN_JEON".equals(ctx.get("selectedItem")) && Integer.valueOf(2).equals(ctx.get("quantity"))
                        && Integer.valueOf(9000).equals(ctx.get("acceptedPrice")) && Boolean.TRUE.equals(ctx.get("purchaseConfirmed"));
            }
            case CONFIRM_ORDER -> {
                boolean changed = false;
                if ("SUNDAE".equals(update.get("item"))) {
                    changed = !Objects.equals(ctx.get("item"), update.get("item")); ctx.put("item", "SUNDAE");
                }
                changed |= copyPositiveInteger(update, ctx, "quantity", 100);
                for (String key : List.of("excludeLiver", "takeout")) {
                    Object value = update.get(key);
                    if (value instanceof Boolean) { changed |= !Objects.equals(ctx.get(key), value); ctx.put(key, value); }
                }
                if (changed) ctx.put("orderConfirmed", false);
                copyBoolean(update, ctx, "orderConfirmed");
                yield "SUNDAE".equals(ctx.get("item")) && Integer.valueOf(1).equals(ctx.get("quantity"))
                        && Boolean.TRUE.equals(ctx.get("excludeLiver")) && Boolean.TRUE.equals(ctx.get("takeout"))
                        && Boolean.TRUE.equals(ctx.get("orderConfirmed"));
            }
        };
    }
    private static void copyBoolean(Map<String, Object> from, Map<String, Object> to, String key) {
        if (from.get(key) instanceof Boolean value) to.put(key, value);
    }
    private static boolean copyPositiveInteger(Map<String, Object> from, Map<String, Object> to, String key, int max) {
        if (from.get(key) instanceof Integer value && value > 0 && value <= max) {
            boolean changed = !Objects.equals(to.get(key), value); to.put(key, value); return changed;
        }
        return false;
    }
    private LlmTurn callChat(String system, List<LlmMessage> history) {
        try {
            LlmTurn turn = json.readValue(stripCodeFence(llm.completeJson(LlmPurpose.CHAT, system, history)), LlmTurn.class);
            if (turn.npcReply() == null || turn.npcReply().isBlank()) throw new IllegalStateException("npcReply 없음");
            return turn;
        } catch (Exception e) {
            log.warn("LLM 응답 처리 실패", e);
            return new LlmTurn(prompts.fallbackReply(), null, null, List.of(), List.of(), Map.of());
        }
    }
    private static String stripCodeFence(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n'), end = text.lastIndexOf("```");
            if (start >= 0 && end > start) text = text.substring(start + 1, end).trim();
        }
        return text;
    }
}
