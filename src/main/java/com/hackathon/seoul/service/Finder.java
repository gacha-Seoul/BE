package com.hackathon.seoul.service;

import com.hackathon.seoul.common.GameException;
import com.hackathon.seoul.domain.*;
import com.hackathon.seoul.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 자주 쓰는 조회 + 404/403 처리 */
@Component
public class Finder {
    private final AppUserRepository userRepo;
    private final RegionRepository regionRepo;
    private final StageRepository stageRepo;
    private final UserRegionRepository userRegionRepo;

    public Finder(AppUserRepository userRepo, RegionRepository regionRepo,
                  StageRepository stageRepo, UserRegionRepository userRegionRepo) {
        this.userRepo = userRepo;
        this.regionRepo = regionRepo;
        this.stageRepo = stageRepo;
        this.userRegionRepo = userRegionRepo;
    }

    public AppUser user(Long id) {
        if (id == null) throw new GameException(HttpStatus.BAD_REQUEST, "userId가 필요해요.");
        return userRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "유저가 없어요: " + id));
    }

    public AppUser lockedUser(Long id) {
        if (id == null) throw new GameException(HttpStatus.BAD_REQUEST, "userId가 필요해요.");
        return userRepo.findLockedById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "유저가 없어요: " + id));
    }

    public Region region(Long id) {
        return regionRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "지역이 없어요: " + id));
    }

    public Stage stage(Long id) {
        return stageRepo.findById(id)
                .orElseThrow(() -> new GameException(HttpStatus.NOT_FOUND, "스테이지가 없어요: " + id));
    }

    /** 가챠로 해금하지 않은 지역의 스테이지는 플레이 불가 */
    public UserRegion unlockedRegion(Long userId, Long regionId) {
        return userRegionRepo.findByUserIdAndRegionId(userId, regionId)
                .orElseThrow(() -> new GameException(HttpStatus.FORBIDDEN, "아직 가챠로 뽑지 않은 지역이에요."));
    }
}
