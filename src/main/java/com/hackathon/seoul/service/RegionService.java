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
    private final RegionRepository regionRepo;
    private final StageRepository stageRepo;
    private final UserRegionRepository userRegionRepo;
    private final StageProgressRepository progressRepo;
    private final GameProperties props;

    public RegionService(Finder finder, RegionRepository regionRepo, StageRepository stageRepo,
                         UserRegionRepository userRegionRepo, StageProgressRepository progressRepo,
                         GameProperties props) {
        this.finder = finder;
        this.regionRepo = regionRepo;
        this.stageRepo = stageRepo;
        this.userRegionRepo = userRegionRepo;
        this.progressRepo = progressRepo;
        this.props = props;
    }

    /** 3D 지도용: 서울 25개 구 전체 + 내 상태 */
    public MapResponse map(Long userId) {
        AppUser user = finder.user(userId);
        Map<Long, UserRegion> mine = userRegionRepo.findByUserId(userId).stream()
                .collect(Collectors.toMap(ur -> ur.getRegion().getId(), Function.identity()));
        Map<Long, Long> clearedByRegion = progressRepo.findByUserId(userId).stream()
                .filter(StageProgress::isCleared)
                .collect(Collectors.groupingBy(p -> p.getStage().getRegion().getId(), Collectors.counting()));

        List<MapRegion> regions = regionRepo.findAll(org.springframework.data.domain.Sort.by("id")).stream()
                .map(r -> {
                    UserRegion ur = mine.get(r.getId());
                    String status = ur == null ? "LOCKED" : (ur.isConquered() ? "CONQUERED" : "UNLOCKED");
                    int total = (int) stageRepo.countByRegionId(r.getId());
                    int cleared = clearedByRegion.getOrDefault(r.getId(), 0L).intValue();
                    return new MapRegion(r.getId(), r.getCode(), r.getName(), r.getNameEn(),
                            status, total > 0, cleared, total);
                }).toList();

        int conquered = (int) mine.values().stream().filter(UserRegion::isConquered).count();
        return new MapResponse(userId, user.getCoins(), props.gachaCost, conquered, regions);
    }

    public RegionDetail detail(Long userId, Long regionId) {
        finder.user(userId);
        Region region = finder.region(regionId);
        return buildDetail(userId, region);
    }

    RegionDetail buildDetail(Long userId, Region region) {
        Optional<UserRegion> ur = userRegionRepo.findByUserIdAndRegionId(userId, region.getId());
        String status = ur.isEmpty() ? "LOCKED" : (ur.get().isConquered() ? "CONQUERED" : "UNLOCKED");

        Map<Long, StageProgress> progress = progressRepo.findByUserIdAndStageRegionId(userId, region.getId())
                .stream().collect(Collectors.toMap(p -> p.getStage().getId(), Function.identity()));

        List<StageSummary> stages = stageRepo.findByRegionIdOrderByOrderNoAsc(region.getId()).stream()
                .map(s -> toSummary(s, progress.get(s.getId())))
                .toList();

        return new RegionDetail(region.getId(), region.getCode(), region.getName(), region.getNameEn(),
                region.getDescription(), status, stages);
    }

    public StageSummary stage(Long userId, Long stageId) {
        finder.user(userId);
        Stage stage = finder.stage(stageId);
        return toSummary(stage, progressRepo.findByUserIdAndStageId(userId, stageId).orElse(null));
    }

    static StageSummary toSummary(Stage s, StageProgress p) {
        return new StageSummary(s.getId(), s.getCategory().name(), s.getCategory().getLabel(), s.getOrderNo(),
                s.getPlace(), s.getNpcRole(), s.getSituation(), s.getRewardCoin(),
                p != null && p.isCleared(), missions(s, p));
    }

    static List<MissionStatus> missions(Stage s, StageProgress p) {
        Set<Integer> done = p == null ? Set.of() : p.getCompletedSet();
        List<String> list = s.getMissionList();
        List<MissionStatus> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new MissionStatus(i, list.get(i), done.contains(i)));
        }
        return result;
    }
}
