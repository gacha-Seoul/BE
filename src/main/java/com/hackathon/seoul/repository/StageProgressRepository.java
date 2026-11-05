package com.hackathon.seoul.repository;

import com.hackathon.seoul.domain.StageProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageProgressRepository extends JpaRepository<StageProgress, Long> {
    Optional<StageProgress> findByUserIdAndStageId(Long userId, Long stageId);
    List<StageProgress> findByUserIdAndStageRegionId(Long userId, Long regionId);
    List<StageProgress> findByUserId(Long userId);
}
