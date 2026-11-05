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

import java.util.*;

/**
 * 퀘스트 대화 엔진 (모든 퀘스트 공통).
 *
 * 퀘스트 = 대화 단계(DialogueStep) 목록. 단계마다 플레이어 대사 + NPC 응답.
 * - 플레이어가 말하면(음성 인식 텍스트) AI가 "현재 단계 대사와 같은 의미인지" 판정
 *   (한 문장에 다음 단계 내용까지 들어 있으면 여러 단계 한 번에 통과)
 * - 통과하면 NPC는 명세 대사를 그대로 말함 / 실패하면 AI가 빠진 정보만 다시 묻는 대사
 * - 단계 전에 필요한 화면 이벤트(상품 선택 등), 마지막 이벤트(도착)는 /event 로 받음
 * - 코인 지급, 해금, 완료 판정은 전부 서버
 * mock 모드: 말할 때마다 현재 단계 통과
 */
@Service
public class QuestService {

    private static final Logger log = LoggerFactory.getLogger(QuestService.class);
    private static final int HISTORY_LIMIT = 20;
    private static final int MAX_MESSAGE_LENGTH = 300;
    private static final String FALLBACK_REPLY = "음… 다시 한 번 말해 줄래요?";

    private final Finder finder;
    private final QuestRepository questRepo;
    private final DialogueStepRepository stepRepo;
    private final QuestProgressRepository progressRepo;
    private final ChatMessageRepository messageRepo;
    private final CourseService courseService;
    private final LlmClient llm;
    private final PromptFactory prompts;
    private final ObjectMapper objectMapper;

    public QuestService(Finder finder, QuestRepository questRepo, DialogueStepRepository stepRepo,
                        QuestProgressRepository progressRepo,
                        ChatMessageRepository messageRepo, CourseService courseService, LlmClient llm,
                        PromptFactory prompts, ObjectMapper objectMapper) {
        this.finder = finder;
        this.questRepo = questRepo;
        this.stepRepo = stepRepo;
        this.progressRepo = progressRepo;
        this.messageRepo = messageRepo;
        this.courseService = courseService;
        this.llm = llm;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
    }

    // ================= API =================

    /** 퀘스트 시작 / 이탈 후 재개 (완료 안 된 퀘스트는 처음부터) */
    @Transactional
    public QuestStartResponse start(Long questId, Long userId) {
        AppUser user = finder.user(userId);
        Quest quest = finder.quest(questId);
        checkPlayable(user, quest);
        QuestProgress progress = getOrCreate(user, quest);
        if (progress.isDone()) throw new GameException(HttpStatus.BAD_REQUEST, "이미 완료한 퀘스트예요. (재수행 불가)");

        progress.resetIfNotDone();
        messageRepo.deleteHistory(userId, questId);

        Line opening = null;
        if (notBlank(quest.getOpeningKo())) {
            opening = new Line(quest.getOpeningSpeaker(), quest.getOpeningKo(), quest.getOpeningRoman(), quest.getOpeningEn());
            messageRepo.save(new ChatMessage(user, quest, ChatMessage.Role.NPC, quest.getOpeningKo()));
        }
        List<DialogueStep> steps = stepRepo.findByQuestIdOrderByStepNoAsc(questId);
        DialogueStep first = steps.isEmpty() ? null : steps.get(0);

        return new QuestStartResponse(quest.getId(), quest.getCode(), quest.getName(), quest.getTitle(), quest.getStory(),
                quest.getDestinationKey(), steps.size(), opening, view(first, steps.size()),
                first == null ? null : missingEvent(first, progress), user.getCoins());
    }

    /** 플레이어 발화 (음성 인식 결과 텍스트) */
    @Transactional
    public TurnResponse talk(Long questId, TalkRequest req) {
        if (req == null || req.message() == null || req.message().isBlank()) {
            throw new GameException(HttpStatus.BAD_REQUEST, "message가 비어 있어요. (무음·인식 실패면 현재 단계 유지하고 재시도)");
        }
        String message = req.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) message = message.substring(0, MAX_MESSAGE_LENGTH);

