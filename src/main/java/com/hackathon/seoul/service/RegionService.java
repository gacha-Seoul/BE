package com.hackathon.seoul.service;

import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.dto.Dtos.*;
import com.hackathon.seoul.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RegionService {
    private final Finder finder;
    private final RegionRepository regions;
    private final StageRepository stages;
    private final UserRegionRepository owned;
    private final QuestRepository quests;
    private final QuestProgressRepository progress;
    private final GameProperties props;
    public RegionService(Finder finder, RegionRepository regions, StageRepository stages,
                         UserRegionRepository owned, QuestRepository quests,
                         QuestProgressRepository progress, GameProperties props) {
        this.finder = finder; this.regions = regions; this.stages = stages; this.owned = owned;
        this.quests = quests; this.progress = progress; this.props = props;
    }
    public MapResponse map(Long userId) {
        AppUser user = finder.user(userId);
        Map<Long, UserRegion> mine = owned.findByUserId(userId).stream()
                .collect(Collectors.toMap(x -> x.getRegion().getId(), Function.identity()));
        List<MapRegion> result = regions.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(r -> {
            List<StageSummary> summaries = stageSummaries(userId, r.getId());
            int cleared = (int) summaries.stream().filter(StageSummary::cleared).count();
            return new MapRegion(r.getId(), r.getCode(), r.getName(), r.getNameEn(), status(mine.get(r.getId())),
                    summaries.stream().anyMatch(x -> x.totalQuests() > 0), cleared, summaries.size());
        }).toList();
        return new MapResponse(userId, user.getCoins(), props.gachaCost,
                (int) mine.values().stream().filter(UserRegion::isConquered).count(), result);
    }
    public RegionDetail detail(Long userId, Long regionId) {
        finder.user(userId); return buildDetail(userId, finder.region(regionId));
    }
    RegionDetail buildDetail(Long userId, Region region) {
        return new RegionDetail(region.getId(), region.getCode(), region.getName(), region.getNameEn(),
                region.getDescription(), status(owned.findByUserIdAndRegionId(userId, region.getId()).orElse(null)),
                stageSummaries(userId, region.getId()));
    }
    private static String status(UserRegion ur) {
        return ur == null ? "LOCKED" : ur.isConquered() ? "CONQUERED" : "UNLOCKED";
    }
    private List<StageSummary> stageSummaries(Long userId, Long regionId) {
        return stages.findByRegionIdOrderByOrderNoAsc(regionId).stream().map(x -> summary(userId, x)).toList();
    }
    public StageSummary stage(Long userId, Long stageId) {
        finder.user(userId); return summary(userId, finder.stage(stageId));
    }
    private StageSummary summary(Long userId, Stage stage) {
        Map<Long, QuestProgress> states = progress.findByUserIdAndQuestStageId(userId, stage.getId()).stream()
                .collect(Collectors.toMap(x -> x.getQuest().getId(), Function.identity()));
        boolean available = owned.findByUserIdAndRegionId(userId, stage.getRegion().getId()).isPresent();
        List<QuestSummary> result = new ArrayList<>();
        for (Quest q : quests.findByStageIdOrderByOrderNoAsc(stage.getId())) {
            QuestProgress state = states.get(q.getId());
            boolean done = state != null && state.isCompleted();
            String status = done ? "COMPLETED" : !available ? "LOCKED" : state == null ? "AVAILABLE" : "IN_PROGRESS";
            result.add(new QuestSummary(q.getId(), q.getCode(), q.getTitle(), q.getOrderNo(), q.getNpcRole(),
                    q.getSituation(), q.getRewardCoin(), status, missions(q, state)));
            available = available && done;
        }
        int completed = (int) result.stream().filter(x -> "COMPLETED".equals(x.status())).count();
        return new StageSummary(stage.getId(), stage.getCategory().name(), stage.getCategory().getLabel(),
                stage.getOrderNo(), stage.getPlace(), stage.getIntroStory(), stage.getImageUrl(),
                !result.isEmpty() && completed == result.size(), completed, result.size(), result);
    }
    static List<MissionStatus> missions(Quest q, QuestProgress p) {
        Set<Integer> done = new HashSet<>();
        if (p != null) {
            try {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(p.getContextJson()).path("completedMissions");
                node.forEach(x -> { if (x.isIntegralNumber()) done.add(x.intValue()); });
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("퀘스트 상태 JSON 오류", e);
            }
        }
        List<MissionStatus> result = new ArrayList<>();
        List<String> text = q.getMissionList();
        for (int i = 0; i < text.size(); i++) result.add(new MissionStatus(i, text.get(i), p != null && p.isCompleted() || done.contains(i)));
        return result;
    }
}