        AppUser user = finder.user(req.userId());
        Quest quest = finder.quest(questId);
        checkPlayable(user, quest);
        QuestProgress progress = getOrCreate(user, quest);
        if (progress.isDone()) throw new GameException(HttpStatus.BAD_REQUEST, "이미 완료한 퀘스트예요.");

        List<DialogueStep> steps = stepRepo.findByQuestIdOrderByStepNoAsc(questId);
        int idx = progress.getPassedSteps();

        // 대화는 끝났고 화면 이벤트(도착 등)만 남은 경우
        if (idx >= steps.size()) {
            return response(user, quest, progress, steps, false, null, null, List.of(), false);
        }

        messageRepo.save(new ChatMessage(user, quest, ChatMessage.Role.USER, message));
        DialogueStep current = steps.get(idx);

        // 단계 전에 필요한 화면 이벤트가 없으면 (예: 상품 선택 전 "얼마예요?") → 다시 묻기
        if (missingEvent(current, progress) != null) {
            String retry = notBlank(current.getRetryKo()) ? current.getRetryKo() : FALLBACK_REPLY;
            messageRepo.save(new ChatMessage(user, quest, ChatMessage.Role.NPC, retry));
            return response(user, quest, progress, steps, false, new Line(current.getSpeaker(), retry, null, null),
                    null, List.of(), false);
        }

        LlmJudgement j = judge(user, quest, steps, idx);
        Set<Integer> passedSet = new HashSet<>(j.passedSteps() == null ? List.of() : j.passedSteps());

        // 서버 검증: 현재 단계부터 연속으로, 필요한 이벤트가 있는 단계까지만
        List<DialogueStep> passed = new ArrayList<>();
        for (int i = idx; i < steps.size(); i++) {
            DialogueStep s = steps.get(i);
            if (!passedSet.contains(s.getStepNo()) || missingEvent(s, progress) != null) break;
            progress.passStep();
            passed.add(s);
        }

        if (passed.isEmpty()) {
            String reply = notBlank(j.npcReply()) ? j.npcReply().trim() : FALLBACK_REPLY;
            messageRepo.save(new ChatMessage(user, quest, ChatMessage.Role.NPC, reply));
            return response(user, quest, progress, steps, false, new Line(current.getSpeaker(), reply, null, null),
                    j.correction(), List.of(), false);
        }

        DialogueStep last = passed.get(passed.size() - 1);
        Line npc = notBlank(last.getNpcKo())
                ? new Line(last.getSpeaker(), last.getNpcKo(), last.getNpcRoman(), last.getNpcEn()) : null;
        if (npc != null) messageRepo.save(new ChatMessage(user, quest, ChatMessage.Role.NPC, npc.ko()));

        List<Effect> effects = new ArrayList<>();
        passed.forEach(s -> s.getEffectList().forEach(e -> effects.add(CourseService.parseEffect(e))));

        return response(user, quest, progress, steps, true, npc, j.correction(), effects, true);
    }

    /** 화면 이벤트 (arrived 도착, selected 상품 선택, selectedFrame 프레임 선택 ...) */
    @Transactional
    public TurnResponse event(Long questId, EventRequest req) {
        if (req == null || req.event() == null || req.event().isBlank()) {
            throw new GameException(HttpStatus.BAD_REQUEST, "event가 비어 있어요.");
        }
        AppUser user = finder.user(req.userId());
        Quest quest = finder.quest(questId);
        checkPlayable(user, quest);
        QuestProgress progress = getOrCreate(user, quest);
        if (progress.isDone()) throw new GameException(HttpStatus.BAD_REQUEST, "이미 완료한 퀘스트예요.");

        List<DialogueStep> steps = stepRepo.findByQuestIdOrderByStepNoAsc(questId);
        boolean known = req.event().equals(quest.getCompleteEvent())
                || steps.stream().anyMatch(s -> req.event().equals(s.getRequiredEvent()));
        if (!known) throw new GameException(HttpStatus.BAD_REQUEST, "이 퀘스트에 없는 이벤트예요: " + req.event());

        progress.addEvent(req.event());
        return response(user, quest, progress, steps, false, null, null, new ArrayList<>(), true);
    }

    /** 대화 기록으로 현장 표현 카드 */
    @Transactional(readOnly = true)
    public PhraseCard phraseCard(Long questId, Long userId) {
        AppUser user = finder.user(userId);
        Quest quest = finder.quest(questId);
        StringBuilder transcript = new StringBuilder();
        for (ChatMessage m : messageRepo.findByUserIdAndQuestIdOrderByIdAsc(userId, questId)) {
            transcript.append(m.getRole() == ChatMessage.Role.USER ? "학습자: " : "NPC: ").append(m.getContent()).append("\n");
        }
        if (transcript.isEmpty()) transcript.append("(대화 없음. 이 장소에서 기본적으로 쓸 표현을 골라 줘.)");
        String raw = llm.completeJson(LlmPurpose.PHRASE_CARD, prompts.phraseCardSystemPrompt(user, quest),
                List.of(new LlmMessage("user", transcript.toString())));
        try {
            return objectMapper.readValue(stripCodeFence(raw), PhraseCard.class);
        } catch (Exception e) {
            log.warn("표현 카드 파싱 실패: {}", raw, e);
            throw new GameException(HttpStatus.BAD_GATEWAY, "표현 카드를 만들지 못했어요. 다시 시도해 주세요.");
        }
    }

    // ================= 완료 판정 + 응답 =================

    private TurnResponse response(AppUser user, Quest quest, QuestProgress progress, List<DialogueStep> steps,
                                  boolean passed, Line npc, Correction correction, List<Effect> effects,
                                  boolean checkComplete) {
        List<Effect> fx = new ArrayList<>(effects);
        int passedSteps = progress.getPassedSteps();
        boolean dialogueDone = passedSteps >= steps.size();
        String waiting = null;

        boolean completedNow = false;
        int coinsEarned = 0;
        Long nextQuestId = null;
        boolean placeCompleted = false;
        boolean courseCompleted = false;
        String ending = null;

        if (dialogueDone) {
            String ce = quest.getCompleteEvent();
            if (notBlank(ce) && !progress.getEventSet().contains(ce)) {
                waiting = ce;
            } else if (checkComplete && progress.markDone()) {
                completedNow = true;
            }
        } else {
            waiting = missingEvent(steps.get(passedSteps), progress);
        }

        List<Quest> placeQuests = questRepo.findByPlaceIdOrderByOrderNoAsc(quest.getPlace().getId());
        if (completedNow) {
            quest.getCompleteEffectList().forEach(e -> fx.add(CourseService.parseEffect(e)));
            coinsEarned = quest.getRewardCoin();
            user.addCoins(coinsEarned);

            nextQuestId = placeQuests.stream().filter(q -> q.getOrderNo() > quest.getOrderNo())
                    .min(Comparator.comparingInt(Quest::getOrderNo)).map(Quest::getId).orElse(null);

            Map<Long, QuestProgress> pp = courseService.progressByQuest(user.getId(), quest.getPlace().getId());
            placeCompleted = placeQuests.stream().allMatch(q -> pp.containsKey(q.getId()) && pp.get(q.getId()).isDone());
            if (placeCompleted) ending = quest.getPlace().getEndingText();

            Long courseId = quest.getPlace().getCourse().getId();
            UserCourse uc = finder.drawnCourse(user.getId(), courseId);
            List<Quest> courseQuests = questRepo.findByPlaceCourseId(courseId);
            long courseDone = progressRepo.findByUserIdAndQuestPlaceCourseId(user.getId(), courseId).stream()
                    .filter(QuestProgress::isDone).count();
            if (!uc.isCompleted() && !courseQuests.isEmpty() && courseDone >= courseQuests.size()) {
                uc.complete();
                courseCompleted = true;
            }
        }

        long placeDone = courseService.progressByQuest(user.getId(), quest.getPlace().getId()).values().stream()
                .filter(QuestProgress::isDone).count();
        DialogueStep next = dialogueDone ? null : steps.get(passedSteps);

        return new TurnResponse(passed, npc, correction, view(next, steps.size()),
                Math.min(passedSteps, steps.size()) + "/" + steps.size(),
                completedNow ? null : waiting, fx, completedNow, coinsEarned, user.getCoins(), nextQuestId,
                placeDone + "/" + placeQuests.size(), placeCompleted, ending, courseCompleted);
    }

    private LlmJudgement judge(AppUser user, Quest quest, List<DialogueStep> steps, int idx) {
        if (llm.isMock()) {
            return new LlmJudgement(List.of(steps.get(idx).getStepNo()), null, null);
        }
        String raw = null;
        try {
            String system = prompts.stepJudgePrompt(user, quest, steps, idx);
            raw = llm.completeJson(LlmPurpose.CHAT, system, history(user.getId(), quest.getId()));
            return objectMapper.readValue(stripCodeFence(raw), LlmJudgement.class);
        } catch (Exception e) {
            log.warn("AI 판정 실패. raw={}", raw, e);
            return new LlmJudgement(List.of(), FALLBACK_REPLY, null);
        }
    }

    // ================= 보조 =================

    /** 코스를 뽑았는지, 장소·퀘스트가 열렸는지 */
    private void checkPlayable(AppUser user, Quest quest) {
        Long courseId = quest.getPlace().getCourse().getId();
        finder.drawnCourse(user.getId(), courseId);
        boolean placeOpen = courseService.placeCards(user.getId(), courseId).stream()
                .anyMatch(c -> c.id().equals(quest.getPlace().getId())
                        && ("AVAILABLE".equals(c.status()) || "DONE".equals(c.status())));
        if (!placeOpen) throw new GameException(HttpStatus.FORBIDDEN, "이전 장소를 먼저 완료해야 해요.");
        List<Quest> placeQuests = questRepo.findByPlaceIdOrderByOrderNoAsc(quest.getPlace().getId());
        Map<Long, QuestProgress> progress = courseService.progressByQuest(user.getId(), quest.getPlace().getId());
        if ("LOCKED".equals(CourseService.questStatus(quest, placeQuests, progress))) {
            throw new GameException(HttpStatus.FORBIDDEN, "이전 퀘스트를 먼저 완료해야 해요.");
        }
    }

    private QuestProgress getOrCreate(AppUser user, Quest quest) {
        return progressRepo.findByUserIdAndQuestId(user.getId(), quest.getId())
                .orElseGet(() -> progressRepo.save(new QuestProgress(user, quest)));
    }

    private static String missingEvent(DialogueStep s, QuestProgress p) {
        String ev = s.getRequiredEvent();
        return (notBlank(ev) && !p.getEventSet().contains(ev)) ? ev : null;
    }

    private static StepView view(DialogueStep s, int total) {
        if (s == null) return null;
        return new StepView(s.getStepNo(), total, s.getTarget(),
                new Line("나", s.getUserKo(), s.getUserRoman(), s.getUserEn()));
    }

    private List<LlmMessage> history(Long userId, Long questId) {
        List<ChatMessage> all = messageRepo.findByUserIdAndQuestIdOrderByIdAsc(userId, questId);
        List<ChatMessage> recent = all.size() > HISTORY_LIMIT ? all.subList(all.size() - HISTORY_LIMIT, all.size()) : all;
        return recent.stream()
                .map(m -> new LlmMessage(m.getRole() == ChatMessage.Role.USER ? "user" : "assistant", m.getContent()))
                .toList();
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

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
